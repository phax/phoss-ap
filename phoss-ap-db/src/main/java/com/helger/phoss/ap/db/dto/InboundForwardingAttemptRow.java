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
import com.helger.phoss.ap.api.model.IInboundForwardingAttempt;

/**
 * Simple implementation of {@link IInboundForwardingAttempt} created from JDBC result object.
 *
 * @author Philip Helger
 */
public class InboundForwardingAttemptRow implements IInboundForwardingAttempt
{
  private final String m_sID;
  private final String m_sInboundTransactionID;
  private final OffsetDateTime m_aAttemptDT;
  private final EAttemptStatus m_eAttemptStatus;
  private final String m_sErrorCode;
  private final String m_sErrorDetails;

  /**
   * Construct an inbound forwarding attempt row from a JDBC result row.
   *
   * @param aRow
   *        The database result row to read from. May not be <code>null</code>.
   */
  public InboundForwardingAttemptRow (@NonNull final DBResultRow aRow)
  {
    m_sID = aRow.getAsString (0);
    m_sInboundTransactionID = aRow.getAsString (1);
    m_aAttemptDT = aRow.getAsOffsetDateTime (2);
    m_eAttemptStatus = EAttemptStatus.getFromIDOrNull (aRow.getAsString (3));
    m_sErrorCode = aRow.getAsString (4);
    m_sErrorDetails = aRow.getAsString (5);
    ValueEnforcer.notEmpty (m_sID, "ID");
    ValueEnforcer.notEmpty (m_sInboundTransactionID, "InboundTransactionID");
    ValueEnforcer.notNull (m_aAttemptDT, "AttemptDT");
    ValueEnforcer.notNull (m_eAttemptStatus, "AttemptStatus");
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getInboundTransactionID ()
  {
    return m_sInboundTransactionID;
  }

  /** {@inheritDoc} */
  @NonNull
  public OffsetDateTime getAttemptDT ()
  {
    return m_aAttemptDT;
  }

  /** {@inheritDoc} */
  @NonNull
  public EAttemptStatus getAttemptStatus ()
  {
    return m_eAttemptStatus;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getErrorCode ()
  {
    return m_sErrorCode;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getErrorDetails ()
  {
    return m_sErrorDetails;
  }
}
