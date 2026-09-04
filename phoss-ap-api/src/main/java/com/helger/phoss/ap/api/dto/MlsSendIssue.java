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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON request DTO for a single issue of an API triggered MLS. It is the transport representation
 * of {@link com.helger.phoss.ap.api.model.MlsOutcomeIssue} and becomes one line response of the
 * created MLS.
 *
 * @author Philip Helger
 * @since 0.13.0
 */
@Schema (description = "Single issue of an API triggered MLS — becomes one line response of the created MLS document.")
public class MlsSendIssue
{
  @Schema (description = "Peppol MLS status reason code: BV (business rule violation, fatal), BW (business rule " +
                         "violation, warning), FD (failure of delivery) or SV (syntax violation)",
           example = "BV",
           requiredMode = Schema.RequiredMode.REQUIRED)
  private String statusReasonCode;

  @Schema (description = "Reference to the location of the error, as an XPath expression. Use 'NA' if no location can be given.",
           example = "cac:AccountingCustomerParty",
           requiredMode = Schema.RequiredMode.REQUIRED)
  private String errorField;

  @Schema (description = "Human-readable description of the issue",
           example = "Unknown customer number 12345",
           requiredMode = Schema.RequiredMode.REQUIRED)
  private String description;

  /**
   * Default constructor for JSON deserialization.
   */
  public MlsSendIssue ()
  {}

  /** @return the MLS status reason code */
  public String getStatusReasonCode ()
  {
    return statusReasonCode;
  }

  /**
   * @param s
   *        The MLS status reason code to set.
   */
  public void setStatusReasonCode (final String s)
  {
    statusReasonCode = s;
  }

  /** @return the error field reference */
  public String getErrorField ()
  {
    return errorField;
  }

  /**
   * @param s
   *        The error field reference to set.
   */
  public void setErrorField (final String s)
  {
    errorField = s;
  }

  /** @return the human-readable description */
  public String getDescription ()
  {
    return description;
  }

  /**
   * @param s
   *        The description to set.
   */
  public void setDescription (final String s)
  {
    description = s;
  }
}
