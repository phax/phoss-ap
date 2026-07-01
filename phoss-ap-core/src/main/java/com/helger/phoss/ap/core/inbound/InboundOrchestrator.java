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
package com.helger.phoss.ap.core.inbound;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.cache.regex.RegExHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.peppol.mls.PeppolMLSBuilder;
import com.helger.peppol.mls.PeppolMLSMarshaller;
import com.helger.peppol.reporting.api.CPeppolReporting;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppolid.CIdentifier;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.peppol.spis.SPIDHelper;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.IInboundForwardingAttemptManager;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EDuplicateDetectionMode;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.model.MlsOutcomeIssue;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IPeppolReceiverCheckSPI;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.CircuitBreakerManager;
import com.helger.phoss.ap.core.helper.BackoffCalculator;
import com.helger.phoss.ap.core.helper.HashHelper;
import com.helger.phoss.ap.core.mls.MlsHandler;
import com.helger.phoss.ap.core.reporting.APPeppolReportingHelper;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.security.certificate.CertificateHelper;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.ITelemetrySpan;
import com.helger.telemetry.Telemetry;

import oasis.names.specification.ubl.schema.xsd.applicationresponse_21.ApplicationResponseType;

/**
 * Internal orchestrator to handle messages received via the Peppol Network
 *
 * @author Philip Helger
 */
@Immutable
public final class InboundOrchestrator
{
  private static final Logger LOGGER = LoggerFactory.getLogger (InboundOrchestrator.class);

  private InboundOrchestrator ()
  {}

  private static void _notifyInboundDuplicateRejected (@NonNull final String sSenderID,
                                                       @NonNull final String sReceiverID,
                                                       @NonNull final String sDocTypeID,
                                                       @NonNull final String sProcessID,
                                                       @Nullable final String sSenderProviderID,
                                                       @Nullable final String sAS4MessageID,
                                                       @NonNull final String sSbdhInstanceID,
                                                       final boolean bIsDuplicateAS4,
                                                       final boolean bIsDuplicateSBDH,
                                                       @NonNull final String sErrorDetails)
  {
    for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
      aHandler.onInboundDuplicateRejected (sSenderID,
                                           sReceiverID,
                                           sDocTypeID,
                                           sProcessID,
                                           sSenderProviderID,
                                           sAS4MessageID,
                                           sSbdhInstanceID,
                                           bIsDuplicateAS4,
                                           bIsDuplicateSBDH,
                                           sErrorDetails);
  }

