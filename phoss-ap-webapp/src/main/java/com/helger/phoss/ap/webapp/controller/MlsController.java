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
package com.helger.phoss.ap.webapp.controller;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.mls.CPeppolMLS;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.mls.EPeppolMLSStatusReasonCode;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.dto.InboundTransactionResponse;
import com.helger.phoss.ap.api.dto.MlsSendIssue;
import com.helger.phoss.ap.api.dto.MlsSendRequest;
import com.helger.phoss.ap.api.dto.MlsSlaEntryResponse;
import com.helger.phoss.ap.api.dto.MlsSlaReportResponse;
import com.helger.phoss.ap.api.dto.ReportResponse;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.model.MlsOutcomeIssue;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.inbound.InboundOrchestrator;
import com.helger.phoss.ap.core.mls.MlsCreationResult;
import com.helger.phoss.ap.core.mls.MlsHandler;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.phoss.ap.db.MlsMetricsManagerJdbc;
import com.helger.phoss.ap.db.MlsMetricsManagerJdbc.MlsSlaReport;
import com.helger.phoss.ap.webapp.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for MLS (Message Level Status) related operations including triggering the MLS of
 * an inbound transaction, querying transactions with missing MLS responses and retrieving MLS SLA
 * compliance reports per Peppol Network Policy.
 *
 * @author Philip Helger
 */
@RestController
@RequestMapping ("/api/mls")
@Tag (name = "MLS",
      description = "Message Level Status — backend triggered MLS, missing responses and SLA reports per Peppol Network Policy")
