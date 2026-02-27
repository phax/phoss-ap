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
package com.helger.phoss.ap.db.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for {@link APJDBCConfiguration}.
 *
 * @author Philip Helger
 */
public final class APJDBCConfigurationTest
{
  @Test
  public void testDefaultsWhenNoConfigIsSet ()
  {
    // String properties return null when not configured
    assertNull (APJDBCConfiguration.getJdbcDriver ());
    assertNull (APJDBCConfiguration.getJdbcUrl ());
    assertNull (APJDBCConfiguration.getJdbcUser ());
    assertNull (APJDBCConfiguration.getJdbcPassword ());
    assertNull (APJDBCConfiguration.getJdbcSchema ());
    assertNull (APJDBCConfiguration.getTargetDatabaseType ());

    // Boolean / numeric properties return their defined defaults
    assertTrue (APJDBCConfiguration.isJdbcExecutionTimeWarningEnabled ());
    assertEquals (APConfigurationProperties.JDBC_EXECUTION_TIME_WARNING_MS_DEFAULT,
                  APJDBCConfiguration.getJdbcExecutionTimeWarningMilliseconds ());

    assertFalse (APJDBCConfiguration.isJdbcDebugConnections ());
    assertFalse (APJDBCConfiguration.isJdbcDebugTransactions ());
    assertFalse (APJDBCConfiguration.isJdbcDebugSQL ());

    assertEquals (APConfigurationProperties.JDBC_POOLING_MAX_CONNECTIONS_DEFAULT,
                  APJDBCConfiguration.getJdbcPoolingMaxConnections ());
    assertEquals (APConfigurationProperties.JDBC_POOLING_MAX_WAIT_MILLIS_DEFAULT,
                  APJDBCConfiguration.getJdbcPoolingMaxWaitMillis ());
    assertEquals (APConfigurationProperties.JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS_DEFAULT,
                  APJDBCConfiguration.getJdbcPoolingBetweenEvictionRunsMillis ());
    assertEquals (APConfigurationProperties.JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS_DEFAULT,
                  APJDBCConfiguration.getJdbcPoolingMinEvictableIdleMillis ());
    assertEquals (APConfigurationProperties.JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS_DEFAULT,
                  APJDBCConfiguration.getJdbcPoolingRemoveAbandonedTimeoutMillis ());
  }

  @Test
  public void testDefaultConstantsValues ()
  {
    assertEquals (1000L, APConfigurationProperties.JDBC_EXECUTION_TIME_WARNING_MS_DEFAULT);
    assertTrue (APConfigurationProperties.JDBC_EXECUTION_TIME_WARNING_ENABLED_DEFAULT);
    assertFalse (APConfigurationProperties.JDBC_DEBUG_CONNECTIONS_DEFAULT);
    assertFalse (APConfigurationProperties.JDBC_DEBUG_TRANSACTIONS_DEFAULT);
    assertFalse (APConfigurationProperties.JDBC_DEBUG_SQL_DEFAULT);
    assertEquals (8, APConfigurationProperties.JDBC_POOLING_MAX_CONNECTIONS_DEFAULT);
    assertEquals (10_000L, APConfigurationProperties.JDBC_POOLING_MAX_WAIT_MILLIS_DEFAULT);
    assertEquals (300_000L, APConfigurationProperties.JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS_DEFAULT);
    assertEquals (1_800_000L, APConfigurationProperties.JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS_DEFAULT);
    assertEquals (300_000L, APConfigurationProperties.JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS_DEFAULT);
  }
}
