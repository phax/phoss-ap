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

import com.helger.base.enforce.ValueEnforcer;
import com.helger.config.IConfig;
import com.helger.db.api.config.IJdbcConfiguration;
import com.helger.db.api.flyway.FlywayConfigurationBuilderConfig;

/**
 * The specific Flyway Configuration builder for phoss AP.
 *
 * @author Philip Helger
 */
public class APFlywayConfigurationBuilder extends FlywayConfigurationBuilderConfig
{
  /**
   * The Flyway configuration prefix.
   */
  public static final String FLYWAY_CONFIG_PREFIX = "phossap.flyway.";

  /**
   * Constructor
   *
   * @param aConfig
   *        The configuration object. May not be <code>null</code>.
   * @param aJdbcConfig
   *        The JDBC configuration to act as a potential fallback for JDBC connection data. May not
   *        be <code>null</code>.
   */
  public APFlywayConfigurationBuilder (@NonNull final IConfig aConfig, @NonNull final IJdbcConfiguration aJdbcConfig)
  {
    super (aConfig, FLYWAY_CONFIG_PREFIX);
    ValueEnforcer.notNull (aJdbcConfig, "JdbcConfig");

    // Fallback to other configuration values
    if (jdbcUrl () == null)
      jdbcUrl (aJdbcConfig.getJdbcUrl ());
    if (jdbcUser () == null)
      jdbcUser (aJdbcConfig.getJdbcUser ());
    if (jdbcPassword () == null)
      jdbcPassword (aJdbcConfig.getJdbcPassword ());
  }
}
