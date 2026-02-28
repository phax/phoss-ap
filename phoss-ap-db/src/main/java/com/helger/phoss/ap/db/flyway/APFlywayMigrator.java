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
package com.helger.phoss.ap.db.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.internal.info.MigrationInfoImpl;
import org.flywaydb.core.internal.jdbc.DriverDataSource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.string.StringHelper;
import com.helger.db.api.config.IJdbcConfiguration;
import com.helger.db.api.flyway.IFlywayConfiguration;

public final class APFlywayMigrator
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APFlywayMigrator.class);

  private APFlywayMigrator ()
  {}

  public static void runFlyway (@NonNull final IJdbcConfiguration aJdbcConfig,
                                @NonNull final IFlywayConfiguration aFlywayCfg)
  {
    if (!aFlywayCfg.isFlywayEnabled ())
    {
      LOGGER.info ("Flyway migration is disabled via configuration");
      return;
    }

    LOGGER.info ("Starting Flyway migration for " + aJdbcConfig.getJdbcDatabaseSystemType ().getDisplayName ());

    final Callback aCallbackLogging = new BaseCallback ()
    {
      public void handle (@NonNull final Event aEvent, @Nullable final Context aContext)
      {
        LOGGER.info ("Flyway: Event " + aEvent.getId ());
        if (aEvent == Event.AFTER_EACH_MIGRATE && aContext != null)
        {
          final MigrationInfo aMI = aContext.getMigrationInfo ();
          if (aMI instanceof final MigrationInfoImpl aMII)
          {
            final ResolvedMigration aRM = aMII.getResolvedMigration ();
            if (aRM != null)
              LOGGER.info ("  Performed migration: " + aRM);
          }
        }
      }
    };

    final FluentConfiguration aFlywayConfig = Flyway.configure ()
                                                    .dataSource (new DriverDataSource (APFlywayMigrator.class.getClassLoader (),
                                                                                       aJdbcConfig.getJdbcDriver (),
                                                                                       aFlywayCfg.getFlywayJdbcUrl (),
                                                                                       aFlywayCfg.getFlywayJdbcUser (),
                                                                                       aFlywayCfg.getFlywayJdbcPassword ()));

    aFlywayConfig.baselineOnMigrate (true);
    aFlywayConfig.validateOnMigrate (false);
    aFlywayConfig.baselineVersion (Integer.toString (aFlywayCfg.getFlywayBaselineVersion ()));
    aFlywayConfig.locations ("db/migrate-" + aJdbcConfig.getJdbcDatabaseSystemType ().getID ());
    aFlywayConfig.callbacks (aCallbackLogging);

    final String sSchema = aJdbcConfig.getJdbcSchema ();
    if (StringHelper.isNotEmpty (sSchema))
      aFlywayConfig.schemas (sSchema);

    aFlywayConfig.createSchemas (aFlywayCfg.isFlywaySchemaCreate ());

    final Flyway aFlyway = aFlywayConfig.load ();
    aFlyway.migrate ();

    LOGGER.info ("Finished Flyway migration");
  }
}
