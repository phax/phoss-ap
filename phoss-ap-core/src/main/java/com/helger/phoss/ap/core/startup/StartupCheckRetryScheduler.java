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
import com.helger.phoss.ap.core.APCoreConfig;

/**
 * Check the configuration of the retry scheduler, which is always running.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckRetryScheduler extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "retry-scheduler";

  public StartupCheckRetryScheduler ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final int nBatchSize = APCoreConfig.getRetrySchedulerBatchSize ();
    if (nBatchSize < 1)
      addError (aErrors, "The retry scheduler batch size must be >= 1, but is " + nBatchSize);

    final Duration aInterval = APCoreConfig.getRetrySchedulerInterval ();
    if (aInterval.toMillis () < 1)
      addError (aErrors, "The retry scheduler interval must be >= 1 ms, but is " + aInterval);
  }
}
