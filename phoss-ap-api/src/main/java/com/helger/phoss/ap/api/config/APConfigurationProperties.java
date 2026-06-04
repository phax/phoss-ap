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

import java.time.Duration;

import com.helger.annotation.concurrent.Immutable;

/**
 * Constants for all configuration property keys used throughout phoss-ap. These keys are resolved
 * from the ph-config configuration sources (properties files, environment variables, system
 * properties).
 *
 * @author Philip Helger
 */
@Immutable
public final class APConfigurationProperties
{
  // Peppol
  public static final String PEPPOL_STAGE = "peppol.stage";
  public static final String PEPPOL_OWNER_SEATID = "peppol.owner.seatid";
  public static final String PEPPOL_OWNER_COUNTRYCODE = "peppol.owner.countrycode";
  public static final String PEPPOL_OWNER_COUNTRYCODE_DEFAULT = "XX";
  public static final String PEPPOL_SENDING_ENABLED = "peppol.sending.enabled";
  public static final boolean PEPPOL_SENDING_ENABLED_DEFAULT = true;
  public static final String PEPPOL_RECEIVING_ENABLED = "peppol.receiving.enabled";
  public static final boolean PEPPOL_RECEIVING_ENABLED_DEFAULT = true;
  public static final String PEPPOL_SMP_URL = "peppol.smp.url";
  public static final String PEPPOL_RECEIVER_CHECK_MODE = "peppol.receiver-check.mode";
  public static final String PEPPOL_DNS_SERVERS = "peppol.dns.servers";
  public static final String PEPPOL_IDENTIFIER_MODE = "peppol.identifier.mode";
  // Certificate revocation (since 0.9.0)
  public static final String PEPPOL_REVOCATION_SOFT_FAIL = "peppol.revocation.soft-fail";
  public static final boolean PEPPOL_REVOCATION_SOFT_FAIL_DEFAULT = false;

  // AS4 endpoint
  public static final String PHASE4_ENDPOINT_ADDRESS = "phase4.endpoint.address";
  public static final String PHASE4_API_REQUIREDTOKEN = "phase4.api.requiredtoken";
  public static final String PHASE4_DUMP_MODE = "phase4.dump.mode";
  public static final String PHASE4_DUMP_MODE_DEFAULT = "direction";

  // Connection pooling
  public static final String JDBC_POOLING_MAX_CONNECTIONS = "pooling.max-connections";
  public static final int JDBC_POOLING_MAX_CONNECTIONS_DEFAULT = 8;
  /** @since 0.9.0 */
  public static final String JDBC_POOLING_MAX_WAIT = "pooling.max-wait";
  /** @since 0.9.0 */
  public static final Duration JDBC_POOLING_MAX_WAIT_DEFAULT = Duration.ofSeconds (10);
  /**
   * @deprecated Since 0.9.0; use {@link #JDBC_POOLING_MAX_WAIT} with the duration grammar instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String JDBC_POOLING_MAX_WAIT_MILLIS = "pooling.max-wait.millis";
  /** @deprecated Since 0.9.0; use {@link #JDBC_POOLING_MAX_WAIT_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long JDBC_POOLING_MAX_WAIT_MILLIS_DEFAULT = 10_000L;
  /** @since 0.9.0 */
  public static final String JDBC_POOLING_BETWEEN_EVICTIONS_RUNS = "pooling.between-evictions-runs";
  /** @since 0.9.0 */
  public static final Duration JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_DEFAULT = Duration.ofMinutes (5);
  /**
   * @deprecated Since 0.9.0; use {@link #JDBC_POOLING_BETWEEN_EVICTIONS_RUNS} with the duration
   *             grammar instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS = "pooling.between-evictions-runs.millis";
  /** @deprecated Since 0.9.0; use {@link #JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS_DEFAULT = 300_000L;
  /** @since 0.9.0 */
  public static final String JDBC_POOLING_MIN_EVICTABLE_IDLE = "pooling.min-evictable-idle";
  /** @since 0.9.0 */
  public static final Duration JDBC_POOLING_MIN_EVICTABLE_IDLE_DEFAULT = Duration.ofMinutes (30);
  /**
   * @deprecated Since 0.9.0; use {@link #JDBC_POOLING_MIN_EVICTABLE_IDLE} with the duration grammar
   *             instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS = "pooling.min-evictable-idle.millis";
  /** @deprecated Since 0.9.0; use {@link #JDBC_POOLING_MIN_EVICTABLE_IDLE_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS_DEFAULT = 1_800_000L;
  /** @since 0.9.0 */
  public static final String JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT = "pooling.remove-abandoned-timeout";
  /** @since 0.9.0 */
  public static final Duration JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_DEFAULT = Duration.ofMinutes (5);
  /**
   * @deprecated Since 0.9.0; use {@link #JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT} with the duration
   *             grammar instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS = "pooling.remove-abandoned-timeout.millis";
  /**
   * @deprecated Since 0.9.0; use {@link #JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_DEFAULT} instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS_DEFAULT = 300_000L;

  // Forwarding
  public static final String FORWARDING_MODE = "forwarding.mode";
  /**
   * Suffix appended to a forwarding configuration prefix to read the selected document forwarder
   * provider SPI ID. For the primary forwarder this results in <code>forwarding.spi.id</code>; for
   * secondary forwarders it results in <code>forwarding.secondary.{n}.spi.id</code>.
   *
   * @since 0.9.1
   */
  public static final String FORWARDING_SPI_ID_SUFFIX = "spi.id";

