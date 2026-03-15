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
package com.helger.phoss.ap.db.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.api.config.JdbcConfiguration;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
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
  public void testConfigFromProperties ()
  {
    final APJdbcConfiguration aJdbcConfig = new APJdbcConfiguration (APConfigProvider.getConfig ());

    // Values from test application.properties
    assertSame (EDatabaseSystemType.POSTGRESQL, aJdbcConfig.getJdbcDatabaseSystemType ());
    assertEquals ("org.postgresql.Driver", aJdbcConfig.getJdbcDriver ());
    assertEquals ("jdbc:postgresql://localhost:5432/phoss-ap", aJdbcConfig.getJdbcUrl ());
    assertEquals ("peppol", aJdbcConfig.getJdbcUser ());
    assertEquals ("peppol", aJdbcConfig.getJdbcPassword ());
    assertEquals ("ap-test", aJdbcConfig.getJdbcSchema ());

    // Boolean / numeric properties return their defined defaults (not set in
    // test properties)
    assertTrue (aJdbcConfig.isJdbcExecutionTimeWarningEnabled ());
    assertEquals (JdbcConfiguration.DEFAULT_EXECUTION_DURATION_WARN_MS,
                  aJdbcConfig.getJdbcExecutionTimeWarningMilliseconds ());

    assertFalse (aJdbcConfig.isJdbcDebugConnections ());
    assertFalse (aJdbcConfig.isJdbcDebugTransactions ());
    assertFalse (aJdbcConfig.isJdbcDebugSQL ());

    // Pooling defaults
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
    assertEquals (8, APConfigurationProperties.JDBC_POOLING_MAX_CONNECTIONS_DEFAULT);
    assertEquals (10_000L, APConfigurationProperties.JDBC_POOLING_MAX_WAIT_MILLIS_DEFAULT);
    assertEquals (300_000L, APConfigurationProperties.JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS_DEFAULT);
    assertEquals (1_800_000L, APConfigurationProperties.JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS_DEFAULT);
    assertEquals (300_000L, APConfigurationProperties.JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS_DEFAULT);
  }
}
