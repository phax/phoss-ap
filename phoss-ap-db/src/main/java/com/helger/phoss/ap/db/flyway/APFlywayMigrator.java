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
package com.helger.phoss.ap.db.flyway;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.db.api.config.IJdbcConfiguration;
import com.helger.db.flyway.FlywayMigrationRunner;
import com.helger.db.flyway.IFlywayConfiguration;

/**
 * Utility class for running Flyway database migrations for the AP.
 *
 * @author Philip Helger
 */
public final class APFlywayMigrator
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APFlywayMigrator.class);

  private APFlywayMigrator ()
  {}

  /**
   * Run Flyway database migration using the provided JDBC and Flyway configuration. If Flyway is
   * disabled via configuration, this method returns immediately.
   *
   * @param aJdbcConfig
   *        The JDBC configuration to use. May not be <code>null</code>.
   * @param aFlywayCfg
   *        The Flyway configuration to use. May not be <code>null</code>.
   */
  public static void runFlyway (@NonNull final IJdbcConfiguration aJdbcConfig,
                                @NonNull final IFlywayConfiguration aFlywayCfg)
  {
    if (!aFlywayCfg.isFlywayEnabled ())
    {
      LOGGER.info ("Flyway migration is disabled via configuration");
      return;
    }

    FlywayMigrationRunner.runFlyway (aJdbcConfig,
                                     aFlywayCfg,
                                     "db/phoss-ap-flyway-" + aJdbcConfig.getJdbcDatabaseSystemType ().getID (),
                                     (JavaMigration []) null,
                                     (Callback []) null);
  }
}
