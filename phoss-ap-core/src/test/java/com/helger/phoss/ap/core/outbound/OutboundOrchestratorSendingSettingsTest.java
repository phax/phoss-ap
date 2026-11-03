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
package com.helger.phoss.ap.core.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.time.Duration;

import org.apache.hc.core5.util.Timeout;
import org.junit.Test;

import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phase4.messaging.http.HttpRetrySettings;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for the AS4 sending settings of {@link OutboundOrchestrator}.
 *
 * @author Philip Helger
 */
public final class OutboundOrchestratorSendingSettingsTest
{
  private static final String [] KEYS = { APConfigurationProperties.PHASE4_SEND_TIMEOUT_CONNECT,
                                          APConfigurationProperties.PHASE4_SEND_TIMEOUT_RESPONSE,
                                          APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_MAX_RETRIES,
                                          APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_INTERVAL };

  private interface ITestBody
  {
    void run ();
  }

  private static void _withProperties (final String [] aKeyValues, final ITestBody aBody)
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.getConfig ();
    final String [] aOldValues = new String [KEYS.length];
    for (int i = 0; i < KEYS.length; ++i)
    {
      aOldValues[i] = System.getProperty (KEYS[i]);
      System.clearProperty (KEYS[i]);
    }
    try
    {
      for (int i = 0; i < aKeyValues.length; i += 2)
        System.setProperty (aKeyValues[i], aKeyValues[i + 1]);
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      aBody.run ();
    }
    finally
    {
      for (int i = 0; i < KEYS.length; ++i)
        if (aOldValues[i] == null)
          System.clearProperty (KEYS[i]);
        else
          System.setProperty (KEYS[i], aOldValues[i]);
      APConfigProvider.setConfig (aOldConfig);
    }
  }

  @Test
  public void testDefaultsEqualPhase4Defaults ()
  {
    _withProperties (new String [0], () -> {
      final Phase4PeppolHttpClientSettings aHCS = OutboundOrchestrator.createSendingHttpClientSettings ();
      assertEquals (Phase4PeppolHttpClientSettings.DEFAULT_PEPPOL_CONNECT_TIMEOUT, aHCS.getConnectTimeout ());
      assertEquals (Phase4PeppolHttpClientSettings.DEFAULT_PEPPOL_RESPONSE_TIMEOUT, aHCS.getResponseTimeout ());

      // Same as the reception awareness of the default Peppol PMode
      final HttpRetrySettings aRetry = OutboundOrchestrator.createSendingHttpRetrySettings ();
      assertEquals (1, aRetry.getMaxRetries ());
      assertEquals (Duration.ofSeconds (10), aRetry.getDurationBeforeRetry ());
    });
  }

  @Test
  public void testConfiguredValuesAreUsed ()
  {
    _withProperties (new String [] { APConfigurationProperties.PHASE4_SEND_TIMEOUT_CONNECT,
                                     "5s",
                                     APConfigurationProperties.PHASE4_SEND_TIMEOUT_RESPONSE,
                                     "60s",
                                     APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_MAX_RETRIES,
                                     "0",
                                     APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_INTERVAL,
                                     "3s" }, () -> {
                                       final Phase4PeppolHttpClientSettings aHCS = OutboundOrchestrator.createSendingHttpClientSettings ();
                                       assertEquals (Timeout.ofSeconds (5), aHCS.getConnectTimeout ());
                                       assertEquals (Timeout.ofSeconds (60), aHCS.getResponseTimeout ());

                                       final HttpRetrySettings aRetry = OutboundOrchestrator.createSendingHttpRetrySettings ();
                                       assertEquals (0, aRetry.getMaxRetries ());
                                       assertFalse (aRetry.isRetryEnabled ());
                                       assertEquals (Duration.ofSeconds (3), aRetry.getDurationBeforeRetry ());
                                     });
  }

  @Test
  public void testUnparsableDurationFallsBackToDefault ()
  {
    // Values that parse but are out of range are rejected by StartupCheckPhase4Sending
    _withProperties (new String [] { APConfigurationProperties.PHASE4_SEND_TIMEOUT_RESPONSE, "soon" }, () -> {
      assertEquals (Phase4PeppolHttpClientSettings.DEFAULT_PEPPOL_RESPONSE_TIMEOUT,
                    OutboundOrchestrator.createSendingHttpClientSettings ().getResponseTimeout ());
    });
  }
}
