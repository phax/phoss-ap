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
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Map;

import org.apache.hc.core5.util.Timeout;
import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.appl.ConfigurationSourceFunction;
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
  private static void _withConfig (@NonNull final Map <String, String> aValues, @NonNull final Runnable aBody)
  {
    final MultiConfigurationValueProvider aVP = ConfigFactory.createDefaultValueProvider ();
    // Highest priority wins over the values from application.properties
    aVP.addConfigurationSource (new ConfigurationSourceFunction (aValues::get), Integer.MAX_VALUE);
    final IConfigWithFallback aOldConfig = APConfigProvider.setConfig (new ConfigWithFallback (aVP));
    try
    {
      aBody.run ();
    }
    finally
    {
      APConfigProvider.setConfig (aOldConfig);
    }
  }

  @Test
  public void testDefaultConstantsEqualPhase4Defaults ()
  {
    // phoss-ap-api does not depend on phase4, so the defaults cannot reference the constants
    assertEquals (Phase4PeppolHttpClientSettings.DEFAULT_PEPPOL_CONNECT_TIMEOUT,
                  Timeout.of (APConfigurationProperties.PHASE4_SEND_TIMEOUT_CONNECT_DEFAULT));
    assertEquals (Phase4PeppolHttpClientSettings.DEFAULT_PEPPOL_RESPONSE_TIMEOUT,
                  Timeout.of (APConfigurationProperties.PHASE4_SEND_TIMEOUT_RESPONSE_DEFAULT));
  }

  @Test
  public void testDefaults ()
  {
    _withConfig (Map.of (), () -> {
      final Phase4PeppolHttpClientSettings aHCS = OutboundOrchestrator.createSendingHttpClientSettings ();
      assertEquals (Phase4PeppolHttpClientSettings.DEFAULT_PEPPOL_CONNECT_TIMEOUT, aHCS.getConnectTimeout ());
      assertEquals (Phase4PeppolHttpClientSettings.DEFAULT_PEPPOL_RESPONSE_TIMEOUT, aHCS.getResponseTimeout ());

      // No phase4 retry - all retries are done by the retry scheduler
      final HttpRetrySettings aRetry = OutboundOrchestrator.createSendingHttpRetrySettings ();
      assertEquals (0, aRetry.getMaxRetries ());
      assertFalse (aRetry.isRetryEnabled ());
      assertEquals (APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_INTERVAL_DEFAULT,
                    aRetry.getDurationBeforeRetry ());
    });
  }

  @Test
  public void testConfiguredValuesAreUsed ()
  {
    _withConfig (Map.of (APConfigurationProperties.PHASE4_SEND_TIMEOUT_CONNECT,
                         "5s",
                         APConfigurationProperties.PHASE4_SEND_TIMEOUT_RESPONSE,
                         "60s",
                         APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_MAX_RETRIES,
                         "2",
                         APConfigurationProperties.PHASE4_SEND_HTTP_RETRY_INTERVAL,
                         "3s"), () -> {
                           final Phase4PeppolHttpClientSettings aHCS = OutboundOrchestrator.createSendingHttpClientSettings ();
                           assertEquals (Timeout.ofSeconds (5), aHCS.getConnectTimeout ());
                           assertEquals (Timeout.ofSeconds (60), aHCS.getResponseTimeout ());

                           final HttpRetrySettings aRetry = OutboundOrchestrator.createSendingHttpRetrySettings ();
                           assertEquals (2, aRetry.getMaxRetries ());
                           assertTrue (aRetry.isRetryEnabled ());
                           assertEquals (Duration.ofSeconds (3), aRetry.getDurationBeforeRetry ());
                         });
  }

  @Test
  public void testUnparsableDurationFallsBackToDefault ()
  {
    // Values that parse but are out of range are rejected by StartupCheckPhase4Sending
    _withConfig (Map.of (APConfigurationProperties.PHASE4_SEND_TIMEOUT_RESPONSE, "soon"), () -> {
      assertEquals (Phase4PeppolHttpClientSettings.DEFAULT_PEPPOL_RESPONSE_TIMEOUT,
                    OutboundOrchestrator.createSendingHttpClientSettings ().getResponseTimeout ());
    });
  }
}
