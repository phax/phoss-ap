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

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReadException;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.phoss.ap.testbackend.CTestBackend;
import com.helger.phoss.ap.testbackend.model.ReceivedDocument;
import com.helger.phoss.ap.testbackend.service.ReportingCompletedCallerService;
import com.helger.phoss.ap.testbackend.store.DocumentStore;

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
public class HttpForwardingController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HttpForwardingController.class);

  private final ReportingCompletedCallerService m_aSvc;

  @Autowired
  private DocumentStore m_aDocumentStore;

  @Value ("${testbackend.http.default-country-code:AT}")
  private String m_sDefaultCountryCode;

  public HttpForwardingController (@NonNull final ReportingCompletedCallerService aSvc)
  {
    m_aSvc = aSvc;
  }

  @PostMapping (path = "/forwarding/url/sync",
                consumes = MediaType.APPLICATION_XML_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity <Map <String, String>> receiveDocumentSync (@RequestBody final byte [] aBody)
  {
    LOGGER.info ("Received HTTP-sync forwarded document (" + aBody.length + " bytes)");

    PeppolSBDHData aSBD;
    try
    {
      aSBD = new PeppolSBDHDataReader (CTestBackend.IF).extractData (new NonBlockingByteArrayInputStream (aBody));
    }
    catch (final PeppolSBDHDataReadException ex)
    {
      return ResponseEntity.internalServerError ()
                           .body (Map.of ("error-source",
                                          "Sbdh parsing",
                                          "error-message",
                                          ex.getMessage (),
                                          "status",
                                          "rejected"));
    }

    // Stores with a random UUID
    final ReceivedDocument aDoc = m_aDocumentStore.storeDocument ("http-sync",
                                                                  "forwarded.xml",
                                                                  aBody,
                                                                  aSBD.getInstanceIdentifier ());
    if (aDoc == null)
      return ResponseEntity.internalServerError ().build ();

    // Always return the JSON with countryCodeC4.
    // In sync mode the AP reads it; in async mode the AP ignores the body.
    return ResponseEntity.ok (Map.of ("countryCodeC4",
                                      m_sDefaultCountryCode,
                                      "documentId",
                                      aDoc.getID (),
                                      "status",
                                      "received"));
  }

  @PostMapping (path = "/forwarding/url/async",
                consumes = MediaType.APPLICATION_XML_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity <Map <String, String>> receiveDocumentAsync (@RequestBody final byte [] aBody)
  {
    LOGGER.info ("Received HTTP-async forwarded document (" + aBody.length + " bytes)");

    PeppolSBDHData aSBD;
    try
    {
      aSBD = new PeppolSBDHDataReader (CTestBackend.IF).extractData (new NonBlockingByteArrayInputStream (aBody));
    }
    catch (final PeppolSBDHDataReadException ex)
    {
      return ResponseEntity.internalServerError ()
                           .body (Map.of ("error-source",
                                          "Sbdh parsing",
                                          "error-message",
                                          ex.getMessage (),
                                          "status",
                                          "rejected"));
    }

    // Stores with a random UUID
    final ReceivedDocument aDoc = m_aDocumentStore.storeDocument ("http-async",
                                                                  "forwarded.xml",
                                                                  aBody,
                                                                  aSBD.getInstanceIdentifier ());
    if (aDoc == null)
      return ResponseEntity.internalServerError ().build ();

    Thread.startVirtualThread ( () -> {
      try
      {
        // Make sure sync response is received first
        Thread.sleep (100);
        m_aSvc.sendCountryC4Back (aSBD.getInstanceIdentifier (), "AT");
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Error sending back async Report status");
      }
    });

    // Always return the JSON with countryCodeC4.
    // In sync mode the AP reads it; in async mode the AP ignores the body.
    return ResponseEntity.ok (Map.of ("documentId", aDoc.getID (), "status", "received"));
  }
}
