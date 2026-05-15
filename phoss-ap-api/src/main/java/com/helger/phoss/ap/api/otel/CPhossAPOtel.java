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
package com.helger.phoss.ap.api.otel;

import com.helger.annotation.concurrent.Immutable;

import io.opentelemetry.api.common.AttributeKey;

/**
 * Constants for the phoss AP OpenTelemetry integration: instrumentation scope, metric names,
 * span names and attribute keys. Metric names follow OpenTelemetry semantic conventions (lowercase
 * dotted namespace) and use the <code>phoss.ap.*</code> prefix for AP-specific signals.
 * <p>
 * This class lives in <code>phoss-ap-api</code> so any module can record spans / instruments
 * without taking a compile-time dependency on <code>phoss-ap-otel</code>. The OTel SDK itself is
 * still wired in <code>phoss-ap-otel</code>.
 *
 * @author Philip Helger
 */
@Immutable
public final class CPhossAPOtel
{
  /**
   * Instrumentation scope name used for the {@link io.opentelemetry.api.trace.Tracer} and
   * {@link io.opentelemetry.api.metrics.Meter}.
   */
  public static final String INSTRUMENTATION_SCOPE_NAME = "com.helger.phoss.ap";

  // === Failure-side counters ===
  public static final String METRIC_INBOUND_VERIFICATION_REJECTIONS = "phoss.ap.inbound.verification.rejections";
  public static final String METRIC_OUTBOUND_VERIFICATION_REJECTIONS = "phoss.ap.outbound.verification.rejections";
  public static final String METRIC_INBOUND_RECEIVER_NOT_SERVICED = "phoss.ap.inbound.receiver.not_serviced";
  public static final String METRIC_INBOUND_MLS_CORRELATION_ERRORS = "phoss.ap.inbound.mls.correlation_errors";
  public static final String METRIC_INBOUND_FORWARDING_ERRORS = "phoss.ap.inbound.forwarding.errors";
  public static final String METRIC_INBOUND_FORWARDING_PERMANENT_FAILURES = "phoss.ap.inbound.forwarding.permanent_failures";
  public static final String METRIC_OUTBOUND_SENDING_PERMANENT_FAILURES = "phoss.ap.outbound.sending.permanent_failures";
  public static final String METRIC_REPORTING_FAILURES = "phoss.ap.reporting.failures";
  public static final String METRIC_UNEXPECTED_EXCEPTIONS = "phoss.ap.unexpected_exceptions";

  // === Lifecycle counters ===
  public static final String METRIC_INBOUND_RECEIVED = "phoss.ap.inbound.received";
  public static final String METRIC_INBOUND_VERIFICATION_ACCEPTED = "phoss.ap.inbound.verification.accepted";
  public static final String METRIC_INBOUND_MLS_CORRELATED = "phoss.ap.inbound.mls.correlated";
  public static final String METRIC_INBOUND_FORWARDED = "phoss.ap.inbound.forwarded";
  public static final String METRIC_OUTBOUND_ACCEPTED = "phoss.ap.outbound.accepted";
  public static final String METRIC_OUTBOUND_VERIFICATION_ACCEPTED = "phoss.ap.outbound.verification.accepted";
  public static final String METRIC_OUTBOUND_SENT = "phoss.ap.outbound.sent";
  public static final String METRIC_REPORTING_SUCCESS = "phoss.ap.reporting.success";

  // === Lifecycle histograms ===
  public static final String METRIC_INBOUND_FORWARDING_DURATION = "phoss.ap.inbound.forwarding.duration";
  public static final String METRIC_OUTBOUND_SENDING_DURATION = "phoss.ap.outbound.sending.duration";
  public static final String METRIC_OUTBOUND_SENDING_ATTEMPTS = "phoss.ap.outbound.sending.attempts";
  public static final String METRIC_MLS_ROUNDTRIP_DURATION = "phoss.ap.mls.roundtrip.duration";
  public static final String METRIC_SCHEDULER_CYCLE_DURATION = "phoss.ap.scheduler.cycle.duration";
  public static final String METRIC_SCHEDULER_CYCLE_ITEMS = "phoss.ap.scheduler.cycle.items";

