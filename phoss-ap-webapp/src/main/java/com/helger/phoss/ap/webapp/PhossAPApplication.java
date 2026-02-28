/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phoss.ap.webapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APMetaManager;
import com.helger.phoss.ap.core.StartupRecovery;
import com.helger.phoss.ap.core.job.ArchivalScheduler;
import com.helger.phoss.ap.core.job.RetryScheduler;
import com.helger.phoss.ap.db.APJdbcMetaManager;

@SpringBootApplication
public class PhossAPApplication
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PhossAPApplication.class);

  public static void main (final String [] args)
  {
    SpringApplication.run (PhossAPApplication.class, args);
  }

  @EventListener (ContextRefreshedEvent.class)
  public void onStartup ()
  {
    LOGGER.info ("Initializing phoss-ap");

    // Initialize all managers
    APBasicMetaManager.getInstance ();
    APJdbcMetaManager.getInstance ();
    APMetaManager.init ();

    // Recover transactions that were in-flight during unclean shutdown
    StartupRecovery.run ();

    // Start background schedulers
    RetryScheduler.start ();
    ArchivalScheduler.start ();

    LOGGER.info ("phoss-ap initialized successfully");
  }

  @EventListener (ContextClosedEvent.class)
  public void onShutdown ()
  {
    LOGGER.info ("Shutting down phoss-ap");

    RetryScheduler.stop ();
    ArchivalScheduler.stop ();
    APMetaManager.shutdown ();

    LOGGER.info ("phoss-ap shutdown complete");
  }
}
