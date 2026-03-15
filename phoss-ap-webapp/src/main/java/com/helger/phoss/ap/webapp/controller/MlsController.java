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

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.phoss.ap.db.MlsMetricsManagerJdbc;
import com.helger.phoss.ap.webapp.dto.InboundTransactionResponse;
import com.helger.phoss.ap.webapp.dto.MlsSlaReportResponse;

@RestController
@RequestMapping ("/api/mls")
public class MlsController
{
  /**
   * Get all inbound business document transactions for which no MLS response
   * has been sent yet.
   *
   * @return List of inbound transactions without MLS response.
   */
  @GetMapping ("/missing")
  public ResponseEntity <List <InboundTransactionResponse>> getMissingMls ()
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();

    final var aTxs = aTxMgr.getAllWithoutMlsResponse ();
    final ICommonsList <InboundTransactionResponse> aResult = aTxs.getAllMapped (InboundTransactionResponse::fromDomain);
    return ResponseEntity.ok (aResult);
  }

  /**
   * Get MLS-1 SLA report (receiving side). Measures M2 - M1: time between
   * receiving the original business document (M1) and successfully sending back
   * the MLS response (M2). SLR: 99.5% within 20 minutes.
   *
   * @return The MLS-1 SLA report.
   */
  @GetMapping ("/sla/mls1")
  public ResponseEntity <MlsSlaReportResponse> getMls1Sla ()
  {
    final MlsMetricsManagerJdbc aMetricsMgr = APJdbcMetaManager.getMlsMetricsMgr ();

    final var aReport = aMetricsMgr.getMls1Report ();
    return ResponseEntity.ok (MlsSlaReportResponse.fromDomain (aReport));
  }

  /**
   * Get MLS-2 SLA report (sending side). Measures M3 - M1: time between
   * successfully sending the business document (M1) and receiving the MLS
   * response from C3 (M3). SLR: 99.5% within 25 minutes.
   *
   * @return The MLS-2 SLA report.
   */
  @GetMapping ("/sla/mls2")
  public ResponseEntity <MlsSlaReportResponse> getMls2Sla ()
  {
    final MlsMetricsManagerJdbc aMetricsMgr = APJdbcMetaManager.getMlsMetricsMgr ();

    final var aReport = aMetricsMgr.getMls2Report ();
    return ResponseEntity.ok (MlsSlaReportResponse.fromDomain (aReport));
  }
}
