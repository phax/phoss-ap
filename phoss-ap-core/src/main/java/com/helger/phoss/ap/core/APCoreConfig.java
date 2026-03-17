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
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.codelist.EAS4DumpMode;
import com.helger.phoss.ap.api.codelist.EDuplicateDetectionMode;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Configuration accessor for core AP settings including Peppol network stage, AS4 phase4 settings,
 * retry and circuit breaker parameters, MLS type, duplicate detection, archival scheduling, and
 * Peppol Reporting schedules. All values are read from the central {@link APConfigProvider}
 * configuration.
 *
 * @author Philip Helger
 */
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

  /**
   * @return {@code true} if the experimental offline mode is enabled.
   */
  public static boolean isOfflineMode ()
  {
    // This property is not documented by purpose, as it is experimental
    return _getConfig ().getAsBoolean ("phossap.offlinemode.enabled", false);
  }

  /**
   * @return The configured Peppol network stage (production or test). May be <code>null</code> if
   *         not configured.
   */
  @Nullable
  public static EPeppolNetwork getPeppolStage ()
  {
    final String sStageID = _getConfig ().getAsString (APConfigurationProperties.PEPPOL_STAGE);
    return EPeppolNetwork.getFromIDOrNull (sStageID);
  }

  /**
   * @return The configured Peppol Seat ID (e.g. {@code "POP000001"}). May be <code>null</code> if
   *         not configured.
   */
  @Nullable
  public static String getPeppolOwnerSeatID ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.PEPPOL_OWNER_SEATID);
  }

  /**
   * @return The Peppol Service Provider ID derived from the Seat ID by removing the 3-character
   *         prefix. May be <code>null</code> if the Seat ID is not configured or invalid.
   */
  @Nullable
  public static String getPeppolOwnerSPID ()
  {
    final String sSeatID = getPeppolOwnerSeatID ();
    return CPhossAP.isPeppolSeatID (sSeatID) ? sSeatID.substring (3) : null;
  }

  /**
   * @return The configured country code of the AP operator (e.g. {@code "AT"}). Never
   *         <code>null</code>.
   */
  @NonNull
  public static String getPeppolOwnerCountryCode ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.PEPPOL_OWNER_COUNTRYCODE,
                                      APConfigurationProperties.PEPPOL_OWNER_COUNTRYCODE_DEFAULT);
  }

  /**
   * @return The configured SMP URL for receiver checks. May be <code>null</code> if not configured.
   */
  @Nullable
  public static String getPeppolSmpUrl ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.PEPPOL_SMP_URL);
  }

  /**
   * @return {@code true} if outbound sending via the Peppol network is enabled.
   */
  public static boolean isSendingEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.PEPPOL_SENDING_ENABLED,
                                       APConfigurationProperties.PEPPOL_SENDING_ENABLED_DEFAULT);
  }

  /**
   * @return {@code true} if inbound receiving via the Peppol network is enabled.
   */
  public static boolean isReceivingEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.PEPPOL_RECEIVING_ENABLED,
                                       APConfigurationProperties.PEPPOL_RECEIVING_ENABLED_DEFAULT);
  }

  /**
   * @return The configured phase4 AS4 endpoint address URL. May be <code>null</code> if not
   *         configured.
   */
  @Nullable
  public static String getPhase4EndpointAddress ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.PHASE4_ENDPOINT_ADDRESS);
  }

  /**
   * @return The required API token for phase4 API access. May be <code>null</code> if not
   *         configured.
   */
  @Nullable
  public static String getPhase4ApiRequiredToken ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.PHASE4_API_REQUIREDTOKEN);
  }

  /**
   * @return The configured AS4 dump mode. Defaults to {@link EAS4DumpMode#DIRECTION}. Never
   *         <code>null</code>.
   */
  @NonNull
  public static EAS4DumpMode getPhase4DumpMode ()
  {
    final String sVal = _getConfig ().getAsString (APConfigurationProperties.PHASE4_DUMP_MODE,
                                                   APConfigurationProperties.PHASE4_DUMP_MODE_DEFAULT);
    return EAS4DumpMode.getFromIDOrDefault (sVal);
  }

  /**
   * @return The maximum number of retry attempts for outbound sending.
   */
  public static int getRetrySendingMaxAttempts ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.RETRY_SENDING_MAX_ATTEMPTS,
                                   APConfigurationProperties.RETRY_SENDING_MAX_ATTEMPTS_DEFAULT);
  }

  /**
   * @return The initial backoff duration in milliseconds for outbound sending retries.
   */
  public static long getRetrySendingInitialBackoffMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.RETRY_SENDING_INITIAL_BACKOFF_MS,
                                    APConfigurationProperties.RETRY_SENDING_INITIAL_BACKOFF_MS_DEFAULT);
  }

  /**
   * @return The backoff multiplier for outbound sending retries.
   */
  public static double getRetrySendingBackoffMultiplier ()
  {
    return _getConfig ().getAsDouble (APConfigurationProperties.RETRY_SENDING_BACKOFF_MULTIPLIER,
                                      APConfigurationProperties.RETRY_SENDING_BACKOFF_MULTIPLIER_DEFAULT);
  }

  /**
   * @return The maximum backoff duration in milliseconds for outbound sending retries.
   */
  public static long getRetrySendingMaxBackoffMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.RETRY_SENDING_MAX_BACKOFF_MS,
                                    APConfigurationProperties.RETRY_SENDING_MAX_BACKOFF_MS_DEFAULT);
  }

  /**
   * @return The maximum number of retry attempts for inbound forwarding.
   */
  @Nonnegative
  public static int getRetryForwardingMaxAttempts ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.RETRY_FORWARDING_MAX_ATTEMPTS,
                                   APConfigurationProperties.RETRY_FORWARDING_MAX_ATTEMPTS_DEFAULT);
  }

  /**
   * @return The initial backoff duration in milliseconds for inbound forwarding retries.
   */
  @Nonnegative
  public static long getRetryForwardingInitialBackoffMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.RETRY_FORWARDING_INITIAL_BACKOFF_MS,
                                    APConfigurationProperties.RETRY_FORWARDING_INITIAL_BACKOFF_MS_DEFAULT);
  }

  /**
   * @return The backoff multiplier for inbound forwarding retries.
   */
  @Nonnegative
  public static double getRetryForwardingBackoffMultiplier ()
  {
    return _getConfig ().getAsDouble (APConfigurationProperties.RETRY_FORWARDING_BACKOFF_MULTIPLIER,
                                      APConfigurationProperties.RETRY_FORWARDING_BACKOFF_MULTIPLIER_DEFAULT);
  }

  /**
   * @return The maximum backoff duration in milliseconds for inbound forwarding retries.
   */
  @Nonnegative
  public static long getRetryForwardingMaxBackoffMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.RETRY_FORWARDING_MAX_BACKOFF_MS,
                                    APConfigurationProperties.RETRY_FORWARDING_MAX_BACKOFF_MS_DEFAULT);
  }

  /**
   * @return The interval in milliseconds at which the retry scheduler checks for transactions to
   *         retry.
   */
  @Nonnegative
  public static long getRetrySchedulerIntervalMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.RETRY_SCHEDULER_INTERVAL_MS,
                                    APConfigurationProperties.RETRY_SCHEDULER_INTERVAL_MS_DEFAULT);
  }

  /**
   * @return The number of consecutive failures before the circuit breaker opens.
   */
  @Nonnegative
  public static int getCircuitBreakerFailureThreshold ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD,
                                   APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD_DEFAULT);
  }

  /**
   * @return The duration in milliseconds the circuit breaker stays open before transitioning to
   *         half-open.
   */
  @Nonnegative
  public static long getCircuitBreakerOpenDurationMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION_MS,
                                    APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION_MS_DEFAULT);
  }

  /**
   * @return The maximum number of attempts allowed in half-open state before the circuit breaker
   *         closes again.
   */
  @Nonnegative
  public static int getCircuitBreakerHalfOpenMaxAttempts ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.CIRCUIT_BREAKER_HALF_OPEN_MAX_ATTEMPTS,
                                   APConfigurationProperties.CIRCUIT_BREAKER_HALF_OPEN_MAX_ATTEMPTS_DEFAULT);
  }

  /**
   * @return {@code true} if outbound document verification via SPI is enabled.
   */
  public static boolean isVerificationOutboundEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.VERIFICATION_OUTBOUND_ENABLED,
                                       APConfigurationProperties.VERIFICATION_OUTBOUND_ENABLED_DEFAULT);
  }

  /**
   * @return {@code true} if inbound document verification via SPI is enabled.
   */
  public static boolean isVerificationInboundEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.VERIFICATION_INBOUND_ENABLED,
                                       APConfigurationProperties.VERIFICATION_INBOUND_ENABLED_DEFAULT);
  }

  /**
   * @return The configured MLS type strategy (e.g. always send, failure only). Defaults to
   *         {@link EPeppolMLSType#ALWAYS_SEND}. Never <code>null</code>.
   */
  @NonNull
  public static EPeppolMLSType getMlsType ()
  {
    final String sVal = _getConfig ().getAsString (APConfigurationProperties.MLS_TYPE);
    final EPeppolMLSType eRet = EPeppolMLSType.getFromIDOrNull (sVal);
    return eRet != null ? eRet : EPeppolMLSType.ALWAYS_SEND;
  }

  /**
   * @return The configured duplicate detection mode for AS4 message IDs. Defaults to
   *         {@link EDuplicateDetectionMode#REJECT}. Never <code>null</code>.
   */
  @NonNull
  public static EDuplicateDetectionMode getDuplicateDetectionAS4Mode ()
  {
    final String sVal = _getConfig ().getAsString (APConfigurationProperties.DUPLICATE_DETECTION_AS4_MODE);
    final EDuplicateDetectionMode eRet = EDuplicateDetectionMode.getFromIDOrNull (sVal);
    return eRet != null ? eRet : EDuplicateDetectionMode.REJECT;
  }

  /**
   * @return The configured duplicate detection mode for SBDH instance IDs. Defaults to
   *         {@link EDuplicateDetectionMode#REJECT}. Never <code>null</code>.
   */
  @NonNull
  public static EDuplicateDetectionMode getDuplicateDetectionSBDHMode ()
  {
    final String sVal = _getConfig ().getAsString (APConfigurationProperties.DUPLICATE_DETECTION_SBDH_MODE);
    final EDuplicateDetectionMode eRet = EDuplicateDetectionMode.getFromIDOrNull (sVal);
    return eRet != null ? eRet : EDuplicateDetectionMode.REJECT;
  }

  /**
   * @return {@code true} if the archival background scheduler is enabled.
   */
  public static boolean isArchivalSchedulerEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.ARCHIVAL_SCHEDULER_ENABLED,
                                       APConfigurationProperties.ARCHIVAL_SCHEDULER_ENABLED_DEFAULT);
  }

  /**
   * @return The interval in milliseconds at which the archival scheduler runs.
   */
  public static long getArchivalSchedulerIntervalMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.ARCHIVAL_SCHEDULER_INTERVAL_MS,
                                    APConfigurationProperties.ARCHIVAL_SCHEDULER_INTERVAL_MS_DEFAULT);
  }

  /**
   * @return {@code true} if startup recovery of in-flight transactions is enabled.
   */
  public static boolean isStartupRecoveryEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.STARTUP_RECOVERY_ENABLED,
                                       APConfigurationProperties.STARTUP_RECOVERY_ENABLED_DEFAULT);
  }

  /**
   * @return The graceful shutdown timeout in milliseconds.
   */
  public static long getShutdownTimeoutMs ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.SHUTDOWN_TIMEOUT_MS,
                                    APConfigurationProperties.SHUTDOWN_TIMEOUT_MS_DEFAULT);
  }

  /**
   * @return {@code true} if scheduled automatic Peppol Reporting is enabled.
   */
  public static boolean isPeppolReportingScheduled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_ENABLED,
                                       APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_ENABLED_DEFAULT);
  }

  /**
   * @return The day of month on which the Peppol Reporting schedule triggers.
   */
  @CheckForSigned
  public static int getPeppolReportingScheduleDayOfMonth ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_DAY_OF_MONTH,
                                   APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_DAY_OF_MONTH_DEFAULT);
  }

  /**
   * @return The hour of day at which the Peppol Reporting schedule triggers.
   */
  @CheckForSigned
  public static int getPeppolReportingScheduleHour ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_HOUR,
                                   APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_HOUR_DEFAULT);
  }

  /**
   * @return The minute of hour at which the Peppol Reporting schedule triggers.
   */
  @CheckForSigned
  public static int getPeppolReportingScheduleMinute ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_MINUTE,
                                   APConfigurationProperties.PEPPOL_REPORTING_SCHEDULE_MINUTE_DEFAULT);
  }
}
