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

import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.phoss.ap.api.model.IInboundTransaction;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON response DTO representing an inbound transaction with all relevant fields for the REST API.
 * Usable both for server-side serialization and client-side deserialization.
 *
 * @author Philip Helger
 */
@Schema (description = "Inbound (received) Peppol transaction — current state, identifiers, " +
                       "forwarding outcome and MLS response status.")
public class InboundTransactionResponse
{
  @Schema (description = "Internal transaction ID assigned by the AP")
  private String id;

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

  @Schema (description = "AS4 Message ID from the inbound message")
  private String as4MessageID;

  @Schema (description = "Peppol SBDH Instance Identifier",
           example = "550e8400-e29b-41d4-a716-446655440000")
  private String sbdhInstanceID;

  @Schema (description = "Peppol Seat ID of the sending AP (C2). Since v0.10.2.")
  private String c2SeatID;

  @Schema (description = "Peppol Seat ID of the receiving AP (C3). Since v0.10.2.")
  private String c3SeatID;

  @Schema (description = "Current transaction status",
           allowableValues = { "received", "rejected", "forwarding", "forwarded", "forward_failed",
                               "permanently_failed" })
  private String status;

  @Schema (description = "Total number of forwarding attempts")
  private int attemptCount;

  @Schema (description = "When the message was received (ISO-8601, UTC)",
           example = "2026-03-27T14:30:00Z")
  private String receivedDT;

  @Schema (description = "When the transaction was successfully completed; null if not yet completed",
           example = "2026-03-27T14:30:05Z",
           nullable = true)
  private String completedDT;

  @Schema (description = "Whether Peppol Reporting has been triggered",
           allowableValues = { "pending", "reported" })
  private String reportingStatus;

  @Schema (description = "Planned date/time of the next forwarding retry; null unless status is forward_failed",
           nullable = true)
  private String nextRetryDT;

  @Schema (description = "Summary error from the last failed forwarding attempt; null on success", nullable = true)
  private String errorDetails;

  @Schema (description = "ISO 3166-1 alpha-2 country code of the final receiver (C4); null if not yet reported. Since v0.1.3.",
           example = "AT",
           nullable = true)
  private String c4CountryCode;

  @Schema (description = "Duplicate detected on the AS4 Message ID level")
  private boolean isDuplicateAS4;

  @Schema (description = "Duplicate detected on the SBDH Instance Identifier level")
  private boolean isDuplicateSBDH;

  @Schema (description = "MLS response code sent or to be sent; null if not yet determined",
           allowableValues = { "RE", "AP", "AB" },
           nullable = true)
  private String mlsResponseCode;

  /**
   * Default constructor for JSON deserialization.
   */
  public InboundTransactionResponse ()
  {}

