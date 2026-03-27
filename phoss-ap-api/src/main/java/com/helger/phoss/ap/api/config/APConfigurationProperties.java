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
  public static final String PEPPOL_IDENTIFIER_MODE = "peppol.identifier.mode";

  // AS4 endpoint
  public static final String PHASE4_ENDPOINT_ADDRESS = "phase4.endpoint.address";
  public static final String PHASE4_API_REQUIREDTOKEN = "phase4.api.requiredtoken";
  public static final String PHASE4_DUMP_MODE = "phase4.dump.mode";
  public static final String PHASE4_DUMP_MODE_DEFAULT = "direction";

  // Connection pooling
  public static final String JDBC_POOLING_MAX_CONNECTIONS = "pooling.max-connections";
  public static final int JDBC_POOLING_MAX_CONNECTIONS_DEFAULT = 8;
  public static final String JDBC_POOLING_MAX_WAIT_MILLIS = "pooling.max-wait.millis";
  public static final long JDBC_POOLING_MAX_WAIT_MILLIS_DEFAULT = 10_000L;
  public static final String JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS = "pooling.between-evictions-runs.millis";
  public static final long JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS_DEFAULT = 300_000L;
  public static final String JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS = "pooling.min-evictable-idle.millis";
  public static final long JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS_DEFAULT = 1_800_000L;
  public static final String JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS = "pooling.remove-abandoned-timeout.millis";
  public static final long JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS_DEFAULT = 300_000L;

  // Forwarding
  public static final String FORWARDING_MODE = "forwarding.mode";

  // Forwarding - HTTP
  public static final String FORWARDING_HTTP_MODE = "forwarding.http.mode";
  public static final String FORWARDING_HTTP_ENDPOINT = "forwarding.http.endpoint";

  // Forwarding - S3
  public static final String FORWARDING_S3_BUCKET = "forwarding.s3.bucket";
  public static final String FORWARDING_S3_REGION = "forwarding.s3.region";
  public static final String FORWARDING_S3_ACCESS_KEY_ID = "forwarding.s3.access-key-id";
  public static final String FORWARDING_S3_SECRET_ACCESS_KEY = "forwarding.s3.secret-access-key";
  public static final String FORWARDING_S3_KEY_PREFIX = "forwarding.s3.key-prefix";

  // Retry sending
  public static final String RETRY_SENDING_MAX_ATTEMPTS = "retry.sending.max-attempts";
  public static final int RETRY_SENDING_MAX_ATTEMPTS_DEFAULT = 3;
  public static final String RETRY_SENDING_INITIAL_BACKOFF_MS = "retry.sending.initial-backoff.ms";
  public static final long RETRY_SENDING_INITIAL_BACKOFF_MS_DEFAULT = 60_000L;
  public static final String RETRY_SENDING_BACKOFF_MULTIPLIER = "retry.sending.backoff-multiplier";
  public static final double RETRY_SENDING_BACKOFF_MULTIPLIER_DEFAULT = 2.0;
  public static final String RETRY_SENDING_MAX_BACKOFF_MS = "retry.sending.max-backoff.ms";
  public static final long RETRY_SENDING_MAX_BACKOFF_MS_DEFAULT = 3_600_000L;

  // Retry forwarding
  public static final String RETRY_FORWARDING_MAX_ATTEMPTS = "retry.forwarding.max-attempts";
  public static final int RETRY_FORWARDING_MAX_ATTEMPTS_DEFAULT = 3;
  public static final String RETRY_FORWARDING_INITIAL_BACKOFF_MS = "retry.forwarding.initial-backoff.ms";
  public static final long RETRY_FORWARDING_INITIAL_BACKOFF_MS_DEFAULT = 60_000L;
  public static final String RETRY_FORWARDING_BACKOFF_MULTIPLIER = "retry.forwarding.backoff-multiplier";
  public static final double RETRY_FORWARDING_BACKOFF_MULTIPLIER_DEFAULT = 2.0;
  public static final String RETRY_FORWARDING_MAX_BACKOFF_MS = "retry.forwarding.max-backoff.ms";
  public static final long RETRY_FORWARDING_MAX_BACKOFF_MS_DEFAULT = 3_600_000L;

  // Retry scheduler
  public static final String RETRY_SCHEDULER_INTERVAL_MS = "retry.scheduler.interval.ms";
  public static final long RETRY_SCHEDULER_INTERVAL_MS_DEFAULT = 60_000L;

  // Circuit breaker
  public static final String CIRCUIT_BREAKER_FAILURE_THRESHOLD = "circuit-breaker.failure-threshold";
  public static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD_DEFAULT = 5;
  public static final String CIRCUIT_BREAKER_OPEN_DURATION_MS = "circuit-breaker.open-duration.ms";
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
  public static final String ARCHIVAL_SCHEDULER_INTERVAL_MS = "archival.scheduler.interval.ms";
  public static final long ARCHIVAL_SCHEDULER_INTERVAL_MS_DEFAULT = 3_600_000L;
  public static final String ARCHIVAL_SCHEDULER_BATCH_SIZE = "archival.scheduler.batch-size";
  public static final int ARCHIVAL_SCHEDULER_BATCH_SIZE_DEFAULT = 100;

  // Document storage
  public static final String STORAGE_MODE = "storage.mode";
  public static final String STORAGE_INBOUND_PATH = "storage.inbound.path";
  public static final String STORAGE_INBOUND_PATH_DEFAULT = "/var/phoss-ap/inbound";
  public static final String STORAGE_OUTBOUND_PATH = "storage.outbound.path";
  public static final String STORAGE_OUTBOUND_PATH_DEFAULT = "/var/phoss-ap/outbound";

  // Document storage - S3 (since 0.1.1)
  public static final String STORAGE_S3_REGION = "storage.s3.region";
  public static final String STORAGE_S3_BUCKET = "storage.s3.bucket";
  public static final String STORAGE_S3_ACCESS_KEY_ID = "storage.s3.access-key-id";
  public static final String STORAGE_S3_SECRET_ACCESS_KEY = "storage.s3.secret-access-key";

  // Outbound S3 submission (sender uploads to S3, AP fetches) (since 0.1.1)
  public static final String OUTBOUND_S3_ENABLED = "outbound.s3.enabled";
  public static final boolean OUTBOUND_S3_ENABLED_DEFAULT = false;
  public static final String OUTBOUND_S3_REGION = "outbound.s3.region";
  public static final String OUTBOUND_S3_BUCKET = "outbound.s3.bucket";
  public static final String OUTBOUND_S3_ACCESS_KEY_ID = "outbound.s3.access-key-id";
  public static final String OUTBOUND_S3_SECRET_ACCESS_KEY = "outbound.s3.secret-access-key";

  // Shutdown / Startup
  public static final String SHUTDOWN_TIMEOUT_MS = "shutdown.timeout.ms";
  public static final long SHUTDOWN_TIMEOUT_MS_DEFAULT = 30_000L;
  public static final String STARTUP_RECOVERY_ENABLED = "startup.recovery.enabled";
  public static final boolean STARTUP_RECOVERY_ENABLED_DEFAULT = true;

  private APConfigurationProperties ()
  {}
}
