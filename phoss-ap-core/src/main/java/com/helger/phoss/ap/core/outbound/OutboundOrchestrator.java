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
import com.helger.base.timing.StopWatch;
import com.helger.base.wrapper.Wrapper;
import com.helger.mime.CMimeType;
import com.helger.peppol.reporting.api.PeppolReportingItem;
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
import com.helger.phase4.logging.Phase4LogCustomizer;
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
import com.helger.phoss.ap.core.reporting.APPeppolReportingHelper;
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

  /** Private constructor to prevent instantiation of this utility class. */
  private OutboundOrchestrator ()
  {}

  /**
   * Submit a raw (payload-only) document for outbound sending. The document is
   * stored to disk, optionally verified, and a new outbound transaction is
   * created in {@link EOutboundStatus#PENDING} state.
   *
   * @param sLogPrefix
   *        Log message prefix for traceability. May not be <code>null</code>.
   * @param aSenderID
   *        The Peppol sender participant identifier. May not be
   *        <code>null</code>.
   * @param aReceiverID
   *        The Peppol receiver participant identifier. May not be
   *        <code>null</code>.
   * @param aDocTypeID
   *        The Peppol document type identifier. May not be <code>null</code>.
   * @param aProcessID
   *        The Peppol process identifier. May not be <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH instance identifier to use. May not be <code>null</code>.
   * @param sC1CountryCode
   *        The C1 country code of the sender. May not be <code>null</code>.
   * @param aDocumentIS
   *        The input stream of the raw document payload. Will not be closed by
   *        this method. May not be <code>null</code>.
   * @param sMlsTo
   *        Optional MLS "To" address. May be <code>null</code>.
   * @param sSbdhStandard
   *        Optional SBDH standard identifier. May be <code>null</code>.
   * @param sSbdhTypeVersion
   *        Optional SBDH type version. May be <code>null</code>.
   * @param sSbdhType
   *        Optional SBDH type. May be <code>null</code>.
   * @param sPayloadMimeType
   *        Optional payload MIME type (e.g. "application/pdf"). May be
   *        <code>null</code> for XML payloads.
   * @return The created {@link IOutboundTransaction} or <code>null</code> if
   *         the document could not be stored or verification failed.
   */
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

    final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
    final IOutboundTransactionManager aOutboundMgr = APJdbcMetaManager.getOutboundTransactionMgr ();

    final File aStorageBasePath = new File (APBasicConfig.getStorageOutboundPath ());
    final OffsetDateTime aCreationDT = aTimestampMgr.getCurrentDateTimeUTC ();
    final Wrapper <String> aTempPathHolder = Wrapper.empty ();

    long nDocumentBytes = -1;
    final MessageDigest aMD = HashHelper.createMessageDigest ();
    // 1. Count size
    // 2. Create message digest
    // 3. Copy to a temporary file
    // 4. Parse the SBDH
    try (final CountingInputStream aCountingIS = new CountingInputStream (aDocumentIS);
         final DigestInputStream aDigestIS = new DigestInputStream (aCountingIS, aMD);
         final OutputStream aFileOS = DocumentStorageHelper.openDocumentStream (aStorageBasePath,
                                                                                aCreationDT,
                                                                                sSbdhInstanceID,
                                                                                ".out",
                                                                                aTempPathHolder::set))
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
        if (aTempPathHolder.isSet ())
          DocumentStorageHelper.deleteDocument (aTempPathHolder.get ());
        return null;
      }
      nDocumentBytes = aCountingIS.getBytesRead ();
    }
    catch (final Exception ex)
    {
      LOGGER.error (sLogPrefix + "Failed to process document to submit", ex);

      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onUnexpectedException ("OutboundOrchestrator.submitRawDocument",
                                        "Failed to process document to submit",
                                        ex);

      // No need to keep the temporary file
      if (aTempPathHolder.isSet ())
        DocumentStorageHelper.deleteDocument (aTempPathHolder.get ());
      return null;
    }

    final String sDocumentHash = HashHelper.getDigestHex (aMD);
    final String sDocumentPath = aTempPathHolder.get ();

    // Optional verification
    if (APCoreConfig.isVerificationOutboundEnabled ())
    {
      for (final IOutboundDocumentVerifierSPI aVerifier : APCoreMetaManager.getAllOutboundVerifiers ())
        if (aVerifier.verifyOutboundDocument (sDocumentPath, aDocTypeID, aProcessID).isFailure ())
        {
          LOGGER.warn (sLogPrefix + "Outbound document verification failed for SBDH '" + sSbdhInstanceID + "'");
          return null;
        }
    }

    // Create in pending state
    final String sTransactionID = aOutboundMgr.create (ETransactionType.BUSINESS_DOCUMENT,
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
                                                       aCreationDT,
                                                       sMlsTo,
                                                       (String) null,
                                                       sSbdhStandard,
                                                       sSbdhTypeVersion,
                                                       sSbdhType,
                                                       sPayloadMimeType);
    return aOutboundMgr.getByID (sTransactionID);
  }

  /**
   * Submit a pre-built Standard Business Document (SBD) for outbound sending.
   * The SBD is parsed to extract Peppol metadata, stored to disk, and a new
   * outbound transaction is created in {@link EOutboundStatus#PENDING} state.
   *
   * @param sLogPrefix
   *        Log message prefix for traceability. May not be <code>null</code>.
   * @param aSbdIS
   *        The input stream containing the complete pre-built SBD. May not be
   *        <code>null</code>.
   * @param sMlsTo
   *        Optional MLS "To" address. May be <code>null</code>.
   * @return The created {@link IOutboundTransaction} or <code>null</code> if
   *         the SBD could not be parsed.
   */
  @Nullable
  public static IOutboundTransaction submitPrebuiltSBD (@NonNull final String sLogPrefix,
                                                        @NonNull final InputStream aSbdIS,
                                                        @Nullable final String sMlsTo)
  {
    LOGGER.info (sLogPrefix + "Submitting pre-built SBD");

    final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();

    final File aStorageBasePath = new File (APBasicConfig.getStorageOutboundPath ());
    final OffsetDateTime aCreationDT = aTimestampMgr.getCurrentDateTimeUTC ();
    final Wrapper <String> aTempPathHolder = Wrapper.empty ();

    final PeppolSBDHData aSbdData;
    long nSbdByteCount = -1;
    final MessageDigest aMD = HashHelper.createMessageDigest ();
    // 1. Count size
    // 2. Create message digest
    // 3. Copy SBDH to a temporary file - the final name can only be deduced
    // after reading the SBDH
    // as it contains the InstanceIdentifier
    // 4. Parse the SBDH
    try (final CountingInputStream aCountingIS = new CountingInputStream (aSbdIS);
         final DigestInputStream aDigestIS = new DigestInputStream (aCountingIS, aMD);
         final OutputStream aFileOS = DocumentStorageHelper.openTemporaryDocumentStream (aStorageBasePath,
                                                                                         aCreationDT,
                                                                                         aTempPathHolder::set);
         final CopyingInputStream aCopyIS = new CopyingInputStream (aDigestIS, aFileOS))
    {
      aSbdData = new PeppolSBDHDataReader (aIF).extractData (aCopyIS);
      nSbdByteCount = aCountingIS.getBytesRead ();
    }
    catch (final Exception ex)
    {
      LOGGER.error (sLogPrefix + "Failed to parse provided SBDH", ex);

      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onUnexpectedException ("OutboundOrchestrator.submitPrebuiltSBD", "Failed to parse provided SBDH", ex);

      // No need to keep the temporary file
      if (aTempPathHolder.isSet ())
        DocumentStorageHelper.deleteDocument (aTempPathHolder.get ());
      return null;
    }

    // Get Document hash in the correct version
    final String sDocumentHash = HashHelper.getDigestHex (aMD);

    final String sSbdhInstanceID = aSbdData.getInstanceIdentifier ();
    LOGGER.info (sLogPrefix + "Found SBDH Instance ID '" + sSbdhInstanceID + "'");

    final String sDocumentPath;
    {
      // Rename temp file to final name
      final File aTempFile = new File (aTempPathHolder.get ());
      final File aDstFile = DocumentStorageHelper.renameFile (aTempFile,
                                                              aTempFile.getParentFile (),
                                                              sSbdhInstanceID,
                                                              ".sbd");
      sDocumentPath = aDstFile.getAbsolutePath ().toString ();
    }

    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();

    // Create in pending state

    final String sInboundTxID = null;
    final String sSbdhStandard = null;
    final String sSbdhTypeVersion = null;
    final String sSbdhType = null;
    final String sPayloadMimeType = null;

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
                                               aCreationDT,
                                               sMlsTo,
                                               sInboundTxID,
                                               sSbdhStandard,
                                               sSbdhTypeVersion,
                                               sSbdhType,
                                               sPayloadMimeType);
    return aMgr.getByID (sTransactionID);
  }

  /**
   * Process a pending outbound transaction by performing SMP lookup and sending
   * the document via AS4/Peppol. This method handles dynamic discovery (NAPTR +
   * SMP), certificate validation, circuit breaker checks, and the actual AS4
   * transmission. On success, the transaction status is updated to
   * {@link EOutboundStatus#SENT}. On failure, the transaction is either marked
   * as {@link EOutboundStatus#FAILED} (with retry scheduling) or
   * {@link EOutboundStatus#PERMANENTLY_FAILED} depending on the error type and
   * attempt count.
   *
   * @param sLogPrefix
   *        Log message prefix for traceability. May not be <code>null</code>.
   * @param aTx
   *        The outbound transaction to process. Must be in pending state. May
   *        not be <code>null</code>.
   * @return The {@link Phase4PeppolSendingReport} containing the full details
   *         of the sending attempt including lookup results, AS4 message IDs,
   *         and timing information. Never <code>null</code>.
   */
  @NonNull
  public static Phase4PeppolSendingReport processPendingOutbound (@NonNull final String sLogPrefix,
                                                                  @NonNull final IOutboundTransaction aTx)
  {
    final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundSendingAttemptManager aAttemptMgr = APJdbcMetaManager.getOutboundSendingAttemptMgr ();

    final String sTxID = aTx.getID ();
    final EPeppolNetwork ePeppolStage = APCoreConfig.getPeppolStage ();
    final ISMLInfo aSMLInfo = ePeppolStage.getSMLInfo ();
    final String sC2SeatID = APCoreConfig.getPeppolSeatID ();
    final StopWatch aOverallSW = StopWatch.createdStarted ();

    final Phase4PeppolSendingReport aSendingReport = new Phase4PeppolSendingReport (aSMLInfo);

    final String sRealLogPrefix = sLogPrefix + "[" + sTxID + "] ";
    LOGGER.info (sRealLogPrefix + "Processing outbound transaction");
    Phase4LogCustomizer.setThreadLocalLogPrefix (sRealLogPrefix);

    // try-catch for overall duration only
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
          aHandler.onOutboundPermanentSendingFailure (sTxID, aTx.getSbdhInstanceID (), sErrMsg);
      };

      // Convert all identifiers to structured data - that should have been
      // verified before
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
        // Only add it here to the sending report, otherwise the interpretation
        // of the report gets
        // more difficult
        aSendingReport.setSBDHInstanceIdentifier (aTx.getSbdhInstanceID ());
        aSendingReport.setCountryC1 (aTx.getC1CountryCode ());
        aSendingReport.setSenderPartyID (sC2SeatID);
        aSendingReport.setAS4MessageID (sAS4MessageID);
        aSendingReport.setAS4SendingDT (aAS4Timestamp);

        final String sAS4ConversationID = MessageHelperMethods.createRandomConversationID ();
        aSendingReport.setAS4ConversationID (sAS4ConversationID);

        final TrustedCAChecker aAPCAChecker = ePeppolStage.isProduction () ? PeppolTrustedCA.peppolProductionAP ()
                                                                           : PeppolTrustedCA.peppolTestAP ();

        EAS4UserMessageSendResult eResult = null;
        PeppolReportingItem aReportingItem = null;
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
              final PeppolUserMessageBuilder aBuilder;
              aBuilder = Phase4PeppolSender.builder ()
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
                                           .peppolAP_CAChecker (aAPCAChecker)
                                           .endpointDetailProvider (new AS4EndpointDetailProviderConstant (aReceiverCert,
                                                                                                           sReceiverAPURL,
                                                                                                           sReceiverTechnicalContact))
                                           .certificateConsumer ( (aAPCertificate, aCheckDT, eCertCheckResult) -> {
                                             // Take specifically the
                                             // AP certificate
                                             // verification
                                             aSendingReport.setC3CertCheckDT (aCheckDT);
                                             aSendingReport.setC3CertCheckResult (eCertCheckResult);
                                           })
                                           // Response stuff
                                           .rawResponseConsumer (aSendingReport::setRawHttpResponse)
                                           .signalMsgConsumer ( (aSignalMsg, aMessageMetadata, aState) -> {
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
                // Add support for other non-XML document types here (e.g. from
                // SP2SP) if needed

                // Default is XML
                if (StringHelper.isNotEmpty (sPayloadMimeType))
                  LOGGER.warn (sRealLogPrefix +
                               "Ignoring unsupported payload MIME type '" +
                               sPayloadMimeType +
                               "' for transaction '" +
                               sTxID +
                               "'");

                // Provide as InputStream to be able to handle larger payloads
                aBuilder.payload (HasInputStream.multiple ( () -> DocumentStorageHelper.openDocumentStream (aTx.getDocumentPath ())));
              }

              eResult = aBuilder.sendMessageAndCheckForReceipt (aCaughtSendingEx::set);
              LOGGER.info (sRealLogPrefix + "Peppol SBDH-building client send result: " + eResult);

              aReportingItem = aBuilder.createPeppolReportingItemAfterSending (aReceiverID.getURIEncoded ());
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
                aSbdData = new PeppolSBDHDataReader (aIF).extractData (aDigestIS);
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

              final PeppolUserMessageSBDHBuilder aBuilder;
              aBuilder = Phase4PeppolSender.sbdhBuilder ()
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
                                           .peppolAP_CAChecker (aAPCAChecker)
                                           .endpointDetailProvider (new AS4EndpointDetailProviderConstant (aReceiverCert,
                                                                                                           sReceiverAPURL,
                                                                                                           sReceiverTechnicalContact))
                                           .certificateConsumer ( (aAPCertificate, aCheckDT, eCertCheckResult) -> {
                                             // Determined by SMP
                                             // lookup
                                             aSendingReport.setC3CertCheckDT (aCheckDT);
                                             aSendingReport.setC3CertCheckResult (eCertCheckResult);
                                           })
                                           // Response stuff
                                           .rawResponseConsumer (aSendingReport::setRawHttpResponse)
                                           .signalMsgConsumer ( (aSignalMsg, aMessageMetadata, aState) -> {
                                             aSendingReport.setAS4ReceivedSignalMsg (aSignalMsg);
                                           });
              eResult = aBuilder.sendMessageAndCheckForReceipt (aCaughtSendingEx::set);
              LOGGER.info (sRealLogPrefix + "Peppol Prebuilt-SBDH client send result: " + eResult);

              aReportingItem = aBuilder.createPeppolReportingItemAfterSending (aReceiverID.getURIEncoded ());
              break;
            }
            default:
              throw new IllegalStateException ("Unsupported source type " + aTx.getSourceType ());
          }

          aSendingSW.stop ();
          aSendingReport.setAS4SendingDurationMillis (aSendingSW.getMillis ());

          if (aCaughtSendingEx.isSet ())
          {
            // Some exception occurred in phase4
            final Phase4Exception ex = aCaughtSendingEx.get ();

            LOGGER.error (sRealLogPrefix + "Outbound transaction '" + sTxID + "' could not be sent with phase4", ex);

            aSendingReport.setAS4SendingError ("An error occurred during the phase4 transmission to '" +
                                               sReceiverAPURL +
                                               "'");
            aSendingReport.setAS4SendingException (ex);
            aSendingReport.setSendingSuccess (false);
            aSendingReport.setOverallSuccess (false);

            if (nNewAttemptCount >= APCoreConfig.getRetrySendingMaxAttempts ())
              onPermanentFailure.accept (ex.getMessage ());
            else
              onFailed.accept (ex.getMessage ());
          }
          else
          {
            // On success
            LOGGER.info (sRealLogPrefix + "Outbound transaction '" + sTxID + "' sent successfully with phase4");

            // Sending result may be null
            final boolean bSendingSuccess = eResult != null && eResult.isSuccess ();
            aSendingReport.setSendingSuccess (bSendingSuccess);

            // Store successful attempt
            final String sAS4ReceiptID = aSendingReport.getAS4ReceivedSignalMsg ().getMessageInfo ().getMessageId ();
            aAttemptMgr.createSuccess (sTxID, sAS4MessageID, aAS4Timestamp, sAS4ReceiptID);

            // Update in DB
            aTxMgr.updateStatusCompleted (sTxID, EOutboundStatus.SENT);

            // Store Reporting data on success only
            final boolean bReportingItemStored;
            if (aReportingItem != null)
            {
              bReportingItemStored = APPeppolReportingHelper.createOutboundPeppolReportingItem (sTxID, aReportingItem)
                                                            .isSuccess ();
              if (bReportingItemStored)
                LOGGER.info (sRealLogPrefix + "Successfully stored for Peppol Reporting");
              else
                LOGGER.error (sRealLogPrefix + "Failed to store for Peppol Reporting");
            }
            else
            {
              bReportingItemStored = false;
              LOGGER.error (sRealLogPrefix + "No Reporting Item could be created so cannot store for Peppol Reporting");
            }

            // Set as last activity
            aSendingReport.setOverallSuccess (bSendingSuccess && bReportingItemStored);
          }
        }
        catch (final Exception ex)
        {
          // Unexpected exception - not a Phase4Exception
          LOGGER.error (sRealLogPrefix + "Outbound sending exception for transaction '" + sTxID + "'", ex);

          for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            aHandler.onUnexpectedException ("OutboundOrchestrator.processPendingOutbound",
                                            "Outbound sending exception for transaction '" + sTxID + "'",
                                            ex);

          aSendingSW.stop ();
          aSendingReport.setAS4SendingError ("Failed to transmit outbound AS4 message to '" + sReceiverAPURL + "'");
          aSendingReport.setAS4SendingException (ex);
          aSendingReport.setAS4SendingDurationMillis (aSendingSW.getMillis ());
          aSendingReport.setSendingSuccess (false);
          aSendingReport.setOverallSuccess (false);

          if (nNewAttemptCount >= APCoreConfig.getRetrySendingMaxAttempts ())
            onPermanentFailure.accept (ex.getMessage ());
          else
            onFailed.accept (ex.getMessage ());
        }

        // Update circuit breaker based on sending result only
        if (aSendingReport.isSendingSuccess ())
          CircuitBreakerManager.recordSuccess (sCircuitBreakerKeyAP);
        else
          CircuitBreakerManager.recordFailure (sCircuitBreakerKeyAP);
      }
      else
      {
        // Circuit Breaker not acquired
        aSendingSW.stop ();
        aSendingReport.setAS4SendingError ("AP access limited by Circuit Breaker");
        aSendingReport.setAS4SendingDurationMillis (aSendingSW.getMillis ());
        aSendingReport.setSendingSuccess (false);
        aSendingReport.setOverallSuccess (false);

        onFailed.accept ("AP access limited by Circuit Breaker '" + sCircuitBreakerKeyAP + "'");
      }
    }
    finally
    {
      // Finalize overall stuff
      aOverallSW.stop ();
      aSendingReport.setOverallDurationMillis (aOverallSW.getMillis ());

      // Don't forget to clean up
      Phase4LogCustomizer.clearThreadLocals ();
    }

    return aSendingReport;
  }
}
