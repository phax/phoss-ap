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
 * Check the configuration of the cleanup scheduler. Only evaluated if the cleanup scheduler is
 * enabled.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckCleanupScheduler extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "cleanup-scheduler";

  public StartupCheckCleanupScheduler ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    if (!APCoreConfig.isCleanupSchedulerEnabled ())
      return;

    if (!APCoreConfig.isArchivalSchedulerEnabled ())
      addError (aErrors,
                "Cleanup scheduler is enabled but the archival scheduler is disabled. " +
                         "Cleanup only operates on already-archived transactions, so archival must be enabled.");

    final Duration aRetention = APCoreConfig.getCleanupSchedulerRetention ();
    if (aRetention.compareTo (APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION_MIN) < 0)
      addError (aErrors,
                "Cleanup retention '" +
                         aRetention +
                         "' is below the minimum of " +
                         APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION_MIN +
                         " - reconfigure '" +
                         APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION +
                         "' to a value of at least 2 days.");

    final Duration aArchivalSafetyWindow = APCoreConfig.getArchivalSchedulerInterval ().multipliedBy (2);
    if (aRetention.compareTo (aArchivalSafetyWindow) < 0)
      addError (aErrors,
                "Cleanup retention '" +
                         aRetention +
                         "' must be greater than 2x the archival interval (" +
                         aArchivalSafetyWindow +
                         ") so the archiver has time to land completed transactions before cleanup considers them.");

    final int nBatchSize = APCoreConfig.getCleanupSchedulerBatchSize ();
    if (nBatchSize < 1)
      addError (aErrors, "The cleanup scheduler batch size must be >= 1, but is " + nBatchSize);

    final Duration aInterval = APCoreConfig.getCleanupSchedulerInterval ();
    if (aInterval.toMillis () < 1)
      addError (aErrors, "The cleanup scheduler interval must be >= 1 ms, but is " + aInterval);
  }
}