  /**
   * Create a response DTO from a domain model inbound transaction.
   *
   * @param aTx
   *        The inbound transaction. May not be <code>null</code>.
   * @return A new response DTO. Never <code>null</code>.
   */
  @NonNull
  public static InboundTransactionResponse fromDomain (@NonNull final IInboundTransaction aTx)
  {
    final InboundTransactionResponse ret = new InboundTransactionResponse ();
    ret.id = aTx.getID ();
    ret.senderID = aTx.getSenderID ();
    ret.receiverID = aTx.getReceiverID ();
    ret.docTypeID = aTx.getDocTypeID ();
    ret.processID = aTx.getProcessID ();
    ret.as4MessageID = aTx.getAS4MessageID ();
    ret.sbdhInstanceID = aTx.getSbdhInstanceID ();
    ret.c2SeatID = aTx.getC2SeatID ();
    ret.c3SeatID = aTx.getC3SeatID ();
    ret.status = aTx.getStatus ().getID ();
    ret.attemptCount = aTx.getAttemptCount ();
    ret.receivedDT = aTx.getReceivedDT () != null ? aTx.getReceivedDT ().toString () : null;
    ret.completedDT = aTx.getCompletedDT () != null ? aTx.getCompletedDT ().toString () : null;
    ret.reportingStatus = aTx.getReportingStatus ().getID ();
    ret.nextRetryDT = aTx.getNextRetryDT () != null ? aTx.getNextRetryDT ().toString () : null;
    ret.errorDetails = aTx.getErrorDetails ();
    ret.c4CountryCode = aTx.getC4CountryCode ();
    ret.isDuplicateAS4 = aTx.isDuplicateAS4 ();
    ret.isDuplicateSBDH = aTx.isDuplicateSBDH ();
    ret.mlsResponseCode = aTx.getMlsResponseCode () != null ? aTx.getMlsResponseCode ().getID () : null;
    return ret;
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

  /** @return the AS4 message ID */
  public String getAS4MessageID ()
  {
    return as4MessageID;
  }

  /**
   * @param s
   *        The AS4 message ID to set.
   */
  public void setAS4MessageID (final String s)
  {
    as4MessageID = s;
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

  /**
   * @return the Peppol Seat ID of the sending AP (C2)
   * @since v0.10.2
   */
  public String getC2SeatID ()
  {
    return c2SeatID;
  }

  /**
   * @param s
   *        The C2 Seat ID to set.
   * @since v0.10.2
   */
  public void setC2SeatID (final String s)
  {
    c2SeatID = s;
  }

  /**
   * @return the Peppol Seat ID of the receiving AP (C3)
   * @since v0.10.2
   */
  public String getC3SeatID ()
  {
    return c3SeatID;
  }

  /**
   * @param s
   *        The C3 Seat ID to set.
   * @since v0.10.2
   */
  public void setC3SeatID (final String s)
  {
    c3SeatID = s;
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

  /** @return the received date-time as a string */
  public String getReceivedDT ()
  {
    return receivedDT;
  }

  /**
   * @param s
   *        The received date-time to set.
   */
  public void setReceivedDT (final String s)
  {
    receivedDT = s;
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

  /**
   * @return the C4 country code, or <code>null</code> if not yet reported
   * @since v0.1.3
   */
  public String getC4CountryCode ()
  {
    return c4CountryCode;
  }

  /**
   * @param s
   *        The C4 country code to set.
   * @since v0.1.3
   */
  public void setC4CountryCode (final String s)
  {
    c4CountryCode = s;
  }

  /** @return <code>true</code> if this is a duplicate AS4 message */
  public boolean isDuplicateAS4 ()
  {
    return isDuplicateAS4;
  }

  /**
   * @param b
   *        <code>true</code> if this is a duplicate AS4 message.
   */
  public void setDuplicateAS4 (final boolean b)
  {
    isDuplicateAS4 = b;
  }

  /** @return <code>true</code> if this is a duplicate SBDH */
  public boolean isDuplicateSBDH ()
  {
    return isDuplicateSBDH;
  }

  /**
   * @param b
   *        <code>true</code> if this is a duplicate SBDH.
   */
  public void setDuplicateSBDH (final boolean b)
  {
    isDuplicateSBDH = b;
  }

  /** @return the MLS response code */
  public String getMlsResponseCode ()
  {
    return mlsResponseCode;
  }

  /**
   * @param s
   *        The MLS response code to set.
   */
  public void setMlsResponseCode (final String s)
  {
    mlsResponseCode = s;
  }

  /**
   * @return This response as a ph-json {@link IJsonObject}. Never <code>null</code>.
   */
  @Schema (hidden = true)
  @NonNull
  public IJsonObject exportAsJson ()
  {
    final IJsonObject ret = new JsonObject ();
    if (id != null)
      ret.add ("id", id);
    if (senderID != null)
      ret.add ("senderID", senderID);
    if (receiverID != null)
      ret.add ("receiverID", receiverID);
    if (docTypeID != null)
      ret.add ("docTypeID", docTypeID);
    if (processID != null)
      ret.add ("processID", processID);
    if (as4MessageID != null)
      ret.add ("as4MessageID", as4MessageID);
    if (sbdhInstanceID != null)
      ret.add ("sbdhInstanceID", sbdhInstanceID);
    if (c2SeatID != null)
      ret.add ("c2SeatID", c2SeatID);
    if (c3SeatID != null)
      ret.add ("c3SeatID", c3SeatID);
    if (status != null)
      ret.add ("status", status);
    ret.add ("attemptCount", attemptCount);
    if (receivedDT != null)
      ret.add ("receivedDT", receivedDT);
    if (completedDT != null)
      ret.add ("completedDT", completedDT);
    if (reportingStatus != null)
      ret.add ("reportingStatus", reportingStatus);
    if (nextRetryDT != null)
      ret.add ("nextRetryDT", nextRetryDT);
    if (errorDetails != null)
      ret.add ("errorDetails", errorDetails);
    if (c4CountryCode != null)
      ret.add ("c4CountryCode", c4CountryCode);
    ret.add ("isDuplicateAS4", isDuplicateAS4);
    ret.add ("isDuplicateSBDH", isDuplicateSBDH);
    if (mlsResponseCode != null)
      ret.add ("mlsResponseCode", mlsResponseCode);
    return ret;
  }
}
