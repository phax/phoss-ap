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
package com.helger.phoss.ap.db;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;

/**
 * Abstract base class for all AP JDBC manager
 *
 * @author Philip Helger
 */
public abstract class AbstractAPJdbcManager extends AbstractJDBCEnabledManager
{
  private final IAPTimestampManager m_aTimestampMgr;

  protected AbstractAPJdbcManager (@NonNull final IAPTimestampManager aTimestampMgr)
  {
    super (APDBExecutor.createNew ());
    m_aTimestampMgr = aTimestampMgr;
  }

  @NonNull
  @Nonempty
  protected final String createUniqueRowID ()
  {
    // Create a UUID v4
    return UUID.randomUUID ().toString ();
  }

  /**
   * Get the clause that locks the selected rows until the end of the transaction. DB2 releases the
   * lock of a row read with its default isolation level "cursor stability" as soon as the cursor
   * leaves the row, so "read stability" is required there.
   *
   * @param eDBType
   *        The database system type. May not be <code>null</code>.
   * @return The clause with a leading space. Never <code>null</code>.
   */
  @NonNull
  public static String getForUpdateClause (@NonNull final EDatabaseSystemType eDBType)
  {
    return eDBType == EDatabaseSystemType.DB2 ? " FOR UPDATE WITH RS" : " FOR UPDATE";
  }

  /**
   * Get the clause that locks the selected rows until the end of the transaction and that skips
   * rows locked by another transaction instead of waiting for them.
   *
   * @param eDBType
   *        The database system type. May not be <code>null</code>.
   * @return The clause with a leading space. Never <code>null</code>.
   * @see #getForUpdateClause(EDatabaseSystemType)
   */
  @NonNull
  public static String getForUpdateSkipLockedClause (@NonNull final EDatabaseSystemType eDBType)
  {
    return eDBType == EDatabaseSystemType.DB2 ? " FOR UPDATE WITH RS SKIP LOCKED DATA" : " FOR UPDATE SKIP LOCKED";
  }

  /**
   * @return The current date time of the application. It is bound as a parameter instead of using
   *         the SQL function <code>NOW()</code>, because a column without time zone (MySQL
   *         <code>DATETIME</code>) holds the local time of the JVM, while <code>NOW()</code> uses the
   *         time zone of the database session. Never <code>null</code>.
   */
  @NonNull
  protected final OffsetDateTime now ()
  {
    return m_aTimestampMgr.getCurrentDateTimeUTC ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("TimestampMgr", m_aTimestampMgr).getToString ();
  }
}
