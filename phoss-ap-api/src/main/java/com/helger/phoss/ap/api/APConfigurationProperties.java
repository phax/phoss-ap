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
package com.helger.phoss.ap.api;

import com.helger.annotation.concurrent.Immutable;

/**
 * Constants for all configuration property keys used throughout phoss-ap. These
 * keys are resolved from the ph-config configuration sources (properties files,
 * environment variables, system properties).
 *
 * @author Philip Helger
 */
@Immutable
public final class APConfigurationProperties
{
  // General
  public static final String GLOBAL_DEBUG = "global.debug";
  public static final String GLOBAL_PRODUCTION = "global.production";
  public static final String GLOBAL_NOSTARTUPINFO = "global.nostartupinfo";
  public static final String GLOBAL_DATAPATH = "global.datapath";

  // Peppol
  public static final String PEPPOL_STAGE = "peppol.stage";
  public static final String PEPPOL_SEATID = "peppol.seatid";
  public static final String PEPPOL_OWNER_COUNTRYCODE = "peppol.owner.countrycode";
  public static final String PEPPOL_SENDING_ENABLED = "peppol.sending.enabled";
  public static final String PEPPOL_RECEIVING_ENABLED = "peppol.receiving.enabled";

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
  public static final String JDBC_EXECUTION_TIME_WARNING_MS = "jdbc.execution-time-warning.ms";
  public static final String JDBC_DEBUG_CONNECTIONS = "jdbc.debug.connections";
  public static final String JDBC_DEBUG_TRANSACTIONS = "jdbc.debug.transactions";
  public static final String JDBC_DEBUG_SQL = "jdbc.debug.sql";

  // Connection pooling
  public static final String JDBC_POOLING_MAX_CONNECTIONS = "jdbc.pooling.max-connections";
  public static final String JDBC_POOLING_MAX_WAIT_MILLIS = "jdbc.pooling.max-wait.millis";
  public static final String JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS = "jdbc.pooling.between-evictions-runs.millis";
  public static final String JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS = "jdbc.pooling.min-evictable-idle.millis";
  public static final String JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS = "jdbc.pooling.remove-abandoned-timeout.millis";

  // Flyway
  public static final String FLYWAY_ENABLED = "flyway.enabled";
  public static final String FLYWAY_JDBC_URL = "flyway.jdbc.url";
  public static final String FLYWAY_JDBC_USER = "flyway.jdbc.user";
  public static final String FLYWAY_JDBC_PASSWORD = "flyway.jdbc.password";
  public static final String FLYWAY_JDBC_SCHEMA_CREATE = "flyway.jdbc.schema-create";
  public static final String FLYWAY_BASELINE_VERSION = "flyway.baseline.version";

  // Forwarding
  public static final String FORWARDING_MODE = "forwarding.mode";
  public static final String FORWARDING_HTTP_ENDPOINT = "forwarding.http.endpoint";
  public static final String FORWARDING_HTTP_CONNECTION_TIMEOUT_MS = "forwarding.http.connection-timeout.ms";
  public static final String FORWARDING_HTTP_RESPONSE_TIMEOUT_MS = "forwarding.http.response-timeout.ms";

  // S3
  public static final String FORWARDING_S3_BUCKET = "forwarding.s3.bucket";
  public static final String FORWARDING_S3_REGION = "forwarding.s3.region";
  public static final String FORWARDING_S3_ACCESS_KEY_ID = "forwarding.s3.access-key-id";
  public static final String FORWARDING_S3_SECRET_ACCESS_KEY = "forwarding.s3.secret-access-key";
  public static final String FORWARDING_S3_KEY_PREFIX = "forwarding.s3.key-prefix";
  public static final String FORWARDING_S3_LINK_ENDPOINT = "forwarding.s3.link.endpoint";

