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
package com.helger.phoss.ap.core.mls;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.mls.PeppolMLSBuilder;
import com.helger.peppol.mls.PeppolMLSMarshaller;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.peppolid.peppol.spis.SPIDHelper;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.model.ForwardableDocument;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.helper.HashHelper;
import com.helger.phoss.ap.core.outbound.MlsSmpFallback;
import com.helger.phoss.ap.core.outbound.OutboundOrchestrator;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.ITelemetrySpan;
import com.helger.telemetry.Telemetry;

/**
 * Handler for Peppol Message Level Status (MLS) responses. Responsible for creating outbound MLS
 * response transactions for inbound documents and for correlating incoming MLS responses to
 * previously sent outbound transactions.
 *
 * @author Philip Helger
 */
public final class MlsHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MlsHandler.class);

  private MlsHandler ()
  {}

  /**
   * Dispatch a copy of a self-generated MLS to the configured MLS copy sink. This is a
   * fire-and-forget dispatch: it never influences the MLS sending to C2 and never touches the
   * inbound transaction status. Nothing happens at all if no sink is configured.
   *
   * @param aMlsTx
   *        The outbound transaction of the MLS. May not be <code>null</code>.
   * @param eResponseCode
   *        The MLS response code, for the log messages and the telemetry span. May not be
   *        <code>null</code>.
   * @since 0.12.0
   */
  private static void _forwardMlsCopy (@NonNull final IOutboundTransaction aMlsTx,
                                       @NonNull final EPeppolMLSResponseCode eResponseCode)
  {
    final IDocumentForwarder aForwarder = APCoreMetaManager.getMlsCopyForwarderOrNull ();
    if (aForwarder == null)
    {
      // Not configured - stay silent
      return;
    }

    final ForwardableDocument aDocument = ForwardableDocument.fromOutboundMlsCopy (aMlsTx);

    PhotonWorkerPool.getInstance ().run ("forward-mls-copy", () -> {
      try (final ITelemetrySpan aSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_MLS_FORWARD_COPY,
                                                             ETelemetrySpanKind.PRODUCER)
                                                 .setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID, aMlsTx.getID ())
                                                 .setAttribute (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID,
                                                                aMlsTx.getSbdhInstanceID ())
                                                 .setAttribute (CPhossAPOtel.ATTR_MLS_RESPONSE_CODE,
                                                                eResponseCode.getID ()))
      {
        try
        {
          final ForwardingResult aResult = aForwarder.forwardDocument (aDocument);
          if (aResult.isSuccess ())
          {
            LOGGER.info ("Forwarded the copy of the MLS (" +
                         eResponseCode.getID () +
                         ") of outbound transaction '" +
                         aMlsTx.getID () +
                         "'");
            aSpan.setStatusOk ();
          }
          else
          {
            LOGGER.warn ("Failed to forward the copy of the MLS (" +
                         eResponseCode.getID () +
                         ") of outbound transaction '" +
                         aMlsTx.getID () +
                         "': " +
                         aResult.getErrorDetails ());
            aSpan.setStatusError (aResult.getErrorDetails ());
          }
        }
        catch (final Exception ex)
        {
          // Be resilient - this must never influence the MLS sending
          LOGGER.error ("Internal error forwarding the copy of the MLS of outbound transaction '" +
                        aMlsTx.getID () +
                        "'",
                        ex);
          aSpan.recordException (ex);
        }
      }
    });
  }

  /**
   * Create the outbound MLS response transaction for the outcome of an inbound document, if
   * required by the MLS strategy. The MLS is <b>not</b> sent by this method - the caller decides
   * when and on which thread that happens, e.g. via {@link #sendCreatedMlsAsync(MlsCreationResult)}.
   *
   * @param aInboundTx
   *        The inbound transaction. Never <code>null</code>.
   * @param aOutcome
   *        The MLS outcome carrying the response code, optional response text, and optional issues
   *        for rejection responses. Never <code>null</code>.
   * @return The creation result, carrying the created outbound MLS transaction. Never
   *         <code>null</code>.
   * @since 0.13.0
   */
  @NonNull
  public static MlsCreationResult createInboundResultMls (@NonNull final IInboundTransaction aInboundTx,
                                                          @NonNull final MlsOutcome aOutcome)
  {
    // Global MLS kill switch
    if (!APCoreConfig.isMlsSendingEnabled ())
    {
      LOGGER.info ("MLS sending is globally disabled - skipping MLS for transaction '" + aInboundTx.getID () + "'");
      return MlsCreationResult.suppressed (ESuccess.SUCCESS, aOutcome.getResponseCode ());
    }

    try (final ITelemetrySpan aSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_MLS_SEND, ETelemetrySpanKind.PRODUCER)
                                               .setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID, aInboundTx.getID ())
                                               .setAttribute (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID,
                                                              aInboundTx.getSbdhInstanceID ())
                                               .setAttribute (CPhossAPOtel.ATTR_MLS_RESPONSE_CODE,
                                                              aOutcome.getResponseCode ().getID ()))
    {
      return _createInboundResultMls (aInboundTx, aOutcome);
    }
  }

  /**
   * Send an MLS that was previously created by {@link #createInboundResultMls(IInboundTransaction, MlsOutcome)}
   * asynchronously. Nothing happens if the MLS was suppressed or its creation failed.
   *
   * @param aCreationResult
   *        The result of the MLS creation. May not be <code>null</code>.
   * @since 0.13.0
   */
  public static void sendCreatedMlsAsync (@NonNull final MlsCreationResult aCreationResult)
  {
    if (!aCreationResult.hasMlsTx ())
    {
      // Nothing to send - either suppressed by the MLS strategy or the creation failed
      return;
    }

    PhotonWorkerPool.getInstance ().run ("send-mls", () -> _sendCreatedMls (aCreationResult));
  }

  /**
   * Handle the outcome of an inbound document by creating an outbound MLS response transaction if
   * required by the MLS strategy, and by sending it right away. The sending happens synchronously
   * on the calling thread.
   *
   * @param aInboundTx
   *        The inbound transaction. Never <code>null</code>.
   * @param aOutcome
   *        The MLS outcome carrying the response code, optional response text, and optional issues
   *        for rejection responses. Never <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  public static ESuccess triggerSendingInboundResultMls (@NonNull final IInboundTransaction aInboundTx,
                                                         @NonNull final MlsOutcome aOutcome)
  {
    // Global MLS kill switch
    if (!APCoreConfig.isMlsSendingEnabled ())
    {
      LOGGER.info ("MLS sending is globally disabled - skipping MLS for transaction '" + aInboundTx.getID () + "'");
      return ESuccess.SUCCESS;
    }

    try (final ITelemetrySpan aSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_MLS_SEND, ETelemetrySpanKind.PRODUCER)
                                               .setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID, aInboundTx.getID ())
                                               .setAttribute (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID,
                                                              aInboundTx.getSbdhInstanceID ())
                                               .setAttribute (CPhossAPOtel.ATTR_MLS_RESPONSE_CODE,
                                                              aOutcome.getResponseCode ().getID ()))
    {
      final MlsCreationResult aCreationResult = _createInboundResultMls (aInboundTx, aOutcome);
      if (aCreationResult.isFailure ())
        return ESuccess.FAILURE;

      if (!aCreationResult.hasMlsTx ())
      {
        // Deliberately not sent - the response code was recorded nevertheless
        return ESuccess.SUCCESS;
      }

      return _sendCreatedMls (aCreationResult);
    }
  }

  /**
   * Create the MLS document, store it and persist the outbound transaction for it. This is the
   * shared body of {@link #createInboundResultMls(IInboundTransaction, MlsOutcome)} and
   * {@link #triggerSendingInboundResultMls(IInboundTransaction, MlsOutcome)}, and expects the
   * global MLS kill switch to have been evaluated by the caller.
   *
   * @param aInboundTx
   *        The inbound transaction. Never <code>null</code>.
   * @param aOutcome
   *        The MLS outcome. Never <code>null</code>.
   * @return The creation result. Never <code>null</code>.
   */
  @NonNull
  private static MlsCreationResult _createInboundResultMls (@NonNull final IInboundTransaction aInboundTx,
                                                            @NonNull final MlsOutcome aOutcome)
  {
    final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();
    final IInboundTransactionManager aInboundMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IOutboundTransactionManager aOutboundMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IDocumentPayloadManager aDocPayloadMgr = APBasicMetaManager.getDocPayloadMgr ();

    final EPeppolMLSResponseCode eResponseCode = aOutcome.getResponseCode ();
    final EPeppolMLSType eMlsType = aInboundTx.getMlsType ();

    // Determine if we should send MLS
    if (eMlsType == EPeppolMLSType.FAILURE_ONLY && eResponseCode.isSuccess ())
    {
      LOGGER.info ("MLS not required for transaction " +
                   aInboundTx.getID () +
                   " (FAILURE_ONLY, outcome=" +
                   eResponseCode.getID () +
                   ")");
      final String sMlsOutboundTransactionID = null;
      return MlsCreationResult.suppressed (aInboundMgr.updateMlsFields (aInboundTx.getID (),
                                                                        eResponseCode,
                                                                        sMlsOutboundTransactionID),
                                           eResponseCode);
    }

    LOGGER.info ("Creating MLS response (" +
                 eResponseCode.getID () +
                 ") for inbound transaction '" +
                 aInboundTx.getID () +
                 "'");

    // Create MLS data structure from MlsOutcome
    final String sSenderPIDValue = SPIDHelper.SPIS_PARTICIPANT_ID_SCHEME + ":" + APCoreConfig.getPeppolOwnerSPID ();
    final IParticipantIdentifier aMLSSenderPID = aIF.createParticipantIdentifierWithDefaultScheme (sSenderPIDValue);
    if (aMLSSenderPID == null)
    {
      // Failed to build PID
      LOGGER.error ("Failed to create MLS sender participant ID with value '" + sSenderPIDValue + "'");
      return MlsCreationResult.failure (eResponseCode);
    }

    // The default SPID receiver is always derived from the sending C2's Seat ID (drop the 3-char
    // prefix). It is the fallback target if a custom MLS_TO is not reachable (MLS SPOG 5.4).
    final String sDefaultSpidValue = SPIDHelper.SPIS_PARTICIPANT_ID_SCHEME +
                                     ":" +
                                     SPIDHelper.getMainIDFromSeatID (aInboundTx.getC2SeatID ());
    final IParticipantIdentifier aDefaultSpidReceiverPID = aIF.createParticipantIdentifierWithDefaultScheme (sDefaultSpidValue);
    if (aDefaultSpidReceiverPID == null)
    {
      // Failed to build PID
      LOGGER.error ("Failed to create default SPID MLS receiver participant ID with value '" +
                    sDefaultSpidValue +
                    "'");
      return MlsCreationResult.failure (eResponseCode);
    }

    // If an MlsTo value is in the DB, it is previously checked and valid
    final IParticipantIdentifier aMLSReceiverPID;
    if (StringHelper.isNotEmpty (aInboundTx.getMlsTo ()))
    {
      // DB value contains meta-scheme, scheme and value
      aMLSReceiverPID = aIF.parseParticipantIdentifier (aInboundTx.getMlsTo ());
      if (aMLSReceiverPID == null)
      {
        // Failed to build PID
        LOGGER.error ("Failed to parse MLS receiver participant ID '" + aInboundTx.getMlsTo () + "'");
        return MlsCreationResult.failure (eResponseCode);
      }
    }
    else
    {
      aMLSReceiverPID = aDefaultSpidReceiverPID;
    }

    final PeppolMLSBuilder aBuilder = aOutcome.getAsMLSBuilder ();
    aBuilder.randomID ()
            .issueDateTimeNow ()
            .senderParticipantID (aMLSSenderPID)
            .receiverParticipantID (aMLSReceiverPID)
            .referenceId (aInboundTx.getSbdhInstanceID ());
    final var aMls = aBuilder.build ();
    if (aMls == null)
    {
      // Failed to build MLS
      LOGGER.error ("Failed to build MLS data structure - see log for details");
      return MlsCreationResult.failure (eResponseCode);
    }

    // Serialize ApplicationResponse to XML
    final byte [] aMlsBytes = new PeppolMLSMarshaller ().getAsBytes (aMls);
    if (aMlsBytes == null)
    {
      // Failed to serialize MLS
      LOGGER.error ("Failed to serialize MLS to bytes - see log for details");
      return MlsCreationResult.failure (eResponseCode);
    }

    LOGGER.info ("Sending MLS from '" +
                 aBuilder.senderParticipantID ().getURIEncoded () +
                 "' to '" +
                 aBuilder.receiverParticipantID ().getURIEncoded () +
                 "'");

    final String sMlsSbdhInstanceID = PeppolSBDHData.createRandomSBDHInstanceIdentifier ();
    final OffsetDateTime aCreationDT = aTimestampMgr.getCurrentDateTimeUTC ();

    // Create an outbound transaction for the MLS response

    // Store MLS document to disk
    final String sDocumentPath = aDocPayloadMgr.storeDocument (APBasicConfig.getStorageOutboundPath (),
                                                               aCreationDT,
                                                               sMlsSbdhInstanceID + ".mls",
                                                               aMlsBytes);

    // MLS can never have an MLS_TO
    final String sMlsTo = null;

    // The SBDH parameters are not needed for SBDH
    final String sSbdhStandard = null;
    final String sSbdhTypeVersion = null;
    final String sSbdhType = null;
    final String sPayloadMimeType = null;

    // Custom fields do not apply to system-generated MLS responses
    final String sCustom1 = null;
    final String sCustom2 = null;
    final String sCustom3 = null;

    // Create outbound transaction
    final String sMlsTxID = aOutboundMgr.create (ETransactionType.MLS_RESPONSE,
                                                 aMLSSenderPID.getURIEncoded (),
                                                 aMLSReceiverPID.getURIEncoded (),
                                                 EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getURIEncoded (),
                                                 EPredefinedProcessIdentifier.urn_peppol_edec_mls.getURIEncoded (),
                                                 sMlsSbdhInstanceID,
                                                 ESourceType.PAYLOAD_ONLY,
                                                 sDocumentPath,
                                                 aMlsBytes.length,
                                                 HashHelper.sha256Hex (aMlsBytes),
                                                 APCoreConfig.getPeppolOwnerCountryCode (),
                                                 aCreationDT,
                                                 sMlsTo,
                                                 aInboundTx.getID (),
                                                 sSbdhStandard,
                                                 sSbdhTypeVersion,
                                                 sSbdhType,
                                                 sPayloadMimeType,
                                                 sCustom1,
                                                 sCustom2,
                                                 sCustom3);
    final var aMlsTx = aOutboundMgr.getByID (sMlsTxID);
    if (aMlsTx == null)
    {
      LOGGER.error ("Failed to submit outbound transaction");
      return MlsCreationResult.failure (eResponseCode);
    }

    // Update inbound with MLS fields
    if (aInboundMgr.updateMlsFields (aInboundTx.getID (), eResponseCode, sMlsTxID).isFailure ())
      LOGGER.error ("Failed to update MLS fields for inbound transaction '" + aInboundTx.getID () + "'");

    // Provide the default SPID as MLS fallback target in case the custom MLS_TO receiver is not
    // reachable via SMP (MLS SPOG section 5.4).
    final MlsSmpFallback aMlsFallback = new MlsSmpFallback (aDefaultSpidReceiverPID, aInboundTx.getSbdhInstanceID ());
    return MlsCreationResult.created (eResponseCode, aMlsTx, aMlsFallback);
  }

  /**
   * Send an already created MLS: hand out the optional copy and perform the AS4 transmission.
   *
   * @param aCreationResult
   *        The result of the MLS creation, which must carry an outbound MLS transaction. May not be
   *        <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  private static ESuccess _sendCreatedMls (@NonNull final MlsCreationResult aCreationResult)
  {
    final IOutboundTransaction aMlsTx = aCreationResult.mlsTx ();
    final EPeppolMLSResponseCode eResponseCode = aCreationResult.responseCode ();
    final MlsSmpFallback aMlsFallback = aCreationResult.smpFallback ();
    if (aMlsTx == null || eResponseCode == null || aMlsFallback == null)
    {
      LOGGER.error ("The MLS creation result contains no outbound transaction that could be sent");
      return ESuccess.FAILURE;
    }

    // Optionally hand C4 a copy of the MLS we are about to send - AP, AB and RE alike
    _forwardMlsCopy (aMlsTx, eResponseCode);

    // Perform actual sending
    final Phase4PeppolSendingReport aSendingReport = OutboundOrchestrator.processPendingOutbound ("[SubmitMLS] ",
                                                                                                  aMlsTx,
                                                                                                  aMlsFallback);
    return ESuccess.valueOf (aSendingReport.isOverallSuccess ());
  }

  /**
   * Correlate an incoming MLS to a previous outbound transaction.
   *
   * @param sLogPrefix
   *        Log prefix. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        SBDH Instance ID. May not be <code>null</code>.
   * @param eResponseCode
   *        The MLS response code received. May not be <code>null</code>.
   * @param aMlsAS4ReceivedDT
   *        The MLS AS4 receiving date time for the SLR. May not be <code>null</code>.
   * @param sMlsID
   *        The MLS document ID received. May not be <code>null</code>.
   * @param sMlsInboundTransactionID
   *        The transaction ID of the inbound MLS transaction. May not be <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  public static ESuccess handleIncomingMls (@NonNull final String sLogPrefix,
                                            @NonNull final String sSbdhInstanceID,
                                            @NonNull final EPeppolMLSResponseCode eResponseCode,
                                            @NonNull final OffsetDateTime aMlsAS4ReceivedDT,
                                            @Nullable final String sMlsID,
                                            @NonNull final String sMlsInboundTransactionID)
  {
    LOGGER.info (sLogPrefix +
                 "Received MLS response (" +
                 eResponseCode.getID () +
                 ") for SBDH '" +
                 sSbdhInstanceID +
                 "'");

    final IOutboundTransactionManager aOutboundMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundTransaction aTx = aOutboundMgr.getBySbdhInstanceID (sSbdhInstanceID);
    if (aTx == null)
    {
      LOGGER.warn (sLogPrefix + "No outbound transaction found for SBDH '" + sSbdhInstanceID + "'");
      return ESuccess.FAILURE;
    }

    final EMlsReceptionStatus eMlsStatus = switch (eResponseCode)
    {
      case ACCEPTANCE -> EMlsReceptionStatus.RECEIVED_AP;
      case ACKNOWLEDGING -> EMlsReceptionStatus.RECEIVED_AB;
      case REJECTION -> EMlsReceptionStatus.RECEIVED_RE;
    };

    // Store in DB
    if (aOutboundMgr.updateMlsStatus (aTx.getID (), eMlsStatus, aMlsAS4ReceivedDT, sMlsID, sMlsInboundTransactionID)
                    .isFailure ())
      return ESuccess.FAILURE;

    LOGGER.info (sLogPrefix +
                 "Updated MLS status for transaction '" +
                 aTx.getID () +
                 "' to '" +
                 eMlsStatus.getID () +
                 "'");

    // Compute round-trip duration if the original outbound send completion timestamp is known
    final OffsetDateTime aOutboundCompletedDT = aTx.getCompletedDT ();
    final Duration aRoundTrip = aOutboundCompletedDT != null ? Duration.between (aOutboundCompletedDT,
                                                                                 aMlsAS4ReceivedDT) : null;
    for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
      aHandler.onInboundMLSCorrelated (sMlsInboundTransactionID,
                                       sSbdhInstanceID,
                                       aTx.getID (),
                                       eResponseCode,
                                       eMlsStatus,
                                       sMlsID,
                                       aMlsAS4ReceivedDT,
                                       aRoundTrip);

    return ESuccess.SUCCESS;
  }
}
