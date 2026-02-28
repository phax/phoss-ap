/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.db.jdbc.IHasDataSource;
import com.helger.phoss.ap.db.config.APJdbcConfiguration;

final class APDataSourceProvider implements IHasDataSource, Closeable
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APDataSourceProvider.class);

  private final BasicDataSource m_aDS;

  public APDataSourceProvider (@NonNull final APJdbcConfiguration aJdbcConfig)
  {
    m_aDS = new BasicDataSource ();
    m_aDS.setDriverClassName (aJdbcConfig.getJdbcDriver ());
    m_aDS.setUrl (aJdbcConfig.getJdbcUrl ());
    m_aDS.setUsername (aJdbcConfig.getJdbcUser ());
    m_aDS.setPassword (aJdbcConfig.getJdbcPassword ());
    m_aDS.setDefaultAutoCommit (Boolean.FALSE);
    m_aDS.setPoolPreparedStatements (true);

    final int nMaxConnections = aJdbcConfig.getJdbcPoolingMaxConnections ();
    m_aDS.setMaxTotal (nMaxConnections);
    m_aDS.setMaxWait (Duration.ofMillis (aJdbcConfig.getJdbcPoolingMaxWaitMillis ()));
    m_aDS.setInitialSize (Math.min (4, nMaxConnections));
    m_aDS.setMinIdle (Math.min (4, nMaxConnections));
    m_aDS.setMaxIdle (nMaxConnections);

    final long nBetweenEvictionRunsMillis = aJdbcConfig.getJdbcPoolingBetweenEvictionRunsMillis ();
    if (nBetweenEvictionRunsMillis > 0)
    {
      m_aDS.setDurationBetweenEvictionRuns (Duration.ofMillis (nBetweenEvictionRunsMillis));
      m_aDS.setTestWhileIdle (true);
    }
    m_aDS.setMinEvictableIdle (Duration.ofMillis (aJdbcConfig.getJdbcPoolingMinEvictableIdleMillis ()));
    m_aDS.setRemoveAbandonedOnBorrow (true);
    m_aDS.setRemoveAbandonedTimeout (Duration.ofMillis (aJdbcConfig.getJdbcPoolingRemoveAbandonedTimeoutMillis ()));

    LOGGER.info ("AP DataSource created with max " + nMaxConnections + " connections to " + aJdbcConfig.getJdbcUrl ());
  }

  @NonNull
  public BasicDataSource getDataSource ()
  {
    return m_aDS;
  }

  public void close () throws IOException
  {
    if (m_aDS != null)
      try
      {
        m_aDS.close ();
        LOGGER.info ("AP DataSource closed");
      }
      catch (final SQLException ex)
      {
        throw new IOException ("Failed to close AP DataSource", ex);
      }
  }
}
