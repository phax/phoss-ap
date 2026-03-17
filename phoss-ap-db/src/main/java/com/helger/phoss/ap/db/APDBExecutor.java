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

import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.db.jdbc.executor.DBExecutor;

/**
 * Special {@link DBExecutor} for the AP
 *
 * @author Philip Helger
 */
final class APDBExecutor extends DBExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APDBExecutor.class);

  private static APDataSourceProvider s_aDSP;

  @NonNull
  private static APDataSourceProvider _getDSPNotNull ()
  {
    final APDataSourceProvider ret = s_aDSP;
    if (ret == null)
      throw new IllegalStateException ("The DataSourceProvider was never initialized");
    return ret;
  }

  /**
   * Set the global {@link APDataSourceProvider} to be used. This method may
   * only be called once.
   *
   * @param aDSP
   *        The data source provider to use. May not be <code>null</code>.
   * @throws IllegalStateException
   *         if a data source provider was already set
   */
  public static void setDataSourceProvider (@NonNull final APDataSourceProvider aDSP)
  {
    ValueEnforcer.notNull (aDSP, "DataSourceProvider");
    if (s_aDSP != null)
      throw new IllegalStateException ("Another DataSourceProvider was already initialized");
    s_aDSP = aDSP;
  }

  private APDBExecutor ()
  {
    super (_getDSPNotNull ());

    final var aJdbcConfig = APJdbcMetaManager.getJdbcConfig ();
    setDebugConnections (aJdbcConfig.isJdbcDebugConnections ());
    setDebugTransactions (aJdbcConfig.isJdbcDebugTransactions ());
    setDebugSQLStatements (aJdbcConfig.isJdbcDebugSQL ());

    if (aJdbcConfig.isJdbcExecutionTimeWarningEnabled ())
    {
      final long nMillis = aJdbcConfig.getJdbcExecutionTimeWarningMilliseconds ();
      if (nMillis > 0)
        setExecutionDurationWarnMS (nMillis);
      else
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Ignoring execution time warning setting because it is invalid.");
    }
    else
    {
      setExecutionDurationWarnMS (0);
    }
  }

  /**
   * @return a {@link Supplier} that creates new {@link APDBExecutor} instances.
   *         Never <code>null</code>.
   */
  @NonNull
  public static Supplier <APDBExecutor> createNew ()
  {
    return APDBExecutor::new;
  }
}
