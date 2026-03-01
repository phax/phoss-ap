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

import com.helger.phoss.ap.api.model.IOutboundTransaction;

public class OutboundTransactionResponse
{
  private String id;
  private String transactionType;
  private String senderID;
  private String receiverID;
  private String docTypeID;
  private String processID;
  private String sbdhInstanceID;
  private String status;
  private int attemptCount;
  private String createdDT;
  private String completedDT;
  private String reportingStatus;
  private String nextRetryDT;
  private String errorDetails;
  private String mlsStatus;

  public OutboundTransactionResponse ()
  {}

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
    return aResp;
  }

  public String getID ()
  {
    return id;
  }

  public void setID (final String s)
  {
    id = s;
  }

  public String getTransactionType ()
  {
    return transactionType;
  }

  public void setTransactionType (final String s)
  {
    transactionType = s;
  }

  public String getSenderID ()
  {
    return senderID;
  }

  public void setSenderID (final String s)
  {
    senderID = s;
  }

  public String getReceiverID ()
  {
    return receiverID;
  }

  public void setReceiverID (final String s)
  {
    receiverID = s;
  }

  public String getDocTypeID ()
  {
    return docTypeID;
  }

  public void setDocTypeID (final String s)
  {
    docTypeID = s;
  }

  public String getProcessID ()
  {
    return processID;
  }

  public void setProcessID (final String s)
  {
    processID = s;
  }

  public String getSbdhInstanceID ()
  {
    return sbdhInstanceID;
  }

  public void setSbdhInstanceID (final String s)
  {
    sbdhInstanceID = s;
  }

  public String getStatus ()
  {
    return status;
  }

  public void setStatus (final String s)
  {
    status = s;
  }

  public int getAttemptCount ()
  {
    return attemptCount;
  }

  public void setAttemptCount (final int n)
  {
    attemptCount = n;
  }

  public String getCreatedDT ()
  {
    return createdDT;
  }

  public void setCreatedDT (final String s)
  {
    createdDT = s;
  }

  public String getCompletedDT ()
  {
    return completedDT;
  }

  public void setCompletedDT (final String s)
  {
    completedDT = s;
  }

  public String getReportingStatus ()
  {
    return reportingStatus;
  }

  public void setReportingStatus (final String s)
  {
    reportingStatus = s;
  }

  public String getNextRetryDT ()
  {
    return nextRetryDT;
  }

  public void setNextRetryDT (final String s)
  {
    nextRetryDT = s;
  }

  public String getErrorDetails ()
  {
    return errorDetails;
  }

  public void setErrorDetails (final String s)
  {
    errorDetails = s;
  }

  public String getMlsStatus ()
  {
    return mlsStatus;
  }

  public void setMlsStatus (final String s)
  {
    mlsStatus = s;
  }
}
