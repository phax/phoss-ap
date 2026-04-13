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

  @NonNull
  protected final OffsetDateTime now ()
  {
    return m_aTimestampMgr.getCurrentDateTimeUTC ();
  }
}
