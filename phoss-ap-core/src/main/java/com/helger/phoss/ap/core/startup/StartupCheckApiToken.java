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

import com.helger.base.string.StringHelper;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.core.APCoreConfig;

/**
 * Check that the API token protecting <code>/api/**</code> is configured. Without it, everybody who
 * can reach the port can submit documents, and they are sent signed with the AP certificate. A
 * short token is only a warning.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckApiToken extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "api-token";

  /** Below this length a token is reported as a warning */
  public static final int MIN_TOKEN_LENGTH = 16;

  public StartupCheckApiToken ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final String sToken = APCoreConfig.getPhase4ApiRequiredToken ();
    if (StringHelper.isEmpty (sToken))
    {
      addError (aErrors,
                "The API token (" +
                         APConfigurationProperties.PHASE4_API_REQUIREDTOKEN +
                         ") is not configured, so '/api/**' is open to everybody who can reach the port. Configure a strong, random token.");
      return;
    }

    // Never log the token itself
    if (sToken.length () < MIN_TOKEN_LENGTH)
      addWarning (aErrors,
                  "The API token (" +
                           APConfigurationProperties.PHASE4_API_REQUIREDTOKEN +
                           ") is shorter than " +
                           MIN_TOKEN_LENGTH +
                           " characters and therefore easy to guess");
  }
}
