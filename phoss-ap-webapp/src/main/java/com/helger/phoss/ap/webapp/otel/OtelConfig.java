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
package com.helger.phoss.ap.webapp.otel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.helger.phoss.ap.core.notification.LifecycleEventManager;
import com.helger.phoss.ap.core.notification.NotificationHandlerManager;
import com.helger.phoss.ap.otel.APLifecycleEventHandlerOtel;
import com.helger.phoss.ap.otel.APNotificationHandlerOtel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

/**
 * Spring configuration that bootstraps OpenTelemetry using the SDK autoconfigure module. Activated
 * when:
 * <ul>
 * <li>the {@code phoss.ap.otel} module is on the classpath, AND</li>
 * <li>the application property {@code otel.enabled} is set to {@code true}.</li>
 * </ul>
 * <p>
 * All other OpenTelemetry configuration (endpoints, headers, sampling, resource attributes) is
 * applied via standard OTel environment variables / system properties such as
 * {@code OTEL_EXPORTER_OTLP_ENDPOINT}, {@code OTEL_SERVICE_NAME}, {@code OTEL_RESOURCE_ATTRIBUTES}.
 * Refer to the OpenTelemetry Java SDK documentation for the full list.
 *
 * @author Philip Helger
 */
@Configuration
@ConditionalOnProperty (name = "otel.enabled", havingValue = "true")
@ConditionalOnClass (AutoConfiguredOpenTelemetrySdk.class)
public class OtelConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (OtelConfig.class);

  private void _initOtel ()
  {
    LOGGER.info ("Initializing OpenTelemetry via SDK autoconfigure");

    // setResultAsGlobal=true makes GlobalOpenTelemetry.get () return this instance, so the
    // PhossAPTelemetry helper resolves it automatically.
    final OpenTelemetry aOtel = AutoConfiguredOpenTelemetrySdk.builder ()
                                                              .setResultAsGlobal ()
                                                              .build ()
                                                              .getOpenTelemetrySdk ();

    LOGGER.info ("Successfully installed the OpenTelemetry SDK: " + aOtel.getClass ().getName ());

    NotificationHandlerManager.registerHandler (new APNotificationHandlerOtel ());
    LifecycleEventManager.registerHandler (new APLifecycleEventHandlerOtel ());
  }

  /**
   * Create the OTel bootstrap bean that runs after the application has started.
   *
   * @return An {@link ApplicationListener} that initializes OpenTelemetry. Never <code>null</code>.
   */
  @Bean
  public ApplicationListener <ApplicationStartedEvent> otelInitializer ()
  {
    return event -> _initOtel ();
  }
}
