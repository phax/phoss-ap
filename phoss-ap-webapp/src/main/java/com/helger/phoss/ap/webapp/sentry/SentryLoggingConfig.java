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
package com.helger.phoss.ap.webapp.sentry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.helger.phoss.ap.core.notification.NotificationHandlerManager;
import com.helger.phoss.ap.sentry.APNotificationHandlerSentry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.sentry.SentryOptions;
import io.sentry.logback.SentryAppender;

@Configuration
// Only activates if DSN is set
@ConditionalOnProperty (name = "sentry.dsn")
// Only if sentry is on the classpath
@ConditionalOnClass (SentryAppender.class)
public class SentryLoggingConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SentryLoggingConfig.class);
  private static final String SENTRY_APPENDER_NAME = "SENTRY";

  @Value ("${sentry.dsn}")
  private String m_sDSN;

  @Value ("${sentry.send-default-pii:false}")
  private boolean m_bSendDefaultPII;

  @Value ("${sentry.logging.minimum-event-level:ERROR}")
  private String m_sMinimumEventLevel;

  @Value ("${sentry.logging.minimum-breadcrumb-level:INFO}")
  private String m_sMinimumBreadcrumbLevel;

  private void _registerSentryAppender ()
  {
    final LoggerContext aLogCtx = (LoggerContext) LoggerFactory.getILoggerFactory ();

    // Avoid duplicate appenders on hot reload
    if (aLogCtx.getLogger (Logger.ROOT_LOGGER_NAME).getAppender (SENTRY_APPENDER_NAME) == null)
    {
      final SentryAppender aAppender = new SentryAppender ();
      aAppender.setName (SENTRY_APPENDER_NAME);
      aAppender.setMinimumEventLevel (Level.toLevel (m_sMinimumEventLevel, Level.ERROR));
      aAppender.setMinimumBreadcrumbLevel (Level.toLevel (m_sMinimumBreadcrumbLevel, Level.INFO));
      final SentryOptions aOptions = new SentryOptions ();
      aOptions.setDsn (m_sDSN);
      aOptions.setSendDefaultPii (m_bSendDefaultPII);
      aAppender.setOptions (aOptions);
      aAppender.setContext (aLogCtx);
      aAppender.start ();
      aLogCtx.getLogger (Logger.ROOT_LOGGER_NAME).addAppender (aAppender);

      LOGGER.info ("Successfully installed the Sentry Log Appender");

      // Manually register the Sentry NotificationHandler implementation
      NotificationHandlerManager.registerHandler (new APNotificationHandlerSentry ());
    }
  }

  @Bean
  public ApplicationListener <ApplicationStartedEvent> sentryAppenderRegistrar ()
  {
    return event -> _registerSentryAppender ();
  }
}