  // Forwarding - Secondary forwarders (since 0.9.0)
  /**
   * Indexed prefix for secondary forwarders. The mode of secondary <code>n</code> is read from
   * <code>forwarding.secondary.{n}.mode</code> and all forwarder-specific properties use the same
   * <code>forwarding.secondary.{n}.</code> base prefix (e.g.
   * <code>forwarding.secondary.1.http.endpoint</code>). Iteration starts at index 1 and stops at
   * the first index where no <code>.mode</code> is set.
   *
   * @since 0.9.0
   */
  public static final String FORWARDING_SECONDARY_PREFIX = "forwarding.secondary.";
  /**
   * Suffix appended to the secondary base prefix to read the forwarding mode.
   *
   * @since 0.9.0
   */
  public static final String FORWARDING_SECONDARY_MODE_SUFFIX = "mode";

  // Forwarding - C4 country code determination
  public static final String FORWARDING_C4_COUNTRYCODE_MODES = "forwarding.c4countrycode.modes";

  // Forwarding - HTTP
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_HTTP_MODE = "forwarding.http.mode";
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_HTTP_ENDPOINT = "forwarding.http.endpoint";
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_HTTP_HEADERS_PREFIX = "forwarding.http.headers.";

  // Forwarding - S3
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_S3_BUCKET = "forwarding.s3.bucket";
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_S3_REGION = "forwarding.s3.region";
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_S3_ACCESS_KEY_ID = "forwarding.s3.access-key-id";
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_S3_SECRET_ACCESS_KEY = "forwarding.s3.secret-access-key";
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_S3_KEY_PREFIX = "forwarding.s3.key-prefix";
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_S3_ENDPOINT = "forwarding.s3.endpoint";
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_S3_PATH_STYLE_ACCESS = "forwarding.s3.path-style-access";
  public static final boolean FORWARDING_S3_PATH_STYLE_ACCESS_DEFAULT = false;

  // Forwarding - Filesystem (since 0.2.0)
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_FILESYSTEM_DIRECTORY = "forwarding.filesystem.directory";
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String FORWARDING_FILESYSTEM_LAYOUT = "forwarding.filesystem.layout";
  public static final String FORWARDING_FILESYSTEM_LAYOUT_DEFAULT = "flat";

