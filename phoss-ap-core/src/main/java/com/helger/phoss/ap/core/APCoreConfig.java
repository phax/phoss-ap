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

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.value.parser.ConfigDurationParser;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.codelist.EAS4DumpMode;
import com.helger.phoss.ap.api.codelist.EC4CountryCodeMode;
import com.helger.phoss.ap.api.codelist.EDuplicateDetectionMode;
import com.helger.phoss.ap.api.codelist.EReceiverCheckMode;
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
@SuppressWarnings ("removal")
public final class APCoreConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APCoreConfig.class);
  // Remember the legacy keys for which a deprecation warning was already logged, so it is emitted
  // only once per key
  private static final Set <String> WARNED_DEPRECATED_KEYS = ConcurrentHashMap.newKeySet ();

  private APCoreConfig ()
  {}

  @NonNull
  private static IConfigWithFallback _getConfig ()
  {
    return APConfigProvider.getConfig ();
  }

  /**
   * Resolve a duration-typed configuration value, preferring the duration-grammar key over the
   * legacy millisecond-typed key. The duration-grammar key accepts compound expressions like
   * <code>10s</code>, <code>5m</code>, <code>2d 5h 30m</code>. If the duration key is missing,
   * blank, or fails to parse, the legacy <code>.ms</code> key is read as a <code>long</code>; if it
   * too is absent the supplied default is returned.
   *
   * @param sDurationKey
   *        The duration-grammar configuration key (e.g.
   *        <code>"retry.sending.initial-backoff"</code>).
   * @param sLegacyMillisKey
   *        The legacy millisecond-typed configuration key (e.g.
   *        <code>"retry.sending.initial-backoff.ms"</code>).
   * @param aDefault
   *        The default value if neither key is configured.
   * @return The resolved duration. Never <code>null</code>.
   */
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
   * @return {@code true} if the experimental offline mode is enabled.
   */
  public static boolean isOfflineMode ()
  {
    // This property is not documented by purpose, as it is experimental
    return _getConfig ().getAsBoolean ("phossap.offlinemode.enabled", false);
  }

  /**
   * @return {@code true} if outbound sending should bypass SMP lookup and send back to this AP's
   *         own AS4 endpoint. This is intentionally enabled only on the Peppol test network.
   * @since 0.10.2
   */
  public static boolean isOutboundDevLoopbackEnabled ()
  {
    // Configuration enabled?
    if (!_getConfig ().getAsBoolean (APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED,
                                     APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED_DEFAULT))
      return false;

    // Only on test
    final EPeppolNetwork ePeppolStage = getPeppolStage ();
    return ePeppolStage != null && ePeppolStage.isTest ();
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
   * @return The configured receiver check mode. Defaults to {@link EReceiverCheckMode#NONE}. Never
   *         <code>null</code>.
   */
  @NonNull
  public static EReceiverCheckMode getReceiverCheckMode ()
  {
    final String sVal = _getConfig ().getAsString (APConfigurationProperties.PEPPOL_RECEIVER_CHECK_MODE);
    return EReceiverCheckMode.getFromIDOrDefault (sVal);
  }

  /**
   * @return The configured DNS server hostnames or IP addresses for Peppol NAPTR lookup, as a
   *         comma-separated string. May be <code>null</code> if not configured (system default DNS
   *         is used).
   * @since 0.1.3
   */
  @Nullable
  public static String getPeppolDnsServers ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.PEPPOL_DNS_SERVERS);
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
   * @return {@code true} if certificate revocation soft-fail mode is enabled. When enabled,
   *         certificate validation succeeds even if the revocation status cannot be determined
   *         (e.g. due to network errors reaching the CRL or OCSP responder). Maps to
   *         {@link com.helger.security.revocation.CertificateRevocationCheckerDefaults#setAllowSoftFail(boolean)}.
   * @since 0.9.0
   */
  public static boolean isRevocationSoftFailAllowed ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.PEPPOL_REVOCATION_SOFT_FAIL,
                                       APConfigurationProperties.PEPPOL_REVOCATION_SOFT_FAIL_DEFAULT);
  }

  /**
   * @return The configured C4 country code determination modes as an ordered list. May be empty if
   *         no automatic determination is configured (only async API reporting). Never
   *         <code>null</code>.
   * @since v0.1.3
   */
  @NonNull
  public static ICommonsOrderedSet <EC4CountryCodeMode> getC4CountryCodeModes ()
  {
    final String sVal = _getConfig ().getAsString (APConfigurationProperties.FORWARDING_C4_COUNTRYCODE_MODES);
    final ICommonsOrderedSet <EC4CountryCodeMode> ret = new CommonsLinkedHashSet <> ();
    if (StringHelper.isNotEmpty (sVal))
    {
      for (final String sPart : StringHelper.getExploded (',', sVal))
      {
        final EC4CountryCodeMode eMode = EC4CountryCodeMode.getFromIDOrNull (sPart.trim ());
        if (eMode != null)
          ret.add (eMode);
      }
    }
    return ret;
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
   * @return The initial backoff duration for outbound sending retries. Never <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getRetrySendingInitialBackoff ()
  {
    return _getDurationOrLegacy (APConfigurationProperties.RETRY_SENDING_INITIAL_BACKOFF,
                                 APConfigurationProperties.RETRY_SENDING_INITIAL_BACKOFF_MS,
                                 APConfigurationProperties.RETRY_SENDING_INITIAL_BACKOFF_DEFAULT);
  }

  /**
   * @return The initial backoff duration in milliseconds for outbound sending retries.
   * @deprecated Since 0.9.0; use {@link #getRetrySendingInitialBackoff()} instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static long getRetrySendingInitialBackoffMs ()
  {
    return getRetrySendingInitialBackoff ().toMillis ();
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
   * @return The maximum backoff duration for outbound sending retries. Never <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getRetrySendingMaxBackoff ()
  {
    return _getDurationOrLegacy (APConfigurationProperties.RETRY_SENDING_MAX_BACKOFF,
                                 APConfigurationProperties.RETRY_SENDING_MAX_BACKOFF_MS,
                                 APConfigurationProperties.RETRY_SENDING_MAX_BACKOFF_DEFAULT);
  }

  /**
   * @return The maximum backoff duration in milliseconds for outbound sending retries.
   * @deprecated Since 0.9.0; use {@link #getRetrySendingMaxBackoff()} instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static long getRetrySendingMaxBackoffMs ()
  {
    return getRetrySendingMaxBackoff ().toMillis ();
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
   * @return The initial backoff duration for inbound forwarding retries. Never <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getRetryForwardingInitialBackoff ()
  {
    return _getDurationOrLegacy (APConfigurationProperties.RETRY_FORWARDING_INITIAL_BACKOFF,
                                 APConfigurationProperties.RETRY_FORWARDING_INITIAL_BACKOFF_MS,
                                 APConfigurationProperties.RETRY_FORWARDING_INITIAL_BACKOFF_DEFAULT);
  }

  /**
   * @return The initial backoff duration in milliseconds for inbound forwarding retries.
   * @deprecated Since 0.9.0; use {@link #getRetryForwardingInitialBackoff()} instead.
   */
  @Nonnegative
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static long getRetryForwardingInitialBackoffMs ()
  {
    return getRetryForwardingInitialBackoff ().toMillis ();
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
   * @return The maximum backoff duration for inbound forwarding retries. Never <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getRetryForwardingMaxBackoff ()
  {
    return _getDurationOrLegacy (APConfigurationProperties.RETRY_FORWARDING_MAX_BACKOFF,
                                 APConfigurationProperties.RETRY_FORWARDING_MAX_BACKOFF_MS,
                                 APConfigurationProperties.RETRY_FORWARDING_MAX_BACKOFF_DEFAULT);
  }

  /**
   * @return The maximum backoff duration in milliseconds for inbound forwarding retries.
   * @deprecated Since 0.9.0; use {@link #getRetryForwardingMaxBackoff()} instead.
   */
  @Nonnegative
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static long getRetryForwardingMaxBackoffMs ()
  {
    return getRetryForwardingMaxBackoff ().toMillis ();
  }

  /**
   * @return The interval at which the retry scheduler checks for transactions to retry. Never
   *         <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getRetrySchedulerInterval ()
  {
    return _getDurationOrLegacy (APConfigurationProperties.RETRY_SCHEDULER_INTERVAL,
                                 APConfigurationProperties.RETRY_SCHEDULER_INTERVAL_MS,
                                 APConfigurationProperties.RETRY_SCHEDULER_INTERVAL_DEFAULT);
  }

  /**
   * @return The interval in milliseconds at which the retry scheduler checks for transactions to
   *         retry.
   * @deprecated Since 0.9.0; use {@link #getRetrySchedulerInterval()} instead.
   */
  @Nonnegative
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static long getRetrySchedulerIntervalMs ()
  {
    return getRetrySchedulerInterval ().toMillis ();
  }

  /**
   * @return The number of transactions to retry per scheduler cycle. Default is {@code 50}.
   * @since 0.1.2
   */
  @Nonnegative
  public static int getRetrySchedulerBatchSize ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.RETRY_SCHEDULER_BATCH_SIZE,
                                   APConfigurationProperties.RETRY_SCHEDULER_BATCH_SIZE_DEFAULT);
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
   * @return The duration the circuit breaker stays open before transitioning to half-open. Never
   *         <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getCircuitBreakerOpenDuration ()
  {
    return _getDurationOrLegacy (APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION,
                                 APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION_MS,
                                 APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION_DEFAULT);
  }

  /**
   * @return The duration in milliseconds the circuit breaker stays open before transitioning to
   *         half-open.
   * @deprecated Since 0.9.0; use {@link #getCircuitBreakerOpenDuration()} instead.
   */
  @Nonnegative
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static long getCircuitBreakerOpenDurationMs ()
  {
    return getCircuitBreakerOpenDuration ().toMillis ();
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
   * @return {@code true} if the outbound S3 submission endpoint is enabled.
   * @since v0.1.1
   */
  public static boolean isOutboundS3Enabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.OUTBOUND_S3_ENABLED,
                                       APConfigurationProperties.OUTBOUND_S3_ENABLED_DEFAULT);
  }

  /**
   * @return The configured S3 region for the outbound sender bucket. May be <code>null</code>.
   * @since v0.1.1
   */
  @Nullable
  public static String getOutboundS3Region ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.OUTBOUND_S3_REGION);
  }

  /**
   * @return The configured S3 bucket from which the AP fetches sender-uploaded documents. May be
   *         <code>null</code>.
   * @since v0.1.1
   */
  @Nullable
  public static String getOutboundS3Bucket ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.OUTBOUND_S3_BUCKET);
  }

  /**
   * @return The configured S3 access key ID for the outbound sender bucket. May be
   *         <code>null</code> for IAM role-based authentication.
   * @since v0.1.1
   */
  @Nullable
  public static String getOutboundS3AccessKeyID ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.OUTBOUND_S3_ACCESS_KEY_ID);
  }

  /**
   * @return The configured S3 secret access key for the outbound sender bucket. May be
   *         <code>null</code> for IAM role-based authentication.
   * @since v0.1.1
   */
  @Nullable
  public static String getOutboundS3SecretAccessKey ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.OUTBOUND_S3_SECRET_ACCESS_KEY);
  }

  /**
   * @return The optional custom S3 endpoint URL for the outbound sender bucket. May be
   *         <code>null</code> for standard AWS S3.
   * @since 0.2.2
   */
  @Nullable
  public static String getOutboundS3Endpoint ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.OUTBOUND_S3_ENDPOINT);
  }

  /**
   * @return <code>true</code> if S3 path-style access should be used for the outbound sender
   *         bucket.
   * @since 0.2.2
   */
  public static boolean isOutboundS3PathStyleAccess ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.OUTBOUND_S3_PATH_STYLE_ACCESS,
                                       APConfigurationProperties.OUTBOUND_S3_PATH_STYLE_ACCESS_DEFAULT);
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
   * @return {@code true} if MLS sending is globally enabled. Defaults to {@code true}.
   * @since v0.1.2
   */
  public static boolean isMlsSendingEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.MLS_SENDING_ENABLED,
                                       APConfigurationProperties.MLS_SENDING_ENABLED_DEFAULT);
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
   * @return The interval at which the archival scheduler runs. Never <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getArchivalSchedulerInterval ()
  {
    return _getDurationOrLegacy (APConfigurationProperties.ARCHIVAL_SCHEDULER_INTERVAL,
                                 APConfigurationProperties.ARCHIVAL_SCHEDULER_INTERVAL_MS,
                                 APConfigurationProperties.ARCHIVAL_SCHEDULER_INTERVAL_DEFAULT);
  }

  /**
   * @return The interval in milliseconds at which the archival scheduler runs.
   * @deprecated Since 0.9.0; use {@link #getArchivalSchedulerInterval()} instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static long getArchivalSchedulerIntervalMs ()
  {
    return getArchivalSchedulerInterval ().toMillis ();
  }

  /**
   * @return The number of transactions to archive per scheduler cycle. Default is {@code 100}.
   * @since 0.1.2
   */
  @Nonnegative
  public static int getArchivalSchedulerBatchSize ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.ARCHIVAL_SCHEDULER_BATCH_SIZE,
                                   APConfigurationProperties.ARCHIVAL_SCHEDULER_BATCH_SIZE_DEFAULT);
  }

  /**
   * @return {@code true} if the cleanup scheduler that deletes archived transactions older than the
   *         configured retention is enabled. Defaults to {@code false}.
   * @since 0.9.0
   */
  public static boolean isCleanupSchedulerEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.CLEANUP_SCHEDULER_ENABLED,
                                       APConfigurationProperties.CLEANUP_SCHEDULER_ENABLED_DEFAULT);
  }

  /**
   * @return The interval at which the cleanup scheduler runs. Configured as a duration string (e.g.
   *         {@code "24h"}, {@code "2d 12h"}). Defaults to
   *         {@link APConfigurationProperties#CLEANUP_SCHEDULER_INTERVAL_DEFAULT}. Never
   *         <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getCleanupSchedulerInterval ()
  {
    final Duration aRet = _getConfig ().getAsConfigDuration (APConfigurationProperties.CLEANUP_SCHEDULER_INTERVAL,
                                                             sErr -> LOGGER.warn ("Invalid value for '" +
                                                                                  APConfigurationProperties.CLEANUP_SCHEDULER_INTERVAL +
                                                                                  "': " +
                                                                                  sErr));
    if (aRet != null)
      return aRet;
    return ConfigDurationParser.parseDuration (APConfigurationProperties.CLEANUP_SCHEDULER_INTERVAL_DEFAULT);
  }

  /**
   * @return The retention duration: archived transactions whose {@code completed_dt} is older than
   *         this value are eligible for cleanup. Configured as a duration string (e.g.
   *         {@code "90d"}, {@code "26w"} not supported — use {@code "182d"}). Defaults to
   *         {@link APConfigurationProperties#CLEANUP_SCHEDULER_RETENTION_DEFAULT}. Never
   *         <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getCleanupSchedulerRetention ()
  {
    final Duration aRet = _getConfig ().getAsConfigDuration (APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION,
                                                             sErr -> LOGGER.warn ("Invalid value for '" +
                                                                                  APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION +
                                                                                  "': " +
                                                                                  sErr));
    if (aRet != null)
      return aRet;
    return ConfigDurationParser.parseDuration (APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION_DEFAULT);
  }

  /**
   * @return The number of archived transactions to clean up per scheduler cycle. Default is
   *         {@code 100}.
   * @since 0.9.0
   */
  @Nonnegative
  public static int getCleanupSchedulerBatchSize ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.CLEANUP_SCHEDULER_BATCH_SIZE,
                                   APConfigurationProperties.CLEANUP_SCHEDULER_BATCH_SIZE_DEFAULT);
  }

  /**
   * @return {@code true} if the management status endpoint is enabled. Defaults to {@code true}.
   * @since 0.1.3
   */
  public static boolean isManagementStatusEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.MANAGEMENT_STATUS_ENABLED,
                                       APConfigurationProperties.MANAGEMENT_STATUS_ENABLED_DEFAULT);
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
   * @return The graceful shutdown timeout. Never <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  public static Duration getShutdownTimeout ()
  {
    return _getDurationOrLegacy (APConfigurationProperties.SHUTDOWN_TIMEOUT,
                                 APConfigurationProperties.SHUTDOWN_TIMEOUT_MS,
                                 APConfigurationProperties.SHUTDOWN_TIMEOUT_DEFAULT);
  }

  /**
   * @return The graceful shutdown timeout in milliseconds.
   * @deprecated Since 0.9.0; use {@link #getShutdownTimeout()} instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static long getShutdownTimeoutMs ()
  {
    return getShutdownTimeout ().toMillis ();
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
