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
package com.helger.phoss.ap.webapp.dto;

/**
 * JSON request DTO for submitting an outbound document by reference to an S3 object. The document
 * payload is fetched from the specified S3 bucket and key rather than being inlined in the HTTP
 * request body.
 *
 * @author Philip Helger
 * @since v0.1.1
 */
public class OutboundS3SubmitRequest
{
  // Required fields
  private String senderID;
  private String receiverID;
  private String docTypeID;
  private String processID;
  private String c1CountryCode;
  private String s3Bucket;
  private String s3Key;

  // Optional fields
  private String sbdhInstanceID;
  private String mlsTo;
  private String sbdhStandard;
  private String sbdhTypeVersion;
  private String sbdhType;
  private String payloadMimeType;

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
}
