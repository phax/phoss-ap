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
 * JSON request DTO for submitting an outbound document by reference to an S3 object. The document
 * payload is fetched from the specified S3 bucket and key rather than being inlined in the HTTP
 * request body.
 *
 * @author Philip Helger
 * @since v0.1.1
 */
@Schema (description = "Request body for /api/outbound/submit-s3 — Peppol identifiers plus an S3 reference. " +
                       "The Sender Backend uploads the document to S3 first, then calls the AP with this payload.")
public class OutboundS3SubmitRequest
{
  @Schema (description = "Peppol Participant ID of the sender (C1)",
           example = "iso6523-actorid-upis::0088:senderbackend",
           requiredMode = Schema.RequiredMode.REQUIRED)
  private String senderID;

  @Schema (description = "Peppol Participant ID of the receiver (C4)",
           example = "iso6523-actorid-upis::0088:receiverbackend",
           requiredMode = Schema.RequiredMode.REQUIRED)
  private String receiverID;

  @Schema (description = "Peppol Document Type Identifier",
           example = "busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:peppol:pint:billing-1@jp:peppol-1::2.1",
           requiredMode = Schema.RequiredMode.REQUIRED)
  private String docTypeID;

  @Schema (description = "Peppol Process Identifier",
           example = "cenbii-procid-ubl::urn:peppol:pint:billing-1@jp-1",
           requiredMode = Schema.RequiredMode.REQUIRED)
  private String processID;

  @Schema (description = "ISO 3166-1 alpha-2 country code of the sender (C1)",
           example = "AT",
           requiredMode = Schema.RequiredMode.REQUIRED)
  private String c1CountryCode;

  @Schema (description = "S3 bucket where the document was uploaded. Defaults to the configured 'outbound.s3.bucket' when omitted.",
           example = "sender-documents")
  private String s3Bucket;

  @Schema (description = "S3 object key of the uploaded document",
           example = "outbound/2026/invoice-12345.xml",
           requiredMode = Schema.RequiredMode.REQUIRED)
  private String s3Key;

  @Schema (description = "Custom SBDH Instance Identifier. A random UUID-based identifier is generated when omitted.",
           example = "550e8400-e29b-41d4-a716-446655440000")
  private String sbdhInstanceID;

  @Schema (description = "Alternative Peppol Participant ID to receive MLS responses")
  private String mlsTo;

  @Schema (description = "SBDH Standard override for non-XML payloads (e.g., urn:peppol:doctype:pdf+xml). Auto-derived from the document type when omitted.")
  private String sbdhStandard;

  @Schema (description = "SBDH TypeVersion override (e.g., '0'). Auto-derived from the document type when omitted.")
  private String sbdhTypeVersion;

  @Schema (description = "SBDH Type override (e.g., 'factur-x'). Auto-derived from the document type when omitted.")
  private String sbdhType;

  @Schema (description = "MIME type for binary payloads (e.g., 'application/pdf'). When set the payload is wrapped in <BinaryContent>; otherwise treated as XML.",
           example = "application/pdf")
  private String payloadMimeType;

  @Schema (description = "Optional custom field 1 (max 255 characters). Stored with the transaction and returned by the status APIs.")
  private String custom1;

  @Schema (description = "Optional custom field 2 (max 255 characters). Stored with the transaction and returned by the status APIs.")
  private String custom2;

  @Schema (description = "Optional custom field 3 (max 255 characters). Stored with the transaction and returned by the status APIs.")
  private String custom3;

  /** @return the sender participant ID */
  public String getSenderID ()
  {
    return senderID;
  }

  /**
   * @param s
   *        The sender participant ID to set.
   */
  public void setSenderID (final String s)
  {
    senderID = s;
  }

  /** @return the receiver participant ID */
  public String getReceiverID ()
  {
    return receiverID;
  }

  /**
   * @param s
   *        The receiver participant ID to set.
   */
  public void setReceiverID (final String s)
  {
    receiverID = s;
  }

  /** @return the document type ID */
  public String getDocTypeID ()
  {
    return docTypeID;
  }

  /**
   * @param s
   *        The document type ID to set.
   */
  public void setDocTypeID (final String s)
  {
    docTypeID = s;
  }

  /** @return the process ID */
  public String getProcessID ()
  {
    return processID;
  }

  /**
   * @param s
   *        The process ID to set.
   */
  public void setProcessID (final String s)
  {
    processID = s;
  }

  /** @return the C1 country code */
  public String getC1CountryCode ()
  {
    return c1CountryCode;
  }

  /**
   * @param s
   *        The C1 country code to set.
   */
  public void setC1CountryCode (final String s)
  {
    c1CountryCode = s;
  }

  /** @return the S3 bucket where the sender uploaded the document */
  public String getS3Bucket ()
  {
    return s3Bucket;
  }

  /**
   * @param s
   *        The S3 bucket to set.
   */
  public void setS3Bucket (final String s)
  {
    s3Bucket = s;
  }

  /** @return the S3 object key of the uploaded document */
  public String getS3Key ()
  {
    return s3Key;
  }

  /**
   * @param s
   *        The S3 key to set.
   */
  public void setS3Key (final String s)
  {
    s3Key = s;
  }

  /** @return the optional SBDH instance ID */
  public String getSbdhInstanceID ()
  {
    return sbdhInstanceID;
  }

  /**
   * @param s
   *        The SBDH instance ID to set.
   */
  public void setSbdhInstanceID (final String s)
  {
    sbdhInstanceID = s;
  }

  /** @return the optional MLS "To" address */
  public String getMlsTo ()
  {
    return mlsTo;
  }

  /**
   * @param s
   *        The MLS "To" address to set.
   */
  public void setMlsTo (final String s)
  {
    mlsTo = s;
  }

  /** @return the optional SBDH standard identifier */
  public String getSbdhStandard ()
  {
    return sbdhStandard;
  }

  /**
   * @param s
   *        The SBDH standard to set.
   */
  public void setSbdhStandard (final String s)
  {
    sbdhStandard = s;
  }

  /** @return the optional SBDH type version */
  public String getSbdhTypeVersion ()
  {
    return sbdhTypeVersion;
  }

  /**
   * @param s
   *        The SBDH type version to set.
   */
  public void setSbdhTypeVersion (final String s)
  {
    sbdhTypeVersion = s;
  }

  /** @return the optional SBDH type */
  public String getSbdhType ()
  {
    return sbdhType;
  }

  /**
   * @param s
   *        The SBDH type to set.
   */
  public void setSbdhType (final String s)
  {
    sbdhType = s;
  }

  /** @return the optional payload MIME type */
  public String getPayloadMimeType ()
  {
    return payloadMimeType;
  }

  /**
   * @param s
   *        The payload MIME type to set.
   */
  public void setPayloadMimeType (final String s)
  {
    payloadMimeType = s;
  }

  /** @return the optional custom field 1 */
  public String getCustom1 ()
  {
    return custom1;
  }

  /**
   * @param s
   *        The custom field 1 to set.
   */
  public void setCustom1 (final String s)
  {
    custom1 = s;
  }

  /** @return the optional custom field 2 */
  public String getCustom2 ()
  {
    return custom2;
  }

  /**
   * @param s
   *        The custom field 2 to set.
   */
  public void setCustom2 (final String s)
  {
    custom2 = s;
  }

  /** @return the optional custom field 3 */
  public String getCustom3 ()
  {
    return custom3;
  }

  /**
   * @param s
   *        The custom field 3 to set.
   */
  public void setCustom3 (final String s)
  {
    custom3 = s;
  }
}
