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
package com.helger.phoss.ap.core.startup;

import org.jspecify.annotations.NonNull;

import com.helger.db.api.EDatabaseSystemType;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.phoss.ap.db.config.APJdbcConfiguration;

/**
 * Check that the configured database type is set and supported. The connection itself is verified
 * when the database is initialized.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckDatabase extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "database-type";

  public StartupCheckDatabase ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    // Creating the configuration object has no side effects
    final APJdbcConfiguration aJdbcConfig = new APJdbcConfiguration (APConfigProvider.getConfig ());
    final EDatabaseSystemType eDBType = aJdbcConfig.getJdbcDatabaseSystemType ();
    if (!APJdbcMetaManager.isAllowedDatabaseType (eDBType))
      addError (aErrors, APJdbcMetaManager.getInvalidDatabaseTypeMessage (aJdbcConfig.getJdbcDatabaseType ()));
  }
}
