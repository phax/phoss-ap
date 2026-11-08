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
import static com.helger.phoss.ap.core.startup.StartupCheckTestHelper.assertOneWarning;
import static com.helger.phoss.ap.core.startup.StartupCheckTestHelper.performCheck;

import java.util.Map;

import org.junit.Test;

import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for class {@link StartupCheckPhase4Sending}.
 *
 * @author Philip Helger
 */
public final class StartupCheckPhase4SendingTest
{
  private final StartupCheckPhase4Sending m_aCheck = new StartupCheckPhase4Sending ();

  @Test
  public void testValid ()
  {
    assertErrors (m_aCheck, 0, performCheck (m_aCheck, Map.of ()));
    assertErrors (m_aCheck,
                  0,
                  performCheck (m_aCheck,
                                Map.of (APConfigurationProperties.PHASE4_SEND_TIMEOUT_CONNECT,
                                        "1ms",
                                        APConfigurationProperties.PHASE4_SEND_TIMEOUT_RESPONSE,
                                        "1ms",
                                        APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_MAX_RETRIES,
                                        "0",
                                        APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_INTERVAL,
                                        "0ms")));
  }

  @Test
  public void testInvalid ()
  {
    // A timeout of 0 means "no timeout" for the HTTP client
    assertErrors (m_aCheck,
                  1,
                  performCheck (m_aCheck, Map.of (APConfigurationProperties.PHASE4_SEND_TIMEOUT_CONNECT, "0s")));
    assertErrors (m_aCheck,
                  1,
                  performCheck (m_aCheck, Map.of (APConfigurationProperties.PHASE4_SEND_TIMEOUT_RESPONSE, "0ms")));
    assertErrors (m_aCheck,
                  1,
                  performCheck (m_aCheck, Map.of (APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_MAX_RETRIES, "-1")));
    assertErrors (m_aCheck,
                  1,
                  performCheck (m_aCheck, Map.of (APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_INTERVAL, "-1s")));
  }

  @Test
  public void testLongestSendExceedsRecoveryGracePeriod ()
  {
    // Defaults: 1 x (10s + 120s) = 130s
    assertErrors (m_aCheck,
                  0,
                  performCheck (m_aCheck, Map.of (APConfigurationProperties.RECOVERY_GRACE_PERIOD_IN_FLIGHT, "131s")));
    assertOneWarning (m_aCheck,
                      performCheck (m_aCheck, Map.of (APConfigurationProperties.RECOVERY_GRACE_PERIOD_IN_FLIGHT, "130s")));
    // 2 x (10s + 120s) + 10s = 270s
    assertOneWarning (m_aCheck,
                      performCheck (m_aCheck,
                                    Map.of (APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_MAX_RETRIES,
                                            "1",
                                            APConfigurationProperties.RECOVERY_GRACE_PERIOD_IN_FLIGHT,
                                            "4m")));
    // 3 x (10s + 10m) + 2 x 10s = 30m 50s - longer than the default grace period of 30m
    assertOneWarning (m_aCheck,
                      performCheck (m_aCheck,
                                    Map.of (APConfigurationProperties.PHASE4_SEND_TIMEOUT_RESPONSE,
                                            "10m",
                                            APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_MAX_RETRIES,
                                            "2")));
  }
}
