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

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.core.reporting.ReportingManager;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.phoss.ap.webapp.dto.InboundTransactionResponse;
import com.helger.phoss.ap.webapp.dto.ReportResponse;

@RestController
@RequestMapping ("/api/inbound")
public class InboundController
{
  @PostMapping ("/report")
  public ResponseEntity <ReportResponse> reportInbound (@RequestParam ("sbdhInstanceID") final String sSbdhInstanceID,
                                                        @RequestParam ("c4CountryCode") final String sC4CountryCode)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = aTxMgr.getBySbdhInstanceID (sSbdhInstanceID);
    if (aTx == null)
      return ResponseEntity.notFound ().build ();

    // Does the transaction already have a C4 Country Code?
    if (StringHelper.isNotEmpty (aTx.getC4CountryCode ()))
      return ResponseEntity.badRequest ().build ();

    // Store the country code for C4 and create the reporting entry
    aTxMgr.updateC4CountryCode (aTx.getID (), sC4CountryCode);
    ReportingManager.storeInboundForReporting (aTx.getID ());

    return ResponseEntity.ok (new ReportResponse (aTx.getID (),
                                                  "updated",
                                                  "C4 country code set to '" + sC4CountryCode + "'"));
  }

  @GetMapping ("/status/{sbdhInstanceID}")
  public ResponseEntity <InboundTransactionResponse> getStatus (@PathVariable final String sbdhInstanceID)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = aTxMgr.getBySbdhInstanceID (sbdhInstanceID);
    if (aTx == null)
      return ResponseEntity.notFound ().build ();

    return ResponseEntity.ok (InboundTransactionResponse.fromDomain (aTx));
  }

  @GetMapping ("/in-processing")
  public ResponseEntity <List <InboundTransactionResponse>> getInProcessing ()
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final var aTxs = aTxMgr.getAllInProcessing ();

    final ICommonsList <InboundTransactionResponse> aResult = aTxs.getAllMapped (InboundTransactionResponse::fromDomain);
    return ResponseEntity.ok (aResult);
  }
}
