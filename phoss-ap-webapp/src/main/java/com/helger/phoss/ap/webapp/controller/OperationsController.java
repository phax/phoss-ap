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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helger.base.state.ESuccess;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.dto.InboundTransactionResponse;
import com.helger.phoss.ap.api.dto.OutboundTransactionResponse;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.basic.APBasicMetaManager;
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
 * REST controller for operational tasks including transaction history querying,
 * payload retrieval, and manual inbound transaction replaying.
 *
 * @author Philip Helger
 */
@RestController
@RequestMapping ("/api/ops")
@Tag (name = "Operations", description = "Operational APIs for transaction history auditing, payload inspection, and manual re-forwarding.")
@SecurityRequirement (name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class OperationsController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (OperationsController.class);

  /**
   * Get historical inbound transactions with pagination.
   *
   * @param limit
   *        Maximum number of transactions to return.
   * @param offset
   *        Pagination offset.
   * @return A paginated list of historical inbound transactions.
   */
  @GetMapping ("/inbound/history")
  @Operation (summary = "Get historical inbound transactions", description = "Returns a paginated list of historical inbound transactions.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "List of transactions") })
  public ResponseEntity <List <InboundTransactionResponse>> getInboundHistory (@RequestParam (name = "limit", defaultValue = "50") final int limit,
                                                                               @RequestParam (name = "offset", defaultValue = "0") final int offset)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final var aTxs = aTxMgr.getAllTransactions (limit, offset);
    return ResponseEntity.ok (aTxs.getAllMapped (InboundTransactionResponse::fromDomain));
  }

  /**
   * Fetch the raw document payload for an inbound transaction.
   *
   * @param sbdhInstanceID
   *        The SBDH Instance ID.
   * @return The raw byte content of the document.
   */
  @GetMapping (value = "/inbound/{sbdhInstanceID}/payload", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation (summary = "Get the payload of an inbound transaction", description = "Returns the raw byte content of the document.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Payload content"),
                   @ApiResponse (responseCode = "404", description = "Transaction or payload not found", content = @Content) })
  public ResponseEntity <byte []> getInboundPayload (@Parameter (description = "SBDH Instance ID", required = true) @PathVariable ("sbdhInstanceID") final String sbdhInstanceID)
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
   * @return 200 on success, 404 if not found, 500 on failure.
   */
  @PostMapping ("/inbound/{sbdhInstanceID}/replay")
  @Operation (summary = "Replay an inbound transaction",
              description = "Forces a re-forwarding of an existing inbound transaction.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Transaction replay initiated"),
                   @ApiResponse (responseCode = "404", description = "Transaction not found", content = @Content) })
  public ResponseEntity <Void> replayInbound (@Parameter (description = "SBDH Instance ID", required = true) @PathVariable ("sbdhInstanceID") final String sbdhInstanceID)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = aTxMgr.getBySbdhInstanceIDIncludingArchive (sbdhInstanceID);
    if (aTx == null)
      return ResponseEntity.notFound ().build ();

    final ESuccess eSuccess = InboundOrchestrator.forwardDocument ("API Replay: ", aTx);
    if (eSuccess.isSuccess ())
      return ResponseEntity.ok ().build ();
    return ResponseEntity.internalServerError ().build ();
  }

  /**
   * Get historical outbound transactions with pagination.
   *
   * @param limit
   *        Maximum number of transactions to return.
   * @param offset
   *        Pagination offset.
   * @return A paginated list of historical outbound transactions.
   */
  @GetMapping ("/outbound/history")
  @Operation (summary = "Get historical outbound transactions", description = "Returns a paginated list of historical outbound transactions.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "List of transactions") })
  public ResponseEntity <List <OutboundTransactionResponse>> getOutboundHistory (@RequestParam (name = "limit", defaultValue = "50") final int limit,
                                                                                @RequestParam (name = "offset", defaultValue = "0") final int offset)
  {
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final var aTxs = aTxMgr.getAllTransactions (limit, offset);
    return ResponseEntity.ok (aTxs.getAllMapped (OutboundTransactionResponse::fromDomain));
  }

  /**
   * Fetch the raw document payload for an outbound transaction.
   *
   * @param sbdhInstanceID
   *        The SBDH Instance ID.
   * @return The raw byte content of the document.
   */
  @GetMapping (value = "/outbound/{sbdhInstanceID}/payload", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation (summary = "Get the payload of an outbound transaction", description = "Returns the raw byte content of the document.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Payload content"),
                   @ApiResponse (responseCode = "404", description = "Transaction or payload not found", content = @Content) })
  public ResponseEntity <byte []> getOutboundPayload (@Parameter (description = "SBDH Instance ID", required = true) @PathVariable ("sbdhInstanceID") final String sbdhInstanceID)
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
}
