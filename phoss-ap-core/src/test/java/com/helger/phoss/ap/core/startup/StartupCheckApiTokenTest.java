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
 * Test class for class {@link StartupCheckApiToken}.
 *
 * @author Philip Helger
 */
public final class StartupCheckApiTokenTest
{
  private final StartupCheckApiToken m_aCheck = new StartupCheckApiToken ();

  @Test
  public void testValid ()
  {
    assertErrors (m_aCheck,
                  0,
                  performCheck (m_aCheck,
                                Map.of (APConfigurationProperties.PHASE4_API_REQUIREDTOKEN, "0123456789abcdef")));
  }

  @Test
  public void testMissing ()
  {
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, Map.of ()));
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, Map.of (APConfigurationProperties.PHASE4_API_REQUIREDTOKEN, "")));
  }

  @Test
  public void testShort ()
  {
    assertOneWarning (m_aCheck,
                      performCheck (m_aCheck, Map.of (APConfigurationProperties.PHASE4_API_REQUIREDTOKEN, "secret")));
  }
}
