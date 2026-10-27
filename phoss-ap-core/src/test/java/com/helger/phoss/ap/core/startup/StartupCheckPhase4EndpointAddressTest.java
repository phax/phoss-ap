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

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for class {@link StartupCheckPhase4EndpointAddress}.
 *
 * @author Philip Helger
 */
public final class StartupCheckPhase4EndpointAddressTest
{
  private final StartupCheckPhase4EndpointAddress m_aCheck = new StartupCheckPhase4EndpointAddress ();

  @NonNull
  private ErrorList _check (@NonNull final String sStage, @NonNull final String sAddress)
  {
    return performCheck (m_aCheck,
                         Map.of (APConfigurationProperties.PEPPOL_STAGE,
                                 sStage,
                                 APConfigurationProperties.PHASE4_ENDPOINT_ADDRESS,
                                 sAddress));
  }

  @Test
  public void testValid ()
  {
    // Not configured - nothing to check
    assertErrors (m_aCheck, 0, performCheck (m_aCheck, Map.of (APConfigurationProperties.PEPPOL_STAGE, "prod")));
    for (final String sStage : new String [] { "test", "prod" })
    {
      assertErrors (m_aCheck, 0, _check (sStage, "https://ap.example.org/as4"));
      assertErrors (m_aCheck, 0, _check (sStage, "https://ap.example.org:443/as4"));
    }
  }

  @Test
  public void testHttp ()
  {
    assertErrors (m_aCheck, 1, _check ("prod", "http://ap.example.org/as4"));
    assertOneWarning (m_aCheck, _check ("test", "http://ap.example.org/as4"));
  }

  @Test
  public void testPort ()
  {
    assertErrors (m_aCheck, 1, _check ("prod", "https://ap.example.org:8443/as4"));
    assertOneWarning (m_aCheck, _check ("test", "https://ap.example.org:8443/as4"));
  }

  @Test
  public void testMalformed ()
  {
    for (final String sStage : new String [] { "test", "prod" })
    {
      assertErrors (m_aCheck, 1, _check (sStage, "ap.example.org/as4"));
      assertErrors (m_aCheck, 1, _check (sStage, "https://ap example.org/as4"));
    }
  }
}
