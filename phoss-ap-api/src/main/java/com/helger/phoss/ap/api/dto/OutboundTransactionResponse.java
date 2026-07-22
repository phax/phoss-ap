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

import org.jspecify.annotations.NonNull;

import com.helger.phoss.ap.api.model.IOutboundTransaction;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON response DTO representing an outbound transaction with all relevant fields for the REST API.
 * Usable both for server-side serialization and client-side deserialization.
 *
 * @author Philip Helger
 */
@Schema (description = "Outbound (sent) Peppol transaction — current state, identifiers, " +
                       "transmission progress and MLS response status.")
public class OutboundTransactionResponse
{
  @Schema (description = "Internal transaction ID assigned by the AP")
  private String id;

  @Schema (description = "Kind of transaction", allowableValues = { "business_document", "mls_response" })
  private String transactionType;

  @Schema (description = "Peppol Participant ID of the sender",
           example = "iso6523-actorid-upis::0088:senderbackend")
  private String senderID;

  @Schema (description = "Peppol Participant ID of the receiver",
           example = "iso6523-actorid-upis::0088:receiverbackend")
  private String receiverID;

  @Schema (description = "Peppol Document Type Identifier")
  private String docTypeID;

  @Schema (description = "Peppol Process Identifier")
  private String processID;

  @Schema (description = "Peppol SBDH Instance Identifier",
           example = "550e8400-e29b-41d4-a716-446655440000")
  private String sbdhInstanceID;

  @Schema (description = "Current transaction status",
           allowableValues = { "pending", "rejected", "sending", "sent", "failed", "permanently_failed" })
  private String status;

  @Schema (description = "Total number of sending attempts so far")
  private int attemptCount;

  @Schema (description = "When the transaction was created (ISO-8601, UTC)", example = "2026-03-27T14:30:00Z")
  private String createdDT;

  @Schema (description = "When the transaction was successfully completed; null if not yet completed",
           example = "2026-03-27T14:30:05Z",
           nullable = true)
  private String completedDT;

  @Schema (description = "Whether Peppol Reporting has been triggered",
           allowableValues = { "pending", "reported" })
  private String reportingStatus;

  @Schema (description = "Planned date/time of the next sending retry; null unless status is failed",
           nullable = true)
  private String nextRetryDT;

  @Schema (description = "Summary error from the last failed sending attempt; null on success", nullable = true)
  private String errorDetails;

  @Schema (description = "MLS response reception status (only for business_document)",
           allowableValues = { "pending", "received_ap", "received_ab", "received_re", "not_applicable" },
           nullable = true)
  private String mlsStatus;

  @Schema (description = "First optional custom field (max 255 characters); null if not set", nullable = true)
  private String custom1;

  @Schema (description = "Second optional custom field (max 255 characters); null if not set", nullable = true)
  private String custom2;

  @Schema (description = "Third optional custom field (max 255 characters); null if not set", nullable = true)
  private String custom3;

  /**
   * Default constructor for JSON deserialization.
   */
  public OutboundTransactionResponse ()
  {}

  /**
   * Create a response DTO from a domain model outbound transaction.
   *
   * @param aTx
   *        The outbound transaction. May not be <code>null</code>.
   * @return A new response DTO. Never <code>null</code>.
   */
  @NonNull
  public static OutboundTransactionResponse fromDomain (@NonNull final IOutboundTransaction aTx)
  {
    final OutboundTransactionResponse aResp = new OutboundTransactionResponse ();
    aResp.id = aTx.getID ();
    aResp.transactionType = aTx.getTransactionType ().getID ();
    aResp.senderID = aTx.getSenderID ();
    aResp.receiverID = aTx.getReceiverID ();
    aResp.docTypeID = aTx.getDocTypeID ();
    aResp.processID = aTx.getProcessID ();
    aResp.sbdhInstanceID = aTx.getSbdhInstanceID ();
    aResp.status = aTx.getStatus ().getID ();
    aResp.attemptCount = aTx.getAttemptCount ();
    aResp.createdDT = aTx.getCreatedDT () != null ? aTx.getCreatedDT ().toString () : null;
    aResp.completedDT = aTx.getCompletedDT () != null ? aTx.getCompletedDT ().toString () : null;
    aResp.reportingStatus = aTx.getReportingStatus ().getID ();
    aResp.nextRetryDT = aTx.getNextRetryDT () != null ? aTx.getNextRetryDT ().toString () : null;
    aResp.errorDetails = aTx.getErrorDetails ();
    aResp.mlsStatus = aTx.getMlsStatus () != null ? aTx.getMlsStatus ().getID () : null;
    aResp.custom1 = aTx.getCustom1 ();
    aResp.custom2 = aTx.getCustom2 ();
    aResp.custom3 = aTx.getCustom3 ();
    return aResp;
  }

