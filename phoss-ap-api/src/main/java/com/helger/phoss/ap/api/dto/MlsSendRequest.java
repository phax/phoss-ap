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
package com.helger.phoss.ap.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON request DTO for triggering the MLS of a previously received inbound document. Called by the
 * Receiver Backend once it knows the outcome of the delivery to C4.
 *
 * @author Philip Helger
 * @since 0.13.0
 */
@Schema (description = "Request body for /api/mls/send — the SBDH Instance Identifier of the received business " +
                       "document plus the MLS status the Receiver Backend wants to report for it.")
public class MlsSendRequest
{
  @Schema (description = "Peppol SBDH Instance Identifier of the received business document the MLS refers to",
           example = "550e8400-e29b-41d4-a716-446655440000",
           requiredMode = Schema.RequiredMode.REQUIRED)
  private String sbdhInstanceID;

  @Schema (description = "Peppol MLS response code: AP (acceptance), AB (acknowledging) or RE (rejection)",
           example = "AP",
           requiredMode = Schema.RequiredMode.REQUIRED)
  private String responseCode;

  @Schema (description = "Optional human-readable response text of the MLS",
           example = "Delivered to C4 backend, order 4711")
  private String responseText;

  @Schema (description = "The issues to report as line responses of the MLS. Mandatory for the response code RE, " +
                         "optional for AP and AB — MLS allows line responses on a positive response code as well.")
  private List <MlsSendIssue> issues;

  /**
   * Default constructor for JSON deserialization.
   */
  public MlsSendRequest ()
  {}

  /** @return the SBDH Instance Identifier of the referenced business document */
  public String getSbdhInstanceID ()
  {
    return sbdhInstanceID;
  }

  /**
   * @param s
   *        The SBDH Instance Identifier to set.
   */
  public void setSbdhInstanceID (final String s)
  {
    sbdhInstanceID = s;
  }

  /** @return the MLS response code */
  public String getResponseCode ()
  {
    return responseCode;
  }

  /**
   * @param s
   *        The MLS response code to set.
   */
  public void setResponseCode (final String s)
  {
    responseCode = s;
  }

  /** @return the optional MLS response text */
  public String getResponseText ()
  {
    return responseText;
  }

  /**
   * @param s
   *        The MLS response text to set.
   */
  public void setResponseText (final String s)
  {
    responseText = s;
  }

  /** @return the issues to report as line responses. May be <code>null</code>. */
  public List <MlsSendIssue> getIssues ()
  {
    return issues;
  }

  /**
   * @param a
   *        The issues to set.
   */
  public void setIssues (final List <MlsSendIssue> a)
  {
    issues = a;
  }
}
