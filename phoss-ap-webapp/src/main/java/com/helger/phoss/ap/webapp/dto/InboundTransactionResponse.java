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

import org.jspecify.annotations.NonNull;

import com.helger.phoss.ap.api.model.IInboundTransaction;

/**
 * JSON response DTO representing an inbound transaction with all relevant fields for the REST API.
 *
 * @author Philip Helger
 */
public class InboundTransactionResponse
{
  private String id;
  private String senderID;
  private String receiverID;
  private String docTypeID;
  private String processID;
  private String as4MessageID;
  private String sbdhInstanceID;
  private String status;
  private int attemptCount;
  private String receivedDT;
  private String completedDT;
  private String reportingStatus;
  private String nextRetryDT;
  private String errorDetails;
  private boolean isDuplicateAS4;
  private boolean isDuplicateSBDH;
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
    final InboundTransactionResponse aResp = new InboundTransactionResponse ();
    aResp.id = aTx.getID ();
    aResp.senderID = aTx.getSenderID ();
    aResp.receiverID = aTx.getReceiverID ();
    aResp.docTypeID = aTx.getDocTypeID ();
    aResp.processID = aTx.getProcessID ();
    aResp.as4MessageID = aTx.getAS4MessageID ();
    aResp.sbdhInstanceID = aTx.getSbdhInstanceID ();
    aResp.status = aTx.getStatus ().getID ();
    aResp.attemptCount = aTx.getAttemptCount ();
    aResp.receivedDT = aTx.getReceivedDT () != null ? aTx.getReceivedDT ().toString () : null;
    aResp.completedDT = aTx.getCompletedDT () != null ? aTx.getCompletedDT ().toString () : null;
    aResp.reportingStatus = aTx.getReportingStatus ().getID ();
    aResp.nextRetryDT = aTx.getNextRetryDT () != null ? aTx.getNextRetryDT ().toString () : null;
    aResp.errorDetails = aTx.getErrorDetails ();
    aResp.isDuplicateAS4 = aTx.isDuplicateAS4 ();
    aResp.isDuplicateSBDH = aTx.isDuplicateSBDH ();
    aResp.mlsResponseCode = aTx.getMlsResponseCode () != null ? aTx.getMlsResponseCode ().getID () : null;
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
}
