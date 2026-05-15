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

import java.time.YearMonth;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helger.phoss.ap.core.reporting.APPeppolReportHelper;

/**
 * This is the primary REST controller for the APIs to create Peppol Reports TSR and EUSR.<br>
 * IMPORTANT: this API will only work, if you configure a Peppol Reporting backend in your pom.xml.
 *
 * @author Philip Helger
 */
@RestController
@RequestMapping ("/api/reporting")
public class PeppolReportingController
{
  /**
   * This API creates a TSR report from the provided year and month
   *
   * @param nYear
   *        The year to use. Must be &ge; 2024
   * @param nMonth
   *        The month to use. Must be &ge; 1 and &le; 12
   * @return The created TSR reporting in XML in UTF-8 encoding
   */
  @GetMapping (path = "/create-tsr/{year}/{month}", produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity <String> createPeppolReportingTSR (@PathVariable (name = "year",
                                                                          required = true) final int nYear,
                                                           @PathVariable (name = "month",
                                                                          required = true) final int nMonth)
  {
    final YearMonth aYearMonth = APPeppolReportHelper.getValidYearMonthInAPI (nYear, nMonth);
    final String sReport = APPeppolReportHelper.createTSRAsString (aYearMonth);
    if (sReport != null)
      return ResponseEntity.ok (sReport);
    return ResponseEntity.internalServerError ().body ("Failed to read Peppol Reporting backend data");
  }

  /**
   * This API creates an EUSR report from the provided year and month
   *
   * @param nYear
   *        The year to use. Must be &ge; 2024
   * @param nMonth
   *        The month to use. Must be &ge; 1 and &le; 12
   * @return The created EUSR reporting in XML in UTF-8 encoding
   */
  @GetMapping (path = "/create-eusr/{year}/{month}", produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity <String> createPeppolReportingEUSR (@PathVariable (name = "year",
                                                                           required = true) final int nYear,
                                                            @PathVariable (name = "month",
                                                                           required = true) final int nMonth)
  {
    final YearMonth aYearMonth = APPeppolReportHelper.getValidYearMonthInAPI (nYear, nMonth);
    final String sReport = APPeppolReportHelper.createEUSRAsString (aYearMonth);
    if (sReport != null)
      return ResponseEntity.ok (sReport);
    return ResponseEntity.internalServerError ().body ("Failed to read Peppol Reporting backend data");
  }

  /**
   * This API creates a TSR and EUSR report for the provided year and month, validate them, store
   * them and send them to the dedicated receiver.
   *
   * @param nYear
   *        The year to use. Must be &ge; 2024
   * @param nMonth
   *        The month to use. Must be &ge; 1 and &le; 12
   * @return A constant string
   */
  @GetMapping (path = "/do-peppol-reporting/{year}/{month}", produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity <String> createValidateStoreAndSend (@PathVariable (name = "year",
                                                                            required = true) final int nYear,
                                                             @PathVariable (name = "month",
                                                                            required = true) final int nMonth)
  {
    final YearMonth aYearMonth = APPeppolReportHelper.getValidYearMonthInAPI (nYear, nMonth);
    if (APPeppolReportHelper.createAndSendPeppolReports (aYearMonth).isSuccess ())
      return ResponseEntity.ok ("Done - check report storage");
    return ResponseEntity.internalServerError ().body ("Error creating or sending Peppol Reports");
  }
}