  // Retry sending
  public static final String RETRY_SENDING_MAX_ATTEMPTS = "retry.sending.max-attempts";
  public static final int RETRY_SENDING_MAX_ATTEMPTS_DEFAULT = 3;
  /** @since 0.9.0 */
  public static final String RETRY_SENDING_INITIAL_BACKOFF = "retry.sending.initial-backoff";
  /** @since 0.9.0 */
  public static final Duration RETRY_SENDING_INITIAL_BACKOFF_DEFAULT = Duration.ofMinutes (3);
  /**
   * @deprecated Since 0.9.0; use {@link #RETRY_SENDING_INITIAL_BACKOFF} with the duration grammar
   *             instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String RETRY_SENDING_INITIAL_BACKOFF_MS = "retry.sending.initial-backoff.ms";
  /** @deprecated Since 0.9.0; use {@link #RETRY_SENDING_INITIAL_BACKOFF_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long RETRY_SENDING_INITIAL_BACKOFF_MS_DEFAULT = 180_000L;
  public static final String RETRY_SENDING_BACKOFF_MULTIPLIER = "retry.sending.backoff-multiplier";
  public static final double RETRY_SENDING_BACKOFF_MULTIPLIER_DEFAULT = 2.0;
  /** @since 0.9.0 */
  public static final String RETRY_SENDING_MAX_BACKOFF = "retry.sending.max-backoff";
  /** @since 0.9.0 */
  public static final Duration RETRY_SENDING_MAX_BACKOFF_DEFAULT = Duration.ofHours (1);
  /**
   * @deprecated Since 0.9.0; use {@link #RETRY_SENDING_MAX_BACKOFF} with the duration grammar
   *             instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String RETRY_SENDING_MAX_BACKOFF_MS = "retry.sending.max-backoff.ms";
  /** @deprecated Since 0.9.0; use {@link #RETRY_SENDING_MAX_BACKOFF_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long RETRY_SENDING_MAX_BACKOFF_MS_DEFAULT = 3_600_000L;

  // Retry forwarding
  public static final String RETRY_FORWARDING_MAX_ATTEMPTS = "retry.forwarding.max-attempts";
  public static final int RETRY_FORWARDING_MAX_ATTEMPTS_DEFAULT = 3;
  /** @since 0.9.0 */
  public static final String RETRY_FORWARDING_INITIAL_BACKOFF = "retry.forwarding.initial-backoff";
  /** @since 0.9.0 */
  public static final Duration RETRY_FORWARDING_INITIAL_BACKOFF_DEFAULT = Duration.ofMinutes (1);
  /**
   * @deprecated Since 0.9.0; use {@link #RETRY_FORWARDING_INITIAL_BACKOFF} with the duration
   *             grammar instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String RETRY_FORWARDING_INITIAL_BACKOFF_MS = "retry.forwarding.initial-backoff.ms";
  /** @deprecated Since 0.9.0; use {@link #RETRY_FORWARDING_INITIAL_BACKOFF_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long RETRY_FORWARDING_INITIAL_BACKOFF_MS_DEFAULT = 60_000L;
  public static final String RETRY_FORWARDING_BACKOFF_MULTIPLIER = "retry.forwarding.backoff-multiplier";
  public static final double RETRY_FORWARDING_BACKOFF_MULTIPLIER_DEFAULT = 2.0;
  /** @since 0.9.0 */
  public static final String RETRY_FORWARDING_MAX_BACKOFF = "retry.forwarding.max-backoff";
  /** @since 0.9.0 */
  public static final Duration RETRY_FORWARDING_MAX_BACKOFF_DEFAULT = Duration.ofHours (1);
  /**
   * @deprecated Since 0.9.0; use {@link #RETRY_FORWARDING_MAX_BACKOFF} with the duration grammar
   *             instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String RETRY_FORWARDING_MAX_BACKOFF_MS = "retry.forwarding.max-backoff.ms";
  /** @deprecated Since 0.9.0; use {@link #RETRY_FORWARDING_MAX_BACKOFF_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long RETRY_FORWARDING_MAX_BACKOFF_MS_DEFAULT = 3_600_000L;

  // Retry scheduler
  /** @since 0.9.0 */
  public static final String RETRY_SCHEDULER_INTERVAL = "retry.scheduler.interval";
  /** @since 0.9.0 */
  public static final Duration RETRY_SCHEDULER_INTERVAL_DEFAULT = Duration.ofMinutes (1);
  /**
   * @deprecated Since 0.9.0; use {@link #RETRY_SCHEDULER_INTERVAL} with the duration grammar
   *             instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String RETRY_SCHEDULER_INTERVAL_MS = "retry.scheduler.interval.ms";
  /** @deprecated Since 0.9.0; use {@link #RETRY_SCHEDULER_INTERVAL_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long RETRY_SCHEDULER_INTERVAL_MS_DEFAULT = 60_000L;
  public static final String RETRY_SCHEDULER_BATCH_SIZE = "retry.scheduler.batch-size";
  public static final int RETRY_SCHEDULER_BATCH_SIZE_DEFAULT = 50;

  // Circuit breaker
  public static final String CIRCUIT_BREAKER_FAILURE_THRESHOLD = "circuit-breaker.failure-threshold";
  public static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD_DEFAULT = 5;
  /** @since 0.9.0 */
  public static final String CIRCUIT_BREAKER_OPEN_DURATION = "circuit-breaker.open-duration";
  /** @since 0.9.0 */
  public static final Duration CIRCUIT_BREAKER_OPEN_DURATION_DEFAULT = Duration.ofMinutes (1);
  /**
   * @deprecated Since 0.9.0; use {@link #CIRCUIT_BREAKER_OPEN_DURATION} with the duration grammar
   *             instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String CIRCUIT_BREAKER_OPEN_DURATION_MS = "circuit-breaker.open-duration.ms";
  /** @deprecated Since 0.9.0; use {@link #CIRCUIT_BREAKER_OPEN_DURATION_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long CIRCUIT_BREAKER_OPEN_DURATION_MS_DEFAULT = 60_000L;
  public static final String CIRCUIT_BREAKER_HALF_OPEN_MAX_ATTEMPTS = "circuit-breaker.half-open-max-attempts";
  public static final int CIRCUIT_BREAKER_HALF_OPEN_MAX_ATTEMPTS_DEFAULT = 1;

  // Verification
  public static final String VERIFICATION_OUTBOUND_ENABLED = "verification.outbound.enabled";
  public static final boolean VERIFICATION_OUTBOUND_ENABLED_DEFAULT = false;
  public static final String VERIFICATION_INBOUND_ENABLED = "verification.inbound.enabled";
  public static final boolean VERIFICATION_INBOUND_ENABLED_DEFAULT = false;
  public static final String VERIFICATION_PHORM_URL = "verification.phorm.url";
  public static final String VERIFICATION_PHORM_TOKEN = "verification.phorm.token";

  // MLS
  public static final String MLS_SENDING_ENABLED = "mls.sending.enabled";
  public static final boolean MLS_SENDING_ENABLED_DEFAULT = true;
  public static final String MLS_TYPE = "mls.type";

  // Reporting
  public static final String PEPPOL_REPORTING_SCHEDULE_ENABLED = "peppol.reporting.schedule.enabled";
  public static final boolean PEPPOL_REPORTING_SCHEDULE_ENABLED_DEFAULT = true;
  public static final String PEPPOL_REPORTING_SCHEDULE_DAY_OF_MONTH = "peppol.reporting.schedule.day-of-month";
  public static final int PEPPOL_REPORTING_SCHEDULE_DAY_OF_MONTH_DEFAULT = 2;
  public static final String PEPPOL_REPORTING_SCHEDULE_HOUR = "peppol.reporting.schedule.hour";
  public static final int PEPPOL_REPORTING_SCHEDULE_HOUR_DEFAULT = 6;
  public static final String PEPPOL_REPORTING_SCHEDULE_MINUTE = "peppol.reporting.schedule.minute";
  public static final int PEPPOL_REPORTING_SCHEDULE_MINUTE_DEFAULT = 7;

  // Duplicate detection
  public static final String DUPLICATE_DETECTION_AS4_MODE = "duplicate.detection.as4.mode";
  public static final String DUPLICATE_DETECTION_SBDH_MODE = "duplicate.detection.sbdh.mode";

  // Archival
  public static final String ARCHIVAL_SCHEDULER_ENABLED = "archival.scheduler.enabled";
  public static final boolean ARCHIVAL_SCHEDULER_ENABLED_DEFAULT = true;
  /** @since 0.9.0 */
  public static final String ARCHIVAL_SCHEDULER_INTERVAL = "archival.scheduler.interval";
  /** @since 0.9.0 */
  public static final Duration ARCHIVAL_SCHEDULER_INTERVAL_DEFAULT = Duration.ofHours (1);
  /**
   * @deprecated Since 0.9.0; use {@link #ARCHIVAL_SCHEDULER_INTERVAL} with the duration grammar
   *             instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String ARCHIVAL_SCHEDULER_INTERVAL_MS = "archival.scheduler.interval.ms";
  /** @deprecated Since 0.9.0; use {@link #ARCHIVAL_SCHEDULER_INTERVAL_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long ARCHIVAL_SCHEDULER_INTERVAL_MS_DEFAULT = 3_600_000L;
  public static final String ARCHIVAL_SCHEDULER_BATCH_SIZE = "archival.scheduler.batch-size";
  public static final int ARCHIVAL_SCHEDULER_BATCH_SIZE_DEFAULT = 100;

  // Cleanup of archived transactions (since 0.9.0)
  public static final String CLEANUP_SCHEDULER_ENABLED = "cleanup.scheduler.enabled";
  public static final boolean CLEANUP_SCHEDULER_ENABLED_DEFAULT = false;
  public static final String CLEANUP_SCHEDULER_INTERVAL = "cleanup.scheduler.interval";
  public static final String CLEANUP_SCHEDULER_INTERVAL_DEFAULT = "24h";
  public static final String CLEANUP_SCHEDULER_RETENTION = "cleanup.scheduler.retention";
  public static final String CLEANUP_SCHEDULER_RETENTION_DEFAULT = "90d";
  public static final Duration CLEANUP_SCHEDULER_RETENTION_MIN = Duration.ofDays (2);
  public static final String CLEANUP_SCHEDULER_BATCH_SIZE = "cleanup.scheduler.batch-size";
  public static final int CLEANUP_SCHEDULER_BATCH_SIZE_DEFAULT = 100;

  // Document storage
  public static final String STORAGE_MODE = "storage.mode";
  public static final String STORAGE_INBOUND_PATH = "storage.inbound.path";
  public static final String STORAGE_INBOUND_PATH_DEFAULT = System.getProperty ("user.home") + "/phoss-ap/inbound";
  public static final String STORAGE_OUTBOUND_PATH = "storage.outbound.path";
  public static final String STORAGE_OUTBOUND_PATH_DEFAULT = System.getProperty ("user.home") + "/phoss-ap/outbound";

  // Document storage - S3 (since 0.1.1)
  public static final String STORAGE_S3_REGION = "storage.s3.region";
  public static final String STORAGE_S3_BUCKET = "storage.s3.bucket";
  public static final String STORAGE_S3_ACCESS_KEY_ID = "storage.s3.access-key-id";
  public static final String STORAGE_S3_SECRET_ACCESS_KEY = "storage.s3.secret-access-key";
  public static final String STORAGE_S3_ENDPOINT = "storage.s3.endpoint";
  public static final String STORAGE_S3_PATH_STYLE_ACCESS = "storage.s3.path-style-access";
  public static final boolean STORAGE_S3_PATH_STYLE_ACCESS_DEFAULT = false;

  // Outbound S3 submission (sender uploads to S3, AP fetches) (since 0.1.1)
  public static final String OUTBOUND_S3_ENABLED = "outbound.s3.enabled";
  public static final boolean OUTBOUND_S3_ENABLED_DEFAULT = false;
  public static final String OUTBOUND_S3_REGION = "outbound.s3.region";
  public static final String OUTBOUND_S3_BUCKET = "outbound.s3.bucket";
  public static final String OUTBOUND_S3_ACCESS_KEY_ID = "outbound.s3.access-key-id";
  public static final String OUTBOUND_S3_SECRET_ACCESS_KEY = "outbound.s3.secret-access-key";
  public static final String OUTBOUND_S3_ENDPOINT = "outbound.s3.endpoint";
  public static final String OUTBOUND_S3_PATH_STYLE_ACCESS = "outbound.s3.path-style-access";
  public static final boolean OUTBOUND_S3_PATH_STYLE_ACCESS_DEFAULT = false;

  // Directory sender (since 0.2.0)
  public static final String DIRSENDER_ENABLED = "dirsender.enabled";
  public static final boolean DIRSENDER_ENABLED_DEFAULT = false;
  public static final String DIRSENDER_DIRECTORY = "dirsender.directory";
  /** @since 0.9.0 */
  public static final String DIRSENDER_SCAN_INTERVAL = "dirsender.scan-interval";
  /** @since 0.9.0 */
  public static final Duration DIRSENDER_SCAN_INTERVAL_DEFAULT = Duration.ofSeconds (30);
  /**
   * @deprecated Since 0.9.0; use {@link #DIRSENDER_SCAN_INTERVAL} with the duration grammar
   *             instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String DIRSENDER_SCAN_INTERVAL_MS = "dirsender.scan-interval.ms";
  /** @deprecated Since 0.9.0; use {@link #DIRSENDER_SCAN_INTERVAL_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long DIRSENDER_SCAN_INTERVAL_MS_DEFAULT = 30_000L;
  /** @since 0.9.0 */
  public static final String DIRSENDER_INITIAL_DELAY = "dirsender.initial-delay";
  /** @since 0.9.0 */
  public static final Duration DIRSENDER_INITIAL_DELAY_DEFAULT = Duration.ofSeconds (30);
  /**
   * @deprecated Since 0.9.0; use {@link #DIRSENDER_INITIAL_DELAY} with the duration grammar
   *             instead.
   */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String DIRSENDER_INITIAL_DELAY_MS = "dirsender.initial-delay.ms";
  /** @deprecated Since 0.9.0; use {@link #DIRSENDER_INITIAL_DELAY_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long DIRSENDER_INITIAL_DELAY_MS_DEFAULT = 30_000L;

  // Management
  public static final String MANAGEMENT_STATUS_ENABLED = "management.status.enabled";
  public static final boolean MANAGEMENT_STATUS_ENABLED_DEFAULT = true;

  // Shutdown / Startup
  /** @since 0.9.0 */
  public static final String SHUTDOWN_TIMEOUT = "shutdown.timeout";
  /** @since 0.9.0 */
  public static final Duration SHUTDOWN_TIMEOUT_DEFAULT = Duration.ofSeconds (30);
  /** @deprecated Since 0.9.0; use {@link #SHUTDOWN_TIMEOUT} with the duration grammar instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final String SHUTDOWN_TIMEOUT_MS = "shutdown.timeout.ms";
  /** @deprecated Since 0.9.0; use {@link #SHUTDOWN_TIMEOUT_DEFAULT} instead. */
  @Deprecated (forRemoval = true, since = "0.9.0")
  public static final long SHUTDOWN_TIMEOUT_MS_DEFAULT = 30_000L;
  public static final String STARTUP_RECOVERY_ENABLED = "startup.recovery.enabled";
  public static final boolean STARTUP_RECOVERY_ENABLED_DEFAULT = true;

  private APConfigurationProperties ()
  {}
}
