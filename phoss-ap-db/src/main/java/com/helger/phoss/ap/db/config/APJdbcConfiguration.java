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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.db.api.config.JdbcConfigurationConfig;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * phoss AP JDBC configuration with lazy initialization.
 *
 * @author Philip Helger
 */
@Immutable
public class APJdbcConfiguration extends JdbcConfigurationConfig
{
  /**
   * The JDBC configuration prefix.
   */
  public static final String CONFIG_PREFIX = "phossap.jdbc.";

  private final IConfigWithFallback m_aConfig;

  /**
   * Constructor
   *
   * @param aConfig
   *        The configuration object to use. May not be <code>null</code>.
   */
  public APJdbcConfiguration (@NonNull final IConfigWithFallback aConfig)
  {
    super (aConfig, CONFIG_PREFIX);
    m_aConfig = aConfig;
  }

  /** @return the maximum number of pooling connections */
  public int getJdbcPoolingMaxConnections ()
  {
    return m_aConfig.getAsInt (CONFIG_PREFIX + APConfigurationProperties.JDBC_POOLING_MAX_CONNECTIONS,
                               APConfigurationProperties.JDBC_POOLING_MAX_CONNECTIONS_DEFAULT);
  }

  /** @return the maximum wait time in milliseconds for a pooled connection */
  public long getJdbcPoolingMaxWaitMillis ()
  {
    return m_aConfig.getAsLong (CONFIG_PREFIX + APConfigurationProperties.JDBC_POOLING_MAX_WAIT_MILLIS,
                                APConfigurationProperties.JDBC_POOLING_MAX_WAIT_MILLIS_DEFAULT);
  }

  /** @return the time in milliseconds between eviction runs */
  public long getJdbcPoolingBetweenEvictionRunsMillis ()
  {
    return m_aConfig.getAsLong (CONFIG_PREFIX + APConfigurationProperties.JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS,
                                APConfigurationProperties.JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS_DEFAULT);
  }

  /** @return the minimum idle time in milliseconds before a connection is eligible for eviction */
  public long getJdbcPoolingMinEvictableIdleMillis ()
  {
    return m_aConfig.getAsLong (CONFIG_PREFIX + APConfigurationProperties.JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS,
                                APConfigurationProperties.JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS_DEFAULT);
  }

  /** @return the timeout in milliseconds for removing abandoned connections */
  public long getJdbcPoolingRemoveAbandonedTimeoutMillis ()
  {
    return m_aConfig.getAsLong (CONFIG_PREFIX + APConfigurationProperties.JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS,
                                APConfigurationProperties.JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS_DEFAULT);
  }
}
