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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.core.reporting.APPeppolReportingHelper;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.phoss.ap.webapp.dto.InboundTransactionResponse;
import com.helger.phoss.ap.webapp.dto.ReportResponse;

/**
 * REST controller for inbound transaction operations including reporting the C4
 * country code, querying transaction status by SBDH Instance ID, and listing
 * all transactions currently in processing.
 *
 * @author Philip Helger
 */
@RestController
@RequestMapping ("/api/inbound")
public class InboundController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (InboundController.class);

  /**
   * Store the C4 country code for a received inbound transaction and create the
   * corresponding Peppol Reporting entry.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance ID of the transaction. May not be
   *        <code>null</code>.
   * @param sC4CountryCode
   *        The C4 country code to store. May not be <code>null</code>.
   * @return A {@link ReportResponse} on success, 404 if the transaction does
   *         not exist, or 400 if the C4 country code was already set.
   */
  @PostMapping ("/report")
  public ResponseEntity <ReportResponse> reportInbound (@RequestParam ("sbdhInstanceID") final String sSbdhInstanceID,
                                                        @RequestParam ("c4CountryCode") final String sC4CountryCode)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();

    // Does a transaction exist for the provided SBDH Instance ID?
    final IInboundTransaction aTx = aTxMgr.getBySbdhInstanceID (sSbdhInstanceID);
    if (aTx == null)
      return ResponseEntity.notFound ().build ();

    // Does the transaction already have a C4 Country Code?
    if (StringHelper.isNotEmpty (aTx.getC4CountryCode ()))
      return ResponseEntity.badRequest ().build ();

    LOGGER.info ("Storing C4 Country Code '" +
                 sC4CountryCode +
                 "' to inbound transaction '" +
                 aTx.getID () +
                 "' with SBDH ID '" +
                 sSbdhInstanceID +
                 "'");

    // Store the country code for C4 and create the reporting entry
    aTxMgr.updateC4CountryCode (aTx.getID (), sC4CountryCode);
    APPeppolReportingHelper.createInboundPeppolReportingItem (aTx.getID ());

    return ResponseEntity.ok (new ReportResponse (aTx.getID (),
                                                  "updated",
                                                  "C4 country code set to '" + sC4CountryCode + "'"));
  }

  /**
   * Get the current status of an inbound transaction by its SBDH Instance ID.
   *
   * @param sbdhInstanceID
   *        The SBDH Instance ID to look up.
   * @return The transaction details, or 404 if not found.
   */
  @GetMapping ("/status/{sbdhInstanceID}")
  public ResponseEntity <InboundTransactionResponse> getStatus (@PathVariable final String sbdhInstanceID)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = aTxMgr.getBySbdhInstanceID (sbdhInstanceID);
    if (aTx == null)
      return ResponseEntity.notFound ().build ();

    return ResponseEntity.ok (InboundTransactionResponse.fromDomain (aTx));
  }

  /**
   * Get all inbound transactions that are currently being processed (not yet
   * completed or permanently failed).
   *
   * @return A list of in-processing inbound transactions.
   */
  @GetMapping ("/in-processing")
  public ResponseEntity <List <InboundTransactionResponse>> getInProcessing ()
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final var aTxs = aTxMgr.getAllInProcessing ();

    final ICommonsList <InboundTransactionResponse> aResult = aTxs.getAllMapped (InboundTransactionResponse::fromDomain);
    return ResponseEntity.ok (aResult);
  }
}
