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

import java.time.Duration;

import org.jspecify.annotations.NonNull;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.core.APCoreConfig;

/**
 * Check the timeouts and the HTTP retry of the AS4 sending.
 *
 * @author Philip Helger
 * @since 0.14.0
 */
public class StartupCheckPhase4Sending extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "phase4-sending";

  public StartupCheckPhase4Sending ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    // The HTTP client takes a timeout of 0 as "no timeout"
    boolean bValid = true;
    final Duration aConnectTimeout = APCoreConfig.getPhase4SendTimeoutConnect ();
    if (aConnectTimeout.toMillis () < 1)
    {
      addError (aErrors, "The AS4 sending connect timeout must be >= 1 ms, but is " + aConnectTimeout);
      bValid = false;
    }

    final Duration aResponseTimeout = APCoreConfig.getPhase4SendTimeoutResponse ();
    if (aResponseTimeout.toMillis () < 1)
    {
      addError (aErrors, "The AS4 sending response timeout must be >= 1 ms, but is " + aResponseTimeout);
      bValid = false;
    }

    final int nMaxRetries = APCoreConfig.getPhase4SendHttpRetryMaxRetries ();
    if (nMaxRetries < 0)
    {
      addError (aErrors, "The AS4 sending HTTP retry max retries must be >= 0, but is " + nMaxRetries);
      bValid = false;
    }

    final Duration aInterval = APCoreConfig.getPhase4SendHttpRetryInterval ();
    if (aInterval.isNegative ())
    {
      addError (aErrors, "The AS4 sending HTTP retry interval must be >= 0, but is " + aInterval);
      bValid = false;
    }

    if (!bValid)
      return;

    // An estimate only: the response timeout applies to each read, not to the whole response
    final Duration aLongestAttempt = aConnectTimeout.plus (aResponseTimeout)
                                                    .multipliedBy (nMaxRetries + 1L)
                                                    .plus (aInterval.multipliedBy (nMaxRetries));
    final Duration aGracePeriod = APCoreConfig.getRecoveryGracePeriodInFlight ();
    if (aLongestAttempt.compareTo (aGracePeriod) >= 0)
      addWarning (aErrors,
                  "One AS4 sending attempt may take " +
                           aLongestAttempt +
                           " or longer, which is not shorter than '" +
                           APConfigurationProperties.RECOVERY_GRACE_PERIOD_IN_FLIGHT +
                           "' (" +
                           aGracePeriod +
                           ") - a slow send may be sent again by the recovery. Increase the grace period or reduce the '" +
                           APConfigurationProperties.PHASE4_SEND_TIMEOUT_RESPONSE +
                           "' or '" +
                           APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_MAX_RETRIES +
                           "'");
  }
}
