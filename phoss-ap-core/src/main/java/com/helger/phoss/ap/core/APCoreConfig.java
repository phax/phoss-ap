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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.phoss.ap.api.codelist.EDuplicateDetectionMode;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

@Immutable
public final class APCoreConfig
{
  private APCoreConfig ()
  {}

  @NonNull
  private static IConfigWithFallback _getConfig ()
  {
    return APConfigProvider.getConfig ();
  }

  // Peppol
  @Nullable
  public static EPeppolNetwork getPeppolStage ()
  {
    final String sStageID = _getConfig ().getAsString (APConfigurationProperties.PEPPOL_STAGE);
    return EPeppolNetwork.getFromIDOrNull (sStageID);
  }

  @Nullable
  public static String getPeppolSeatID ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.PEPPOL_SEATID);
  }

  @NonNull
  public static String getPeppolOwnerCountryCode ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.PEPPOL_OWNER_COUNTRYCODE,
                                      APConfigurationProperties.PEPPOL_OWNER_COUNTRYCODE_DEFAULT);
  }

  public static boolean isSendingEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.PEPPOL_SENDING_ENABLED,
                                       APConfigurationProperties.PEPPOL_SENDING_ENABLED_DEFAULT);
  }

  public static boolean isReceivingEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.PEPPOL_RECEIVING_ENABLED,
                                       APConfigurationProperties.PEPPOL_RECEIVING_ENABLED_DEFAULT);
  }

  // AS4
  @Nullable
  public static String getPhase4EndpointAddress ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.PHASE4_ENDPOINT_ADDRESS);
  }

  @Nullable
  public static String getPhase4ApiRequiredToken ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.PHASE4_API_REQUIREDTOKEN);
  }

  @Nullable
  public static String getPhase4DumpPath ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.PHASE4_DUMP_PATH);
  }

  // Retry sending
  public static int getRetrySendingMaxAttempts ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.RETRY_SENDING_MAX_ATTEMPTS,
                                   APConfigurationProperties.RETRY_SENDING_MAX_ATTEMPTS_DEFAULT);
  }

  public static long getRetrySendingInitialBackoffMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.RETRY_SENDING_INITIAL_BACKOFF_MS,
                                    APConfigurationProperties.RETRY_SENDING_INITIAL_BACKOFF_MS_DEFAULT);
  }

  public static double getRetrySendingBackoffMultiplier ()
  {
    return _getConfig ().getAsDouble (APConfigurationProperties.RETRY_SENDING_BACKOFF_MULTIPLIER,
                                      APConfigurationProperties.RETRY_SENDING_BACKOFF_MULTIPLIER_DEFAULT);
  }

  public static long getRetrySendingMaxBackoffMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.RETRY_SENDING_MAX_BACKOFF_MS,
                                    APConfigurationProperties.RETRY_SENDING_MAX_BACKOFF_MS_DEFAULT);
  }

  // Retry forwarding
  @Nonnegative
  public static int getRetryForwardingMaxAttempts ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.RETRY_FORWARDING_MAX_ATTEMPTS,
                                   APConfigurationProperties.RETRY_FORWARDING_MAX_ATTEMPTS_DEFAULT);
  }

  @Nonnegative
  public static long getRetryForwardingInitialBackoffMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.RETRY_FORWARDING_INITIAL_BACKOFF_MS,
                                    APConfigurationProperties.RETRY_FORWARDING_INITIAL_BACKOFF_MS_DEFAULT);
  }

  @Nonnegative
  public static double getRetryForwardingBackoffMultiplier ()
  {
    return _getConfig ().getAsDouble (APConfigurationProperties.RETRY_FORWARDING_BACKOFF_MULTIPLIER,
                                      APConfigurationProperties.RETRY_FORWARDING_BACKOFF_MULTIPLIER_DEFAULT);
  }

  @Nonnegative
  public static long getRetryForwardingMaxBackoffMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.RETRY_FORWARDING_MAX_BACKOFF_MS,
                                    APConfigurationProperties.RETRY_FORWARDING_MAX_BACKOFF_MS_DEFAULT);
  }

  @Nonnegative
  public static long getRetrySchedulerIntervalMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.RETRY_SCHEDULER_INTERVAL_MS,
                                    APConfigurationProperties.RETRY_SCHEDULER_INTERVAL_MS_DEFAULT);
  }

  // Circuit breaker
  @Nonnegative
  public static int getCircuitBreakerFailureThreshold ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD,
                                   APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD_DEFAULT);
  }

  @Nonnegative
  public static long getCircuitBreakerOpenDurationMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION_MS,
                                    APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION_MS_DEFAULT);
  }

  @Nonnegative
  public static int getCircuitBreakerHalfOpenMaxAttempts ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.CIRCUIT_BREAKER_HALF_OPEN_MAX_ATTEMPTS,
                                   APConfigurationProperties.CIRCUIT_BREAKER_HALF_OPEN_MAX_ATTEMPTS_DEFAULT);
  }

  // Verification
  public static boolean isVerificationOutboundEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.VERIFICATION_OUTBOUND_ENABLED,
                                       APConfigurationProperties.VERIFICATION_OUTBOUND_ENABLED_DEFAULT);
  }

  public static boolean isVerificationInboundEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.VERIFICATION_INBOUND_ENABLED,
                                       APConfigurationProperties.VERIFICATION_INBOUND_ENABLED_DEFAULT);
  }

  // MLS
  @NonNull
  public static EPeppolMLSType getMlsType ()
  {
    final String sVal = _getConfig ().getAsString (APConfigurationProperties.MLS_TYPE);
    final EPeppolMLSType eRet = EPeppolMLSType.getFromIDOrNull (sVal);
    return eRet != null ? eRet : EPeppolMLSType.ALWAYS_SEND;
  }

  // Duplicate detection
  @NonNull
  public static EDuplicateDetectionMode getDuplicateDetectionAS4Mode ()
  {
    final String sVal = _getConfig ().getAsString (APConfigurationProperties.DUPLICATE_DETECTION_AS4_MODE);
    final EDuplicateDetectionMode eRet = EDuplicateDetectionMode.getFromIDOrNull (sVal);
    return eRet != null ? eRet : EDuplicateDetectionMode.REJECT;
  }

  @NonNull
  public static EDuplicateDetectionMode getDuplicateDetectionSBDHMode ()
  {
    final String sVal = _getConfig ().getAsString (APConfigurationProperties.DUPLICATE_DETECTION_SBDH_MODE);
    final EDuplicateDetectionMode eRet = EDuplicateDetectionMode.getFromIDOrNull (sVal);
    return eRet != null ? eRet : EDuplicateDetectionMode.REJECT;
  }

  // Archival
  public static boolean isArchivalSchedulerEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.ARCHIVAL_SCHEDULER_ENABLED,
                                       APConfigurationProperties.ARCHIVAL_SCHEDULER_ENABLED_DEFAULT);
  }

  public static long getArchivalSchedulerIntervalMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.ARCHIVAL_SCHEDULER_INTERVAL_MS,
                                    APConfigurationProperties.ARCHIVAL_SCHEDULER_INTERVAL_MS_DEFAULT);
  }

  // Notification
  public static boolean isNotificationEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.NOTIFICATION_ENABLED,
                                       APConfigurationProperties.NOTIFICATION_ENABLED_DEFAULT);
  }

  // Startup recovery
  public static boolean isStartupRecoveryEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.STARTUP_RECOVERY_ENABLED,
                                       APConfigurationProperties.STARTUP_RECOVERY_ENABLED_DEFAULT);
  }

  public static long getShutdownTimeoutMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.SHUTDOWN_TIMEOUT_MS,
                                    APConfigurationProperties.SHUTDOWN_TIMEOUT_MS_DEFAULT);
  }

  // Peppol Reporting
  public static boolean isPeppolReportingScheduled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_ENABLED,
                                       APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_ENABLED_DEFAULT);
  }

  @CheckForSigned
  public static int getPeppolReportingScheduleDayOfMonth ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_DAY_OF_MONTH,
                                   APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_DAY_OF_MONTH_DEFAULT);
  }

  @CheckForSigned
  public static int getPeppolReportingScheduleHour ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_HOUR,
                                   APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_HOUR_DEFAULT);
  }

  @CheckForSigned
  public static int getPeppolReportingScheduleMinute ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_MINUTE,
                                   APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_MINUTE_DEFAULT);
  }
}
