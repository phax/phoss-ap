/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.ap.core.outbound;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.WillNotClose;
import com.helger.base.io.stream.CountingInputStream;
import com.helger.base.io.stream.HasInputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringHex;
import com.helger.base.timing.StopWatch;
import com.helger.base.wrapper.Wrapper;
import com.helger.io.file.FileOperationManager;
import com.helger.mime.CMimeType;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderConstant;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderPeppol;
import com.helger.phase4.dynamicdiscovery.Phase4SMPException;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageBuilder;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageSBDHBuilder;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.util.Phase4Exception;
import com.helger.phoss.ap.api.IOutboundSendingAttemptManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.api.spi.IOutboundDocumentVerifierSPI;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.basic.storage.DocumentStorageHelper;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.CircuitBreakerManager;
import com.helger.phoss.ap.core.helper.BackoffCalculator;
import com.helger.phoss.ap.core.helper.CopyingInputStream;
import com.helger.phoss.ap.core.helper.HashHelper;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.smpclient.peppol.CachingSMPClientReadOnly;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.smpclient.url.PeppolNaptrURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;

/**
 * Main class to handle outbound transactions.
 *
 * @author Philip Helger
 */
public final class OutboundOrchestrator
{
  private static final Logger LOGGER = LoggerFactory.getLogger (OutboundOrchestrator.class);

  private OutboundOrchestrator ()
  {}

  @Nullable
  public static IOutboundTransaction submitRawDocument (@NonNull final String sLogPrefix,
                                                        @NonNull final IParticipantIdentifier aSenderID,
                                                        @NonNull final IParticipantIdentifier aReceiverID,
                                                        @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                        @NonNull final IProcessIdentifier aProcessID,
                                                        @NonNull final String sSbdhInstanceID,
                                                        @NonNull final String sC1CountryCode,
                                                        @NonNull @WillNotClose final InputStream aDocumentIS,
                                                        @Nullable final String sMlsTo,
                                                        @Nullable final String sSbdhStandard,
                                                        @Nullable final String sSbdhTypeVersion,
                                                        @Nullable final String sSbdhType,
                                                        @Nullable final String sPayloadMimeType)
  {
    LOGGER.info (sLogPrefix + "Submitting raw document with SBDH Instance ID '" + sSbdhInstanceID + "'");

    final File aStorageBasePath = new File (APBasicConfig.getStorageOutboundPath ());
    final OffsetDateTime aAS4SendingDT = APBasicMetaManager.getTimestampMgr ().getCurrentDateTime ();
    final Wrapper <File> aTempFileHolder = Wrapper.empty ();

    long nDocumentBytes = -1;
    final MessageDigest aMD = HashHelper.MD_ALGO.createMessageDigest ();
    // 1. Count size
    // 2. Create message digest
    // 3. Copy to a temporary file
    // 4. Parse the SBDH
    try (final CountingInputStream aCountingIS = new CountingInputStream (aDocumentIS);
         final DigestInputStream aDigestIS = new DigestInputStream (aCountingIS, aMD);
         final OutputStream aFileOS = DocumentStorageHelper.openDocumentStream (aStorageBasePath,
                                                                                aAS4SendingDT,
                                                                                sSbdhInstanceID + ".out",
                                                                                aTempFileHolder::set))
    {
      if (StreamHelper.copyByteStream ()
                      .from (aDigestIS)
                      .closeFrom (false)
                      .to (aFileOS)
                      .closeTo (false)
                      .build ()
                      .isFailure ())
      {
        LOGGER.error (sLogPrefix + "Failed to store incoming document to disk");

        // No need to keep the temporary file
        StreamHelper.close (aFileOS);
        if (aTempFileHolder.isSet ())
          FileOperationManager.INSTANCE.deleteFileIfExisting (aTempFileHolder.get ());
        return null;
      }
      nDocumentBytes = aCountingIS.getBytesRead ();
    }
    catch (final Exception ex)
    {
      LOGGER.error (sLogPrefix + "Failed to process document to submit", ex);

      // No need to keep the temporary file
      if (aTempFileHolder.isSet ())
        FileOperationManager.INSTANCE.deleteFileIfExisting (aTempFileHolder.get ());
      return null;
    }

    final String sDocumentHash = StringHex.getHexEncoded (aMD.digest ());
    final File aDocumentFile = aTempFileHolder.get ().getAbsoluteFile ();
    final String sDocumentPath = aDocumentFile.toString ();

    // Optional verification
    if (APCoreConfig.isVerificationOutboundEnabled ())
    {
      for (final IOutboundDocumentVerifierSPI aVerifier : APCoreMetaManager.getAllOutboundVerifiers ())
        if (aVerifier.verifyDocument (aDocumentFile, aDocTypeID, aProcessID).isFailure ())
        {
          LOGGER.warn (sLogPrefix + "Outbound document verification failed for SBDH '" + sSbdhInstanceID + "'");
          return null;
        }
    }

    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();

    // Create in pending state
    final String sTransactionID = aMgr.create (ETransactionType.BUSINESS_DOCUMENT,
                                               aSenderID.getURIEncoded (),
                                               aReceiverID.getURIEncoded (),
                                               aDocTypeID.getURIEncoded (),
                                               aProcessID.getURIEncoded (),
                                               sSbdhInstanceID,
                                               ESourceType.PAYLOAD_ONLY,
                                               sDocumentPath,
                                               nDocumentBytes,
                                               sDocumentHash,
                                               sC1CountryCode,
                                               aAS4SendingDT,
                                               sMlsTo,
                                               (String) null,
                                               sSbdhStandard,
                                               sSbdhTypeVersion,
                                               sSbdhType,
                                               sPayloadMimeType);
    return aMgr.getByID (sTransactionID);
  }