  /** @return the transaction ID */
  public String getID ()
  {
    return id;
  }

  /**
   * @param s
   *        The transaction ID to set.
   */
  public void setID (final String s)
  {
    id = s;
  }

  /** @return the transaction type */
  public String getTransactionType ()
  {
    return transactionType;
  }

  /**
   * @param s
   *        The transaction type to set.
   */
  public void setTransactionType (final String s)
  {
    transactionType = s;
  }

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

  /** @return the SBDH instance ID */
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

  /** @return the transaction status */
  public String getStatus ()
  {
    return status;
  }

  /**
   * @param s
   *        The transaction status to set.
   */
  public void setStatus (final String s)
  {
    status = s;
  }

  /** @return the number of delivery attempts */
  public int getAttemptCount ()
  {
    return attemptCount;
  }

  /**
   * @param n
   *        The attempt count to set.
   */
  public void setAttemptCount (final int n)
  {
    attemptCount = n;
  }

  /** @return the created date-time as a string */
  public String getCreatedDT ()
  {
    return createdDT;
  }

  /**
   * @param s
   *        The created date-time to set.
   */
  public void setCreatedDT (final String s)
  {
    createdDT = s;
  }

  /** @return the completed date-time as a string */
  public String getCompletedDT ()
  {
    return completedDT;
  }

  /**
   * @param s
   *        The completed date-time to set.
   */
  public void setCompletedDT (final String s)
  {
    completedDT = s;
  }

  /** @return the Peppol reporting status */
  public String getReportingStatus ()
  {
    return reportingStatus;
  }

  /**
   * @param s
   *        The reporting status to set.
   */
  public void setReportingStatus (final String s)
  {
    reportingStatus = s;
  }

  /** @return the next retry date-time as a string */
  public String getNextRetryDT ()
  {
    return nextRetryDT;
  }

  /**
   * @param s
   *        The next retry date-time to set.
   */
  public void setNextRetryDT (final String s)
  {
    nextRetryDT = s;
  }

  /** @return the error details message */
  public String getErrorDetails ()
  {
    return errorDetails;
  }

  /**
   * @param s
   *        The error details to set.
   */
  public void setErrorDetails (final String s)
  {
    errorDetails = s;
  }

  /** @return the MLS status */
  public String getMlsStatus ()
  {
    return mlsStatus;
  }

  /**
   * @param s
   *        The MLS status to set.
   */
  public void setMlsStatus (final String s)
  {
    mlsStatus = s;
  }

  /** @return the first custom field */
  public String getCustom1 ()
  {
    return custom1;
  }

  /**
   * @param s
   *        The first custom field to set.
   */
  public void setCustom1 (final String s)
  {
    custom1 = s;
  }

  /** @return the second custom field */
  public String getCustom2 ()
  {
    return custom2;
  }

  /**
   * @param s
   *        The second custom field to set.
   */
  public void setCustom2 (final String s)
  {
    custom2 = s;
  }

  /** @return the third custom field */
  public String getCustom3 ()
  {
    return custom3;
  }

  /**
   * @param s
   *        The third custom field to set.
   */
  public void setCustom3 (final String s)
  {
    custom3 = s;
  }
}
