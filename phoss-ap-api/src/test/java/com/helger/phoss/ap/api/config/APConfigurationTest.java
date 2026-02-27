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
package com.helger.phoss.ap.api.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;

/**
 * Test class for class {@link APConfiguration}.
 *
 * @author Philip Helger
 */
public final class APConfigurationTest
{
  @Test
  public void testBasic ()
  {
    final var aOldConfig = APConfiguration.getConfig ();
    assertNotNull (aOldConfig);

    try
    {
      final var aNewConfig = new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ());
      APConfiguration.setConfig (aNewConfig);
      assertSame (aNewConfig, APConfiguration.getConfig ());
    }
    finally
    {
      APConfiguration.setConfig (aOldConfig);
    }
  }
}
