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
import java.time.OffsetDateTime;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.io.stream.CountingInputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHex;
import com.helger.base.wrapper.Wrapper;
import com.helger.io.file.FileOperationManager;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.dynamicdiscovery.AS4EndpointDetailProviderPeppol;
import com.helger.phase4.model.message.MessageHelperMethods;
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
  public static IOutboundTransaction submitRawDocument (@NonNull final String sSenderID,
                                                        @NonNull final String sReceiverID,
                                                        @NonNull final String sDocTypeID,
                                                        @NonNull final String sProcessID,
                                                        @NonNull final String sSbdhInstanceID,
                                                        @NonNull final String sC1CountryCode,
                                                        @NonNull final InputStream aDocumentIS,
                                                        @Nullable final String sMlsTo)
  {
    LOGGER.info ("Submitting raw document with SBDH Instance ID '" + sSbdhInstanceID + "'");

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
        LOGGER.error ("Failed to store incoming document to disk");
      }
      nDocumentBytes = aCountingIS.getBytesRead ();
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to process document to submit", ex);
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
      {
        if (aVerifier.verifyDocument (aDocumentFile, sDocTypeID, sProcessID).isFailure ())
        {
          LOGGER.warn ("Outbound document verification failed for SBDH: " + sSbdhInstanceID);
          return null;
        }
      }
    }

    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();

    // Create in pending state
    final String sTransactionID = aMgr.create (ETransactionType.BUSINESS_DOCUMENT,
                                               sSenderID,
                                               sReceiverID,
                                               sDocTypeID,
                                               sProcessID,
                                               sSbdhInstanceID,
                                               ESourceType.RAW_XML,
                                               sDocumentPath,
                                               nDocumentBytes,
                                               sDocumentHash,
                                               sC1CountryCode,
                                               aAS4SendingDT,
                                               sMlsTo,
                                               null);
    return aMgr.getByID (sTransactionID);
  }

  @Nullable
  public static IOutboundTransaction submitPrebuiltSBD (final @NonNull InputStream aSbdIS,
                                                        @Nullable final String sMlsTo)
  {
    LOGGER.info ("Submitting pre-built SBD");

    final File aStorageBasePath = new File (APBasicConfig.getStorageOutboundPath ());
    final OffsetDateTime aAS4SendingDT = APBasicMetaManager.getTimestampMgr ().getCurrentDateTime ();
    final Wrapper <File> aTempFileHolder = Wrapper.empty ();

    final PeppolSBDHData aData;
    long nSbdBytes = -1;
    final MessageDigest aMD = HashHelper.MD_ALGO.createMessageDigest ();
    // 1. Count size
    // 2. Create message digest
    // 3. Copy to a temporary file
    // 4. Parse the SBDH
    try (final CountingInputStream aCountingIS = new CountingInputStream (aSbdIS);
         final DigestInputStream aDigestIS = new DigestInputStream (aCountingIS, aMD);
         final OutputStream aFileOS = DocumentStorageHelper.openTemporaryDocumentStream (aStorageBasePath,
                                                                                         aAS4SendingDT,
                                                                                         aTempFileHolder::set);
         final CopyingInputStream aCopyIS = new CopyingInputStream (aDigestIS, aFileOS))
    {
      aData = new PeppolSBDHDataReader (PeppolIdentifierFactory.INSTANCE).extractData (aCopyIS);
      nSbdBytes = aCountingIS.getBytesRead ();
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to parse provided SBDH", ex);
      // No need to keep the temporary file
      if (aTempFileHolder.isSet ())
        DocumentStorageHelper.deleteDocument (aTempFileHolder.get ().toString ());
      return null;
    }

    final String sDocumentHash = StringHex.getHexEncoded (aMD.digest ());

    final String sSbdhInstanceID = aData.getInstanceIdentifier ();
    LOGGER.info ("Found SBDH Instance ID '" + sSbdhInstanceID + "'");

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
                                               aData.getSenderURIEncoded (),
                                               aData.getReceiverURIEncoded (),
                                               aData.getDocumentTypeURIEncoded (),
                                               aData.getProcessURIEncoded (),
                                               sSbdhInstanceID,
                                               ESourceType.PREBUILT_SBD,
                                               sDocumentPath,
                                               nSbdBytes,
                                               sDocumentHash,
                                               aData.getCountryC1 (),
                                               aAS4SendingDT,
                                               sMlsTo,
                                               null);
    return aMgr.getByID (sTransactionID);
  }

  public static void processPendingOutbound (@NonNull final String sLogPrefix, @NonNull final IOutboundTransaction aTx)
  {
    final String sTxID = aTx.getID ();
    final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundSendingAttemptManager aAttemptMgr = APJdbcMetaManager.getOutboundSendingAttemptMgr ();

    LOGGER.info (sLogPrefix + "Processing outbound transaction '" + sTxID + "'");

    final int nNewAttemptCount = aTx.getAttemptCount () + 1;
    final String sAS4MessageID = MessageHelperMethods.createRandomMessageID ();
    final OffsetDateTime aAS4Timestamp = aTimestampMgr.getCurrentDateTime ();

    final Consumer <String> onFailed = sErrMsg -> {
      aAttemptMgr.create (sTxID, sAS4MessageID, aAS4Timestamp, null, null, EAttemptStatus.FAILED, sErrMsg);
      final OffsetDateTime aNextRetry = BackoffCalculator.calculateNextRetry (nNewAttemptCount,
                                                                              APCoreConfig.getRetrySendingInitialBackoffMs (),
                                                                              APCoreConfig.getRetrySendingBackoffMultiplier (),
                                                                              APCoreConfig.getRetrySendingMaxBackoffMs ());
      aTxMgr.updateStatusAndRetry (sTxID, EOutboundStatus.FAILED, nNewAttemptCount, aNextRetry, sErrMsg);
    };

    final Consumer <String> onPermanentFailure = sErrMsg -> {
      aAttemptMgr.create (sTxID, sAS4MessageID, aAS4Timestamp, null, null, EAttemptStatus.FAILED, sErrMsg);
      aTxMgr.updateStatusAndRetry (sTxID, EOutboundStatus.PERMANENTLY_FAILED, nNewAttemptCount, null, sErrMsg);

      // Notify
      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onPermanentSendingFailure (sTxID, aTx.getSbdhInstanceID (), sErrMsg);
    };

    // SMP lookup to find endpoint URL
    final IParticipantIdentifier aReceiverID = PeppolIdentifierFactory.INSTANCE.parseParticipantIdentifier (aTx.getReceiverID ());
    if (aReceiverID == null)
      throw new IllegalStateException ("Failed to parse participant identifier '" + aTx.getReceiverID () + "'");

    final IDocumentTypeIdentifier aDocTypeID = PeppolIdentifierFactory.INSTANCE.parseDocumentTypeIdentifier (aTx.getDocTypeID ());
    if (aDocTypeID == null)
      throw new IllegalStateException ("Failed to parse document type identifier '" + aTx.getDocTypeID () + "'");

    final IProcessIdentifier aProcessID = PeppolIdentifierFactory.INSTANCE.parseProcessIdentifier (aTx.getProcessID ());
    if (aProcessID == null)
      throw new IllegalStateException ("Failed to parse process identifier '" + aTx.getProcessID () + "'");

    // Try to resolve SMP host
    final SMPClientReadOnly aSMPClient;
    try
    {
      aSMPClient = new CachingSMPClientReadOnly (PeppolNaptrURLProvider.INSTANCE,
                                                 aReceiverID,
                                                 APCoreConfig.getPeppolStage ().getSMLInfo ());
      APBasicConfig.applyHttpProxySettings (aSMPClient.httpClientSettings ());
    }
    catch (final SMPDNSResolutionException ex)
    {
      onPermanentFailure.accept ("The participant ID '" +
                                 aTx.getReceiverID () +
                                 "' is not registered in the Peppol Network. Technical details: " +
                                 ex.getMessage ());
      return;
    }

    // Perform SMP lookup
    final AS4EndpointDetailProviderPeppol aEndpointDetails = AS4EndpointDetailProviderPeppol.create (aSMPClient);
    final String sCircuitBreakerKeySMP = "smp::" + aSMPClient.getSMPHostURI ();
    if (CircuitBreakerManager.tryAcquirePermit (sCircuitBreakerKeySMP))
    {
      try
      {
        aEndpointDetails.init (aDocTypeID, aProcessID, aReceiverID);
        CircuitBreakerManager.recordSuccess (sCircuitBreakerKeySMP);
      }
      catch (final Phase4Exception ex)
      {
        CircuitBreakerManager.recordFailure (sCircuitBreakerKeySMP);
        if (ex.isRetryFeasible ())
          onFailed.accept (ex.getMessage ());
        else
          onPermanentFailure.accept (ex.getMessage ());
        return;
      }
    }

    try
    {
      // TODO: Circuit breaker check

      aTxMgr.updateStatus (sTxID, EOutboundStatus.SENDING);

      // TODO: phase4 sending via Phase4PeppolSender.builder()

      // Actual sending would happen here using Phase4PeppolSender

      // On success:
      final String sAS4ReceiptID = null; // TODO
      aAttemptMgr.createSuccess (sTxID, sAS4MessageID, aAS4Timestamp, sAS4ReceiptID);
      aTxMgr.updateStatusCompleted (sTxID, EOutboundStatus.SENT);

      LOGGER.info (sLogPrefix + "Outbound transaction sent successfully '" + sTxID + "'");
    }
    catch (final Exception ex)
    {
      LOGGER.error (sLogPrefix + "Outbound sending failed for transaction '" + sTxID + "'", ex);
      if (nNewAttemptCount >= APCoreConfig.getRetrySendingMaxAttempts ())
        onPermanentFailure.accept (ex.getMessage ());
      else
        onFailed.accept (ex.getMessage ());
    }
  }
}
