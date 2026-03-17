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
package com.helger.phoss.ap.testbackend.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service that calls back to phoss-ap's inbound report endpoint to notify
 * that a document has been successfully received and to provide the C4
 * country code for Peppol reporting.
 *
 * @author Philip Helger
 */
@Service
public class ReportingCompletedCallerService
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ReportingCompletedCallerService.class);

  @Value ("${testbackend.phossap.base-url:http://localhost:8080}")
  private String m_sPhossAPBaseURL;

  @Value ("${testbackend.http.default-country-code:AT}")
  private String m_sDefaultCountryCode;

  /**
   * Send the C4 country code callback to phoss-ap's inbound report endpoint.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier of the inbound transaction. May not be
   *        {@code null}.
   * @param sC4CountryCode
   *        The C4 country code to report, or {@code null} to use the configured
   *        default.
   * @return A map containing the callback status, target URL, HTTP status code
   *         and response body.
   * @throws IOException
   *         If the HTTP request fails.
   * @throws InterruptedException
   *         If the HTTP request is interrupted.
   */
  public Map <String, String> sendCountryC4Back (@NonNull final String sSbdhInstanceID,
                                                 @Nullable final String sC4CountryCode) throws IOException,
                                                                                        InterruptedException
  {
    final String sCountryCode = sC4CountryCode != null ? sC4CountryCode : m_sDefaultCountryCode;

    LOGGER.info ("Triggering Peppol Rporting callback to phoss-ap: sbdhInstanceID=" +
                 sSbdhInstanceID +
                 "; c4CountryCode=" +
                 sCountryCode);

    final String sTargetURL = m_sPhossAPBaseURL +
                              "/api/inbound/report?sbdhInstanceID=" +
                              URLEncoder.encode (sSbdhInstanceID, StandardCharsets.UTF_8) +
                              "&c4CountryCode=" +
                              URLEncoder.encode (sCountryCode, StandardCharsets.UTF_8);

    try (final HttpClient aClient = HttpClient.newHttpClient ())
    {
      final HttpRequest aRequest = HttpRequest.newBuilder ()
                                              .uri (URI.create (sTargetURL))
                                              .header ("X-Token", "phoss-ap-development-token")
                                              .POST (HttpRequest.BodyPublishers.noBody ())
                                              .build ();

      final HttpResponse <String> aResponse = aClient.send (aRequest, HttpResponse.BodyHandlers.ofString ());

      LOGGER.info ("Callback response: HTTP " + aResponse.statusCode () + " - " + aResponse.body ());

      return Map.of ("status",
                     "callback_sent",
                     "targetUrl",
                     sTargetURL,
                     "httpStatus",
                     String.valueOf (aResponse.statusCode ()),
                     "response",
                     aResponse.body ());
    }
  }
}