  @Nullable
  public static IOutboundTransaction submitPrebuiltSBD (@NonNull final String sLogPrefix,
                                                        @NonNull final InputStream aSbdIS,
                                                        @Nullable final String sMlsTo)
  {
    LOGGER.info (sLogPrefix + "Submitting pre-built SBD");

    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();

    final File aStorageBasePath = new File (APBasicConfig.getStorageOutboundPath ());
    final OffsetDateTime aAS4SendingDT = APBasicMetaManager.getTimestampMgr ().getCurrentDateTime ();
    final Wrapper <File> aTempFileHolder = Wrapper.empty ();

    final PeppolSBDHData aSbdData;
    long nSbdByteCount = -1;
    final MessageDigest aMD = HashHelper.createMessageDigest ();
    // 1. Count size
    // 2. Create message digest
    // 3. Copy SBDH to a temporary file - the final name can only be deduced after reading the SBDH
    // as it contains the InstanceIdentifier
    // 4. Parse the SBDH
    try (final CountingInputStream aCountingIS = new CountingInputStream (aSbdIS);
         final DigestInputStream aDigestIS = new DigestInputStream (aCountingIS, aMD);
         final OutputStream aFileOS = DocumentStorageHelper.openTemporaryDocumentStream (aStorageBasePath,
                                                                                         aAS4SendingDT,
                                                                                         aTempFileHolder::set);
         final CopyingInputStream aCopyIS = new CopyingInputStream (aDigestIS, aFileOS))
    {
      aSbdData = new PeppolSBDHDataReader (aIF).extractData (aCopyIS);
      nSbdByteCount = aCountingIS.getBytesRead ();
    }
    catch (final Exception ex)
    {
      LOGGER.error (sLogPrefix + "Failed to parse provided SBDH", ex);
      // No need to keep the temporary file
      if (aTempFileHolder.isSet ())
        DocumentStorageHelper.deleteDocument (aTempFileHolder.get ().toString ());
      return null;
    }

    // Get Document hash in the correct version
    final String sDocumentHash = HashHelper.getDigestHex (aMD);

    final String sSbdhInstanceID = aSbdData.getInstanceIdentifier ();
    LOGGER.info (sLogPrefix + "Found SBDH Instance ID '" + sSbdhInstanceID + "'");

    final String sDocumentPath;
    {
      // Rename temp file to final name
      final File aTempFile = aTempFileHolder.get ();
      final File aDstFile = new File (aTempFile.getParentFile (), sSbdhInstanceID + ".sbd");
      FileOperationManager.INSTANCE.renameFile (aTempFile, aDstFile);
      sDocumentPath = aDstFile.getAbsolutePath ().toString ();
    }

    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();

    // Create in pending state
    final String sTransactionID = aMgr.create (ETransactionType.BUSINESS_DOCUMENT,
                                               aSbdData.getSenderURIEncoded (),
                                               aSbdData.getReceiverURIEncoded (),
                                               aSbdData.getDocumentTypeURIEncoded (),
                                               aSbdData.getProcessURIEncoded (),
                                               sSbdhInstanceID,
                                               ESourceType.PREBUILT_SBD,
                                               sDocumentPath,
                                               nSbdByteCount,
                                               sDocumentHash,
                                               aSbdData.getCountryC1 (),
                                               aAS4SendingDT,
                                               sMlsTo,
                                               (String) null,
                                               (String) null,
                                               (String) null,
                                               (String) null,
                                               (String) null);
    return aMgr.getByID (sTransactionID);
  }