  // === Span names (used by manual instrumentation in caller modules) ===
  public static final String SPAN_INBOUND_RECEIVE = "phoss.ap.inbound.receive";
  public static final String SPAN_INBOUND_FORWARD = "phoss.ap.inbound.forward";
  public static final String SPAN_OUTBOUND_SEND = "phoss.ap.outbound.send";
  public static final String SPAN_SMP_LOOKUP = "phoss.ap.smp.lookup";
  public static final String SPAN_MLS_CORRELATE = "phoss.ap.mls.correlate";
  public static final String SPAN_VERIFICATION = "phoss.ap.verification";
  public static final String SPAN_REPORTING_TSR = "phoss.ap.reporting.tsr";
  public static final String SPAN_REPORTING_EUSR = "phoss.ap.reporting.eusr";
  public static final String SPAN_ARCHIVAL = "phoss.ap.archival";
  public static final String SPAN_FORWARDER_DISPATCH = "phoss.ap.forwarder.dispatch";
  public static final String SPAN_SCHEDULER_CYCLE = "phoss.ap.scheduler.cycle";

  // === Attribute keys ===
  public static final AttributeKey <String> ATTR_TRANSACTION_ID = AttributeKey.stringKey ("phoss.ap.transaction.id");
  public static final AttributeKey <String> ATTR_SBDH_INSTANCE_ID = AttributeKey.stringKey ("phoss.ap.sbdh.instance_id");
  public static final AttributeKey <String> ATTR_SENDER_ID = AttributeKey.stringKey ("phoss.ap.sender.id");
  public static final AttributeKey <String> ATTR_RECEIVER_ID = AttributeKey.stringKey ("phoss.ap.receiver.id");
  public static final AttributeKey <String> ATTR_DOCTYPE_ID = AttributeKey.stringKey ("phoss.ap.doctype.id");
  public static final AttributeKey <String> ATTR_PROCESS_ID = AttributeKey.stringKey ("phoss.ap.process.id");
  public static final AttributeKey <String> ATTR_MLS_RESPONSE_CODE = AttributeKey.stringKey ("phoss.ap.mls.response_code");
  public static final AttributeKey <String> ATTR_REPORT_TYPE = AttributeKey.stringKey ("phoss.ap.report.type");
  public static final AttributeKey <String> ATTR_REPORT_YEAR_MONTH = AttributeKey.stringKey ("phoss.ap.report.year_month");
  public static final AttributeKey <Boolean> ATTR_IS_RETRY = AttributeKey.booleanKey ("phoss.ap.is_retry");
  public static final AttributeKey <Boolean> ATTR_IS_OUTBOUND = AttributeKey.booleanKey ("phoss.ap.is_outbound");
  public static final AttributeKey <Boolean> ATTR_IS_DUPLICATE_AS4 = AttributeKey.booleanKey ("phoss.ap.is_duplicate_as4");
  public static final AttributeKey <Boolean> ATTR_IS_DUPLICATE_SBDH = AttributeKey.booleanKey ("phoss.ap.is_duplicate_sbdh");
  public static final AttributeKey <String> ATTR_SCHEDULER_NAME = AttributeKey.stringKey ("phoss.ap.scheduler.name");
  public static final AttributeKey <String> ATTR_FORWARDER_TYPE = AttributeKey.stringKey ("phoss.ap.forwarder.type");
  public static final AttributeKey <String> ATTR_EXCEPTION_CONTEXT = AttributeKey.stringKey ("phoss.ap.exception.context");
  public static final AttributeKey <String> ATTR_EXCEPTION_CLASS = AttributeKey.stringKey ("phoss.ap.exception.class");

  private CPhossAPOtel ()
  {}
}
