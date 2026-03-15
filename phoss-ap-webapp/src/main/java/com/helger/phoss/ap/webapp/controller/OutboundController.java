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

import java.io.InputStream;
import java.util.List;

import org.jspecify.annotations.NonNull;
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

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.json.JsonValue;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.outbound.OutboundOrchestrator;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.phoss.ap.webapp.dto.OutboundTransactionResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping ("/api/outbound")
public class OutboundController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (OutboundController.class);

  @PostMapping (value = "/submit/{senderID}/{receiverID}/{docTypeID}/{processID}/{c1CountryCode}",
                produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity <String> submitRawDocument (@PathVariable ("senderID") final String sSenderID,
                                                    @PathVariable ("receiverID") final String sReceiverID,
                                                    @PathVariable ("docTypeID") final String sDocTypeID,
                                                    @PathVariable ("processID") final String sProcessID,
                                                    @PathVariable ("c1CountryCode") final String sC1CountryCode,
                                                    @NonNull final HttpServletRequest aServletRequest,
                                                    @RequestParam (value = "sbdhInstanceID",
                                                                   required = false) final String sSbdhInstanceID,
                                                    @RequestParam (value = "mlsTo",
                                                                   required = false) final String sMlsTo,
                                                    @RequestParam (value = "sbdhStandard",
                                                                   required = false) final String sSbdhStandard,
                                                    @RequestParam (value = "sbdhTypeVersion",
                                                                   required = false) final String sSbdhTypeVersion,
                                                    @RequestParam (value = "sbdhType",
                                                                   required = false) final String sSbdhType,
                                                    @RequestParam (value = "payloadMimeType",
                                                                   required = false) final String sPayloadMimeType) throws Exception
  {
    if (!APCoreConfig.isSendingEnabled ())
    {
      LOGGER.info ("Peppol AP sending is disabled");
      return ResponseEntity.notFound ().build ();
    }

    final String sEffectiveSbdhInstanceID = StringHelper.isNotEmpty (sSbdhInstanceID) ? sSbdhInstanceID : PeppolSBDHData
                                                                                                                        .createRandomSBDHInstanceIdentifier ();

    // Parse the identifiers
    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();

    // Start configuring here
    IParticipantIdentifier aSenderID = aIF.parseParticipantIdentifier (sSenderID);
    if (aSenderID == null)
    {
      // Fallback to default scheme
      aSenderID = aIF.createParticipantIdentifierWithDefaultScheme (sSenderID);
    }
    if (aSenderID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the sending participant ID '" + sSenderID + "'")
                                           .getAsJsonString ());
    }

    IParticipantIdentifier aReceiverID = aIF.parseParticipantIdentifier (sReceiverID);
    if (aReceiverID == null)
    {
      // Fallback to default scheme
      aReceiverID = aIF.createParticipantIdentifierWithDefaultScheme (sReceiverID);
    }
    if (aReceiverID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the receiving participant ID '" +
                                                    sReceiverID +
                                                    "'").getAsJsonString ());
    }

    IDocumentTypeIdentifier aDocTypeID = aIF.parseDocumentTypeIdentifier (sDocTypeID);
    if (aDocTypeID == null)
    {
      // Fallback to default scheme
      aDocTypeID = aIF.createDocumentTypeIdentifierWithDefaultScheme (sDocTypeID);
    }
    if (aDocTypeID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the document type ID '" + sDocTypeID + "'")
                                           .getAsJsonString ());
    }

    IProcessIdentifier aProcessID = aIF.parseProcessIdentifier (sProcessID);
    if (aProcessID == null)
    {
      // Fallback to default scheme
      aProcessID = aIF.createProcessIdentifierWithDefaultScheme (sProcessID);
    }
    if (aProcessID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the process ID '" + sProcessID + "'")
                                           .getAsJsonString ());
    }

    // Read the InputStream only once
    try (final InputStream aIS = aServletRequest.getInputStream ())
    {
      // Store in DB
      final IOutboundTransaction aTx = OutboundOrchestrator.submitRawDocument ("[SubmitRaw] ",
                                                                               aSenderID,
                                                                               aReceiverID,
                                                                               aDocTypeID,
                                                                               aProcessID,
                                                                               sEffectiveSbdhInstanceID,
                                                                               sC1CountryCode,
                                                                               aIS,
                                                                               sMlsTo,
                                                                               sSbdhStandard,
                                                                               sSbdhTypeVersion,
                                                                               sSbdhType,
                                                                               sPayloadMimeType);
      if (aTx == null)
      {
        return ResponseEntity.unprocessableContent ()
                             .body (JsonValue.create ("Failed to submit outbound transaction").getAsJsonString ());
      }

      // Perform actual sending
      final Phase4PeppolSendingReport aSendingReport = OutboundOrchestrator.processPendingOutbound ("[SubmitRaw] ",
                                                                                                    aTx);
      if (!aSendingReport.isOverallSuccess ())
      {
        return ResponseEntity.unprocessableContent ().body (aSendingReport.getAsJsonString ());
      }

      // Sending success
      return ResponseEntity.ok (aSendingReport.getAsJsonString ());
    }
  }

  @PostMapping (value = "/submit-sbd", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity <String> submitPrebuiltSBD (@NonNull final HttpServletRequest aServletRequest,
                                                    @RequestParam (value = "mlsTo",
                                                                   required = false) final String sMlsTo) throws Exception
  {
    if (!APCoreConfig.isSendingEnabled ())
    {
      LOGGER.info ("Peppol AP sending is disabled");
      return ResponseEntity.notFound ().build ();
    }

    // Read the InputStream only once
    try (final InputStream aIS = aServletRequest.getInputStream ())
    {
      // Store in DB
      final IOutboundTransaction aTx = OutboundOrchestrator.submitPrebuiltSBD ("[SubmitPrebuiltSBD] ", aIS, sMlsTo);
      if (aTx == null)
      {
        return ResponseEntity.badRequest ()
                             .body (JsonValue.create ("Failed to submit outbound SBD transaction").getAsJsonString ());
      }

      // Perform actual sending
      final Phase4PeppolSendingReport aSendingReport = OutboundOrchestrator.processPendingOutbound ("[SubmitPrebuiltSBD] ",
                                                                                                    aTx);
      if (!aSendingReport.isOverallSuccess ())
      {
        return ResponseEntity.unprocessableContent ().body (aSendingReport.getAsJsonString ());
      }

      // Sending success
      return ResponseEntity.ok (aSendingReport.getAsJsonString ());
    }
  }

  @GetMapping ("/status/{sbdhInstanceID}")
  public ResponseEntity <OutboundTransactionResponse> getStatus (@PathVariable ("sbdhInstanceID") final String sSbdhInstanceID)
  {
    LOGGER.info ("Checking for status of transmission with ID '" + sSbdhInstanceID + "'");

    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundTransaction aTx = aTxMgr.getBySbdhInstanceID (sSbdhInstanceID);
    if (aTx == null)
    {
      LOGGER.info ("No such transaction");
      return ResponseEntity.notFound ().build ();
    }

    return ResponseEntity.ok (OutboundTransactionResponse.fromDomain (aTx));
  }

  @GetMapping ("/in-transmission")
  public ResponseEntity <List <OutboundTransactionResponse>> getInTransmission ()
  {
    LOGGER.info ("Checking for all outbound transmissions in progress");

    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final var aTxs = aTxMgr.getAllInTransmission ();
    final ICommonsList <OutboundTransactionResponse> aResult = aTxs.getAllMapped (OutboundTransactionResponse::fromDomain);
    return ResponseEntity.ok (aResult);
  }
}
