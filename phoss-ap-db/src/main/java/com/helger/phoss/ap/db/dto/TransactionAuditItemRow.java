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
import com.helger.phoss.ap.api.model.ITransactionAuditItem;

/**
 * Implementation of {@link ITransactionAuditItem} created from JDBC result row.
 *
 * @author Philip Helger
 */
public class TransactionAuditItemRow implements ITransactionAuditItem
{
  private final long m_nID;
  private final String m_sTransactionID;
  private final String m_sSbdhInstanceID;
  private final String m_sDirection;
  private final String m_sEventType;
  private final String m_sAction;
  private final String m_sFromStatus;
  private final String m_sToStatus;
  private final Integer m_aAttemptCount;
  private final String m_sPerformedBy;
  private final OffsetDateTime m_aPerformedDT;
  private final String m_sDetails;

  /**
   * Construct an audit item row from a JDBC result row.
   *
   * @param aRow
   *        The database result row. May not be <code>null</code>.
   */
  public TransactionAuditItemRow (@NonNull final DBResultRow aRow)
  {
    m_nID = aRow.getAsLong (0);
    m_sTransactionID = aRow.getAsString (1);
    m_sSbdhInstanceID = aRow.getAsString (2);
    m_sDirection = aRow.getAsString (3);
    m_sEventType = aRow.getAsString (4);
    m_sAction = aRow.getAsString (5);
    m_sFromStatus = aRow.getAsString (6);
    m_sToStatus = aRow.getAsString (7);
    m_aAttemptCount = aRow.getAsIntObj (8);
    m_sPerformedBy = aRow.getAsString (9);
    m_aPerformedDT = aRow.getAsOffsetDateTime (10);
    m_sDetails = aRow.getAsString (11);

    ValueEnforcer.notEmpty (m_sSbdhInstanceID, "SbdhInstanceID");
    ValueEnforcer.notEmpty (m_sDirection, "Direction");
    ValueEnforcer.notEmpty (m_sEventType, "EventType");
    ValueEnforcer.notEmpty (m_sAction, "Action");
    ValueEnforcer.notEmpty (m_sPerformedBy, "PerformedBy");
    ValueEnforcer.notNull (m_aPerformedDT, "PerformedDT");
  }

  @Override
  public long getID ()
  {
    return m_nID;
  }

  @Override
  @Nullable
  public String getTransactionID ()
  {
    return m_sTransactionID;
  }

  @Override
  @NonNull
  @Nonempty
  public String getSbdhInstanceID ()
  {
    return m_sSbdhInstanceID;
  }

  @Override
  @NonNull
  @Nonempty
  public String getDirection ()
  {
    return m_sDirection;
  }

  @Override
  @NonNull
  @Nonempty
  public String getEventType ()
  {
    return m_sEventType;
  }

  @Override
  @NonNull
  @Nonempty
  public String getAction ()
  {
    return m_sAction;
  }

  @Override
  @Nullable
  public String getFromStatus ()
  {
    return m_sFromStatus;
  }

  @Override
  @Nullable
  public String getToStatus ()
  {
    return m_sToStatus;
  }

  @Override
  @Nullable
  public Integer getAttemptCount ()
  {
    return m_aAttemptCount;
  }

  @Override
  @NonNull
  @Nonempty
  public String getPerformedBy ()
  {
    return m_sPerformedBy;
  }

  @Override
  @NonNull
  public OffsetDateTime getPerformedDT ()
  {
    return m_aPerformedDT;
  }

  @Override
  @Nullable
  public String getDetails ()
  {
    return m_sDetails;
  }
}
