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
  // General
  public static final String GLOBAL_DEBUG = "global.debug";
  public static final boolean GLOBAL_DEBUG_DEFAULT = false;
  public static final String GLOBAL_PRODUCTION = "global.production";
  public static final boolean GLOBAL_PRODUCTION_DEFAULT = false;
  public static final String GLOBAL_NOSTARTUPINFO = "global.nostartupinfo";
  public static final String GLOBAL_DATAPATH = "global.datapath";

  // Peppol
  public static final String PEPPOL_STAGE = "peppol.stage";
  public static final String PEPPOL_SEATID = "peppol.seatid";
  public static final String PEPPOL_OWNER_COUNTRYCODE = "peppol.owner.countrycode";
  public static final String PEPPOL_OWNER_COUNTRYCODE_DEFAULT = "XX";
  public static final String PEPPOL_SENDING_ENABLED = "peppol.sending.enabled";
  public static final boolean PEPPOL_SENDING_ENABLED_DEFAULT = true;
  public static final String PEPPOL_RECEIVING_ENABLED = "peppol.receiving.enabled";
  public static final boolean PEPPOL_RECEIVING_ENABLED_DEFAULT = true;

  // AS4 endpoint
  public static final String PHASE4_ENDPOINT_ADDRESS = "phase4.endpoint.address";
  public static final String PHASE4_API_REQUIREDTOKEN = "phase4.api.requiredtoken";
  public static final String PHASE4_DUMP_PATH = "phase4.dump.path";

  // Database
  public static final String JDBC_DATABASE_TYPE = "jdbc.database-type";
  public static final String JDBC_DRIVER = "jdbc.driver";
  public static final String JDBC_URL = "jdbc.url";
  public static final String JDBC_USER = "jdbc.user";
  public static final String JDBC_PASSWORD = "jdbc.password";
  public static final String JDBC_SCHEMA = "jdbc.schema";
  public static final String JDBC_EXECUTION_TIME_WARNING_ENABLED = "jdbc.execution-time-warning.enabled";
  public static final boolean JDBC_EXECUTION_TIME_WARNING_ENABLED_DEFAULT = true;
  public static final String JDBC_EXECUTION_TIME_WARNING_MS = "jdbc.execution-time-warning.ms";
  public static final long JDBC_EXECUTION_TIME_WARNING_MS_DEFAULT = 1000L;
  public static final String JDBC_DEBUG_CONNECTIONS = "jdbc.debug.connections";
  public static final boolean JDBC_DEBUG_CONNECTIONS_DEFAULT = false;
  public static final String JDBC_DEBUG_TRANSACTIONS = "jdbc.debug.transactions";
  public static final boolean JDBC_DEBUG_TRANSACTIONS_DEFAULT = false;
  public static final String JDBC_DEBUG_SQL = "jdbc.debug.sql";
  public static final boolean JDBC_DEBUG_SQL_DEFAULT = false;

  // Connection pooling
  public static final String JDBC_POOLING_MAX_CONNECTIONS = "jdbc.pooling.max-connections";
  public static final int JDBC_POOLING_MAX_CONNECTIONS_DEFAULT = 8;
  public static final String JDBC_POOLING_MAX_WAIT_MILLIS = "jdbc.pooling.max-wait.millis";
  public static final long JDBC_POOLING_MAX_WAIT_MILLIS_DEFAULT = 10_000L;
  public static final String JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS = "jdbc.pooling.between-evictions-runs.millis";
  public static final long JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS_DEFAULT = 300_000L;
  public static final String JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS = "jdbc.pooling.min-evictable-idle.millis";
  public static final long JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS_DEFAULT = 1_800_000L;
  public static final String JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS = "jdbc.pooling.remove-abandoned-timeout.millis";
  public static final long JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS_DEFAULT = 300_000L;

  // Flyway
  public static final String FLYWAY_ENABLED = "flyway.enabled";
  public static final boolean FLYWAY_ENABLED_DEFAULT = true;
  public static final String FLYWAY_JDBC_URL = "flyway.jdbc.url";
  public static final String FLYWAY_JDBC_USER = "flyway.jdbc.user";
  public static final String FLYWAY_JDBC_PASSWORD = "flyway.jdbc.password";
  public static final String FLYWAY_JDBC_SCHEMA_CREATE = "flyway.jdbc.schema-create";
  public static final boolean FLYWAY_JDBC_SCHEMA_CREATE_DEFAULT = false;
  public static final String FLYWAY_BASELINE_VERSION = "flyway.baseline.version";
  public static final int FLYWAY_BASELINE_VERSION_DEFAULT = 0;

  // Forwarding
  public static final String FORWARDING_HTTP_ENDPOINT = "forwarding.http.endpoint";

  // S3
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

  // MLS
  public static final String MLS_TYPE = "mls.type";

  // Reporting
  public static final String PEPPOL_REPORTING_SENDERID = "peppol.reporting.senderid";
  public static final String PEPPOL_REPORTING_SCHEDULED = "peppol.reporting.scheduled";
  public static final String REPORTING_LEADER_ENABLED = "reporting.leader.enabled";
  public static final String PEPPOL_REPORTING_SCHEDULE_DAY_OF_MONTH = "peppol.reporting.schedule.day-of-month";
  public static final String PEPPOL_REPORTING_SCHEDULE_HOUR = "peppol.reporting.schedule.hour";
  public static final String PEPPOL_REPORTING_SCHEDULE_MINUTE = "peppol.reporting.schedule.minute";

  // Duplicate detection
  public static final String DUPLICATE_DETECTION_AS4_MODE = "duplicate.detection.as4.mode";
  public static final String DUPLICATE_DETECTION_SBDH_MODE = "duplicate.detection.sbdh.mode";

  // Archival
  public static final String ARCHIVAL_SCHEDULER_ENABLED = "archival.scheduler.enabled";
  public static final boolean ARCHIVAL_SCHEDULER_ENABLED_DEFAULT = true;
  public static final String ARCHIVAL_SCHEDULER_INTERVAL_MS = "archival.scheduler.interval.ms";
  public static final long ARCHIVAL_SCHEDULER_INTERVAL_MS_DEFAULT = 3_600_000L;

  // Notification
  public static final String NOTIFICATION_ENABLED = "notification.enabled";
  public static final boolean NOTIFICATION_ENABLED_DEFAULT = true;

  // HTTP Proxy
  public static final String HTTP_PROXY_ENABLED = "http.proxy.enabled";
  public static final boolean HTTP_PROXY_ENABLED_DEFAULT = false;
  public static final String HTTP_PROXY_HOST = "http.proxy.host";
  public static final String HTTP_PROXY_PORT = "http.proxy.port";
  public static final String HTTP_PROXY_USERNAME = "http.proxy.username";
  public static final String HTTP_PROXY_PASSWORD = "http.proxy.password";
  public static final String HTTP_PROXY_NON_PROXY_HOSTS = "http.proxy.nonProxyHosts";

  // Shutdown / Startup
  public static final String SHUTDOWN_TIMEOUT_MS = "shutdown.timeout.ms";
  public static final long SHUTDOWN_TIMEOUT_MS_DEFAULT = 30_000L;
  public static final String STARTUP_RECOVERY_ENABLED = "startup.recovery.enabled";
  public static final boolean STARTUP_RECOVERY_ENABLED_DEFAULT = true;

  private APConfigurationProperties ()
  {}
}
