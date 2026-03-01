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
package com.helger.phoss.ap.webapp;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.core.outbound.OutboundOrchestrator;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.phoss.ap.webapp.dto.OutboundTransactionResponse;
import com.helger.phoss.ap.webapp.dto.SubmitResponse;

@RestController
@RequestMapping ("/api/outbound")
public class OutboundController
{
  @PostMapping ("/submit")
  public ResponseEntity <SubmitResponse> submitRawDocument (@RequestParam ("senderID") final String sSenderID,
                                                            @RequestParam ("receiverID") final String sReceiverID,
                                                            @RequestParam ("docTypeID") final String sDocTypeID,
                                                            @RequestParam ("processID") final String sProcessID,
                                                            @RequestParam ("sbdhInstanceID") final String sSbdhInstanceID,
                                                            @RequestParam ("c1CountryCode") final String sC1CountryCode,
                                                            @RequestParam ("document") final MultipartFile aDocument,
                                                            @RequestParam (value = "mlsTo",
                                                                           required = false) final String sMlsTo) throws Exception
  {
    final byte [] aDocBytes = aDocument.getBytes ();
    final IOutboundTransaction aTx = OutboundOrchestrator.submitRawDocument (sSenderID,
                                                                             sReceiverID,
                                                                             sDocTypeID,
                                                                             sProcessID,
                                                                             sSbdhInstanceID,
                                                                             sC1CountryCode,
                                                                             aDocBytes,
                                                                             sMlsTo);
    if (aTx == null)
      return ResponseEntity.unprocessableContent ().body (new SubmitResponse (null, sSbdhInstanceID, "rejected"));

    // Perform actual sending
    OutboundOrchestrator.processPendingOutbound (aTx);

    return ResponseEntity.ok (new SubmitResponse (aTx.getID (), aTx.getSbdhInstanceID (), aTx.getStatus ().getID ()));
  }

  @PostMapping ("/submit-sbd")
  public ResponseEntity <SubmitResponse> submitPrebuiltSBD (@RequestParam ("senderID") final String sSenderID,
                                                            @RequestParam ("receiverID") final String sReceiverID,
                                                            @RequestParam ("docTypeID") final String sDocTypeID,
                                                            @RequestParam ("processID") final String sProcessID,
                                                            @RequestParam ("sbdhInstanceID") final String sSbdhInstanceID,
                                                            @RequestParam ("c1CountryCode") final String sC1CountryCode,
                                                            @RequestParam ("document") final MultipartFile aDocument,
                                                            @RequestParam (value = "mlsTo",
                                                                           required = false) final String sMlsTo) throws Exception
  {
    final byte [] aDocBytes = aDocument.getBytes ();
    final IOutboundTransaction aTx = OutboundOrchestrator.submitPrebuiltSBD (sSenderID,
                                                                             sReceiverID,
                                                                             sDocTypeID,
                                                                             sProcessID,
                                                                             sSbdhInstanceID,
                                                                             sC1CountryCode,
                                                                             aDocBytes,
                                                                             sMlsTo);
    if (aTx == null)
      return ResponseEntity.unprocessableContent ().body (new SubmitResponse (null, sSbdhInstanceID, "rejected"));

    // Perform actual sending
    OutboundOrchestrator.processPendingOutbound (aTx);

    return ResponseEntity.ok (new SubmitResponse (aTx.getID (), aTx.getSbdhInstanceID (), aTx.getStatus ().getID ()));
  }

  @GetMapping ("/status/{sbdhInstanceID}")
  public ResponseEntity <OutboundTransactionResponse> getStatus (@PathVariable final String sbdhInstanceID)
  {
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundTransaction aTx = aTxMgr.getBySbdhInstanceID (sbdhInstanceID);
    if (aTx == null)
      return ResponseEntity.notFound ().build ();

    return ResponseEntity.ok (OutboundTransactionResponse.fromDomain (aTx));
  }

  @GetMapping ("/in-transmission")
  public ResponseEntity <List <OutboundTransactionResponse>> getInTransmission ()
  {
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final var aTxs = aTxMgr.getAllInTransmission ();
    final ICommonsList <OutboundTransactionResponse> aResult = aTxs.getAllMapped (OutboundTransactionResponse::fromDomain);
    return ResponseEntity.ok (aResult);
  }
}
