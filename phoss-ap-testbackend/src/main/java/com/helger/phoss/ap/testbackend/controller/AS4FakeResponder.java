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
package com.helger.phoss.ap.testbackend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Receives HTTP-forwarded documents from phoss-ap's {@code HttpDocumentForwarderSPI}. Handles both
 * sync and async modes:
 * <ul>
 * <li><b>Sync mode</b>: the AP expects a JSON response containing {@code countryCodeC4}</li>
 * <li><b>Async mode</b>: the AP only checks for HTTP 200; reporting is triggered later via the
 * callback API</li>
 * </ul>
 * The endpoint URL matches the default in phoss-ap's {@code application.properties}:
 * {@code forwarding.http.endpoint=http://localhost:8888/forwarding/url}
 *
 * @author Philip Helger
 */
@RestController
@RequestMapping ("/api/as4mock")
public class AS4FakeResponder
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4FakeResponder.class);

  /**
   * Return a plain text response with the given HTTP status code. This endpoint is used to simulate
   * AS4 responses for testing purposes.
   *
   * @param aServletRequest
   *        The raw servlet request.
   * @param sContentType
   *        The Content-Type request header.
   * @param nStatus
   *        The HTTP status code to return.
   * @return A plain text response body with the requested status code.
   */
  // Must use "HttpServletRequest" to avoid going through other filter layers
  @PostMapping (path = "/plaintext/{status}",
                consumes = MediaType.MULTIPART_RELATED_VALUE,
                produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity <String> plainTextResponse (@SuppressWarnings ("unused") final HttpServletRequest aServletRequest,
                                                    @SuppressWarnings ("unused") @RequestHeader ("Content-Type") final String sContentType,
                                                    @PathVariable (value = "status") final int nStatus)
  {
    LOGGER.info ("In plaintext/" + nStatus);
    return ResponseEntity.status (nStatus).body ("Plain text response with status " + nStatus);
  }
}
