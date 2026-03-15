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
package com.helger.phoss.ap.api.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.source.MultiConfigurationValueProvider;

/**
 * Base configuration provider for the phoss AP.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class APConfigProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APConfigProvider.class);

  private static final IConfigWithFallback DEFAULT_CONFIG;
  static
  {
    final MultiConfigurationValueProvider aVP = ConfigFactory.createDefaultValueProvider ();
    final ConfigWithFallback aCfg = new ConfigWithFallback (aVP);
    DEFAULT_CONFIG = aCfg;
  }

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static IConfigWithFallback s_aConfig = DEFAULT_CONFIG;

  private APConfigProvider ()
  {}

  /**
   * @return The current configuration. Never <code>null</code>.
   */
  @NonNull
  public static IConfigWithFallback getConfig ()
  {
    // Called very often
    RW_LOCK.readLock ().lock ();
    try
    {
      return s_aConfig;
    }
    finally
    {
      RW_LOCK.readLock ().unlock ();
    }
  }

  /**
   * Replace the current configuration with a new one.
   *
   * @param aNewConfig
   *        The new configuration. Never <code>null</code>.
   * @return The previous configuration. Never <code>null</code>.
   */
  @NonNull
  public static IConfigWithFallback setConfig (@NonNull final IConfigWithFallback aNewConfig)
  {
    ValueEnforcer.notNull (aNewConfig, "NewConfig");

    final IConfigWithFallback aOld;
    RW_LOCK.writeLock ().lock ();
    try
    {
      aOld = s_aConfig;
      if (EqualsHelper.identityEqual (aOld, aNewConfig))
      {
        // No change
        return aOld;
      }
      s_aConfig = aNewConfig;
    }
    finally
    {
      RW_LOCK.writeLock ().unlock ();
    }
    LOGGER.info ("phoss AP configuration provider was changed");
    return aOld;
  }
}