  /**
   * Process an inbound Peppol document received via the Peppol Network. This performs duplicate
   * detection, receiver checks, payload storage, persistence, optional verification, incoming MLS
   * correlation and forwarding to the configured C4 endpoint. All side effects (DB updates,
   * notifications, MLS responses, forwarding) happen internally.
   *
   * @param sLogPrefix
   *        Log message prefix for traceability. May not be <code>null</code>.
   * @param sIncomingID
   *        The unique incoming message ID assigned by the AS4 layer. May not be <code>null</code>.
   * @param sAS4MessageID
   *        The AS4 message ID. May not be <code>null</code>.
   * @param aSigningCert
   *        The signing certificate of the received message (C2). May be <code>null</code>.
   * @param aProvidedAS4Timestamp
   *        The AS4 message timestamp resolved to an offset date time, or <code>null</code> if the
   *        incoming message did not contain one - in which case the current date time is used.
   * @param aPeppolSBD
   *        The parsed Peppol SBDH data. May not be <code>null</code>.
   * @param aSBDBytes
   *        The raw Standard Business Document bytes. May not be <code>null</code>.
   * @return A list of processing error details that the boundary layer must report back to the AS4
   *         layer as EBMS errors. Never <code>null</code> but maybe empty.
   * @throws Exception
   *         In case of an unexpected processing error.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <String> processIncomingDocument (@NonNull final String sLogPrefix,
                                                               @NonNull final String sIncomingID,
                                                               @NonNull final String sAS4MessageID,
                                                               @Nullable final X509Certificate aSigningCert,
                                                               @Nullable final OffsetDateTime aProvidedAS4Timestamp,
                                                               @NonNull final PeppolSBDHData aPeppolSBD,
                                                               final byte @NonNull [] aSBDBytes) throws Exception
  {
    final ICommonsList <String> aProcessingErrors = new CommonsArrayList <> ();

    try (final ITelemetrySpan aSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_INBOUND_RECEIVE,
                                                           ETelemetrySpanKind.CONSUMER))
    {
      try
      {
        final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
        final IInboundTransactionManager aInboundMgr = APJdbcMetaManager.getInboundTransactionMgr ();
        final IDocumentPayloadManager aDocPayloadMgr = APBasicMetaManager.getDocPayloadMgr ();
        final Locale aDisplayLocale = CPhossAP.DEFAULT_LOCALE;

        final String sSenderID = aPeppolSBD.getSenderURIEncoded ();
        final String sReceiverID = aPeppolSBD.getReceiverURIEncoded ();
        final IDocumentTypeIdentifier aDocTypeID = aPeppolSBD.getDocumentTypeAsIdentifier ();
        final String sDocTypeID = aDocTypeID.getURIEncoded ();
        final IProcessIdentifier aProcessID = aPeppolSBD.getProcessAsIdentifier ();
        final String sProcessID = aProcessID.getURIEncoded ();
        final String sSbdhInstanceID = aPeppolSBD.getInstanceIdentifier ();
        aSpan.setAttribute (CPhossAPOtel.ATTR_SENDER_ID, sSenderID);
        aSpan.setAttribute (CPhossAPOtel.ATTR_RECEIVER_ID, sReceiverID);
        aSpan.setAttribute (CPhossAPOtel.ATTR_DOCTYPE_ID, sDocTypeID);
        aSpan.setAttribute (CPhossAPOtel.ATTR_PROCESS_ID, sProcessID);
        aSpan.setAttribute (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID, sSbdhInstanceID);

        String sC1CountryCode = aPeppolSBD.getCountryC1 ();
        if (StringHelper.isEmpty (sC1CountryCode))
        {
          // Fallback to ZZ to make sure the reporting item can be created
          sC1CountryCode = CPeppolReporting.REPLACEMENT_COUNTRY_CODE;
        }
        final String sC2ID = CertificateHelper.getSubjectCN (aSigningCert);
        if (!CPhossAP.isPeppolSeatID (sC2ID))
          LOGGER.error ("Received C2 ID '" + sC2ID + "' does not seem to be a valid Peppol Seat ID");
        final String sC3ID = APCoreConfig.getPeppolOwnerSeatID ();

        LOGGER.info (sLogPrefix +
                     "Received inbound SBD - SBDH ID '" +
                     sSbdhInstanceID +
                     "'; AS4 ID '" +
                     sAS4MessageID +
                     "'");

        // Signing certificate CN
        String sSigningCertCN = "";
        if (aSigningCert != null)
          sSigningCertCN = aSigningCert.getSubjectX500Principal ().getName ();

        // Duplicate detection
        boolean bIsDuplicateAS4 = false;
        boolean bIsDuplicateSBDH = false;
        try (final ITelemetrySpan aDupSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_INBOUND_DUPLICATE_CHECK,
                                                                  ETelemetrySpanKind.INTERNAL))
        {
          if (aInboundMgr.containsByAS4MessageID (sAS4MessageID))
          {
            bIsDuplicateAS4 = true;
            aDupSpan.setAttribute (CPhossAPOtel.ATTR_IS_DUPLICATE_AS4, true);
            if (APCoreConfig.getDuplicateDetectionAS4Mode () == EDuplicateDetectionMode.REJECT)
            {
              final String sMsg = "Rejecting duplicate AS4 message '" + sAS4MessageID + "'";
              LOGGER.error (sLogPrefix + sMsg);
              aProcessingErrors.add (sMsg);
              _notifyInboundDuplicateRejected (sSenderID,
                                               sReceiverID,
                                               sDocTypeID,
                                               sProcessID,
                                               sC2ID,
                                               sAS4MessageID,
                                               sSbdhInstanceID,
                                               bIsDuplicateAS4,
                                               bIsDuplicateSBDH,
                                               sMsg);
              return aProcessingErrors;
            }

            final String sMsg = "Found duplicate AS4 message '" + sAS4MessageID + "' - processing it anyway";
            LOGGER.error (sLogPrefix + sMsg);
          }

          if (aInboundMgr.containsBySbdhInstanceID (sSbdhInstanceID))
          {
            bIsDuplicateSBDH = true;
            aDupSpan.setAttribute (CPhossAPOtel.ATTR_IS_DUPLICATE_SBDH, true);
            if (APCoreConfig.getDuplicateDetectionSBDHMode () == EDuplicateDetectionMode.REJECT)
            {
              final String sMsg = "Rejecting duplicate SBDH instance '" + sSbdhInstanceID + "'";
              LOGGER.error (sLogPrefix + sMsg);
              aProcessingErrors.add (sMsg);
              _notifyInboundDuplicateRejected (sSenderID,
                                               sReceiverID,
                                               sDocTypeID,
                                               sProcessID,
                                               sC2ID,
                                               sAS4MessageID,
                                               sSbdhInstanceID,
                                               bIsDuplicateAS4,
                                               bIsDuplicateSBDH,
                                               sMsg);
              return aProcessingErrors;
            }

            final String sMsg = "Found duplicate SBDH instance '" + sSbdhInstanceID + "' - processing it anyway";
            LOGGER.error (sLogPrefix + sMsg);
          }
        }

        // Receiver check
        for (final IPeppolReceiverCheckSPI aReceiverCheck : APCoreMetaManager.getAllPeppolReceiverChecks ())
        {
          if (!aReceiverCheck.isReceiverServiced (sReceiverID, sDocTypeID, sProcessID))
          {
            LOGGER.error (sLogPrefix + "Receiver not serviced '" + sReceiverID + "'");
            aProcessingErrors.add ("PEPPOL:NOT_SERVICED");

            for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
              aHandler.onInboundReceiverNotServiced (sSenderID, sReceiverID, sDocTypeID, sProcessID, sSbdhInstanceID);
            return aProcessingErrors;
          }
        }

        // Create SBDH hash
        final String sSbdhHash = HashHelper.sha256Hex (aSBDBytes);

        // Resolve the AS4 timestamp - fall back to the current date time if absent
        final OffsetDateTime aAS4Timestamp;
        if (aProvidedAS4Timestamp != null)
          aAS4Timestamp = aProvidedAS4Timestamp;
        else
        {
          // Get current time stamp in UTC
          aAS4Timestamp = aTimestampMgr.getCurrentDateTimeUTC ();
          LOGGER.warn (sLogPrefix +
                       "The incoming AS4 message has not AS4 message timestamp - using the current date time instead");
        }

        // Find MLS receiver
        String sValidMlsTo = null;
        {
          final String sScheme = aPeppolSBD.getMLSToScheme ();
          final String sValue = aPeppolSBD.getMLSToValue ();
          if (PeppolIdentifierHelper.PARTICIPANT_SCHEME_ISO6523_ACTORID_UPIS.equals (sScheme))
          {
            // Scheme is valid
            if (sValue != null &&
                sValue.startsWith (SPIDHelper.SPIS_PARTICIPANT_ID_SCHEME + ":") &&
                sValue.length () > 5 &&
                RegExHelper.stringMatchesPattern (SPIDHelper.REGEX_COMPLETE, sValue.substring (5)))
            {
              // Value is valid as well - use it
              sValidMlsTo = CIdentifier.getURIEncoded (sScheme, sValue);
            }
          }

          if (sValidMlsTo == null && (sScheme != null || sValue != null))
          {
            LOGGER.warn (sLogPrefix +
                         "Some MLS_TO parts were provided ('" +
                         sScheme +
                         "' and '" +
                         sValue +
                         "') but they were ignored because they are invalid");
          }
        }

        // Store document to disk
        final String sDocumentPath = aDocPayloadMgr.storeDocument (APBasicConfig.getStorageInboundPath (),
                                                                   aAS4Timestamp,
                                                                   sSbdhInstanceID + ".sbd",
                                                                   aSBDBytes);

        // Store in DB
        final String sTxID = aInboundMgr.create (sIncomingID,
                                                 sC2ID,
                                                 sC3ID,
                                                 sSigningCertCN,
                                                 sSenderID,
                                                 sReceiverID,
                                                 sDocTypeID,
                                                 sProcessID,
                                                 sDocumentPath,
                                                 aSBDBytes.length,
                                                 sSbdhHash,
                                                 sAS4MessageID,
                                                 aAS4Timestamp,
                                                 sSbdhInstanceID,
                                                 sC1CountryCode,
                                                 bIsDuplicateAS4,
                                                 bIsDuplicateSBDH,
                                                 sValidMlsTo,
                                                 APCoreConfig.getMlsType ());
        final IInboundTransaction aInboundTx = aInboundMgr.getByID (sTxID);
        if (aInboundTx == null)
          throw new IllegalStateException ("Failed to store incoming transaction");

        for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
          aHandler.onInboundDocumentReceived (sTxID,
                                              sSenderID,
                                              sReceiverID,
                                              sDocTypeID,
                                              sProcessID,
                                              sSbdhInstanceID,
                                              bIsDuplicateAS4,
                                              bIsDuplicateSBDH);

        // Optional verification
        if (APCoreConfig.isVerificationInboundEnabled ())
        {
          try (final ITelemetrySpan aVerifySpan = Telemetry.startSpan (CPhossAPOtel.SPAN_VERIFICATION,
                                                                       ETelemetrySpanKind.INTERNAL)
                                                           .setAttribute (CPhossAPOtel.ATTR_IS_OUTBOUND, false)
                                                           .setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID, sTxID)
                                                           .setAttribute (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID,
                                                                          sSbdhInstanceID))
          {
            for (final IInboundDocumentVerifierSPI aVerifier : APCoreMetaManager.getAllInboundVerifiers ())
            {
              final MlsOutcome aVerifierOutcome = aVerifier.verifyInboundDocument (sDocumentPath,
                                                                                   aDocTypeID,
                                                                                   aProcessID);
              if (aVerifierOutcome != null && aVerifierOutcome.getResponseCode ().isFailure ())
              {
                aVerifySpan.setStatusError ("Inbound verification failed");
                LOGGER.warn (sLogPrefix + "Inbound document verification failed for '" + sSbdhInstanceID + "'");
                aInboundMgr.updateStatus (sTxID, EInboundStatus.REJECTED);

                // Dop't send MLS as response to MLR or MLS
                if (!CPhossAP.isMLR (aDocTypeID, aProcessID) && !CPhossAP.isMLS (aDocTypeID, aProcessID))
                {
                  // Send asynchronously
                  PhotonWorkerPool.getInstance ().run ("send-mls", () -> {
                    // Send negative MLS (RE) back to C2 with the verifier's detailed outcome
                    MlsHandler.triggerSendingInboundResultMls (aInboundTx, aVerifierOutcome);
                  });
                }

                // No processing error - MLS

                for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
                  aHandler.onInboundVerificationRejection (sTxID, sSbdhInstanceID, "Inbound verification failed");
                return aProcessingErrors;
              }
            }

            // All verifiers accepted
            for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
              aHandler.onInboundVerificationAccepted (sTxID, sSbdhInstanceID);
          }
        }

        if (CPhossAP.isMLS (aDocTypeID, aProcessID))
        {
          LOGGER.info (sLogPrefix + "Handling incoming MLS message");
          final ErrorList aXSDErrors = new ErrorList ();
          final ApplicationResponseType aMLS = new PeppolMLSMarshaller ().setCollectErrors (aXSDErrors)
                                                                         .read (aPeppolSBD.getBusinessMessageNoClone ());
          if (aMLS == null)
          {
            LOGGER.error (sLogPrefix + "Failed to parse incoming MLS");
            // Add all XSD errors to the output
            for (final IError aError : aXSDErrors)
            {
              final String sDetails = "Peppol MLS XSD Issue: " + aError.getAsString (aDisplayLocale);
              aProcessingErrors.add (sDetails);
            }
            return aProcessingErrors;
          }

          final PeppolMLSBuilder aBuilder = PeppolMLSBuilder.createForApplicationResponse (aMLS);

          // The reference ID in the MLS is the SBDH Instance ID of the original
          // outbound business document
          final String sReferencedSbdhInstanceID = aBuilder.referenceId ();
          if (StringHelper.isEmpty (sReferencedSbdhInstanceID))
          {
            LOGGER.error (sLogPrefix + "MLS message '" + sSbdhInstanceID + "' has no reference ID - cannot correlate");
            aInboundMgr.updateStatus (sTxID, EInboundStatus.PERMANENTLY_FAILED);
            return aProcessingErrors;
          }

          // Correlate with the original outbound transaction and update its MLS
          // status
          if (Telemetry.withSpan (CPhossAPOtel.SPAN_MLS_CORRELATE, ETelemetrySpanKind.INTERNAL, aCorrelateSpan -> {
            aCorrelateSpan.setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID, sTxID)
                          .setAttribute (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID, sSbdhInstanceID)
                          .setAttribute (CPhossAPOtel.ATTR_MLS_RESPONSE_CODE, aBuilder.responseCode ().getID ());
            return MlsHandler.handleIncomingMls (sLogPrefix,
                                                 sReferencedSbdhInstanceID,
                                                 aBuilder.responseCode (),
                                                 aAS4Timestamp,
                                                 aBuilder.id (),
                                                 sTxID);
          }).isFailure ())
          {
            for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
              aHandler.onInboundMLSCorrelationError (sTxID, sReferencedSbdhInstanceID, aBuilder.responseCode ());
          }
        }

        // Forward - Business Document and MLS
        if (forwardDocument (sLogPrefix, aInboundTx).isFailure ())
        {
          // Forwarding failed

          for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            aHandler.onInboundForwardingError (sTxID, false);
        }
        else
        {
          // Forwarding success
          if (aInboundTx.getMlsType () == EPeppolMLSType.ALWAYS_SEND)
          {
            // Try to send back positive MLS
            // Don't send MLS as response to MLS
            if (!CPhossAP.isMLS (aDocTypeID, aProcessID))
            {
              // Send asynchronously
              PhotonWorkerPool.getInstance ().run ("send-mls", () -> {
                // AP for delivery with confirmation (e.g. http), AB for delivery without
                // confirmation (e.g. SFTP, S3, file system)
                final MlsOutcome aOutcome = APCoreMetaManager.getForwarder ().isWithDeliveryConfirmation () ? MlsOutcome
                                                                                                                        .acceptance ()
                                                                                                            : MlsOutcome.acknowledging ();
                MlsHandler.triggerSendingInboundResultMls (aInboundTx, aOutcome);
              });
            }
          }
        }

        return aProcessingErrors;
      }
      catch (final Exception ex)
      {
        aSpan.recordException (ex).setStatusError (ex.getMessage ());
        throw ex;
      }
    }
  }

  /**
   * Forward a received inbound document to the configured C4 endpoint. Handles retry scheduling
   * with exponential backoff and triggers MLS rejection responses when maximum retries are
   * exhausted.
   *
   * @param sLogPrefix
   *        Log message prefix for traceability. May not be <code>null</code>.
   * @param aInboundTx
   *        The inbound transaction to forward. May not be <code>null</code>.
   * @return {@link ESuccess#SUCCESS} if forwarding succeeded, {@link ESuccess#FAILURE} otherwise.
   */
  @NonNull
  public static ESuccess forwardDocument (@NonNull final String sLogPrefix,
                                          @NonNull final IInboundTransaction aInboundTx)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundForwardingAttemptManager aAttemptMgr = APJdbcMetaManager.getInboundForwardingAttemptMgr ();
    final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();