@SecurityRequirement (name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class MlsController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MlsController.class);

  /**
   * Get all inbound business document transactions for which no MLS response has been sent yet.
   *
   * @return List of inbound transactions without MLS response.
   */
  @GetMapping ("/missing")
  @Operation (summary = "List inbound transactions missing an MLS response",
              description = "Returns all inbound business document transactions for which no MLS response has been sent yet " +
                            "(mls_response_code IS NULL). Excludes incoming MLS messages themselves.")
  @ApiResponses ({ @ApiResponse (responseCode = "200",
                                 description = "List of inbound transactions without an MLS response"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content) })
  public ResponseEntity <List <InboundTransactionResponse>> getMissingMls ()
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();

    final var aTxs = aTxMgr.getAllWithoutMlsResponse ();
    final ICommonsList <InboundTransactionResponse> aResult = aTxs.getAllMapped (InboundTransactionResponse::fromDomain);
    return ResponseEntity.ok (aResult);
  }

  /**
   * Create a response DTO from a domain model MLS SLA report. This factory method depends on
   * {@code phoss-ap-db} types and therefore stays in the webapp module.
   *
   * @param aReport
   *        The MLS SLA report. May not be <code>null</code>.
   * @return A new response DTO. Never <code>null</code>.
   */
  @NonNull
  private static MlsSlaReportResponse _fromDomain (@NonNull final MlsSlaReport aReport)
  {
    final MlsSlaReportResponse ret = new MlsSlaReportResponse ();
    ret.setTotalCount (aReport.totalCount ());
    ret.setWithinSlaCount (aReport.withinSlaCount ());
    ret.setCompliancePercent (aReport.compliancePercent ());
    ret.setTargetPercent (aReport.targetPercent ());
    ret.setThresholdSeconds (aReport.thresholdSeconds ());
    ret.setMeetingSla (aReport.isMeetingSla ());
    ret.setEntries (aReport.entries ()
                           .getAllMapped (e -> new MlsSlaEntryResponse (e.sbdhInstanceID (),
                                                                        e.m1 ().toString (),
                                                                        e.m2OrM3 ().toString (),
                                                                        e.durationSeconds (),
                                                                        e.withinSla ())));
    return ret;
  }

  /**
   * Get MLS-1 SLA report (receiving side). Measures M2 - M1: time between receiving the original
   * business document (M1) and successfully sending back the MLS response (M2). SLR: 99.5% within
   * 20 minutes.
   *
   * @return The MLS-1 SLA report.
   */
  @GetMapping ("/sla/mls1")
  @Operation (summary = "MLS-1 SLA report (receiving side)",
              description = "Measures M2 - M1: time between receiving the original business document (M1) and successfully " +
                            "sending back the MLS response (M2). Per Peppol Network Policy: 99.5% must be within 20 minutes.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "MLS-1 SLA report"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content) })
  public ResponseEntity <MlsSlaReportResponse> getMls1Sla ()
  {
    final MlsMetricsManagerJdbc aMetricsMgr = APJdbcMetaManager.getMlsMetricsMgr ();

    final var aReport = aMetricsMgr.getMls1Report ();
    return ResponseEntity.ok (_fromDomain (aReport));
  }

  /**
   * Get MLS-2 SLA report (sending side). Measures M3 - M1: time between successfully sending the
   * business document (M1) and receiving the MLS response from C3 (M3). SLR: 99.5% within 25
   * minutes.
   *
   * @return The MLS-2 SLA report.
   */
  @GetMapping ("/sla/mls2")
  @Operation (summary = "MLS-2 SLA report (sending side)",
              description = "Measures M3 - M1: time between successfully sending the business document (M1) and receiving " +
                            "the MLS response from C3 (M3). Per Peppol Network Policy: 99.5% must be within 25 minutes.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "MLS-2 SLA report"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content) })
  public ResponseEntity <MlsSlaReportResponse> getMls2Sla ()
  {
    final MlsMetricsManagerJdbc aMetricsMgr = APJdbcMetaManager.getMlsMetricsMgr ();

    final var aReport = aMetricsMgr.getMls2Report ();
    return ResponseEntity.ok (_fromDomain (aReport));
  }

  /**
   * Build the response of the MLS sending endpoint.
   *
   * @param eStatus
   *        The HTTP status to use. May not be <code>null</code>.
   * @param sTransactionID
   *        The ID of the affected inbound transaction. May be <code>null</code> if it is unknown.
   * @param sStatus
   *        The machine readable status keyword. May not be <code>null</code>.
   * @param sMessage
   *        The human readable message. May not be <code>null</code>.
   * @return The response entity. Never <code>null</code>.
   */
  @NonNull
  private static ResponseEntity <ReportResponse> _mlsSendResponse (@NonNull final HttpStatus eStatus,
                                                                   @Nullable final String sTransactionID,
                                                                   @NonNull final String sStatus,
                                                                   @NonNull final String sMessage)
  {
    return ResponseEntity.status (eStatus).body (new ReportResponse (sTransactionID, sStatus, sMessage));
  }

  /**
   * Trigger the MLS of a previously received inbound document.
   *
   * @param aRequest
   *        The JSON request body carrying the SBDH Instance Identifier and the MLS status to
   *        report. May not be <code>null</code>.
   * @return A {@link ReportResponse} describing the outcome.
   * @since 0.13.0
   */
  @PostMapping (value = "/send",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation (summary = "Trigger the MLS of an inbound transaction",
              description = "Creates, persists and sends the MLS of a previously received inbound business document. " +
                            "Called by the Receiver Backend once it knows the outcome of the delivery to C4. " +
                            "This is the counterpart of 'mls.sending.trigger=api', in which the AP does not send the " +
                            "positive MLS on its own after a successful forwarding - but the endpoint works in the " +
                            "trigger mode 'auto' as well, as long as no MLS was determined for the transaction yet. " +
                            "The endpoint returns as soon as the outbound MLS transaction is persisted; the AS4 " +
                            "transmission and its retries happen in the background. " +
                            "Note that with 'mls.type=FAILURE_ONLY' a submitted 'AP' or 'AB' is recorded but not sent - " +
                            "the response carries the status 'recorded' in that case. Since 0.13.0.")
  @ApiResponses ({ @ApiResponse (responseCode = "200",
                                 description = "MLS created and handed to the outbound sender (status 'sent'), or " +
                                               "recorded only because of the MLS type of the transaction (status 'recorded')"),
                   @ApiResponse (responseCode = "400",
                                 description = "Missing SBDH Instance Identifier, unknown response code, unknown status " +
                                               "reason code, incomplete issue, or 'RE' without any issue"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content),
                   @ApiResponse (responseCode = "404",
                                 description = "No inbound transaction with the given SBDH Instance ID"),
                   @ApiResponse (responseCode = "409",
                                 description = "An MLS was already determined for this transaction"),
                   @ApiResponse (responseCode = "422",
                                 description = "The transaction is itself an MLS or MLR document and is never answered with an MLS"),
                   @ApiResponse (responseCode = "500", description = "The MLS could not be created"),
                   @ApiResponse (responseCode = "503", description = "MLS sending is globally disabled") })
  public ResponseEntity <ReportResponse> sendMls (@RequestBody final MlsSendRequest aRequest)
  {
    // Global MLS kill switch
    if (!APCoreConfig.isMlsSendingEnabled ())
    {
      return _mlsSendResponse (HttpStatus.SERVICE_UNAVAILABLE,
                               null,
                               "disabled",
                               "MLS sending is globally disabled via '" +
                                            APConfigurationProperties.MLS_SENDING_ENABLED +
                                            "'");
    }

    if (StringHelper.isEmpty (aRequest.getSbdhInstanceID ()))
      return _mlsSendResponse (HttpStatus.BAD_REQUEST, null, "invalid", "The field 'sbdhInstanceID' is mandatory");

    final EPeppolMLSResponseCode eResponseCode = EPeppolMLSResponseCode.getFromIDOrNull (aRequest.getResponseCode ());
    if (eResponseCode == null)
    {
      return _mlsSendResponse (HttpStatus.BAD_REQUEST,
                               null,
                               "invalid",
                               "The field 'responseCode' must be one of 'AP', 'AB' or 'RE' but is '" +
                                            aRequest.getResponseCode () +
                                            "'");
    }

    // Map the issues to the domain model. MLS allows line responses on a positive response code as
    // well, so they are not limited to a rejection
    final ICommonsList <MlsOutcomeIssue> aIssues = new CommonsArrayList <> ();
    if (aRequest.getIssues () != null)
      for (final MlsSendIssue aIssue : aRequest.getIssues ())
      {
        if (aIssue == null)
          continue;

        final EPeppolMLSStatusReasonCode eStatusReasonCode = EPeppolMLSStatusReasonCode.getFromIDOrNull (aIssue.getStatusReasonCode ());
        if (eStatusReasonCode == null)
        {
          return _mlsSendResponse (HttpStatus.BAD_REQUEST,
                                   null,
                                   "invalid",
                                   "The 'statusReasonCode' of an issue must be one of 'BV', 'BW', 'FD' or 'SV' but is '" +
                                                aIssue.getStatusReasonCode () +
                                                "'");
        }

        if (StringHelper.isEmpty (aIssue.getErrorField ()))
        {
          return _mlsSendResponse (HttpStatus.BAD_REQUEST,
                                   null,
                                   "invalid",
                                   "The 'errorField' of an issue is mandatory - use '" +
                                                CPeppolMLS.LINE_ID_NOT_AVAILABLE +
                                                "' if no location can be given");
        }

        if (StringHelper.isEmpty (aIssue.getDescription ()))
          return _mlsSendResponse (HttpStatus.BAD_REQUEST, null, "invalid", "The 'description' of an issue is mandatory");

        aIssues.add (new MlsOutcomeIssue (aIssue.getErrorField (), eStatusReasonCode, aIssue.getDescription ()));
      }

    // A rejection must name at least one reason
    if (eResponseCode == EPeppolMLSResponseCode.REJECTION && aIssues.isEmpty ())
    {
      return _mlsSendResponse (HttpStatus.BAD_REQUEST,
                               null,
                               "invalid",
                               "An MLS with the response code '" +
                                            EPeppolMLSResponseCode.REJECTION.getID () +
                                            "' requires at least one issue");
    }

    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    // Deliberately not looking into the archive - an archived transaction is done
    final IInboundTransaction aTx = aTxMgr.getBySbdhInstanceID (aRequest.getSbdhInstanceID ());
    if (aTx == null)
    {
      return _mlsSendResponse (HttpStatus.NOT_FOUND,
                               null,
                               "not-found",
                               "No inbound transaction with the SBDH Instance Identifier '" +
                                            aRequest.getSbdhInstanceID () +
                                            "'");
    }

    // An MLS is never answered with an MLS
    if (CPhossAP.isMLS (aTx.getDocTypeID (), aTx.getProcessID ()) ||
        CPhossAP.isMLR (aTx.getDocTypeID (), aTx.getProcessID ()))
    {
      return _mlsSendResponse (HttpStatus.UNPROCESSABLE_ENTITY,
                               aTx.getID (),
                               "not-eligible",
                               "The inbound transaction is an MLS or an MLR document and is never answered with an MLS");
    }

    // Peppol expects exactly one MLS per business document
    if (aTx.getMlsResponseCode () != null)
    {
      return _mlsSendResponse (HttpStatus.CONFLICT,
                               aTx.getID (),
                               "conflict",
                               "The MLS '" +
                                            aTx.getMlsResponseCode ().getID () +
                                            "' was already determined for this inbound transaction");
    }

    // A document that was forwarded although it was rejected already got its negative MLS (RE)
    if (InboundOrchestrator.isMlsSuppressedAfterRejection (aTx))
    {
      return _mlsSendResponse (HttpStatus.CONFLICT,
                               aTx.getID (),
                               "conflict",
                               "The inbound transaction was rejected by the verification - C2 already received the negative MLS of that rejection");
    }

    LOGGER.info ("Received the API triggered MLS (" +
                 eResponseCode.getID () +
                 ") for inbound transaction '" +
                 aTx.getID () +
                 "' with SBDH ID '" +
                 aTx.getSbdhInstanceID () +
                 "'");

    final MlsOutcome aOutcome = new MlsOutcome (eResponseCode, aRequest.getResponseText (), aIssues);
    final MlsCreationResult aCreationResult = MlsHandler.createInboundResultMls (aTx, aOutcome);
    if (aCreationResult.isFailure ())
    {
      return _mlsSendResponse (HttpStatus.INTERNAL_SERVER_ERROR,
                               aTx.getID (),
                               "failed",
                               "Failed to create the MLS - see the server log for details");
    }

    if (!aCreationResult.hasMlsTx ())
    {
      // The MLS type of the transaction does not want this MLS on the wire - the response code was
      // recorded nevertheless
      return _mlsSendResponse (HttpStatus.OK,
                               aTx.getID (),
                               "recorded",
                               "The MLS '" +
                                       eResponseCode.getID () +
                                       "' was recorded but not sent, because the MLS type of the transaction is '" +
                                       aTx.getMlsType ().getID () +
                                       "'");
    }

    // The AS4 transmission and its retries happen in the background
    MlsHandler.sendCreatedMlsAsync (aCreationResult);

    return _mlsSendResponse (HttpStatus.OK,
                             aTx.getID (),
                             "sent",
                             "The MLS '" +
                                     eResponseCode.getID () +
                                     "' was created as outbound transaction '" +
                                     aCreationResult.getMlsTxID () +
                                     "'");
  }
}
