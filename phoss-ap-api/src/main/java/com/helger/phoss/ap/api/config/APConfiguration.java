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
package com.helger.phoss.ap.api.config;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;

/**
 * Base configuration provider for the phoss AP.
 *
 * @author Philip Helger
 */
@Immutable
public final class APConfiguration
{
  private static final IConfigWithFallback DEFAULT_INSTANCE = new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ());
  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  private static IConfigWithFallback s_aConfig = DEFAULT_INSTANCE;

  private APConfiguration ()
  {}

  public static void setConfig (@NonNull final IConfigWithFallback aConfig)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    RW_LOCK.writeLocked ( () -> s_aConfig = aConfig);
  }

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
}
