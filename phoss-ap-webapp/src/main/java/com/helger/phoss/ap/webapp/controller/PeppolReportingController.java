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

import org.slf4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.eusr.EndUserStatisticsReport;
import com.helger.peppol.reporting.jaxb.eusr.EndUserStatisticsReport110Marshaller;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppol.reporting.jaxb.tsr.TransactionStatisticsReport101Marshaller;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;
import com.helger.peppol.reporting.tsr.TransactionStatisticsReport;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.reporting.APPeppolReportHelper;

/**
 * This is the primary REST controller for the APIs to create Peppol Reports TSR
 * and EUSR.<br>
 * IMPORTANT: this API will only work, if you configure a Peppol Reporting
 * backend in your pom.xml.
 *
 * @author Philip Helger
 */
@RestController
@RequestMapping ("/api/reporting")
public class PeppolReportingController
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (PeppolReportingController.class);

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
    // Check parameters
    final YearMonth aYearMonth = APPeppolReportHelper.getValidYearMonthInAPI (nYear, nMonth);

    LOGGER.info ("Trying to create Peppol Reporting TSR for " + aYearMonth);

    try
    {
      // Now get all items from data storage and store them in a list (we start
      // with an initial size of 1K to avoid too many copy operations)
      final ICommonsList <PeppolReportingItem> aReportingItems = new CommonsArrayList <> (1024);
      if (PeppolReportingBackend.withBackendDo (APConfigProvider.getConfig (),
                                                aBackend -> aBackend.forEachReportingItem (aYearMonth,
                                                                                           aReportingItems::add))
                                .isSuccess ())
      {
        // Create report with the read transactions
        final TransactionStatisticsReportType aReport = TransactionStatisticsReport.builder ()
                                                                                   .monthOf (aYearMonth)
                                                                                   .reportingServiceProviderID (APCoreConfig.getPeppolSeatID ())
                                                                                   .reportingItemList (aReportingItems)
                                                                                   .build ();
        return ResponseEntity.ok (new TransactionStatisticsReport101Marshaller ().getAsString (aReport));
      }
      return ResponseEntity.internalServerError ().body ("Failed to read Peppol Reporting backend data");
    }
    catch (final PeppolReportingBackendException ex)
    {
      LOGGER.error ("Failed to read Peppol Reporting Items", ex);
      return ResponseEntity.internalServerError ()
                           .body ("Failed to read Peppol Reporting backend data: " + ex.getMessage ());
    }
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
    // Check parameters
    final YearMonth aYearMonth = APPeppolReportHelper.getValidYearMonthInAPI (nYear, nMonth);

    LOGGER.info ("Trying to create Peppol Reporting EUSR for " + aYearMonth);

    try
    {
      // Now get all items from data storage and store them in a list (we start
      // with an initial size of 1K to avoid too many copy operations)
      final ICommonsList <PeppolReportingItem> aReportingItems = new CommonsArrayList <> (1024);
      if (PeppolReportingBackend.withBackendDo (APConfigProvider.getConfig (),
                                                aBackend -> aBackend.forEachReportingItem (aYearMonth,
                                                                                           aReportingItems::add))
                                .isSuccess ())
      {
        // Create report with the read transactions
        final EndUserStatisticsReportType aReport = EndUserStatisticsReport.builder ()
                                                                           .monthOf (aYearMonth)
                                                                           .reportingServiceProviderID (APCoreConfig.getPeppolSeatID ())
                                                                           .reportingItemList (aReportingItems)
                                                                           .build ();
        return ResponseEntity.ok (new EndUserStatisticsReport110Marshaller ().getAsString (aReport));
      }
      return ResponseEntity.internalServerError ().body ("Failed to read Peppol Reporting backend data");
    }
    catch (final PeppolReportingBackendException ex)
    {
      LOGGER.error ("Failed to read Peppol Reporting Items", ex);
      return ResponseEntity.internalServerError ()
                           .body ("Failed to read Peppol Reporting backend data: " + ex.getMessage ());
    }
  }

  /**
   * This API creates a TSR and EUSR report for the provided year and month,
   * validate them, store them and send them to the dedicated receiver.
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
    // Check parameters
    final YearMonth aYearMonth = APPeppolReportHelper.getValidYearMonthInAPI (nYear, nMonth);
    if (APPeppolReportHelper.createAndSendPeppolReports (aYearMonth).isSuccess ())
      ResponseEntity.ok ("Done - check report storage");
    return ResponseEntity.internalServerError ().body ("Error creating or sending Peppol Reports");
  }
}
