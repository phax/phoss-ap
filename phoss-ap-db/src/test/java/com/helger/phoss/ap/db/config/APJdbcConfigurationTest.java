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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.db.api.EDatabaseSystemType;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Test class for {@link APJdbcConfiguration}.
 *
 * @author Philip Helger
 */
public final class APJdbcConfigurationTest
{
  @Rule
  public final ScopeTestRule m_aRule = new ScopeTestRule ();

  @Test
  public void testDefaultsWhenNoConfigIsSet ()
  {
    final APJdbcConfiguration aJdbcConfig = APJdbcMetaManager.getJdbcConfig ();

    // String properties return null when not configured
    assertSame (EDatabaseSystemType.POSTGRESQL, aJdbcConfig.getJdbcDatabaseSystemType ());
    assertNull (aJdbcConfig.getJdbcDriver ());
    assertNull (aJdbcConfig.getJdbcUrl ());
    assertNull (aJdbcConfig.getJdbcUser ());
    assertNull (aJdbcConfig.getJdbcPassword ());
    assertNull (aJdbcConfig.getJdbcSchema ());

    // Boolean / numeric properties return their defined defaults
    assertTrue (aJdbcConfig.isJdbcExecutionTimeWarningEnabled ());
    assertEquals (APConfigurationProperties.JDBC_EXECUTION_TIME_WARNING_MS_DEFAULT,
                  aJdbcConfig.getJdbcExecutionTimeWarningMilliseconds ());

    assertFalse (aJdbcConfig.isJdbcDebugConnections ());
    assertFalse (aJdbcConfig.isJdbcDebugTransactions ());
    assertFalse (aJdbcConfig.isJdbcDebugSQL ());

    assertEquals (APConfigurationProperties.JDBC_POOLING_MAX_CONNECTIONS_DEFAULT,
                  aJdbcConfig.getJdbcPoolingMaxConnections ());
    assertEquals (APConfigurationProperties.JDBC_POOLING_MAX_WAIT_MILLIS_DEFAULT,
                  aJdbcConfig.getJdbcPoolingMaxWaitMillis ());
    assertEquals (APConfigurationProperties.JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS_DEFAULT,
                  aJdbcConfig.getJdbcPoolingBetweenEvictionRunsMillis ());
    assertEquals (APConfigurationProperties.JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS_DEFAULT,
                  aJdbcConfig.getJdbcPoolingMinEvictableIdleMillis ());
    assertEquals (APConfigurationProperties.JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS_DEFAULT,
                  aJdbcConfig.getJdbcPoolingRemoveAbandonedTimeoutMillis ());
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
