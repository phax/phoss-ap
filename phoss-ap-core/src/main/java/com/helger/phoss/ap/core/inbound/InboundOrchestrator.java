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

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EContinue;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.cache.regex.RegExHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.peppol.mls.CPeppolMLS;
import com.helger.peppol.mls.PeppolMLSBuilder;
import com.helger.peppol.mls.PeppolMLSMarshaller;
import com.helger.peppol.reporting.api.CPeppolReporting;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppolid.CIdentifier;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.peppol.spis.SPIDHelper;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.IInboundForwardingAttemptManager;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EDuplicateDetectionMode;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EVerificationFailMode;
import com.helger.phoss.ap.api.codelist.EVerificationOutcomeCategory;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.model.MlsOutcomeIssue;
import com.helger.phoss.ap.api.model.VerificationOutcome;
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
  /**
   * The prefix used in the <code>error_details</code> of an inbound transaction, if a document
   * verifier backend service was unavailable.
   *
   * @since 0.12.0
   */
  public static final String ERROR_DETAILS_VERIFIER_UNAVAILABLE = "VERIFIER_UNAVAILABLE";
  /**
   * The prefix used in the <code>error_details</code> of an inbound transaction, if a document
   * verifier rejected the document.
   *
   * @since 0.12.0
   */
  public static final String ERROR_DETAILS_VERIFICATION_REJECTED = "VERIFICATION_REJECTED";

  private static final String EXPECTED_MLS_PREFIX = SPIDHelper.SPIS_PARTICIPANT_ID_SCHEME + ":";

  private static final Logger LOGGER = LoggerFactory.getLogger (InboundOrchestrator.class);

  private InboundOrchestrator ()
  {}

  /**
   * Determine the valid <code>MLS_TO</code> participant identifier (URI encoded) from the provided
   * SBDH <code>MLS_TO</code> scheme and value. Implements the MLS SPOG section 5.1 checks: the
   * value must use the SPIS participant identifier scheme, be syntactically valid, and its Main ID
   * must correlate to the sending C2's SPID Main ID (derived from the AP certificate Seat ID) -
   * since redirecting an MLS to a different Service Provider is not allowed.
   *
   * @param sScheme
   *        The <code>MLS_TO</code> scheme from the SBDH. May be <code>null</code>.
   * @param sValue
   *        The <code>MLS_TO</code> value from the SBDH. May be <code>null</code>.
   * @param sC2SeatID
   *        The sending C2's Seat ID (from the Peppol AP certificate CN). May be <code>null</code>.
   * @return The URI encoded valid <code>MLS_TO</code> value, or <code>null</code> if it is absent,
   *         syntactically invalid, or does not correlate to the sending C2.
   */
  @Nullable
  @VisibleForTesting
  static String getValidMlsTo (@Nullable final String sScheme,
                               @Nullable final String sValue,
                               @Nullable final String sC2SeatID)
  {
    // Scheme must be the ISO6523 actor id upis scheme
    if (!PeppolIdentifierHelper.PARTICIPANT_SCHEME_ISO6523_ACTORID_UPIS.equals (sScheme))
      return null;

    // Value must be syntactically valid as an SPIS participant identifier
    if (sValue == null || sValue.length () <= EXPECTED_MLS_PREFIX.length () || !sValue.startsWith (EXPECTED_MLS_PREFIX))
      return null;

    final String sSpidValue = sValue.substring (EXPECTED_MLS_PREFIX.length ());
    // Value must be syntactically valid as an SPIS participant identifier
    if (!RegExHelper.stringMatchesPattern (SPIDHelper.REGEX_COMPLETE, sSpidValue))
      return null;

    // MLS SPOG section 5.1: the MLS_TO Main ID must correlate to the sending C2's SPID Main ID
    final String sMlsToMainID = SPIDHelper.getMainID (sSpidValue);
    final String sC2MainID = SPIDHelper.getMainIDFromSeatID (sC2SeatID);
    if (!sMlsToMainID.equalsIgnoreCase (sC2MainID))
      return null;

    return CIdentifier.getURIEncoded (sScheme, sValue);
  }

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
   * The aggregated result of running all registered inbound document verifiers.
   *
   * @param outcome
   *        The decisive outcome: {@link EVerificationOutcomeCategory#PASSED} if all verifiers
   *        accepted the document, {@link EVerificationOutcomeCategory#REJECTION} if at least one
   *        verifier rejected it and {@link EVerificationOutcomeCategory#SERVICE_UNAVAILABLE} if no
   *        verifier rejected it, but at least one of them was unavailable. May not be
   *        <code>null</code>.
   * @param verifierName
   *        The name of the verifier that led to this result. <code>null</code> if and only if the
   *        outcome is {@link EVerificationOutcomeCategory#PASSED}.
   */
  static record VerifierResult (@NonNull VerificationOutcome outcome, @Nullable String verifierName)
  {
    /**
     * Get the MLS details to be sent to C2, if the provided verifier result ends up as a rejection.
     * If the verifier provided no MLS details, they are created from the outcome message.
     *
     * @param aVR
     *        The verifier result. May not be <code>null</code>.
     * @return Never <code>null</code>.
     */
    @NonNull
    MlsOutcome getMlsOutcome ()
    {
      final MlsOutcome aMlsOutcome = outcome ().getMlsOutcome ();
      if (aMlsOutcome != null)
        return aMlsOutcome;

      final String sMessage = StringHelper.getNotNull (outcome ().getMessage (), "no details available");
      if (outcome ().isServiceUnavailable ())
      {
        // Yes, Business Rule Violation is a stretch ...
        return MlsOutcome.rejection ("Document verification could not be performed",
                                     MlsOutcomeIssue.businessRuleViolation (CPeppolMLS.LINE_ID_NOT_AVAILABLE,
                                                                            "The document verifier '" +
                                                                                                              verifierName () +
                                                                                                              "' is unavailable: " +
                                                                                                              sMessage));
      }

      // Yes, Business Rule Violation is a stretch ...
      return MlsOutcome.rejection ("Document verification failed",
                                   MlsOutcomeIssue.businessRuleViolation (CPeppolMLS.LINE_ID_NOT_AVAILABLE,
                                                                          "The document verifier '" +
                                                                                                            verifierName () +
                                                                                                            "' rejected the document: " +
                                                                                                            sMessage));
    }
  }

  /**
   * Run all registered inbound document verifiers. A verifier that rejects the document wins
   * immediately. An unavailable verifier is only remembered - the remaining verifiers are still
   * evaluated, so that the rejection of another verifier takes precedence over the unavailability.
   *
   * @param sLogPrefix
   *        Log message prefix. May not be <code>null</code>.
   * @param aVerifiers
   *        The verifiers to be evaluated, in the order of their evaluation. May not be
   *        <code>null</code>.
   * @param sDocumentPath
   *        The path of the stored document. May not be <code>null</code>.
   * @param aDocTypeID
   *        The document type identifier. May not be <code>null</code>.
   * @param aProcessID
   *        The process identifier. May not be <code>null</code>.
   * @return The aggregated result. Never <code>null</code>.
   */
  @NonNull
  @VisibleForTesting
  static VerifierResult runInboundVerifiers (@NonNull final String sLogPrefix,
                                             @NonNull final Iterable <? extends IInboundDocumentVerifierSPI> aVerifiers,
                                             @NonNull final String sDocumentPath,
                                             @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                             @NonNull final IProcessIdentifier aProcessID)
  {
    VerifierResult aUnavailable = null;

    for (final IInboundDocumentVerifierSPI aVerifier : aVerifiers)
    {
      final String sVerifierName = aVerifier.getVerifierName ();
      final VerificationOutcome aOutcome = aVerifier.verifyInboundDocument (sDocumentPath, aDocTypeID, aProcessID);
      if (aOutcome == null)
      {
        // The SPI contract demands a non-null outcome, but it is not enforced at runtime - be
        // resilient and treat it like a passed verification, as the old API did for "null"
        LOGGER.warn (sLogPrefix +
                     "The inbound document verifier '" +
                     sVerifierName +
                     "' returned no outcome - treating the document as verified");
        continue;
      }

      switch (aOutcome.getCategory ())
      {
        case SERVICE_UNAVAILABLE:
        {
          LOGGER.warn (sLogPrefix +
                       "The inbound document verifier '" +
                       sVerifierName +
                       "' is unavailable: " +
                       aOutcome.getMessage ());

          // Remember the first unavailable verifier only, but evaluate the remaining ones as well
          if (aUnavailable == null)
            aUnavailable = new VerifierResult (aOutcome, sVerifierName);
          break;
        }
        case REJECTION:
        {
          // An explicit rejection always wins
          return new VerifierResult (aOutcome, sVerifierName);
        }
        case PASSED:
        {
          // successful verification
        }
      }
    }

    return aUnavailable != null ? aUnavailable : new VerifierResult (VerificationOutcome.passed (), null);
  }

  /**
   * Reject an inbound document, because it did not pass the verification. The forwarding attempt
   * count is deliberately left unchanged, because the document is never forwarded.
   *
   * @param sLogPrefix
   *        Log message prefix. May not be <code>null</code>.
   * @param aInboundTx
   *        The affected inbound transaction. May not be <code>null</code>.
   * @param aOutcome
   *        The outcome to be sent as MLS to C2. May not be <code>null</code>.
   * @param sErrorDetails
   *        The error details to be stored in the DB. May not be <code>null</code>.
   * @param sReason
   *        The human readable reason, used for logging and for the notification handlers. May not
   *        be <code>null</code>.
   */
  private static void _rejectAfterVerification (@NonNull final String sLogPrefix,
                                                @NonNull final IInboundTransaction aInboundTx,
                                                @NonNull final MlsOutcome aOutcome,
                                                @NonNull final String sErrorDetails,
                                                @NonNull final String sReason)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sTxID = aInboundTx.getID ();
    final String sSbdhInstanceID = aInboundTx.getSbdhInstanceID ();

    LOGGER.warn (sLogPrefix + "Inbound document verification failed for '" + sSbdhInstanceID + "': " + sReason);

    // Don't touch the forwarding attempt count - the document is never forwarded
    aTxMgr.updateStatusAndNextRetry (sTxID, EInboundStatus.REJECTED, null, sErrorDetails);

    // Don't send MLS as response to MLR or MLS
    if (!CPhossAP.isMLR (aInboundTx.getDocTypeID (), aInboundTx.getProcessID ()) &&
        !CPhossAP.isMLS (aInboundTx.getDocTypeID (), aInboundTx.getProcessID ()))
    {
      // Send asynchronously
      PhotonWorkerPool.getInstance ().run ("send-mls", () -> {
        // Send negative MLS (RE) back to C2 with the verifier's detailed outcome
        MlsHandler.triggerSendingInboundResultMls (aInboundTx, aOutcome);
      });
    }

    for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
      aHandler.onInboundVerificationRejection (sTxID, sSbdhInstanceID, sReason);
  }

  /**
   * Defer the verification of an inbound document, because a verifier backend service is
   * unavailable. If the document was received longer ago than the configured maximum deferral
   * duration, it is rejected instead, so that C2 finally gets an answer. The forwarding attempt
   * count is deliberately left unchanged, because the deferred verification is retried
   * independently of the forwarding.
   *
   * @param sLogPrefix
   *        Log message prefix. May not be <code>null</code>.
   * @param aInboundTx
   *        The affected inbound transaction. May not be <code>null</code>.
   * @param aVR
   *        The verifier result of category {@link EVerificationOutcomeCategory#SERVICE_UNAVAILABLE}
   *        . May not be <code>null</code>.
   */
  private static void _deferVerification (@NonNull final String sLogPrefix,
                                          @NonNull final IInboundTransaction aInboundTx,
                                          @NonNull final VerifierResult aVR)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final OffsetDateTime aNow = APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ();
    final String sErrorDetails = ERROR_DETAILS_VERIFIER_UNAVAILABLE +
                                 " [" +
                                 aVR.verifierName () +
                                 "]: " +
                                 StringHelper.getNotNull (aVR.outcome ().getMessage (), "Verifier unavailable");
    final Duration aMaxDuration = APCoreConfig.getVerificationDeferredMaxDuration ();
    final OffsetDateTime aDeadline = aInboundTx.getReceivedDT ().plus (aMaxDuration);

    if (!aNow.isBefore (aDeadline))
    {
      // Deferring forever is not an option - C2 needs a final answer
      final String sReason = "The document verifier '" +
                             aVR.verifierName () +
                             "' was unavailable for more than " +
                             aMaxDuration;
      _rejectAfterVerification (sLogPrefix,
                                aInboundTx,
                                aVR.getMlsOutcome (),
                                sErrorDetails + " (maximum deferral duration of " + aMaxDuration + " exceeded)",
                                sReason);
      return;
    }

    // Never schedule the next re-verification beyond the deadline, so that the rejection happens at
    // the first scheduler cycle at or after it and not one full retry interval later
    OffsetDateTime aNextRetry = aNow.plus (APCoreConfig.getVerificationDeferredRetryInterval ());
    if (aNextRetry.isAfter (aDeadline))
      aNextRetry = aDeadline;

    LOGGER.warn (sLogPrefix +
                 "Deferring the verification of inbound document '" +
                 aInboundTx.getSbdhInstanceID () +
                 "' until " +
                 aNextRetry +
                 ", because the verifier '" +
                 aVR.verifierName () +
                 "' is unavailable");

    aTxMgr.updateStatusAndNextRetry (aInboundTx.getID (),
                                     EInboundStatus.VERIFICATION_DEFERRED,
                                     aNextRetry,
                                     sErrorDetails);

    // Fired on every deferral - this is the signal that a verifier needs operator attention
    for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
      aHandler.onInboundVerificationDeferred (aInboundTx.getID (),
                                              aInboundTx.getSbdhInstanceID (),
                                              aVR.verifierName (),
                                              aNextRetry,
                                              sErrorDetails);
  }

  /**
   * Apply the configured {@link EVerificationFailMode} onto the provided verifier result and update
   * the inbound transaction accordingly.
   *
   * @param sLogPrefix
   *        Log message prefix. May not be <code>null</code>.
   * @param aInboundTx
   *        The affected inbound transaction. May not be <code>null</code>.
   * @param aVR
   *        The verifier result to be handled. May not be <code>null</code>.
   * @return <code>EContinue.CONTINUE</code> if the processing of the document may continue,
   *         <code>EContinue.BREAK</code> if the document was rejected or if its verification was
   *         deferred.
   */
  private static @NonNull EContinue _handleVerifierResult (@NonNull final String sLogPrefix,
                                                           @NonNull final IInboundTransaction aInboundTx,
                                                           @NonNull final VerifierResult aVR)
  {
    if (aVR.outcome ().isRejected ())
    {
      final String sText = StringHelper.getNotNull (aVR.outcome ().getMessage (), "Verification failed");
      _rejectAfterVerification (sLogPrefix,
                                aInboundTx,
                                aVR.getMlsOutcome (),
                                ERROR_DETAILS_VERIFICATION_REJECTED + " [" + aVR.verifierName () + "]: " + sText,
                                "The document verifier '" + aVR.verifierName () + "' rejected the document");
      return EContinue.BREAK;
    }

    if (aVR.outcome ().isServiceUnavailable ())
    {
      final EVerificationFailMode eFailMode = APCoreConfig.getVerificationFailMode ();
      return switch (eFailMode)
      {
        case DEFERRED ->
        {
          _deferVerification (sLogPrefix, aInboundTx, aVR);
          yield EContinue.BREAK;
        }
        case OPEN ->
        {
          // Deliberately no "verification accepted" callback - nothing was verified at all
          LOGGER.warn (sLogPrefix +
                       "The document verifier '" +
                       aVR.verifierName () +
                       "' is unavailable, but the fail mode is '" +
                       eFailMode.getID () +
                       "' - forwarding the unverified document '" +
                       aInboundTx.getSbdhInstanceID () +
                       "'");
          yield EContinue.CONTINUE;
        }
        default ->
        {
          // CLOSED - handle it like a rejection
          final String sText = StringHelper.getNotNull (aVR.outcome ().getMessage (), "Verifier unavailable");
          final String sReason = "The document verifier '" +
                                 aVR.verifierName () +
                                 "' is unavailable and the fail mode is '" +
                                 eFailMode.getID () +
                                 "'";
          _rejectAfterVerification (sLogPrefix,
                                    aInboundTx,
                                    aVR.getMlsOutcome (),
                                    ERROR_DETAILS_VERIFIER_UNAVAILABLE + " [" + aVR.verifierName () + "]: " + sText,
                                    sReason);
          yield EContinue.BREAK;
        }
      };
    }

    // All verifiers accepted
    for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
      aHandler.onInboundVerificationAccepted (aInboundTx.getID (), aInboundTx.getSbdhInstanceID ());
    return EContinue.CONTINUE;
  }

  /**
   * Run the optional inbound document verification for the provided transaction and handle its
   * result.
   *
   * @param sLogPrefix
   *        Log message prefix. May not be <code>null</code>.
   * @param aInboundTx
   *        The affected inbound transaction. May not be <code>null</code>.
   * @param aDocTypeID
   *        The document type identifier. May not be <code>null</code>.
   * @param aProcessID
   *        The process identifier. May not be <code>null</code>.
   * @return <code>EContinue.CONTINUE</code> if the processing of the document may continue,
   *         <code>EContinue.BREAK</code> if the document was rejected or if its verification was
   *         deferred.
   */
  private static @NonNull EContinue _verifyInboundDocument (@NonNull final String sLogPrefix,
                                                            @NonNull final IInboundTransaction aInboundTx,
                                                            @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                            @NonNull final IProcessIdentifier aProcessID)
  {
    try (final ITelemetrySpan aVerifySpan = Telemetry.startSpan (CPhossAPOtel.SPAN_VERIFICATION,
                                                                 ETelemetrySpanKind.INTERNAL)
                                                     .setAttribute (CPhossAPOtel.ATTR_IS_OUTBOUND, false)
                                                     .setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID,
                                                                    aInboundTx.getID ())
                                                     .setAttribute (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID,
                                                                    aInboundTx.getSbdhInstanceID ()))
    {
      final VerifierResult aVR = runInboundVerifiers (sLogPrefix,
                                                      APCoreMetaManager.getAllInboundVerifiers (),
                                                      aInboundTx.getDocumentPath (),
                                                      aDocTypeID,
                                                      aProcessID);
      if (aVR.outcome ().isRejected ())
        aVerifySpan.setStatusError ("Inbound verification failed");
      else
        if (aVR.outcome ().isServiceUnavailable ())
          aVerifySpan.setStatusError ("Inbound verifier service unavailable");

      return _handleVerifierResult (sLogPrefix, aInboundTx, aVR);
    }
  }

  /**
   * Handle an incoming MLS document: parse it and correlate it with the referenced outbound
   * transaction.
   *
   * @param sLogPrefix
   *        Log message prefix. May not be <code>null</code>.
   * @param aInboundTx
   *        The inbound transaction of the MLS. May not be <code>null</code>.
   * @param aBusinessMessage
   *        The business message of the received SBD. May not be <code>null</code>.
   * @param aProcessingErrors
   *        The list of processing errors to be filled. May not be <code>null</code>.
   * @return {@link ESuccess#FAILURE} if the MLS could not be interpreted and the processing of the
   *         document must be stopped, {@link ESuccess#SUCCESS} otherwise.
   */
  @NonNull
  private static ESuccess _handleIncomingMls (@NonNull final String sLogPrefix,
                                              @NonNull final IInboundTransaction aInboundTx,
                                              @NonNull final Element aBusinessMessage,
                                              @NonNull final List <String> aProcessingErrors)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sTxID = aInboundTx.getID ();
    final String sSbdhInstanceID = aInboundTx.getSbdhInstanceID ();

    LOGGER.info (sLogPrefix + "Handling incoming MLS message");

    // Read as UBL ApplicationResponse
    final ErrorList aXSDErrors = new ErrorList ();
    final ApplicationResponseType aMLS = new PeppolMLSMarshaller ().setCollectErrors (aXSDErrors)
                                                                   .read (aBusinessMessage);
    if (aMLS == null)
    {
      LOGGER.error (sLogPrefix + "Failed to parse incoming MLS");
      // Add all XSD errors to the output
      for (final IError aError : aXSDErrors)
      {
        final String sDetails = "Peppol MLS XSD Issue: " + aError.getAsString (CPhossAP.DEFAULT_LOCALE);
        aProcessingErrors.add (sDetails);
      }
      return ESuccess.FAILURE;
    }

    // Read as Peppol MLS
    final PeppolMLSBuilder aBuilder = PeppolMLSBuilder.createForApplicationResponse (aMLS);

    // The reference ID in the MLS is the SBDH Instance ID of the original
    // outbound business document
    final String sReferencedSbdhInstanceID = aBuilder.referenceId ();
    if (StringHelper.isEmpty (sReferencedSbdhInstanceID))
    {
      LOGGER.error (sLogPrefix + "MLS message '" + sSbdhInstanceID + "' has no reference ID - cannot correlate");
      aTxMgr.updateStatus (sTxID, EInboundStatus.PERMANENTLY_FAILED);
      return ESuccess.FAILURE;
    }

    // Correlate with the original outbound transaction and update its MLS
    // status
    if (Telemetry.withSpan (CPhossAPOtel.SPAN_MLS_CORRELATE, ETelemetrySpanKind.INTERNAL, aCorrelateSpan -> {
      aCorrelateSpan.setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID, sTxID)
                    .setAttribute (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID, sSbdhInstanceID)
                    .setAttribute (CPhossAPOtel.ATTR_MLS_RESPONSE_CODE, aBuilder.responseCode ().getID ());
      {
        return MlsHandler.handleIncomingMls (sLogPrefix,
                                             sReferencedSbdhInstanceID,
                                             aBuilder.responseCode (),
                                             aInboundTx.getAS4Timestamp (),
                                             aBuilder.id (),
                                             sTxID);
      }
    }).isFailure ())
    {
      // Call callbacks
      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onInboundMLSCorrelationError (sTxID, sReferencedSbdhInstanceID, aBuilder.responseCode ());
    }
    return ESuccess.SUCCESS;
  }

  /**
   * Send the positive MLS to C2 after an inbound document was successfully forwarded to C4, if MLS
   * sending is enabled for the transaction.
   *
   * @param aInboundTx
   *        The successfully forwarded inbound transaction. May not be <code>null</code>.
   */
  private static void _sendPositiveMlsAfterForwarding (@NonNull final IInboundTransaction aInboundTx)
  {
    if (aInboundTx.getMlsType () == EPeppolMLSType.ALWAYS_SEND)
    {
      // Try to send back positive MLS
      // Don't send MLS as response to MLS
      if (!CPhossAP.isMLR (aInboundTx.getDocTypeID (), aInboundTx.getProcessID ()) &&
          !CPhossAP.isMLS (aInboundTx.getDocTypeID (), aInboundTx.getProcessID ()))
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

  /**
   * Handle an inbound document that will never be forwarded to C4: send the MLS to C2 and call the
   * notification handlers. The status of the transaction must already have been updated by the
   * caller.
   *
   * @param aInboundTx
   *        The affected inbound transaction. May not be <code>null</code>.
   * @param sReason
   *        The human readable reason, passed on to the notification handlers. May not be
   *        <code>null</code>.
   */
  private static void _handlePermanentForwardingFailure (@NonNull final IInboundTransaction aInboundTx,
                                                         @NonNull final String sReason)
  {
    // Don't send MLS as response to MLR or MLS
    if (!CPhossAP.isMLR (aInboundTx.getDocTypeID (), aInboundTx.getProcessID ()) &&
        !CPhossAP.isMLS (aInboundTx.getDocTypeID (), aInboundTx.getProcessID ()))
    {
      // Send asynchronously
      PhotonWorkerPool.getInstance ().run ("send-mls", () -> {
        // Deliberately "acknowledging" (AB) and not a rejection with the status reason code "FD".
        // The PNP reserves "FD" for a permanent inability to deliver, and we still assume that this
        // problem is resolvable later - so phoss AP never sends "FD"
        MlsHandler.triggerSendingInboundResultMls (aInboundTx,
                                                   MlsOutcome.acknowledging ("Forwarding to C4 failed for now"));
      });
    }

    for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
      aHandler.onInboundPermanentForwardingFailure (aInboundTx.getID (), aInboundTx.getSbdhInstanceID (), sReason);
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
          sValidMlsTo = getValidMlsTo (sScheme, sValue, sC2ID);

          if (sValidMlsTo == null && (sScheme != null || sValue != null))
          {
            LOGGER.warn (sLogPrefix +
                         "Some MLS_TO parts were provided ('" +
                         sScheme +
                         "' and '" +
                         sValue +
                         "') but they were ignored because they are invalid or do not correlate to the sending C2");
          }
        }

        // Store SBD in a persistent storage
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

        // Call callbacks
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
          // No processing error is created here - a rejection is signaled via MLS and a deferred
          // verification is picked up by the retry scheduler
          if (_verifyInboundDocument (sLogPrefix, aInboundTx, aDocTypeID, aProcessID).isBreak ())
            return aProcessingErrors;
        }

        if (CPhossAP.isMLS (aDocTypeID, aProcessID))
        {
          if (_handleIncomingMls (sLogPrefix, aInboundTx, aPeppolSBD.getBusinessMessageNoClone (), aProcessingErrors)
                                                                                                                     .isFailure ())
            return aProcessingErrors;
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
          _sendPositiveMlsAfterForwarding (aInboundTx);
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
            final String sReason = "No document forwarder configured";
            LOGGER.error (sLogPrefix + "Internal error - " + sReason);
            // The attempt count is left unchanged, because no forwarding was attempted
            aTxMgr.updateStatusAndRetry (aInboundTx.getID (),
                                         EInboundStatus.PERMANENTLY_FAILED,
                                         aInboundTx.getAttemptCount (),
                                         null,
                                         sReason);
            // C2 must get an answer, even though this is a local configuration error
            _handlePermanentForwardingFailure (aInboundTx, sReason);
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

            _handlePermanentForwardingFailure (aInboundTx,
                                               aResult.isRetryAllowed () ? "Max retries exhausted"
                                                                         : "Retry disallowed by receiver");
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
        else
        {
          // The circuit breaker is open, so no forwarding was attempted at all. The transaction
          // must nevertheless be scheduled for a retry - otherwise it would stay in its current
          // status forever, because only "forward_failed" is picked up by the retry scheduler.
          // No forwarding attempt row is created and the attempt count is left unchanged, because
          // nothing was tried
          final OffsetDateTime aNextRetry = aTimestampMgr.getCurrentDateTimeUTC ()
                                                         .plus (APCoreConfig.getRetryForwardingInitialBackoff ());
          LOGGER.warn (sLogPrefix +
                       "The circuit breaker '" +
                       sCircuitBreakerID +
                       "' is open - not forwarding transaction '" +
                       aInboundTx.getID () +
                       "' now, retrying at " +
                       aNextRetry);
          aTxMgr.updateStatusAndRetry (aInboundTx.getID (),
                                       EInboundStatus.FORWARD_FAILED,
                                       aInboundTx.getAttemptCount (),
                                       aNextRetry,
                                       "The circuit breaker '" + sCircuitBreakerID + "' is open");
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

  /**
   * Read the stored SBD of an inbound MLS transaction and correlate it with the referenced outbound
   * transaction. This is needed, if the MLS could not be correlated when it was received, because
   * its verification was deferred.
   *
   * @param sLogPrefix
   *        Log message prefix. May not be <code>null</code>.
   * @param aInboundTx
   *        The inbound transaction of the MLS. May not be <code>null</code>.
   * @return {@link ESuccess#FAILURE} if the MLS could not be interpreted and the processing of the
   *         document must be stopped, {@link ESuccess#SUCCESS} otherwise.
   */
  @NonNull
  private static ESuccess _correlateStoredMls (@NonNull final String sLogPrefix,
                                               @NonNull final IInboundTransaction aInboundTx)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IDocumentPayloadManager aDocPayloadMgr = APBasicMetaManager.getDocPayloadMgr ();
    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();

    final PeppolSBDHData aSbdData;
    try (final InputStream aIS = aDocPayloadMgr.openDocumentStreamForRead (aInboundTx.getDocumentPath ()))
    {
      aSbdData = new PeppolSBDHDataReader (aIF).extractData (aIS);
    }
    catch (final Exception ex)
    {
      LOGGER.error (sLogPrefix +
                    "Failed to re-read the stored SBD of inbound MLS '" +
                    aInboundTx.getSbdhInstanceID () +
                    "' from '" +
                    aInboundTx.getDocumentPath () +
                    "'",
                    ex);
      aTxMgr.updateStatusAndNextRetry (aInboundTx.getID (),
                                       EInboundStatus.PERMANENTLY_FAILED,
                                       null,
                                       "Failed to re-read the stored SBD: " + ex.getMessage ());
      return ESuccess.FAILURE;
    }

    // The processing errors are of no use here - the AS4 response was sent long ago
    final ICommonsList <String> aProcessingErrors = new CommonsArrayList <> ();
    final ESuccess eMLS = _handleIncomingMls (sLogPrefix,
                                              aInboundTx,
                                              aSbdData.getBusinessMessageNoClone (),
                                              aProcessingErrors);
    if (eMLS.isFailure ())
    {
      LOGGER.error (sLogPrefix +
                    "Failed to correlate the deferred inbound MLS '" +
                    aInboundTx.getSbdhInstanceID () +
                    "': " +
                    aProcessingErrors);
      if (aInboundTx.getStatus () != EInboundStatus.PERMANENTLY_FAILED)
        aTxMgr.updateStatusAndNextRetry (aInboundTx.getID (),
                                         EInboundStatus.PERMANENTLY_FAILED,
                                         null,
                                         "Failed to interpret the received MLS");
      return ESuccess.FAILURE;
    }
    return ESuccess.SUCCESS;
  }

  /**
   * Resume the processing of an inbound document whose verification was deferred, because a
   * verifier backend service was unavailable. The verification is repeated and - if it succeeds -
   * the processing continues exactly where
   * {@link #processIncomingDocument(String, String, String, java.security.cert.X509Certificate, OffsetDateTime, PeppolSBDHData, byte[])}
   * left it: an incoming MLS is correlated, the document is forwarded to C4 and the positive MLS is
   * sent to C2.
   * <p>
   * If the verifier is still unavailable, the verification is deferred again, until the configured
   * maximum deferral duration is exceeded. The forwarding attempt count is never modified by the
   * deferral.
   * </p>
   *
   * @param sLogPrefix
   *        Log message prefix for traceability. May not be <code>null</code>.
   * @param aInboundTx
   *        The inbound transaction to be resumed. May not be <code>null</code>.
   * @return {@link ESuccess#SUCCESS} only if the document was verified and forwarded to C4.
   * @since 0.12.0
   */
  @NonNull
  public static ESuccess resumeDeferredInboundDocument (@NonNull final String sLogPrefix,
                                                        @NonNull final IInboundTransaction aInboundTx)
  {
    ValueEnforcer.notNull (sLogPrefix, "LogPrefix");
    ValueEnforcer.notNull (aInboundTx, "InboundTx");

    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sTxID = aInboundTx.getID ();

    if (APCoreConfig.isVerificationInboundEnabled ())
    {
      // The stored identifiers are URI encoded, so they must be parsed and not created
      final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();
      final IDocumentTypeIdentifier aDocTypeID = aIF.parseDocumentTypeIdentifier (aInboundTx.getDocTypeID ());
      final IProcessIdentifier aProcessID = aIF.parseProcessIdentifier (aInboundTx.getProcessID ());
      if (aDocTypeID == null || aProcessID == null)
      {
        LOGGER.error (sLogPrefix +
                      "Inbound transaction '" +
                      sTxID +
                      "' contains the invalid document type ID '" +
                      aInboundTx.getDocTypeID () +
                      "' or the invalid process ID '" +
                      aInboundTx.getProcessID () +
                      "' - cannot re-verify it");
        aTxMgr.updateStatusAndNextRetry (sTxID,
                                         EInboundStatus.PERMANENTLY_FAILED,
                                         null,
                                         "Invalid document type ID or process ID - re-verification impossible");
        return ESuccess.FAILURE;
      }

      if (_verifyInboundDocument (sLogPrefix, aInboundTx, aDocTypeID, aProcessID).isBreak ())
        return ESuccess.FAILURE;
    }
    else
    {
      // Verification was switched off in the meantime - continue as if it succeeded
      LOGGER.info (sLogPrefix +
                   "Inbound verification is disabled - continuing with the deferred document '" +
                   aInboundTx.getSbdhInstanceID () +
                   "'");
    }

    // Now do what was skipped when the document was received
    if (CPhossAP.isMLS (aInboundTx.getDocTypeID (), aInboundTx.getProcessID ()))
      if (_correlateStoredMls (sLogPrefix, aInboundTx).isFailure ())
        return ESuccess.FAILURE;

    // Forward - Business Document and MLS
    final ESuccess eForward = forwardDocument (sLogPrefix, aInboundTx);
    if (eForward.isSuccess ())
      _sendPositiveMlsAfterForwarding (aInboundTx);
    return eForward;
  }
}
