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
package com.helger.phoss.ap.dirsender;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Configuration accessor for the directory-based SBD sender.
 *
 * @author Philip Helger
 * @since v0.2.0
 */
@Immutable
@SuppressWarnings ("removal")
public final class APDirSenderConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APDirSenderConfig.class);
  // Remember the legacy keys for which a deprecation warning was already logged, so it is emitted
  // only once per key
  private static final Set <String> WARNED_DEPRECATED_KEYS = ConcurrentHashMap.newKeySet ();

  private APDirSenderConfig ()
  {}

  @NonNull
  private static IConfigWithFallback _getConfig ()
  {
    return APConfigProvider.getConfig ();
  }

  @NonNull
  private static Duration _getDurationOrLegacy (@NonNull final String sDurationKey,
                                                @NonNull final String sLegacyMillisKey,
                                                @NonNull final Duration aDefault)
  {
    final Duration aDuration = _getConfig ().getAsConfigDuration (sDurationKey,
                                                                  sErr -> LOGGER.warn ("Failed to parse configuration key '" +
                                                                                       sDurationKey +
                                                                                       "' as duration: " +
                                                                                       sErr));
    if (aDuration != null)
      return aDuration;

    if (_getConfig ().containsConfiguredValue (sLegacyMillisKey))
    {
      if (WARNED_DEPRECATED_KEYS.add (sLegacyMillisKey))
        LOGGER.warn ("Configuration key '" +
                     sLegacyMillisKey +
                     "' is deprecated; please use '" +
                     sDurationKey +
                     "' with the duration grammar (e.g. '5s', '1m 30s') instead.");
      return Duration.ofMillis (_getConfig ().getAsLong (sLegacyMillisKey, aDefault.toMillis ()));
    }

    return aDefault;
  }

  /**
   * @return {@code true} if the directory sender is enabled.
   */
  public static boolean isEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.DIRSENDER_ENABLED,
                                       APConfigurationProperties.DIRSENDER_ENABLED_DEFAULT);
  }

  /**
   * @return The absolute path of the watch directory. May be <code>null</code> if not configured.
   */
  @Nullable
  public static String getDirectory ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.DIRSENDER_DIRECTORY);
  }

  /**
   * @return The interval between directory scans. Never <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getScanInterval ()
  {
    return _getDurationOrLegacy (APConfigurationProperties.DIRSENDER_SCAN_INTERVAL,
                                 APConfigurationProperties.DIRSENDER_SCAN_INTERVAL_MS,
                                 APConfigurationProperties.DIRSENDER_SCAN_INTERVAL_DEFAULT);
  }

  /**
   * @return The interval in milliseconds between directory scans.
   * @deprecated Since 0.9.0; use {@link #getScanInterval()} instead.
   */
  @Nonnegative
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static long getScanIntervalMs ()
  {
    return getScanInterval ().toMillis ();
  }

  /**
   * @return The delay before the first directory scan after startup. Never <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getInitialDelay ()
  {
    return _getDurationOrLegacy (APConfigurationProperties.DIRSENDER_INITIAL_DELAY,
                                 APConfigurationProperties.DIRSENDER_INITIAL_DELAY_MS,
                                 APConfigurationProperties.DIRSENDER_INITIAL_DELAY_DEFAULT);
  }

  /**
   * @return The delay in milliseconds before the first directory scan after startup.
   * @deprecated Since 0.9.0; use {@link #getInitialDelay()} instead.
   */
  @Nonnegative
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static long getInitialDelayMs ()
  {
    return getInitialDelay ().toMillis ();
  }
}