    boolean bForwardSuccess = false;
    try (final ITelemetrySpan aSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_INBOUND_FORWARD,
                                                           ETelemetrySpanKind.PRODUCER)
                                               .setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID, aInboundTx.getID ())
                                               .setAttribute (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID,
                                                              aInboundTx.getSbdhInstanceID ())
                                               .setAttribute (CPhossAPOtel.ATTR_IS_RETRY,
                                                              aInboundTx.getAttemptCount () > 0))
    {
      try
      {
        final String sCircuitBreakerID = "phoss-ap-forwarder";
        if (CircuitBreakerManager.tryAcquirePermit (sCircuitBreakerID))
        {
          final IDocumentForwarder aForwarder = APCoreMetaManager.getForwarder ();
          if (aForwarder == null)
          {
            LOGGER.error (sLogPrefix + "Internal error - No document forwarder configured");
            aTxMgr.updateStatus (aInboundTx.getID (), EInboundStatus.PERMANENTLY_FAILED);
            return ESuccess.FAILURE;
          }

          // Set status
          aTxMgr.updateStatus (aInboundTx.getID (), EInboundStatus.FORWARDING);

          // Actual forwarding
          ForwardingResult aResult;
          try
          {
            aResult = aForwarder.forwardDocument (aInboundTx);
          }
          catch (final Exception ex)
          {
            // Be resilient...
            aResult = ForwardingResult.failure ("forward_exception",
                                                "Internal error forwarding the document: " +
                                                                     ex.getMessage () +
                                                                     " (" +
                                                                     ex.getClass ().getName () +
                                                                     ")");

            for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            {
              aHandler.onUnexpectedException ("InboundOrchestrator.forwardDocument",
                                              "Internal error forwarding document for transaction '" +
                                                                                     aInboundTx.getID () +
                                                                                     "'",
                                              ex);
            }
          }

          if (aResult.isSuccess ())
          {
            // Forwarding worked
            CircuitBreakerManager.recordSuccess (sCircuitBreakerID);
            aAttemptMgr.createSuccess (aInboundTx.getID ());

            aTxMgr.updateStatusCompleted (aInboundTx.getID (), EInboundStatus.FORWARDED);
            LOGGER.info (sLogPrefix + "Forwarding successful for transaction '" + aInboundTx.getID () + "'");

            final OffsetDateTime aReceivedDT = aInboundTx.getAS4Timestamp ();
            final Duration aForwardingDuration = aReceivedDT != null ? Duration.between (aReceivedDT,
                                                                                         aTimestampMgr.getCurrentDateTimeUTC ())
                                                                     : null;
            final boolean bIsRetry = aInboundTx.getAttemptCount () > 0;
            for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
            {
              aHandler.onInboundDocumentForwarded (aInboundTx.getID (),
                                                   aInboundTx.getSbdhInstanceID (),
                                                   aForwardingDuration,
                                                   bIsRetry);
            }

            bForwardSuccess = true;

            // Determine C4 country code: either from sync response or via configured resolution
            // modes
            String sC4CountryCode = aResult.getCountryCodeC4 ();
            if (sC4CountryCode == null)
            {
              sC4CountryCode = Telemetry.withSpan (CPhossAPOtel.SPAN_INBOUND_C4_RESOLVE,
                                                   ETelemetrySpanKind.INTERNAL,
                                                   aResolveSpan -> {
                                                     aResolveSpan.setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID,
                                                                                aInboundTx.getID ())
                                                                 .setAttribute (CPhossAPOtel.ATTR_RECEIVER_ID,
                                                                                aInboundTx.getReceiverID ());
                                                     return C4CountryCodeResolver.resolve (aInboundTx);
                                                   });
            }

            if (sC4CountryCode != null)
            {
              // We can store the reporting item immediately
              aTxMgr.updateC4CountryCode (aInboundTx.getID (), sC4CountryCode);
              if (APPeppolReportingHelper.createInboundPeppolReportingItem (aInboundTx.getID ()).isFailure ())
              {
                LOGGER.error (sLogPrefix +
                              "Forwarding successful, but failed to store Peppol Reporting entry for '" +
                              aInboundTx.getID () +
                              "'");
              }
            }

            // Fire-and-forget dispatch to all configured secondary forwarders. Failures are logged
            // only - no retry, no SLA, no effect on the inbound transaction status.
            final ICommonsList <IDocumentForwarder> aSecondaryForwarders = APCoreMetaManager.getAllSecondaryForwarders ();
            if (aSecondaryForwarders.isNotEmpty ())
            {
              PhotonWorkerPool.getInstance ().run ("forward-secondary", () -> {
                int nIndex = 0;
                for (final IDocumentForwarder aSecondary : aSecondaryForwarders)
                {
                  nIndex++;
                  try (final ITelemetrySpan aSecSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_INBOUND_FORWARD_SECONDARY,
                                                                            ETelemetrySpanKind.PRODUCER)
                                                                .setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID,
                                                                               aInboundTx.getID ())
                                                                .setAttribute (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID,
                                                                               aInboundTx.getSbdhInstanceID ())
                                                                .setAttribute (CPhossAPOtel.ATTR_FORWARDER_INDEX,
                                                                               nIndex))
                  {
                    try
                    {
                      final ForwardingResult aSecResult = aSecondary.forwardDocument (aInboundTx);
                      if (aSecResult.isSuccess ())
                      {
                        LOGGER.info (sLogPrefix +
                                     "Secondary forwarding #" +
                                     nIndex +
                                     " successful for transaction '" +
                                     aInboundTx.getID () +
                                     "'");
                        aSecSpan.setStatusOk ();
                      }
                      else
                      {
                        LOGGER.warn (sLogPrefix +
                                     "Secondary forwarding #" +
                                     nIndex +
                                     " failed (ignored) for transaction '" +
                                     aInboundTx.getID () +
                                     "': " +
                                     aSecResult.getErrorDetails ());
                        aSecSpan.setStatusError (aSecResult.getErrorDetails ());
                      }
                    }
                    catch (final Exception ex)
                    {
                      // Catch everything so a failing secondary does not prevent the others from
                      // running.
                      LOGGER.error (sLogPrefix +
                                    "Secondary forwarding #" +
                                    nIndex +
                                    " threw exception (ignored) for transaction '" +
                                    aInboundTx.getID () +
                                    "'",
                                    ex);
                      aSecSpan.recordException (ex).setStatusError (ex.getMessage ());
                    }
                  }
                }
              });
            }

            return ESuccess.SUCCESS;
          }

          // Forwarding failed
          CircuitBreakerManager.recordFailure (sCircuitBreakerID);
          aAttemptMgr.createFailure (aInboundTx.getID (), aResult.getErrorCode (), aResult.getErrorDetails ());

          final int nNewAttemptCount = aInboundTx.getAttemptCount () + 1;
          final int nMaxRetryAttempts = APCoreConfig.getRetryForwardingMaxAttempts ();
          if (!aResult.isRetryAllowed () || nNewAttemptCount >= nMaxRetryAttempts)
          {
            // Maximum number of retries are exhausted - we go on "permanently
            // failed"
            final String sFailureReason = aResult.isRetryAllowed () ? "Max retries (" +
                                                                      nMaxRetryAttempts +
                                                                      ") exhausted: " +
                                                                      aResult.getErrorDetails ()
                                                                    : "Retry disallowed by receiver: " +
                                                                      aResult.getErrorDetails ();
            aTxMgr.updateStatusAndRetry (aInboundTx.getID (),
                                         EInboundStatus.PERMANENTLY_FAILED,
                                         nNewAttemptCount,
                                         null,
                                         sFailureReason);

            // Don't send MLS as response to MLS
            if (!CPhossAP.isMLR (aInboundTx.getDocTypeID (), aInboundTx.getProcessID ()) &&
                !CPhossAP.isMLS (aInboundTx.getDocTypeID (), aInboundTx.getProcessID ()))
            {
              // Send asynchronously
              PhotonWorkerPool.getInstance ().run ("send-mls", () -> {
                // Send negative MLS (RE) with AB reason back to C2
                // The PNP states, that "FD" can only be used in case of "permanent failure". We
                // still expect that this error is a "temporary failure", so we are supposed to send
                // "acknowledging" as we assume it will be resolved later
                final MlsOutcome aOutcome = true ? MlsOutcome.acknowledging ("Forwarding to C4 failed for now")
                                                 : MlsOutcome.rejection ("Forwarding to C4 failed",
                                                                         MlsOutcomeIssue.failureOfDelivery ("Permanent inability to forward document to C4"));
                MlsHandler.triggerSendingInboundResultMls (aInboundTx, aOutcome);
              });
            }

            for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            {
              aHandler.onInboundPermanentForwardingFailure (aInboundTx.getID (),
                                                            aInboundTx.getSbdhInstanceID (),
                                                            aResult.isRetryAllowed () ? "Max retries exhausted"
                                                                                      : "Retry disallowed by receiver");
            }
          }
          else
          {
            // Calculate the next retry and remember it
            final var aNextRetry = BackoffCalculator.calculateNextRetry (nNewAttemptCount,
                                                                         APCoreConfig.getRetryForwardingInitialBackoff (),
                                                                         APCoreConfig.getRetryForwardingBackoffMultiplier (),
                                                                         APCoreConfig.getRetryForwardingMaxBackoff ());
            aTxMgr.updateStatusAndRetry (aInboundTx.getID (),
                                         EInboundStatus.FORWARD_FAILED,
                                         nNewAttemptCount,
                                         aNextRetry,
                                         aResult.getErrorDetails ());
          }
        }
      }
      catch (final RuntimeException ex)
      {
        aSpan.recordException (ex);
        throw ex;
      }
      finally
      {
        if (bForwardSuccess)
          aSpan.setStatusOk ();
        else
          aSpan.setStatusError (null);
      }
    }

    return bForwardSuccess ? ESuccess.SUCCESS : ESuccess.FAILURE;
  }
}