  // SFTP
  public static final String FORWARDING_SFTP_HOST = "forwarding.sftp.host";
  public static final String FORWARDING_SFTP_PORT = "forwarding.sftp.port";
  public static final String FORWARDING_SFTP_CONNECTION_TIMEOUT_MS = "forwarding.sftp.connectiontimeoutms";
  public static final String FORWARDING_SFTP_USER = "forwarding.sftp.user";
  public static final String FORWARDING_SFTP_PASSWORD = "forwarding.sftp.password";
  public static final String FORWARDING_SFTP_KEYPAIR_PRIVATEKEYPATH = "forwarding.sftp.keypair.privatekeypath";
  public static final String FORWARDING_SFTP_KEYPAIR_PUBLICKEYPATH = "forwarding.sftp.keypair.publickeypath";
  public static final String FORWARDING_SFTP_KEYPAIR_PASSPHRASE = "forwarding.sftp.keypair.passphrase";
  public static final String FORWARDING_SFTP_KNOWNHOSTSPATH = "forwarding.sftp.knownhostspath";
  public static final String FORWARDING_SFTP_MAXCONNECTIONS = "forwarding.sftp.maxconnections";
  public static final String FORWARDING_SFTP_DIRECTORY = "forwarding.sftp.directory";

  // Retry
  public static final String RETRY_SENDING_MAX_ATTEMPTS = "retry.sending.max-attempts";
  public static final String RETRY_SENDING_INITIAL_BACKOFF_MS = "retry.sending.initial-backoff.ms";
  public static final String RETRY_SENDING_BACKOFF_MULTIPLIER = "retry.sending.backoff-multiplier";
  public static final String RETRY_SENDING_MAX_BACKOFF_MS = "retry.sending.max-backoff.ms";
  public static final String RETRY_FORWARDING_MAX_ATTEMPTS = "retry.forwarding.max-attempts";
  public static final String RETRY_FORWARDING_INITIAL_BACKOFF_MS = "retry.forwarding.initial-backoff.ms";
  public static final String RETRY_FORWARDING_BACKOFF_MULTIPLIER = "retry.forwarding.backoff-multiplier";
  public static final String RETRY_FORWARDING_MAX_BACKOFF_MS = "retry.forwarding.max-backoff.ms";
  public static final String RETRY_SCHEDULER_INTERVAL_MS = "retry.scheduler.interval.ms";

  // Circuit breaker
  public static final String CIRCUIT_BREAKER_FAILURE_THRESHOLD = "circuit-breaker.failure-threshold";
  public static final String CIRCUIT_BREAKER_OPEN_DURATION_MS = "circuit-breaker.open-duration.ms";
  public static final String CIRCUIT_BREAKER_HALF_OPEN_MAX_ATTEMPTS = "circuit-breaker.half-open-max-attempts";

  // Verification
  public static final String VERIFICATION_OUTBOUND_ENABLED = "verification.outbound.enabled";
  public static final String VERIFICATION_INBOUND_ENABLED = "verification.inbound.enabled";

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
  public static final String ARCHIVAL_SCHEDULER_INTERVAL_MS = "archival.scheduler.interval.ms";

  // Notification
  public static final String NOTIFICATION_ENABLED = "notification.enabled";

  // HTTP Proxy
  public static final String HTTP_PROXY_ENABLED = "http.proxy.enabled";
  public static final String HTTP_PROXY_HOST = "http.proxy.host";
  public static final String HTTP_PROXY_PORT = "http.proxy.port";
  public static final String HTTP_PROXY_USERNAME = "http.proxy.username";
  public static final String HTTP_PROXY_PASSWORD = "http.proxy.password";
  public static final String HTTP_PROXY_NON_PROXY_HOSTS = "http.proxy.nonProxyHosts";

  // Shutdown / Startup
  public static final String SHUTDOWN_TIMEOUT_MS = "shutdown.timeout.ms";
  public static final String STARTUP_RECOVERY_ENABLED = "startup.recovery.enabled";

  private APConfigurationProperties ()
  {}
}
