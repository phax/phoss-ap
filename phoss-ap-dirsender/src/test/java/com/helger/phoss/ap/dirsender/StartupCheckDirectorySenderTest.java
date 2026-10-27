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
package com.helger.phoss.ap.dirsender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.ICommonsList;
import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.spi.IAPStartupCheckSPI;
import com.helger.phoss.ap.core.startup.APStartupCheckManager;

/**
 * Test class for class {@link StartupCheckDirectorySender}.
 *
 * @author Philip Helger
 */
public final class StartupCheckDirectorySenderTest
{
  @NonNull
  private static ErrorList _check (@NonNull final Map <String, String> aValues)
  {
    final MultiConfigurationValueProvider aVP = ConfigFactory.createDefaultValueProvider ();
    // Highest priority wins over the values from application.properties
    aVP.addConfigurationSource (new ConfigurationSourceFunction (aValues::get), Integer.MAX_VALUE);
    final IConfigWithFallback aOldConfig = APConfigProvider.setConfig (new ConfigWithFallback (aVP));
    try
    {
      final ErrorList ret = new ErrorList ();
      new StartupCheckDirectorySender ().performCheck (ret);
      return ret;
    }
    finally
    {
      APConfigProvider.setConfig (aOldConfig);
    }
  }

  @Test
  public void testValid ()
  {
    // Disabled by default
    assertEquals (0, _check (Map.of ()).size ());
    assertEquals (0,
                  _check (Map.of (APConfigurationProperties.DIRSENDER_ENABLED,
                                  "true",
                                  APConfigurationProperties.DIRSENDER_DIRECTORY,
                                  new File ("src/test").getAbsolutePath ())).size ());
  }

  @Test
  public void testInvalid ()
  {
    // Directory missing
    ErrorList aFindings = _check (Map.of (APConfigurationProperties.DIRSENDER_ENABLED, "true"));
    assertEquals (1, aFindings.size ());
    assertTrue (aFindings.containsAtLeastOneError ());

    // Directory does not exist
    aFindings = _check (Map.of (APConfigurationProperties.DIRSENDER_ENABLED,
                                "true",
                                APConfigurationProperties.DIRSENDER_DIRECTORY,
                                new File ("does-not-exist").getAbsolutePath ()));
    assertEquals (1, aFindings.size ());
    assertTrue (aFindings.containsAtLeastOneError ());

    // Not a directory
    aFindings = _check (Map.of (APConfigurationProperties.DIRSENDER_ENABLED,
                                "true",
                                APConfigurationProperties.DIRSENDER_DIRECTORY,
                                new File ("pom.xml").getAbsolutePath ()));
    assertEquals (1, aFindings.size ());
    assertTrue (aFindings.containsAtLeastOneError ());
  }

  @Test
  public void testRegisteredViaSPI ()
  {
    APStartupCheckManager.initSPI ();
    final ICommonsList <IAPStartupCheckSPI> aChecks = APStartupCheckManager.getAllChecks ();
    assertTrue (aChecks.containsAny (x -> x instanceof StartupCheckDirectorySender));
  }
}
