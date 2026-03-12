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
package com.helger.phoss.ap.db.dto;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;
import com.helger.phoss.ap.api.model.IOutboundSendingAttempt;

/**
 * Simple implementation of {@link IOutboundSendingAttempt} created from JDBC result object.
 *
 * @author Philip Helger
 */
public class OutboundSendingAttemptRow implements IOutboundSendingAttempt
{
  private final String m_sID;
  private final String m_sOutboundTransactionID;
  private final String m_sAS4MessageID;
  private final OffsetDateTime m_aAS4Timestamp;
  private final String m_sReceiptMessageID;
  private final Integer m_aHttpStatusCode;
  private final OffsetDateTime m_aAttemptDT;
  private final EAttemptStatus m_eAttemptStatus;
  private final String m_sErrorDetails;
  private final String m_sSendingReport;

  public OutboundSendingAttemptRow (@NonNull final DBResultRow aRow)
  {
    m_sID = aRow.getAsString (0);
    m_sOutboundTransactionID = aRow.getAsString (1);
    m_sAS4MessageID = aRow.getAsString (2);
    m_aAS4Timestamp = aRow.getAsOffsetDateTime (3);
    m_sReceiptMessageID = aRow.getAsString (4);
    m_aHttpStatusCode = aRow.getAsIntObj (5);
    m_aAttemptDT = aRow.getAsOffsetDateTime (6);
    m_eAttemptStatus = EAttemptStatus.getFromIDOrNull (aRow.getAsString (7));
    m_sErrorDetails = aRow.getAsString (8);
    m_sSendingReport = aRow.getAsString (9);
    ValueEnforcer.notEmpty (m_sID, "ID");
    ValueEnforcer.notEmpty (m_sOutboundTransactionID, "OutboundTransactionID");
    ValueEnforcer.notEmpty (m_sAS4MessageID, "AS4MessageID");
    ValueEnforcer.notNull (m_aAS4Timestamp, "AS4Timestamp");
    ValueEnforcer.notNull (m_aAttemptDT, "AttemptDT");
    ValueEnforcer.notNull (m_eAttemptStatus, "AttemptStatus");
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  @Nonempty
  public String getOutboundTransactionID ()
  {
    return m_sOutboundTransactionID;
  }

  @NonNull
  @Nonempty
  public String getAS4MessageID ()
  {
    return m_sAS4MessageID;
  }

  @NonNull
  public OffsetDateTime getAS4Timestamp ()
  {
    return m_aAS4Timestamp;
  }

  @Nullable
  public String getReceiptMessageID ()
  {
    return m_sReceiptMessageID;
  }

  @Nullable
  public Integer getHttpStatusCode ()
  {
    return m_aHttpStatusCode;
  }

  @NonNull
  public OffsetDateTime getAttemptDT ()
  {
    return m_aAttemptDT;
  }

  @NonNull
  public EAttemptStatus getAttemptStatus ()
  {
    return m_eAttemptStatus;
  }

  @Nullable
  public String getErrorDetails ()
  {
    return m_sErrorDetails;
  }

  @Nullable
  public String getSendingReport ()
  {
    return m_sSendingReport;
  }
}
