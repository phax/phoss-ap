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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.spi.APStartupCheckError;
import com.helger.phoss.ap.api.spi.IAPStartupCheckSPI;

/**
 * Helper for the startup check tests.
 *
 * @author Philip Helger
 */
final class StartupCheckTestHelper
{
  private StartupCheckTestHelper ()
  {}

  /**
   * @param aValues
   *        The configuration values that override the regular test configuration. May not be
   *        <code>null</code>.
   * @return A new configuration. Never <code>null</code>.
   */
  @NonNull
  static IConfigWithFallback createConfig (@NonNull final Map <String, String> aValues)
  {
    final MultiConfigurationValueProvider aVP = ConfigFactory.createDefaultValueProvider ();
    // Highest priority wins over the values from application.properties
    aVP.addConfigurationSource (new ConfigurationSourceFunction (aValues::get), Integer.MAX_VALUE);
    return new ConfigWithFallback (aVP);
  }

  /**
   * Run a check with a configuration that has the provided values on top of the regular test
   * configuration. The previous configuration is restored afterwards.
   *
   * @param aCheck
   *        The check to run. May not be <code>null</code>.
   * @param aValues
   *        The configuration values to set. May not be <code>null</code>.
   * @return The findings. Never <code>null</code>.
   */
  @NonNull
  static ErrorList performCheck (@NonNull final IAPStartupCheckSPI aCheck, @NonNull final Map <String, String> aValues)
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.setConfig (createConfig (aValues));
    try
    {
      final ErrorList ret = new ErrorList ();
      aCheck.performCheck (ret);
      return ret;
    }
    finally
    {
      APConfigProvider.setConfig (aOldConfig);
    }
  }

  /**
   * Assert that the findings contain exactly the provided number of errors and no warnings, and
   * that all of them name the check.
   *
   * @param aCheck
   *        The check that produced the findings. May not be <code>null</code>.
   * @param nExpectedErrors
   *        The expected number of errors.
   * @param aFindings
   *        The findings to check. May not be <code>null</code>.
   */
  static void assertErrors (@NonNull final IAPStartupCheckSPI aCheck,
                            final int nExpectedErrors,
                            @NonNull final ErrorList aFindings)
  {
    assertEquals (aFindings.toString (), nExpectedErrors, aFindings.size ());
    for (final IError aFinding : aFindings)
    {
      assertTrue (aFinding.getErrorText (Locale.ROOT), aFinding.isError ());
      assertEquals (aCheck.getCheckName (), aFinding.getErrorFieldName ());
      assertEquals (APStartupCheckError.ERROR_ID, aFinding.getErrorID ());
    }
  }

  /**
   * Assert that the findings contain exactly one warning and no error.
   *
   * @param aCheck
   *        The check that produced the findings. May not be <code>null</code>.
   * @param aFindings
   *        The findings to check. May not be <code>null</code>.
   */
  static void assertOneWarning (@NonNull final IAPStartupCheckSPI aCheck, @NonNull final ErrorList aFindings)
  {
    assertEquals (aFindings.toString (), 1, aFindings.size ());
    final IError aFinding = aFindings.getFirstOrNull ();
    assertFalse (aFinding.getErrorText (Locale.ROOT), aFinding.isError ());
    assertEquals (aCheck.getCheckName (), aFinding.getErrorFieldName ());
  }
}
