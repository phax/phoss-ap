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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helger.phoss.ap.testbackend.service.ReportingCompletedCallerService;

/**
 * Trigger async callbacks to phoss-ap. This controller lets you simulate the C4 (Receiver Backend)
 * reporting back to the AP after receiving a document via async HTTP forwarding.
 * <p>
 * Calls phoss-ap's {@code POST /api/inbound/report} endpoint with the SBDH Instance ID and C4
 * country code.
 * </p>
 *
 * @author Philip Helger
 */
@RestController
@RequestMapping ("/api/callback")
public class CallbackController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CallbackController.class);

  private final ReportingCompletedCallerService m_aSvc;

  /**
   * Constructor for {@link CallbackController}.
   *
   * @param aSvc
   *        The reporting completed caller service. May not be {@code null}.
   */
  public CallbackController (@NonNull final ReportingCompletedCallerService aSvc)
  {
    m_aSvc = aSvc;
  }

  /**
   * Trigger the C4 country code callback to phoss-ap for a specific SBDH Instance ID.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier of the inbound transaction.
   * @param sC4CountryCode
   *        The C4 country code. Defaults to the configured default if not provided.
   * @return Status of the callback invocation.
   */
  @PostMapping ("/report")
  public ResponseEntity <Map <String, String>> triggerReport (@RequestParam ("sbdhInstanceID") final String sSbdhInstanceID,
                                                              @RequestParam (value = "c4CountryCode",
                                                                             required = false) final String sC4CountryCode)
  {
    try
    {
      return ResponseEntity.ok (m_aSvc.sendCountryC4Back (sSbdhInstanceID, sC4CountryCode));
    }
    catch (final InterruptedException ex)
    {
      Thread.currentThread ().interrupt ();
      LOGGER.error ("Callback interrupted", ex);
      return ResponseEntity.internalServerError ()
                           .body (Map.of ("status", "error", "message", "Interrupted: " + ex.getMessage ()));
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Callback failed", ex);
      return ResponseEntity.internalServerError ().body (Map.of ("status", "error", "message", ex.getMessage ()));
    }
  }
}
