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
package com.helger.phoss.ap.core.metrics;

import com.helger.annotation.concurrent.Immutable;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.telemetry.ITelemetryCounter;
import com.helger.telemetry.ITelemetryHistogram;
import com.helger.telemetry.TelemetryMetrics;

/**
 * Central registry of the phoss AP's named metric instruments. Each instrument is created once at
 * class-load time via the vendor-neutral {@link TelemetryMetrics} facade — if no
 * {@code ITelemetryMeterSPI} is registered the underlying instruments are cheap no-ops, so
 * referencing this class on a deployment without an observability backend has no cost.
 * <p>
 * Instruments are grouped by topic (inbound, outbound, reporting, schedulers, general); the
 * happy-path counter and its corresponding failure counter sit next to each other.
 *
 * @author Philip Helger
 */
@Immutable
public final class APMetrics
{
  // === Inbound ===

  public static final ITelemetryCounter INBOUND_RECEIVED = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_RECEIVED,
                                                                                     "Inbound AS4 messages received and persisted",
                                                                                     "{message}");
  public static final ITelemetryCounter INBOUND_RECEIVER_NOT_SERVICED = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_RECEIVER_NOT_SERVICED,
                                                                                                  "Inbound messages for which the receiver is not serviced by this AP",
                                                                                                  "{message}");
  public static final ITelemetryCounter INBOUND_DUPLICATE_REJECTIONS = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_DUPLICATE_REJECTIONS,
                                                                                                 "Inbound messages rejected because a duplicate AS4 Message ID or SBDH Instance ID was already received",
                                                                                                 "{message}");
  public static final ITelemetryCounter INBOUND_VERIFICATION_ACCEPTED = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_VERIFICATION_ACCEPTED,
                                                                                                  "Inbound documents that passed verification",
                                                                                                  "{document}");
  public static final ITelemetryCounter INBOUND_VERIFICATION_DEFERRED = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_VERIFICATION_DEFERRED,
                                                                                                 "Inbound documents whose verification was deferred, because a verifier made no verdict",
                                                                                                 "{document}");
  public static final ITelemetryCounter INBOUND_VERIFICATION_REJECTIONS = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_VERIFICATION_REJECTIONS,
                                                                                                    "Inbound transactions rejected by verification",
                                                                                                    "{transaction}");
  public static final ITelemetryCounter INBOUND_MLS_CORRELATED = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_MLS_CORRELATED,
                                                                                           "Inbound MLS messages successfully correlated to an outbound transaction",
                                                                                           "{message}");
  public static final ITelemetryCounter INBOUND_MLS_CORRELATION_ERRORS = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_MLS_CORRELATION_ERRORS,
                                                                                                   "Inbound MLS messages that could not be correlated to an outbound transaction",
                                                                                                   "{message}");
  public static final ITelemetryHistogram MLS_ROUNDTRIP_DURATION = TelemetryMetrics.histogram (CPhossAPOtel.METRIC_MLS_ROUNDTRIP_DURATION,
                                                                                               "Wall-clock duration from outbound send completion to MLS reception (powers MLS-1/MLS-2 SLA dashboards)",
                                                                                               "s");
  public static final ITelemetryCounter INBOUND_FORWARDED = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_FORWARDED,
                                                                                      "Inbound documents successfully forwarded to the Receiver Backend",
                                                                                      "{transaction}");
  public static final ITelemetryCounter INBOUND_VERIFICATION_REJECTIONS_FORWARDED = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_VERIFICATION_REJECTIONS_FORWARDED,
                                                                                                              "Inbound documents that were forwarded to the Receiver Backend despite having been rejected by verification",
                                                                                                              "{transaction}");
  public static final ITelemetryHistogram INBOUND_FORWARDING_DURATION = TelemetryMetrics.histogram (CPhossAPOtel.METRIC_INBOUND_FORWARDING_DURATION,
                                                                                                    "Wall-clock duration from inbound AS4 reception to successful forwarding",
                                                                                                    "s");
  public static final ITelemetryCounter INBOUND_FORWARDING_ERRORS = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_FORWARDING_ERRORS,
                                                                                              "Inbound forwarding attempts that failed (transient or permanent)",
                                                                                              "{attempt}");
  public static final ITelemetryCounter INBOUND_FORWARDING_PERMANENT_FAILURES = TelemetryMetrics.counter (CPhossAPOtel.METRIC_INBOUND_FORWARDING_PERMANENT_FAILURES,
                                                                                                         "Inbound transactions that exhausted all forwarding retries",
                                                                                                         "{transaction}");

  // === Outbound ===

  public static final ITelemetryCounter OUTBOUND_ACCEPTED = TelemetryMetrics.counter (CPhossAPOtel.METRIC_OUTBOUND_ACCEPTED,
                                                                                      "Outbound transactions accepted by the AP and queued for sending",
                                                                                      "{transaction}");
  public static final ITelemetryCounter OUTBOUND_VERIFICATION_ACCEPTED = TelemetryMetrics.counter (CPhossAPOtel.METRIC_OUTBOUND_VERIFICATION_ACCEPTED,
                                                                                                   "Outbound documents that passed verification",
                                                                                                   "{document}");
  public static final ITelemetryCounter OUTBOUND_VERIFICATION_REJECTIONS = TelemetryMetrics.counter (CPhossAPOtel.METRIC_OUTBOUND_VERIFICATION_REJECTIONS,
                                                                                                     "Outbound documents rejected by verification before sending",
                                                                                                     "{document}");
  public static final ITelemetryCounter OUTBOUND_SENT = TelemetryMetrics.counter (CPhossAPOtel.METRIC_OUTBOUND_SENT,
                                                                                  "Outbound transactions successfully sent via AS4 and receipt confirmed",
                                                                                  "{transaction}");
  public static final ITelemetryHistogram OUTBOUND_SENDING_DURATION = TelemetryMetrics.histogram (CPhossAPOtel.METRIC_OUTBOUND_SENDING_DURATION,
                                                                                                  "Wall-clock duration from outbound transaction creation to confirmed AS4 receipt",
                                                                                                  "s");
  public static final ITelemetryHistogram OUTBOUND_SENDING_ATTEMPTS = TelemetryMetrics.histogram (CPhossAPOtel.METRIC_OUTBOUND_SENDING_ATTEMPTS,
                                                                                                  "Number of AS4 sending attempts before confirmed receipt",
                                                                                                  "{attempt}");
  public static final ITelemetryCounter OUTBOUND_SENDING_PERMANENT_FAILURES = TelemetryMetrics.counter (CPhossAPOtel.METRIC_OUTBOUND_SENDING_PERMANENT_FAILURES,
                                                                                                       "Outbound transactions that exhausted all sending retries",
                                                                                                       "{transaction}");
  public static final ITelemetryCounter OUTBOUND_MLS_SPECIAL_TO_NOT_REACHABLE = TelemetryMetrics.counter (CPhossAPOtel.METRIC_OUTBOUND_MLS_SPECIAL_TO_NOT_REACHABLE,
                                                                                                         "Outbound MLS messages whose custom MLS_TO receiver was not reachable, triggering fallback to the default SPID",
                                                                                                         "{message}");

  // === Reporting ===

  public static final ITelemetryCounter REPORTING_SUCCESS = TelemetryMetrics.counter (CPhossAPOtel.METRIC_REPORTING_SUCCESS,
                                                                                      "Peppol Reporting (TSR/EUSR) reports successfully sent to OpenPeppol",
                                                                                      "{report}");
  public static final ITelemetryCounter REPORTING_FAILURES = TelemetryMetrics.counter (CPhossAPOtel.METRIC_REPORTING_FAILURES,
                                                                                       "Peppol Reporting (TSR/EUSR) generation/validation/sending failures",
                                                                                       "{report}");

  // === Schedulers ===

  public static final ITelemetryHistogram SCHEDULER_CYCLE_DURATION = TelemetryMetrics.histogram (CPhossAPOtel.METRIC_SCHEDULER_CYCLE_DURATION,
                                                                                                 "Wall-clock duration of a scheduler cycle, tagged with the scheduler name",
                                                                                                 "s");
  public static final ITelemetryHistogram SCHEDULER_CYCLE_ITEMS = TelemetryMetrics.histogram (CPhossAPOtel.METRIC_SCHEDULER_CYCLE_ITEMS,
                                                                                              "Number of items processed in one scheduler cycle, tagged with the scheduler name",
                                                                                              "{item}");

  // === General ===

  public static final ITelemetryCounter UNEXPECTED_EXCEPTIONS = TelemetryMetrics.counter (CPhossAPOtel.METRIC_UNEXPECTED_EXCEPTIONS,
                                                                                          "Unexpected exceptions raised inside the AP that are not covered by a more specific counter",
                                                                                          "{exception}");

  private APMetrics ()
  {}
}
