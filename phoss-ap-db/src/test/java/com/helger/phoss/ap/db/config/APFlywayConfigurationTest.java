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
 * Test class for {@link APFlywayConfiguration}.
 *
 * @author Philip Helger
 */
public final class APFlywayConfigurationTest
{
  @Test
  public void testDefaultsWhenNoConfigIsSet ()
  {
    assertTrue (APFlywayConfiguration.isFlywayEnabled ());
    assertFalse (APFlywayConfiguration.isFlywaySchemaCreate ());
    assertEquals (0, APFlywayConfiguration.getFlywayBaselineVersion ());

    // Flyway JDBC falls back to JDBC config, which also returns null
    assertNull (APFlywayConfiguration.getFlywayJdbcUrl ());
    assertNull (APFlywayConfiguration.getFlywayJdbcUser ());
    assertNull (APFlywayConfiguration.getFlywayJdbcPassword ());
  }

  @Test
  public void testDefaultConstantsValues ()
  {
    assertTrue (APConfigurationProperties.FLYWAY_ENABLED_DEFAULT);
    assertFalse (APConfigurationProperties.FLYWAY_JDBC_SCHEMA_CREATE_DEFAULT);
    assertEquals (0, APConfigurationProperties.FLYWAY_BASELINE_VERSION_DEFAULT);
  }
}
