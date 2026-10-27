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

import static com.helger.phoss.ap.core.startup.StartupCheckTestHelper.assertErrors;
import static com.helger.phoss.ap.core.startup.StartupCheckTestHelper.performCheck;

import java.util.Map;

import org.junit.Test;

import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for class {@link StartupCheckCleanupScheduler}.
 *
 * @author Philip Helger
 */
public final class StartupCheckCleanupSchedulerTest
{
  private final StartupCheckCleanupScheduler m_aCheck = new StartupCheckCleanupScheduler ();

  @Test
  public void testValid ()
  {
    // Disabled by default
    assertErrors (m_aCheck, 0, performCheck (m_aCheck, Map.of ()));
    // Not evaluated if disabled
    assertErrors (m_aCheck,
                  0,
                  performCheck (m_aCheck,
                                Map.of (APConfigurationProperties.ARCHIVAL_SCHEDULER_ENABLED,
                                        "false",
                                        APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION,
                                        "1d")));
    // Enabled with the defaults
    assertErrors (m_aCheck,
                  0,
                  performCheck (m_aCheck, Map.of (APConfigurationProperties.CLEANUP_SCHEDULER_ENABLED, "true")));
  }

  @Test
  public void testInvalid ()
  {
    final ICommonsMap <String, String> aBase = new CommonsHashMap <> ();
    aBase.put (APConfigurationProperties.CLEANUP_SCHEDULER_ENABLED, "true");

    // Archival disabled
    ICommonsMap <String, String> aMap = aBase.getClone ();
    aMap.put (APConfigurationProperties.ARCHIVAL_SCHEDULER_ENABLED, "false");
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, aMap));

    // Retention below the minimum
    aMap = aBase.getClone ();
    aMap.put (APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION, "1d");
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, aMap));

    // Retention not greater than 2x the archival interval
    aMap = aBase.getClone ();
    aMap.put (APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION, "3d");
    aMap.put (APConfigurationProperties.ARCHIVAL_SCHEDULER_INTERVAL, "2d");
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, aMap));

    // Batch size
    aMap = aBase.getClone ();
    aMap.put (APConfigurationProperties.CLEANUP_SCHEDULER_BATCH_SIZE, "0");
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, aMap));

    // Interval
    aMap = aBase.getClone ();
    aMap.put (APConfigurationProperties.CLEANUP_SCHEDULER_INTERVAL, "0ms");
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, aMap));

    // All at once - all are reported
    aMap = aBase.getClone ();
    aMap.put (APConfigurationProperties.ARCHIVAL_SCHEDULER_ENABLED, "false");
    aMap.put (APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION, "1d");
    aMap.put (APConfigurationProperties.CLEANUP_SCHEDULER_BATCH_SIZE, "0");
    aMap.put (APConfigurationProperties.CLEANUP_SCHEDULER_INTERVAL, "0ms");
    assertErrors (m_aCheck, 4, performCheck (m_aCheck, aMap));
  }
}
