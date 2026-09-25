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

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helger.base.state.ESuccess;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.ITransactionAuditManager;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.dto.CircuitBreakerResponse;
import com.helger.phoss.ap.api.dto.InboundTransactionResponse;
import com.helger.phoss.ap.api.dto.OutboundTransactionResponse;
import com.helger.phoss.ap.api.dto.TimelineEventItem;
import com.helger.phoss.ap.api.dto.TransactionTimelineResponse;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.api.model.ITransactionAuditItem;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.CircuitBreakerManager;

import com.helger.phoss.ap.core.inbound.InboundOrchestrator;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.phoss.ap.webapp.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for operational tasks including transaction history querying, payload retrieval,
 * and manual inbound transaction replaying.
 *
 * @author Philip Helger
 */
@RestController
@RequestMapping ("/api/ops")
@Tag (name = "Operations",
      description = "Operational APIs for transaction history auditing, payload inspection, and manual re-forwarding.")
@SecurityRequirement (name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class OperationsController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (OperationsController.class);

  private static String _resolveUser (@Nullable final String sHeaderUser)
  {
    if (sHeaderUser != null && !sHeaderUser.trim ().isEmpty ())
      return sHeaderUser.trim ();
    return "SYSTEM";
  }

  /**
   * Get historical inbound transactions with pagination.
   *
   * @param offset
   *        Pagination offset.
   * @param limit
   *        Maximum number of transactions to return.
   * @return A paginated list of historical inbound transactions.
   */
  @GetMapping ("/inbound/history")
  @Operation (summary = "Get historical inbound transactions",
              description = "Returns a paginated list of historical inbound transactions.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "List of transactions") })
  public ResponseEntity <List <InboundTransactionResponse>> getInboundHistory (@RequestParam (name = "offset",
                                                                                              defaultValue = "0") final int offset,
                                                                               @RequestParam (name = "limit",
                                                                                              defaultValue = "50") final int limit)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final var aTxs = aTxMgr.getAllTransactions (offset, limit);
    return ResponseEntity.ok (new ArrayList <> (aTxs.getAllMapped (InboundTransactionResponse::fromDomain)));
  }

  /**
   * Get the total count of active inbound transactions.
   *
   * @return The count of active (non-archived) inbound transactions.
   */
  /**
   * Get map of replayed inbound transactions and their replay counts.
   *
   * @return Map of SBDH instance ID to replay count.
   */
  @GetMapping ("/inbound/replayed-summary")
  @Operation (summary = "Get map of replayed inbound transactions and their replay counts",
              description = "Returns a map of SBDH Instance ID to replay count for all inbound transactions that have been replayed.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Summary returned") })
  public ResponseEntity <Map <String, Integer>> getInboundReplayedSummary ()
  {
    final ITransactionAuditManager aAuditMgr = APJdbcMetaManager.getTransactionAuditMgr ();
    final ICommonsMap <String, Integer> aCounts = aAuditMgr != null ? aAuditMgr.getReplayCountsBySbdhInstanceID () : null;
    return ResponseEntity.ok (aCounts != null ? new HashMap <> (aCounts) : Collections.emptyMap ());
  }

  @GetMapping ("/inbound/size")
  @Operation (summary = "Get total count of active inbound transactions",
              description = "Returns the count of active (non-archived) inbound transactions.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Transaction count") })
  public ResponseEntity <Long> getInboundSize ()
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    return ResponseEntity.ok (Long.valueOf (aTxMgr.getTransactionCount ()));
  }

  /**
   * Fetch the raw document payload for an inbound transaction.
   *
   * @param sbdhInstanceID
   *        The SBDH Instance ID.
   * @return The raw byte content of the document.
   */
  @GetMapping (value = "/inbound/{sbdhInstanceID}/payload", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation (summary = "Get the payload of an inbound transaction",
              description = "Returns the raw byte content of the document.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Payload content"),
                   @ApiResponse (responseCode = "404",
                                 description = "Transaction or payload not found",
                                 content = @Content) })
  public ResponseEntity <byte []> getInboundPayload (@Parameter (description = "SBDH Instance ID",
                                                                 required = true) @PathVariable ("sbdhInstanceID") final String sbdhInstanceID)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = aTxMgr.getBySbdhInstanceIDIncludingArchive (sbdhInstanceID);
    if (aTx == null)
      return ResponseEntity.notFound ().build ();

    try
    {
      final byte [] aBytes = APBasicMetaManager.getDocPayloadMgr ().readDocument (aTx.getDocumentPath ());
      return ResponseEntity.ok (aBytes);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to read payload for inbound transaction " + sbdhInstanceID, ex);
      return ResponseEntity.notFound ().build ();
    }
  }

  /**
   * Voluntarily replay (re-forward) an inbound transaction.
   *
   * @param sbdhInstanceID
   *        The SBDH Instance ID of the transaction to replay.
   * @param sAuditUser
   *        Operator username, ID, or email passed in the X-Audit-User header.
   * @return 200 on success, 404 if not found, 500 on failure.
   */
  @PostMapping ("/inbound/{sbdhInstanceID}/replay")
  @Operation (summary = "Replay an inbound transaction",
              description = "Forces a re-forwarding of an existing inbound transaction.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Transaction replay initiated"),
                   @ApiResponse (responseCode = "404", description = "Transaction not found", content = @Content) })
  public ResponseEntity <InboundTransactionResponse> replayInbound (@Parameter (description = "SBDH Instance ID",
                                                                                required = true) @PathVariable ("sbdhInstanceID") final String sbdhInstanceID,
                                                                    @RequestHeader (name = "X-Audit-User",
                                                                                    required = false) final String sAuditUser)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = aTxMgr.getBySbdhInstanceIDIncludingArchive (sbdhInstanceID);
    if (aTx == null)
      return ResponseEntity.notFound ().build ();

    final String sUser = _resolveUser (sAuditUser);

    // Record operator audit event
    final ITransactionAuditManager aAuditMgr = APJdbcMetaManager.getTransactionAuditMgr ();
    if (aAuditMgr != null)
    {
      aAuditMgr.recordAudit (aTx.getID (),
                             sbdhInstanceID,
                             "INBOUND",
                             "OPERATOR_ACTION",
                             "REPLAY_INBOUND",
                             aTx.getStatus () != null ? aTx.getStatus ().getID () : null,
                             null,
                             Integer.valueOf (aTx.getAttemptCount ()),
                             sUser,
                             "Manual operator replay initiated");
    }

    final ESuccess eSuccess = InboundOrchestrator.forwardDocument ("API Replay: ", aTx);
    final IInboundTransaction aUpdatedTx = aTxMgr.getBySbdhInstanceIDIncludingArchive (sbdhInstanceID);
    final IInboundTransaction aFinalTx = aUpdatedTx != null ? aUpdatedTx : aTx;

    if (eSuccess.isSuccess ())
      return ResponseEntity.ok (InboundTransactionResponse.fromDomain (aFinalTx));
    return ResponseEntity.internalServerError ().body (InboundTransactionResponse.fromDomain (aFinalTx));
  }

  public ResponseEntity <InboundTransactionResponse> replayInbound (final String sbdhInstanceID)
  {
    return replayInbound (sbdhInstanceID, null);
  }

  /**
   * Re-verify an inbound transaction whose verification was deferred and forward it to C4.
   *
   * @param sbdhInstanceID
   *        The SBDH Instance ID of the transaction to re-verify and forward.
   * @return 200 on success, 404 if not found, 409 if the transaction is not in status
   *         <code>verification_deferred</code>, 500 if the verification or the forwarding failed.
   * @since 0.12.0
   */
  @PostMapping ("/inbound/{sbdhInstanceID}/reverify-and-forward")
  @Operation (summary = "Re-verify and forward an inbound transaction",
              description = "Re-evaluates all registered inbound document verifiers against the stored payload and forwards the document, if they all accept it. This is the manual trigger for documents in status 'verification_deferred'; transactions in any other status are rejected with 409.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Transaction re-verified and forwarded"),
                   @ApiResponse (responseCode = "404", description = "Transaction not found", content = @Content),
                   @ApiResponse (responseCode = "409",
                                 description = "Transaction is not in status 'verification_deferred'",
                                 content = @Content),
                   @ApiResponse (responseCode = "500",
                                 description = "Re-verification or forwarding failed",
                                 content = @Content) })
  public ResponseEntity <InboundTransactionResponse> reverifyAndForwardInbound (@Parameter (description = "SBDH Instance ID",
                                                                                            required = true) @PathVariable ("sbdhInstanceID") final String sbdhInstanceID,
                                                                                @RequestHeader (name = "X-Audit-User",
                                                                                                required = false) final String sAuditUser)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    // Deliberately not looking into the archive - an archived transaction is done
    final IInboundTransaction aTx = aTxMgr.getBySbdhInstanceID (sbdhInstanceID);
    if (aTx == null)
      return ResponseEntity.notFound ().build ();

    // Only a deferred verification may be resumed. Re-verifying anything else would forward an
    // already forwarded document a second time and could send an MLS that contradicts the one that
    // was already sent for this document
    if (aTx.getStatus () != EInboundStatus.VERIFICATION_DEFERRED)
    {
      LOGGER.warn ("The inbound transaction '" +
                   sbdhInstanceID +
                   "' is in status '" +
                   aTx.getStatus ().getID () +
                   "' and not in status '" +
                   EInboundStatus.VERIFICATION_DEFERRED.getID () +
                   "' - not re-verifying it");
      return ResponseEntity.status (HttpStatus.CONFLICT).body (InboundTransactionResponse.fromDomain (aTx));
    }

    final String sUser = _resolveUser (sAuditUser);

    // Record operator audit event
    final ITransactionAuditManager aAuditMgr = APJdbcMetaManager.getTransactionAuditMgr ();
    if (aAuditMgr != null)
    {
      aAuditMgr.recordAudit (aTx.getID (),
                             sbdhInstanceID,
                             "INBOUND",
                             "OPERATOR_ACTION",
                             "REVERIFY_AND_FORWARD_INBOUND",
                             aTx.getStatus ().getID (),
                             null,
                             Integer.valueOf (aTx.getAttemptCount ()),
                             sUser,
                             "Manual operator reverify and forward initiated");
    }

    final ESuccess eSuccess = InboundOrchestrator.resumeDeferredInboundDocument ("API ReverifyAndForward: ", aTx);
    final IInboundTransaction aUpdatedTx = aTxMgr.getBySbdhInstanceID (sbdhInstanceID);
    final IInboundTransaction aFinalTx = aUpdatedTx != null ? aUpdatedTx : aTx;

    if (eSuccess.isSuccess ())
      return ResponseEntity.ok (InboundTransactionResponse.fromDomain (aFinalTx));
    return ResponseEntity.internalServerError ().body (InboundTransactionResponse.fromDomain (aFinalTx));
  }

  public ResponseEntity <InboundTransactionResponse> reverifyAndForwardInbound (final String sbdhInstanceID)
  {
    return reverifyAndForwardInbound (sbdhInstanceID, null);
  }

  /**
   * Get the full lifecycle and audit timeline for an inbound transaction.
   *
   * @param sbdhInstanceID
   *        The SBDH Instance ID of the transaction.
   * @return 200 with the timeline, or 404 if not found.
   */
  @GetMapping ("/inbound/{sbdhInstanceID}/timeline")
  @Operation (summary = "Get inbound transaction timeline",
              description = "Returns the chronological lifecycle status transitions and operator audit history for an inbound transaction.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Timeline returned"),
                   @ApiResponse (responseCode = "404", description = "Transaction not found", content = @Content) })
  public ResponseEntity <TransactionTimelineResponse> getInboundTimeline (@Parameter (description = "SBDH Instance ID",
                                                                                      required = true) @PathVariable ("sbdhInstanceID") final String sbdhInstanceID)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = aTxMgr.getBySbdhInstanceIDIncludingArchive (sbdhInstanceID);
    final ITransactionAuditManager aAuditMgr = APJdbcMetaManager.getTransactionAuditMgr ();
    final ICommonsList <ITransactionAuditItem> aAuditItems = aAuditMgr != null ? aAuditMgr.getTimelineBySbdhInstanceID (sbdhInstanceID)
                                                                               : null;

    if (aTx == null && (aAuditItems == null || aAuditItems.isEmpty ()))
      return ResponseEntity.notFound ().build ();

    final List <TimelineEventItem> aEvents = new ArrayList <> ();
    if (aAuditItems != null)
      for (final ITransactionAuditItem aItem : aAuditItems)
        aEvents.add (new TimelineEventItem (aItem));

    return ResponseEntity.ok (new TransactionTimelineResponse (sbdhInstanceID, "INBOUND", aEvents));
  }

  /**
   * Get historical outbound transactions with pagination.
   *
   * @param offset
   *        Pagination offset.
   * @param limit
   *        Maximum number of transactions to return.
   * @return A paginated list of historical outbound transactions.
   */
  @GetMapping ("/outbound/history")
  @Operation (summary = "Get historical outbound transactions",
              description = "Returns a paginated list of historical outbound transactions.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "List of transactions") })
  public ResponseEntity <List <OutboundTransactionResponse>> getOutboundHistory (@RequestParam (name = "offset",
                                                                                                defaultValue = "0") final int offset,
                                                                                 @RequestParam (name = "limit",
                                                                                                defaultValue = "50") final int limit)
  {
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final var aTxs = aTxMgr.getAllTransactions (offset, limit);
    return ResponseEntity.ok (new ArrayList <> (aTxs.getAllMapped (OutboundTransactionResponse::fromDomain)));
  }

  /**
   * Get the total count of active outbound transactions.
   *
   * @return The count of active (non-archived) outbound transactions.
   */
  @GetMapping ("/outbound/size")
  @Operation (summary = "Get total count of active outbound transactions",
              description = "Returns the count of active (non-archived) outbound transactions.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Transaction count") })
  public ResponseEntity <Long> getOutboundSize ()
  {
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    return ResponseEntity.ok (Long.valueOf (aTxMgr.getTransactionCount ()));
  }

  /**
   * Fetch the raw document payload for an outbound transaction.
   *
   * @param sbdhInstanceID
   *        The SBDH Instance ID.
   * @return The raw byte content of the document.
   */
  @GetMapping (value = "/outbound/{sbdhInstanceID}/payload", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation (summary = "Get the payload of an outbound transaction",
              description = "Returns the raw byte content of the document.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Payload content"),
                   @ApiResponse (responseCode = "404",
                                 description = "Transaction or payload not found",
                                 content = @Content) })
  public ResponseEntity <byte []> getOutboundPayload (@Parameter (description = "SBDH Instance ID",
                                                                  required = true) @PathVariable ("sbdhInstanceID") final String sbdhInstanceID)
  {
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundTransaction aTx = aTxMgr.getBySbdhInstanceIDIncludingArchive (sbdhInstanceID);
    if (aTx == null)
      return ResponseEntity.notFound ().build ();

    try
    {
      final byte [] aBytes = APBasicMetaManager.getDocPayloadMgr ().readDocument (aTx.getDocumentPath ());
      return ResponseEntity.ok (aBytes);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to read payload for outbound transaction " + sbdhInstanceID, ex);
      return ResponseEntity.notFound ().build ();
    }
  }

  /**
   * Get the full lifecycle and audit timeline for an outbound transaction.
   *
   * @param sbdhInstanceID
   *        The SBDH Instance ID of the transaction.
   * @return 200 with the timeline, or 404 if not found.
   */
  @GetMapping ("/outbound/{sbdhInstanceID}/timeline")
  @Operation (summary = "Get outbound transaction timeline",
              description = "Returns the chronological lifecycle status transitions and operator audit history for an outbound transaction.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Timeline returned"),
                   @ApiResponse (responseCode = "404", description = "Transaction not found", content = @Content) })
  public ResponseEntity <TransactionTimelineResponse> getOutboundTimeline (@Parameter (description = "SBDH Instance ID",
                                                                                        required = true) @PathVariable ("sbdhInstanceID") final String sbdhInstanceID)
  {
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundTransaction aTx = aTxMgr.getBySbdhInstanceIDIncludingArchive (sbdhInstanceID);
    final ITransactionAuditManager aAuditMgr = APJdbcMetaManager.getTransactionAuditMgr ();
    final ICommonsList <ITransactionAuditItem> aAuditItems = aAuditMgr != null ? aAuditMgr.getTimelineBySbdhInstanceID (sbdhInstanceID)
                                                                               : null;

    if (aTx == null && (aAuditItems == null || aAuditItems.isEmpty ()))
      return ResponseEntity.notFound ().build ();

    final List <TimelineEventItem> aEvents = new ArrayList <> ();
    if (aAuditItems != null)
      for (final ITransactionAuditItem aItem : aAuditItems)
        aEvents.add (new TimelineEventItem (aItem));

    return ResponseEntity.ok (new TransactionTimelineResponse (sbdhInstanceID, "OUTBOUND", aEvents));
  }

  /**
   * List all known circuit breakers and their current state.
   *
   * @return The list of all circuit breakers that were used at least once.
   * @since 0.13.0
   */
  @GetMapping ("/circuit-breakers")
  @Operation (summary = "List all circuit breakers",
              description = "Returns the current state of every circuit breaker that was used at least once - which remote system is guarded, whether it is currently suspended, since when, for how much longer, how many failures were counted and what the last failure was.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "List of circuit breakers") })
  public ResponseEntity <List <CircuitBreakerResponse>> getAllCircuitBreakers ()
  {
    final var aInfos = CircuitBreakerManager.getAllInfos ();
    return ResponseEntity.ok (new ArrayList <> (aInfos.getAllMapped (CircuitBreakerResponse::fromDomain)));
  }

  /**
   * Reset a single circuit breaker, so that the guarded remote system is contacted again
   * immediately.
   *
   * @param circuitKey
   *        The key of the circuit breaker to reset.
   * @return 200 on success, 404 if no circuit breaker with that key is known.
   * @since 0.13.0
   */
  @PostMapping ("/circuit-breakers/{circuitKey}/reset")
  @Operation (summary = "Reset a circuit breaker",
              description = "Forgets the circuit breaker with the provided key, so that the next call creates a new, closed one from the current configuration. Use this if a suspended SMP or AP is known to be healthy again and the remaining open duration should not be waited out.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Circuit breaker was reset"),
                   @ApiResponse (responseCode = "404", description = "Circuit breaker not found", content = @Content) })
  public ResponseEntity <Void> resetCircuitBreaker (@Parameter (description = "Circuit breaker key, e.g. 'smp$https://smp.example.org'",
                                                                required = true) @PathVariable ("circuitKey") final String circuitKey)
  {
    if (!CircuitBreakerManager.reset (circuitKey))
      return ResponseEntity.notFound ().build ();

    LOGGER.info ("The circuit breaker '" + circuitKey + "' was reset via the Operations API");
    return ResponseEntity.ok ().build ();
  }
}
