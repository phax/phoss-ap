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
package com.helger.phoss.ap.basic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.helger.httpclient.HttpClientSettings;
import com.helger.network.proxy.ProxySelectorProxySettingsManager;

/**
 * Test class for class {@link APBasicConfig}.
 *
 * @author Philip Helger
 */
public final class APBasicConfigTest
{
  @Test
  public void testDefaults ()
  {
    assertNotNull (APBasicConfig.getStorageMode ());
    assertNotNull (APBasicConfig.getStorageInboundPath ());
    assertNotNull (APBasicConfig.getStorageOutboundPath ());
    assertNotNull (APBasicConfig.getPeppolIdentifierMode ());
  }

  @Test
  public void testApplyHttpProxySettingsDoesNotThrow ()
  {
    final HttpClientSettings aHCS = new HttpClientSettings ();
    APBasicConfig.applyHttpProxySettings (aHCS);
    // No exception = success; proxy may or may not be configured in test env
  }

  @Test
  public void testInstallGlobalProxySelectorWithoutProxy ()
  {
    APBasicConfig.installGlobalProxySelector ();
    // No proxy is configured in the test environment, so the default ProxySelector must stay
    // untouched
    assertFalse (ProxySelectorProxySettingsManager.isDefault ());
  }
}