  @NonNull
  public static Phase4PeppolSendingReport processPendingOutbound (@NonNull final String sLogPrefix,
                                                                  @NonNull final IOutboundTransaction aTx)
  {
    final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundSendingAttemptManager aAttemptMgr = APJdbcMetaManager.getOutboundSendingAttemptMgr ();

    final String sTxID = aTx.getID ();
    final EPeppolNetwork eStage = APCoreConfig.getPeppolStage ();
    final ISMLInfo aSMLInfo = eStage.getSMLInfo ();
    final String sC2SeatID = APCoreConfig.getPeppolSeatID ();
    final StopWatch aOverallSW = StopWatch.createdStarted ();

    final Phase4PeppolSendingReport aSendingReport = new Phase4PeppolSendingReport (aSMLInfo);

    LOGGER.info (sLogPrefix + "Processing outbound transaction '" + sTxID + "'");

    try
    {
      final int nNewAttemptCount = aTx.getAttemptCount () + 1;
      final String sAS4MessageID = MessageHelperMethods.createRandomMessageID ();
      final OffsetDateTime aAS4Timestamp = aTimestampMgr.getCurrentDateTimeUTC ();

      // Callback on recoverable error
      final Consumer <String> onFailed = sErrMsg -> {
        aAttemptMgr.create (sTxID, sAS4MessageID, aAS4Timestamp, null, null, EAttemptStatus.FAILED, sErrMsg);
        final OffsetDateTime aNextRetry = BackoffCalculator.calculateNextRetry (nNewAttemptCount,
                                                                                APCoreConfig.getRetrySendingInitialBackoffMs (),
                                                                                APCoreConfig.getRetrySendingBackoffMultiplier (),
                                                                                APCoreConfig.getRetrySendingMaxBackoffMs ());
        aTxMgr.updateStatusAndRetry (sTxID, EOutboundStatus.FAILED, nNewAttemptCount, aNextRetry, sErrMsg);
      };

      // Callback on permanent failure
      final Consumer <String> onPermanentFailure = sErrMsg -> {
        aAttemptMgr.create (sTxID, sAS4MessageID, aAS4Timestamp, null, null, EAttemptStatus.FAILED, sErrMsg);
        aTxMgr.updateStatusAndRetry (sTxID, EOutboundStatus.PERMANENTLY_FAILED, nNewAttemptCount, null, sErrMsg);

        // Notify
        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onPermanentSendingFailure (sTxID, aTx.getSbdhInstanceID (), sErrMsg);
      };

      // Convert all identifiers to structured data - that should have been verified before
      final IParticipantIdentifier aSenderID = aIF.parseParticipantIdentifier (aTx.getSenderID ());
      if (aSenderID == null)
        throw new IllegalStateException ("Failed to parse sender participant identifier '" + aTx.getSenderID () + "'");
      aSendingReport.setSenderID (aSenderID);

      final IParticipantIdentifier aReceiverID = aIF.parseParticipantIdentifier (aTx.getReceiverID ());
      if (aReceiverID == null)
        throw new IllegalStateException ("Failed to parse receiver participant identifier '" +
                                         aTx.getReceiverID () +
                                         "'");
      aSendingReport.setReceiverID (aReceiverID);

      final IDocumentTypeIdentifier aDocTypeID = aIF.parseDocumentTypeIdentifier (aTx.getDocTypeID ());
      if (aDocTypeID == null)
        throw new IllegalStateException ("Failed to parse document type identifier '" + aTx.getDocTypeID () + "'");
      aSendingReport.setDocTypeID (aDocTypeID);

      final IProcessIdentifier aProcessID = aIF.parseProcessIdentifier (aTx.getProcessID ());
      if (aProcessID == null)
        throw new IllegalStateException ("Failed to parse process identifier '" + aTx.getProcessID () + "'");
      aSendingReport.setProcessID (aProcessID);

      // Avoid message is taken by another thread
      aTxMgr.updateStatus (sTxID, EOutboundStatus.SENDING);

      // SMP lookup to find endpoint URL
      // Try to resolve SMP host - performs NAPTR lookup
      final StopWatch aLookupSW = StopWatch.createdStarted ();
      final SMPClientReadOnly aSMPClient;
      try
      {
        aSMPClient = new CachingSMPClientReadOnly (PeppolNaptrURLProvider.INSTANCE, aReceiverID, aSMLInfo);
        APBasicConfig.applyHttpProxySettings (aSMPClient.httpClientSettings ());

        // Remember the host URL from NAPTR lookup
        aSendingReport.setC3SMPURL (aSMPClient.getSMPHostURI ());
      }
      catch (final SMPDNSResolutionException ex)
      {
        final String sMsg = "The participant ID '" + aTx.getReceiverID () + "' is not registered in the Peppol Network";
        aSendingReport.setLookupError (sMsg);
        aSendingReport.setLookupException (ex);

        // Remember duration
        aLookupSW.stop ();
        aSendingReport.setLookupDurationMillis (aLookupSW.getMillis ());

        onPermanentFailure.accept (sMsg + ". Technical details: " + ex.getMessage ());

        return aSendingReport;
      }

      // Perform SMP lookup
      final X509Certificate aReceiverCert;
      final String sReceiverAPURL;
      String sReceiverTechnicalContact;
      final String sCircuitBreakerKeySMP = "smp$" + aSMPClient.getSMPHostURI ();
      if (!CircuitBreakerManager.tryAcquirePermit (sCircuitBreakerKeySMP))
      {
        aLookupSW.stop ();
        aSendingReport.setLookupError ("SMP access limited by Circuit Breaker");
        aSendingReport.setLookupDurationMillis (aLookupSW.getMillis ());

        onFailed.accept ("SMP access limited by Circuit Breaker '" + sCircuitBreakerKeySMP + "'");
        return aSendingReport;
      }
      final AS4EndpointDetailProviderPeppol aEndpointDetails = AS4EndpointDetailProviderPeppol.create (aSMPClient);
      try
      {
        // Throws an exception in case of error
        aEndpointDetails.init (aDocTypeID, aProcessID, aReceiverID);
        aLookupSW.stop ();
        aReceiverCert = aEndpointDetails.getReceiverAPCertificate ();
        sReceiverAPURL = aEndpointDetails.getReceiverAPEndpointURL ();
        sReceiverTechnicalContact = aEndpointDetails.getReceiverTechnicalContact ();

        // Updated sending report
        aSendingReport.setC3Cert (aReceiverCert);
        aSendingReport.setC3EndpointURL (sReceiverAPURL);
        aSendingReport.setC3TechnicalContact (sReceiverTechnicalContact);
        aSendingReport.setLookupDurationMillis (aLookupSW.getMillis ());

        CircuitBreakerManager.recordSuccess (sCircuitBreakerKeySMP);
      }
      catch (final Phase4Exception ex)
      {
        CircuitBreakerManager.recordFailure (sCircuitBreakerKeySMP);

        aLookupSW.stop ();
        if (ex instanceof Phase4SMPException)
        {
          aSendingReport.setLookupError (ex.getMessage ());
          aSendingReport.setLookupException ((Exception) ex.getCause ());
        }
        else
        {
          aSendingReport.setLookupError ("Error fetching Service Details from SMP");
          aSendingReport.setLookupException (ex);
        }
        aSendingReport.setLookupDurationMillis (aLookupSW.getMillis ());

        if (ex.isRetryFeasible ())
          onFailed.accept (ex.getMessage ());
        else
          onPermanentFailure.accept (ex.getMessage ());
        return aSendingReport;
      }

      final StopWatch aSendingSW = StopWatch.createdStarted ();
      final String sCircuitBreakerKeyAP = "ap$" + sReceiverAPURL;
      if (CircuitBreakerManager.tryAcquirePermit (sCircuitBreakerKeyAP))
      {
        // Only add it here to the sending report, otherwise the interpretation of the report gets
        // more difficult
        aSendingReport.setSBDHInstanceIdentifier (aTx.getSbdhInstanceID ());
        aSendingReport.setCountryC1 (aTx.getC1CountryCode ());
        aSendingReport.setSenderPartyID (sC2SeatID);
        aSendingReport.setAS4MessageID (sAS4MessageID);
        aSendingReport.setAS4SendingDT (aAS4Timestamp);

        final String sAS4ConversationID = MessageHelperMethods.createRandomConversationID ();
        aSendingReport.setAS4ConversationID (sAS4ConversationID);

        final TrustedCAChecker aAPCA = eStage.isProduction () ? PeppolTrustedCA.peppolProductionAP ()
                                                              : PeppolTrustedCA.peppolTestAP ();

        EAS4UserMessageSendResult eResult = null;
        boolean bExceptionCaught = false;
        try
        {
          // Actual sending using Phase4PeppolSender
          final Phase4PeppolHttpClientSettings aHCS = new Phase4PeppolHttpClientSettings ();
          APBasicConfig.applyHttpProxySettings (aHCS);

          final Wrapper <Phase4Exception> aCaughtSendingEx = new Wrapper <> ();
          switch (aTx.getSourceType ())
          {
            case PAYLOAD_ONLY:
            {
              final PeppolUserMessageBuilder aBuilder = Phase4PeppolSender.builder ()
                                                                          .httpClientFactory (aHCS)
                                                                          // AS4 input
                                                                          .messageID (sAS4MessageID)
                                                                          .conversationID (sAS4ConversationID)
                                                                          .sendingDateTime (aAS4Timestamp)
                                                                          // Peppol IDs
                                                                          .senderParticipantID (aSenderID)
                                                                          .receiverParticipantID (aReceiverID)
                                                                          .documentTypeID (aDocTypeID)
                                                                          .processID (aProcessID)
                                                                          .countryC1 (aTx.getC1CountryCode ())
                                                                          .senderPartyID (sC2SeatID)
                                                                          .sbdhInstanceIdentifier (aTx.getSbdhInstanceID ())
                                                                          // Certificate stuff
                                                                          .peppolAP_CAChecker (aAPCA)
                                                                          .endpointDetailProvider (new AS4EndpointDetailProviderConstant (aReceiverCert,
                                                                                                                                          sReceiverAPURL,
                                                                                                                                          sReceiverTechnicalContact))
                                                                          .certificateConsumer ( (aAPCertificate,
                                                                                                  aCheckDT,
                                                                                                  eCertCheckResult) -> {
                                                                            // Take specifically the
                                                                            // AP certificate
                                                                            // verification
                                                                            aSendingReport.setC3CertCheckDT (aCheckDT);
                                                                            aSendingReport.setC3CertCheckResult (eCertCheckResult);
                                                                          })
                                                                          // Response stuff
                                                                          .rawResponseConsumer (aSendingReport::setRawHttpResponse)
                                                                          .signalMsgConsumer ( (aSignalMsg,
                                                                                                aMessageMetadata,
                                                                                                aState) -> {
                                                                            aSendingReport.setAS4ReceivedSignalMsg (aSignalMsg);
                                                                          })
                                                                          .disableValidation ();

              // Add the optional SBDH parameters required for e.g. PDF sending
              if (StringHelper.isNotEmpty (aTx.getSbdhStandard ()))
                aBuilder.sbdhStandard (aTx.getSbdhStandard ());
              if (StringHelper.isNotEmpty (aTx.getSbdhTypeVersion ()))
                aBuilder.sbdhTypeVersion (aTx.getSbdhTypeVersion ());
              if (StringHelper.isNotEmpty (aTx.getSbdhType ()))
                aBuilder.sbdhType (aTx.getSbdhType ());

              // Set the main payload
              final String sPayloadMimeType = aTx.getPayloadMimeType ();
              if (CMimeType.APPLICATION_PDF.getAsStringWithoutParameters ().equals (sPayloadMimeType))
              {
                // Send PDF - must fit into a byte array due to XML constraints
                final byte [] aPDFBytes = DocumentStorageHelper.readDocument (aTx.getDocumentPath ());
                aBuilder.payloadBinaryContent (aPDFBytes, CMimeType.APPLICATION_PDF, null);
              }
              else
              {
                // Add support for other non-XML document types here (e.g. from SP2SP) if needed

                // Default is XML
                if (StringHelper.isNotEmpty (sPayloadMimeType))
                  LOGGER.warn (sLogPrefix +
                               "Ignoring unsupported payload MIME type '" +
                               sPayloadMimeType +
                               "' for transaction '" +
                               sTxID +
                               "'");

                // Provide as InputStream to be able to handle larger payloads
                aBuilder.payload (HasInputStream.multiple ( () -> DocumentStorageHelper.openDocumentStream (aTx.getDocumentPath ())));
              }

              eResult = aBuilder.sendMessageAndCheckForReceipt (aCaughtSendingEx::set);
              LOGGER.info (sLogPrefix + "Peppol SBDH-building client send result: " + eResult);
              break;
            }
            case PREBUILT_SBD:
            {
              final PeppolSBDHData aSbdData;
              final MessageDigest aMD = HashHelper.createMessageDigest ();
              try (final InputStream aFileIS = DocumentStorageHelper.openDocumentStream (aTx.getDocumentPath ());
                   final CountingInputStream aCountingIS = new CountingInputStream (aFileIS);
                   final DigestInputStream aDigestIS = new DigestInputStream (aCountingIS, aMD))
              {
                aSbdData = new PeppolSBDHDataReader (aIF).extractData (aFileIS);
                if (aSbdData == null)
                  throw new IllegalStateException ("Failed to read SBDH from file '" + aTx.getDocumentPath () + "'");

                // Check if the read size matches the stored size
                final long nReadByteCount = aCountingIS.getBytesRead ();
                if (nReadByteCount != aTx.getDocumentSize ())
                  throw new IllegalStateException ("The size of the SBDH from file '" +
                                                   aTx.getDocumentPath () +
                                                   "' was stored to be " +
                                                   aTx.getDocumentSize () +
                                                   " but " +
                                                   nReadByteCount +
                                                   " bytes were read now");

                // Check if the read digest matches the stored digest
                final String sReadHash = HashHelper.getDigestHex (aMD);
                if (!sReadHash.equals (aTx.getDocumentHash ()))
                  throw new IllegalStateException ("The hash of the SBDH from file '" +
                                                   aTx.getDocumentPath () +
                                                   "' was stored to be '" +
                                                   aTx.getDocumentHash () +
                                                   "' but the re-read document now creates the hash '" +
                                                   sReadHash +
                                                   "'");
              }

              final PeppolUserMessageSBDHBuilder aBuilder = Phase4PeppolSender.sbdhBuilder ()
                                                                              .httpClientFactory (aHCS)
                                                                              // AS4 input
                                                                              .messageID (sAS4MessageID)
                                                                              .conversationID (sAS4ConversationID)
                                                                              .sendingDateTime (aAS4Timestamp)
                                                                              // SBD
                                                                              .payloadAndMetadata (aSbdData)
                                                                              // Remaining IDs
                                                                              .senderPartyID (sC2SeatID)
                                                                              // Certificate stuff
                                                                              .peppolAP_CAChecker (aAPCA)
                                                                              .endpointDetailProvider (new AS4EndpointDetailProviderConstant (aReceiverCert,
                                                                                                                                              sReceiverAPURL,
                                                                                                                                              sReceiverTechnicalContact))
                                                                              .certificateConsumer ( (aAPCertificate,
                                                                                                      aCheckDT,
                                                                                                      eCertCheckResult) -> {
                                                                                // Determined by SMP
                                                                                // lookup
                                                                                aSendingReport.setC3CertCheckDT (aCheckDT);
                                                                                aSendingReport.setC3CertCheckResult (eCertCheckResult);
                                                                              })
                                                                              // Response stuff
                                                                              .rawResponseConsumer (aSendingReport::setRawHttpResponse)
                                                                              .signalMsgConsumer ( (aSignalMsg,
                                                                                                    aMessageMetadata,
                                                                                                    aState) -> {
                                                                                aSendingReport.setAS4ReceivedSignalMsg (aSignalMsg);
                                                                              });
              eResult = aBuilder.sendMessageAndCheckForReceipt (aCaughtSendingEx::set);
              LOGGER.info (sLogPrefix + "Peppol Prebuilt-SBDH client send result: " + eResult);
              break;
            }
            default:
              throw new IllegalStateException ("Unsupported source type " + aTx.getSourceType ());
          }

          aSendingSW.stop ();
          aSendingReport.setAS4SendingDurationMillis (aSendingSW.getMillis ());

          if (aCaughtSendingEx.isSet ())
          {
            final Phase4Exception ex = aCaughtSendingEx.get ();

            bExceptionCaught = true;
            LOGGER.error (sLogPrefix + "Outbound sending failed for transaction '" + sTxID + "'", ex);

            aSendingReport.setAS4SendingError ("An error occurred during the AS4 transmission to '" +
                                               sReceiverAPURL +
                                               "'");
            aSendingReport.setAS4SendingException (ex);

            if (nNewAttemptCount >= APCoreConfig.getRetrySendingMaxAttempts ())
              onPermanentFailure.accept (ex.getMessage ());
            else
              onFailed.accept (ex.getMessage ());
          }
          else
          {
            // On success

            // Sending result may be null
            final boolean bSendingSuccess = eResult != null && eResult.isSuccess ();
            aSendingReport.setSendingSuccess (bSendingSuccess);
            aSendingReport.setOverallSuccess (bSendingSuccess && !bExceptionCaught);

            // Store successful attempt
            final String sAS4ReceiptID = aSendingReport.getAS4ReceivedSignalMsg ().getMessageInfo ().getMessageId ();
            aAttemptMgr.createSuccess (sTxID, sAS4MessageID, aAS4Timestamp, sAS4ReceiptID);

            // Update in DB
            aTxMgr.updateStatusCompleted (sTxID, EOutboundStatus.SENT);

            LOGGER.info (sLogPrefix + "Outbound transaction sent successfully '" + sTxID + "'");
          }
        }
        catch (final Exception ex)
        {
          bExceptionCaught = true;
          LOGGER.error (sLogPrefix + "Outbound sending exception for transaction '" + sTxID + "'", ex);

          aSendingSW.stop ();
          aSendingReport.setAS4SendingError ("Failed to transmit outbound message to '" + sReceiverAPURL + "'");
          aSendingReport.setAS4SendingException (ex);
          aSendingReport.setAS4SendingDurationMillis (aSendingSW.getMillis ());

          if (nNewAttemptCount >= APCoreConfig.getRetrySendingMaxAttempts ())
            onPermanentFailure.accept (ex.getMessage ());
          else
            onFailed.accept (ex.getMessage ());
        }
      }
      else
      {
        aSendingSW.stop ();
        aSendingReport.setAS4SendingError ("AP access limited by Circuit Breaker");
        aSendingReport.setAS4SendingDurationMillis (aSendingSW.getMillis ());

        onFailed.accept ("AP access limited by Circuit Breaker '" + sCircuitBreakerKeyAP + "'");
      }

      return aSendingReport;
    }
    finally
    {
      aOverallSW.stop ();
      aSendingReport.setOverallDurationMillis (aOverallSW.getMillis ());
    }
  }
}
