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

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.core.APCoreMetaManager;

/**
 * Test class for class {@link StartupCheckForwarding}.
 *
 * @author Philip Helger
 */
public final class StartupCheckForwardingTest
{
  private final StartupCheckForwarding m_aCheck = new StartupCheckForwarding ();

  @NonNull
  private static ICommonsMap <String, String> _base ()
  {
    final ICommonsMap <String, String> ret = new CommonsHashMap <> ();
    ret.put (APConfigurationProperties.FORWARDING_MODE, "http_post_sync");
    return ret;
  }

  @NonNull
  private static String _secondaryModeKey (final int nIndex)
  {
    return APConfigurationProperties.FORWARDING_SECONDARY_PREFIX +
           nIndex +
           "." +
           APConfigurationProperties.FORWARDING_SECONDARY_MODE_SUFFIX;
  }

  @Test
  public void testValid ()
  {
    final ICommonsMap <String, String> aMap = _base ();
    assertErrors (m_aCheck, 0, performCheck (m_aCheck, aMap));

    aMap.put (_secondaryModeKey (1), "filesystem");
    aMap.put (_secondaryModeKey (2), "sftp");
    aMap.put (APConfigurationProperties.FORWARDING_MLS_COPY_ENABLED, "true");
    assertErrors (m_aCheck, 0, performCheck (m_aCheck, aMap));

    aMap.put (APConfigurationProperties.FORWARDING_MLS_COPY_MODE, "s3_link");
    assertErrors (m_aCheck, 0, performCheck (m_aCheck, aMap));
  }

  @Test
  public void testInvalidModes ()
  {
    // Primary missing
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, new CommonsHashMap <> ()));

    // All three invalid - all are reported
    final ICommonsMap <String, String> aMap = new CommonsHashMap <> ();
    aMap.put (APConfigurationProperties.FORWARDING_MODE, "foo");
    aMap.put (_secondaryModeKey (1), "bar");
    aMap.put (APConfigurationProperties.FORWARDING_MLS_COPY_ENABLED, "true");
    aMap.put (APConfigurationProperties.FORWARDING_MLS_COPY_MODE, "baz");
    assertErrors (m_aCheck, 3, performCheck (m_aCheck, aMap));
  }

  @Test
  public void testTooManySecondaries ()
  {
    final ICommonsMap <String, String> aMap = _base ();
    for (int i = 1; i <= APCoreMetaManager.MAX_SECONDARY_FORWARDERS; ++i)
      aMap.put (_secondaryModeKey (i), "filesystem");
    assertErrors (m_aCheck, 0, performCheck (m_aCheck, aMap));

    aMap.put (_secondaryModeKey (APCoreMetaManager.MAX_SECONDARY_FORWARDERS + 1), "filesystem");
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, aMap));
  }

  @Test
  public void testSpi ()
  {
    // No provider ID
    final ICommonsMap <String, String> aMap = new CommonsHashMap <> ();
    aMap.put (APConfigurationProperties.FORWARDING_MODE, "spi");
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, aMap));

    // No provider with that ID
    aMap.put (IDocumentForwarder.DEFAULT_CONFIG_KEY_PREFIX + APConfigurationProperties.FORWARDING_SPI_ID_SUFFIX, "does-not-exist");
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, aMap));
  }
}
