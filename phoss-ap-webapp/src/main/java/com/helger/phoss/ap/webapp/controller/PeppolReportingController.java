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

import com.helger.base.string.StringHelper;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.reporting.APPeppolReportHelper;
import com.helger.phoss.ap.webapp.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This is the primary REST controller for the APIs to create Peppol Reports TSR and EUSR.<br>
 * IMPORTANT: this API will only work, if you configure a Peppol Reporting backend in your pom.xml.
 *
 * @author Philip Helger
 */
@RestController
@RequestMapping ("/api/reporting")
@Tag (name = "Peppol Reporting",
      description = "Read reporting records and create and send Peppol TSR and EUSR reports. Requires a configured Peppol Reporting backend.")
@SecurityRequirement (name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class PeppolReportingController
{
  /**
   * Read the local reporting records for one UTC calendar month without creating or sending reports.
   */
  @GetMapping (path = "/records/{year}/{month}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation (summary = "Read Peppol reporting records",
              description = "Returns a JSON array of local PeppolReportingItem records using the library's versioned " +
                            "JSON representation. An empty period returns []. Requires a configured API token. " +
                            "The response contains raw records used by both TSR and EUSR, not generated reports.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Reporting records as a JSON array",
                                 content = @Content (mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                     array = @ArraySchema (schema = @Schema (type = "object",
                                                                                             description = "Versioned PeppolReportingItem JSON")))),
                   @ApiResponse (responseCode = "400", description = "Invalid or future reporting period"),
                   @ApiResponse (responseCode = "401", description = "Missing, invalid or unconfigured API token"),
                   @ApiResponse (responseCode = "500", description = "Failed to read reporting records") })
  public ResponseEntity <String> getPeppolReportingRecords (@Parameter (description = "Calendar year, at least 2024.",
                                                                       required = true) @PathVariable ("year") final int nYear,
                                                           @Parameter (description = "Calendar month, 1-12.",
                                                                       required = true) @PathVariable ("month") final int nMonth)
  {
    // The shared filter permits requests when no token is configured. Never expose records in that case.
    if (StringHelper.isEmpty (APCoreConfig.getPhase4ApiRequiredToken ()))
      return ResponseEntity.status (401).body ("{\"error\":\"API token is not configured\"}");

    if (nYear < 2024 || nMonth < 1 || nMonth > 12)
      return ResponseEntity.badRequest ().body ("{\"error\":\"Invalid reporting period\"}");

    final YearMonth aYearMonth;
    try
    {
      aYearMonth = APPeppolReportHelper.getValidYearMonthInAPI (nYear, nMonth);
    }
    catch (final IllegalArgumentException ex)
    {
      return ResponseEntity.badRequest ().body ("{\"error\":\"Invalid or future reporting period\"}");
    }

    final String sRecords = APPeppolReportHelper.getReportingRecordsAsString (aYearMonth);
    if (sRecords == null)
      return ResponseEntity.internalServerError ().body ("{\"error\":\"Failed to read reporting records\"}");
    return ResponseEntity.ok ().header ("Cache-Control", "no-store").body (sRecords);
  }

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
  @Operation (summary = "Create Transaction Statistics Report (TSR)",
              description = "Creates a TSR for the given year/month and returns it as XML.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "TSR XML document"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content),
                   @ApiResponse (responseCode = "500", description = "Failed to read Peppol Reporting backend data") })
  public ResponseEntity <String> createPeppolReportingTSR (@Parameter (description = "Calendar year. Must be >= 2024.",
                                                                       required = true,
                                                                       example = "2026") @PathVariable (name = "year",
                                                                                                        required = true) final int nYear,
                                                           @Parameter (description = "Calendar month (1-12).",
                                                                       required = true,
                                                                       example = "3") @PathVariable (name = "month",
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
  @Operation (summary = "Create End User Statistics Report (EUSR)",
              description = "Creates an EUSR for the given year/month and returns it as XML.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "EUSR XML document"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content),
                   @ApiResponse (responseCode = "500", description = "Failed to read Peppol Reporting backend data") })
  public ResponseEntity <String> createPeppolReportingEUSR (@Parameter (description = "Calendar year. Must be >= 2024.",
                                                                        required = true,
                                                                        example = "2026") @PathVariable (name = "year",
                                                                                                         required = true) final int nYear,
                                                            @Parameter (description = "Calendar month (1-12).",
                                                                        required = true,
                                                                        example = "3") @PathVariable (name = "month",
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
  @Operation (summary = "Create, validate, store and send TSR + EUSR",
              description = "Performs the full Peppol Reporting flow for the given year/month: creates both reports, " +
                            "validates them, stores them and sends them to the dedicated receiver.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Reports created, stored and sent successfully"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content),
                   @ApiResponse (responseCode = "500", description = "Error creating or sending Peppol Reports") })
  public ResponseEntity <String> createValidateStoreAndSend (@Parameter (description = "Calendar year. Must be >= 2024.",
                                                                         required = true,
                                                                         example = "2026") @PathVariable (name = "year",
                                                                                                          required = true) final int nYear,
                                                             @Parameter (description = "Calendar month (1-12).",
                                                                         required = true,
                                                                         example = "3") @PathVariable (name = "month",
                                                                                                       required = true) final int nMonth)
  {
    final YearMonth aYearMonth = APPeppolReportHelper.getValidYearMonthInAPI (nYear, nMonth);
    if (APPeppolReportHelper.createAndSendPeppolReports (aYearMonth).isSuccess ())
      return ResponseEntity.ok ("Done - check report storage");
    return ResponseEntity.internalServerError ().body ("Error creating or sending Peppol Reports");
  }
}
