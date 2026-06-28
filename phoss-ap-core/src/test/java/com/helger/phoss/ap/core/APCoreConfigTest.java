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
package com.helger.phoss.ap.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for class {@link APCoreConfig}.
 *
 * @author Philip Helger
 */
public final class APCoreConfigTest
{
  @Test
  public void testOutboundDevLoopbackRequiresTestStageAndFlag ()
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.getConfig ();
    final String sOldStage = System.getProperty (APConfigurationProperties.PEPPOL_STAGE);
    final String sOldFlag = System.getProperty (APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED);

    try
    {
      System.clearProperty (APConfigurationProperties.PEPPOL_STAGE);
      System.clearProperty (APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED);
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertFalse (APCoreConfig.isOutboundDevLoopbackEnabled ());

      System.setProperty (APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED, "true");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertFalse (APCoreConfig.isOutboundDevLoopbackEnabled ());

      System.setProperty (APConfigurationProperties.PEPPOL_STAGE, "production");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertFalse (APCoreConfig.isOutboundDevLoopbackEnabled ());

      System.setProperty (APConfigurationProperties.PEPPOL_STAGE, "test");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertTrue (APCoreConfig.isOutboundDevLoopbackEnabled ());
    }
    finally
    {
      if (sOldStage == null)
        System.clearProperty (APConfigurationProperties.PEPPOL_STAGE);
      else
        System.setProperty (APConfigurationProperties.PEPPOL_STAGE, sOldStage);

      if (sOldFlag == null)
        System.clearProperty (APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED);
      else
        System.setProperty (APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED, sOldFlag);

      APConfigProvider.setConfig (aOldConfig);
    }
  }
}
