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
package com.helger.phoss.ap.webapp.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.helger.config.ConfigFactory;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.phoss.ap.api.config.APConfigProvider;

import jakarta.annotation.PostConstruct;

/**
 * Bridges Spring Boot profile-specific properties files into the ph-config configuration system
 * used by phoss-ap. When Spring is started with e.g. {@code --spring.profiles.active=private}, this
 * bean loads {@code application-private.properties} into the ph-config
 * {@link MultiConfigurationValueProvider} so that those values are visible to
 * {@link APConfigProvider} and all code that reads configuration via ph-config.
 *
 * @author Philip Helger
 */
@Configuration
public class SpringProfileConfigIntegration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SpringProfileConfigIntegration.class);

  private final Environment m_aEnvironment;

  /**
   * Constructor.
   *
   * @param aEnvironment
   *        The Spring {@link Environment} to read active profiles from. May not be
   *        <code>null</code>.
   */
  public SpringProfileConfigIntegration (@NonNull final Environment aEnvironment)
  {
    m_aEnvironment = aEnvironment;
  }

  /**
   * Integrate active Spring profile configuration files into the ph-config system. Called
   * automatically after bean construction.
   */
  @PostConstruct
  public void integrateSpringProfiles ()
  {
    final String [] aActiveProfiles = m_aEnvironment.getActiveProfiles ();
    if (aActiveProfiles.length == 0)
    {
      LOGGER.info ("No active Spring profiles — no additional configuration sources to add");
    }
    else
    {
      LOGGER.info ("Active Spring profiles: " + String.join (", ", aActiveProfiles));

      final var aCVP = APConfigProvider.getConfig ().getConfigurationValueProvider ();
      if (aCVP instanceof final MultiConfigurationValueProvider aMCSVP)
      {
        final int nAdded = ConfigFactory.addProfilePropertiesSources (aMCSVP, aActiveProfiles);
        LOGGER.info ("Integrated " + nAdded + " Spring profile configuration file(s) into ph-config");
      }
      else
      {
        LOGGER.warn ("Cannot integrate Spring profile configurations — " +
                     "APConfigProvider does not use a MultiConfigurationValueProvider");
      }
    }
  }
}
