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
package com.helger.phoss.ap.otel;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.phoss.ap.api.CPhossAPVersion;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;

/**
 * Central access point for the OpenTelemetry {@link Meter} and its cached instruments used by the
 * phoss AP. Tracing goes through the {@code com.helger.phoss.ap.api.trace} abstraction, not this
 * class.
 * <p>
 * By default, this class resolves the globally configured {@link OpenTelemetry} instance — which
 * is populated by the OTel SDK autoconfigure module when the application starts. For tests, a
 * custom {@link OpenTelemetry} may be installed via {@link #install(OpenTelemetry)}.
 * <p>
 * All instruments are created lazily on first access and cached for the lifetime of the JVM. Cache
 * reads use volatile, lock-free; cache population and {@link #install(OpenTelemetry)} go through a
 * shared {@link SimpleReadWriteLock}.
 * <p>
 * Instruments are organised by topic (inbound, outbound, reporting, schedulers, general), with the
 * happy-path counter and its corresponding failure counter declared next to each other.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class PhossAPTelemetry
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PhossAPTelemetry.class);

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();

  private static volatile OpenTelemetry s_aOpenTelemetry;
  private static volatile Meter s_aMeter;

  // === Inbound ===
  private static volatile LongCounter s_aInboundReceived;
  private static volatile LongCounter s_aInboundReceiverNotServiced;
  private static volatile LongCounter s_aInboundVerificationAccepted;
  private static volatile LongCounter s_aInboundVerificationRejections;
  private static volatile LongCounter s_aInboundMLSCorrelated;
  private static volatile LongCounter s_aInboundMLSCorrelationErrors;
  private static volatile DoubleHistogram s_aMLSRoundtripDuration;
  private static volatile LongCounter s_aInboundForwarded;
  private static volatile DoubleHistogram s_aInboundForwardingDuration;
  private static volatile LongCounter s_aInboundForwardingErrors;
  private static volatile LongCounter s_aInboundForwardingPermanentFailures;

  // === Outbound ===
  private static volatile LongCounter s_aOutboundAccepted;
  private static volatile LongCounter s_aOutboundVerificationAccepted;
  private static volatile LongCounter s_aOutboundVerificationRejections;
  private static volatile LongCounter s_aOutboundSent;
  private static volatile DoubleHistogram s_aOutboundSendingDuration;
  private static volatile LongHistogram s_aOutboundSendingAttempts;
  private static volatile LongCounter s_aOutboundSendingPermanentFailures;

  // === Reporting ===
  private static volatile LongCounter s_aReportingSuccess;
  private static volatile LongCounter s_aReportingFailures;

  // === Schedulers ===
  private static volatile DoubleHistogram s_aSchedulerCycleDuration;
  private static volatile LongHistogram s_aSchedulerCycleItems;

  // === General ===
  private static volatile LongCounter s_aUnexpectedExceptions;

  private PhossAPTelemetry ()
  {}

  /**
   * Install a custom {@link OpenTelemetry} instance. Intended primarily for tests. Calling this
   * resets all cached instruments.
   *
   * @param aOpenTelemetry
   *        The OpenTelemetry instance to use. May not be <code>null</code>.
   */
  public static void install (@NonNull final OpenTelemetry aOpenTelemetry)
  {
    RW_LOCK.writeLocked ( () -> {
      s_aOpenTelemetry = aOpenTelemetry;
      s_aMeter = null;

      // Inbound
      s_aInboundReceived = null;
      s_aInboundReceiverNotServiced = null;
      s_aInboundVerificationAccepted = null;
      s_aInboundVerificationRejections = null;
      s_aInboundMLSCorrelated = null;
      s_aInboundMLSCorrelationErrors = null;
      s_aMLSRoundtripDuration = null;
      s_aInboundForwarded = null;
      s_aInboundForwardingDuration = null;
      s_aInboundForwardingErrors = null;
      s_aInboundForwardingPermanentFailures = null;

      // Outbound
      s_aOutboundAccepted = null;
      s_aOutboundVerificationAccepted = null;
      s_aOutboundVerificationRejections = null;
      s_aOutboundSent = null;
      s_aOutboundSendingDuration = null;
      s_aOutboundSendingAttempts = null;
      s_aOutboundSendingPermanentFailures = null;

      // Reporting
      s_aReportingSuccess = null;
      s_aReportingFailures = null;

      // Schedulers
      s_aSchedulerCycleDuration = null;
      s_aSchedulerCycleItems = null;

      // General
      s_aUnexpectedExceptions = null;
    });
    LOGGER.info ("Installed custom OpenTelemetry instance: " + aOpenTelemetry.getClass ().getName ());
  }

  @NonNull
  private static OpenTelemetry _getOpenTelemetry ()
  {
    OpenTelemetry aRet = s_aOpenTelemetry;
    if (aRet == null)
      aRet = GlobalOpenTelemetry.get ();
    return aRet;
  }

  /**
   * @return The phoss AP {@link Meter}. Never <code>null</code>.
   */
  @NonNull
  public static Meter meter ()
  {
    final Meter aFast = s_aMeter;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      Meter aRet = s_aMeter;
      if (aRet == null)
      {
        aRet = _getOpenTelemetry ().getMeterProvider ()
                                   .meterBuilder (CPhossAPOtel.INSTRUMENTATION_SCOPE_NAME)
                                   .setInstrumentationVersion (CPhossAPVersion.BUILD_VERSION)
                                   .build ();
        s_aMeter = aRet;
      }
      return aRet;
    });
  }

  // === Inbound ===

  @NonNull
  public static LongCounter inboundReceived ()
  {
    final LongCounter aFast = s_aInboundReceived;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aInboundReceived;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_INBOUND_RECEIVED)
                       .setDescription ("Inbound AS4 messages received and persisted")
                       .setUnit ("{message}")
                       .build ();
        s_aInboundReceived = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter inboundReceiverNotServiced ()
  {
    final LongCounter aFast = s_aInboundReceiverNotServiced;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aInboundReceiverNotServiced;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_INBOUND_RECEIVER_NOT_SERVICED)
                       .setDescription ("Inbound messages for which the receiver is not serviced by this AP")
                       .setUnit ("{message}")
                       .build ();
        s_aInboundReceiverNotServiced = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter inboundVerificationAccepted ()
  {
    final LongCounter aFast = s_aInboundVerificationAccepted;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aInboundVerificationAccepted;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_INBOUND_VERIFICATION_ACCEPTED)
                       .setDescription ("Inbound documents that passed verification")
                       .setUnit ("{document}")
                       .build ();
        s_aInboundVerificationAccepted = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter inboundVerificationRejections ()
  {
    final LongCounter aFast = s_aInboundVerificationRejections;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aInboundVerificationRejections;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_INBOUND_VERIFICATION_REJECTIONS)
                       .setDescription ("Inbound transactions rejected by verification")
                       .setUnit ("{transaction}")
                       .build ();
        s_aInboundVerificationRejections = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter inboundMLSCorrelated ()
  {
    final LongCounter aFast = s_aInboundMLSCorrelated;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aInboundMLSCorrelated;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_INBOUND_MLS_CORRELATED)
                       .setDescription ("Inbound MLS messages successfully correlated to an outbound transaction")
                       .setUnit ("{message}")
                       .build ();
        s_aInboundMLSCorrelated = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter inboundMLSCorrelationErrors ()
  {
    final LongCounter aFast = s_aInboundMLSCorrelationErrors;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aInboundMLSCorrelationErrors;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_INBOUND_MLS_CORRELATION_ERRORS)
                       .setDescription ("Inbound MLS messages that could not be correlated to an outbound transaction")
                       .setUnit ("{message}")
                       .build ();
        s_aInboundMLSCorrelationErrors = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static DoubleHistogram mlsRoundtripDuration ()
  {
    final DoubleHistogram aFast = s_aMLSRoundtripDuration;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      DoubleHistogram aRet = s_aMLSRoundtripDuration;
      if (aRet == null)
      {
        aRet = meter ().histogramBuilder (CPhossAPOtel.METRIC_MLS_ROUNDTRIP_DURATION)
                       .setDescription ("Wall-clock duration from outbound send completion to MLS reception (powers MLS-1/MLS-2 SLA dashboards)")
                       .setUnit ("s")
                       .build ();
        s_aMLSRoundtripDuration = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter inboundForwarded ()
  {
    final LongCounter aFast = s_aInboundForwarded;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aInboundForwarded;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_INBOUND_FORWARDED)
                       .setDescription ("Inbound documents successfully forwarded to the Receiver Backend")
                       .setUnit ("{transaction}")
                       .build ();
        s_aInboundForwarded = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static DoubleHistogram inboundForwardingDuration ()
  {
    final DoubleHistogram aFast = s_aInboundForwardingDuration;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      DoubleHistogram aRet = s_aInboundForwardingDuration;
      if (aRet == null)
      {
        aRet = meter ().histogramBuilder (CPhossAPOtel.METRIC_INBOUND_FORWARDING_DURATION)
                       .setDescription ("Wall-clock duration from inbound AS4 reception to successful forwarding")
                       .setUnit ("s")
                       .build ();
        s_aInboundForwardingDuration = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter inboundForwardingErrors ()
  {
    final LongCounter aFast = s_aInboundForwardingErrors;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aInboundForwardingErrors;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_INBOUND_FORWARDING_ERRORS)
                       .setDescription ("Inbound forwarding attempts that failed (transient or permanent)")
                       .setUnit ("{attempt}")
                       .build ();
        s_aInboundForwardingErrors = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter inboundForwardingPermanentFailures ()
  {
    final LongCounter aFast = s_aInboundForwardingPermanentFailures;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aInboundForwardingPermanentFailures;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_INBOUND_FORWARDING_PERMANENT_FAILURES)
                       .setDescription ("Inbound transactions that exhausted all forwarding retries")
                       .setUnit ("{transaction}")
                       .build ();
        s_aInboundForwardingPermanentFailures = aRet;
      }
      return aRet;
    });
  }

  // === Outbound ===

  @NonNull
  public static LongCounter outboundAccepted ()
  {
    final LongCounter aFast = s_aOutboundAccepted;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aOutboundAccepted;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_OUTBOUND_ACCEPTED)
                       .setDescription ("Outbound transactions accepted by the AP and queued for sending")
                       .setUnit ("{transaction}")
                       .build ();
        s_aOutboundAccepted = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter outboundVerificationAccepted ()
  {
    final LongCounter aFast = s_aOutboundVerificationAccepted;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aOutboundVerificationAccepted;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_OUTBOUND_VERIFICATION_ACCEPTED)
                       .setDescription ("Outbound documents that passed verification")
                       .setUnit ("{document}")
                       .build ();
        s_aOutboundVerificationAccepted = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter outboundVerificationRejections ()
  {
    final LongCounter aFast = s_aOutboundVerificationRejections;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aOutboundVerificationRejections;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_OUTBOUND_VERIFICATION_REJECTIONS)
                       .setDescription ("Outbound documents rejected by verification before sending")
                       .setUnit ("{document}")
                       .build ();
        s_aOutboundVerificationRejections = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter outboundSent ()
  {
    final LongCounter aFast = s_aOutboundSent;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aOutboundSent;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_OUTBOUND_SENT)
                       .setDescription ("Outbound transactions successfully sent via AS4 and receipt confirmed")
                       .setUnit ("{transaction}")
                       .build ();
        s_aOutboundSent = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static DoubleHistogram outboundSendingDuration ()
  {
    final DoubleHistogram aFast = s_aOutboundSendingDuration;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      DoubleHistogram aRet = s_aOutboundSendingDuration;
      if (aRet == null)
      {
        aRet = meter ().histogramBuilder (CPhossAPOtel.METRIC_OUTBOUND_SENDING_DURATION)
                       .setDescription ("Wall-clock duration from outbound transaction creation to confirmed AS4 receipt")
                       .setUnit ("s")
                       .build ();
        s_aOutboundSendingDuration = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongHistogram outboundSendingAttempts ()
  {
    final LongHistogram aFast = s_aOutboundSendingAttempts;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongHistogram aRet = s_aOutboundSendingAttempts;
      if (aRet == null)
      {
        aRet = meter ().histogramBuilder (CPhossAPOtel.METRIC_OUTBOUND_SENDING_ATTEMPTS)
                       .ofLongs ()
                       .setDescription ("Number of AS4 sending attempts before confirmed receipt")
                       .setUnit ("{attempt}")
                       .build ();
        s_aOutboundSendingAttempts = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter outboundSendingPermanentFailures ()
  {
    final LongCounter aFast = s_aOutboundSendingPermanentFailures;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aOutboundSendingPermanentFailures;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_OUTBOUND_SENDING_PERMANENT_FAILURES)
                       .setDescription ("Outbound transactions that exhausted all sending retries")
                       .setUnit ("{transaction}")
                       .build ();
        s_aOutboundSendingPermanentFailures = aRet;
      }
      return aRet;
    });
  }

  // === Reporting ===

  @NonNull
  public static LongCounter reportingSuccess ()
  {
    final LongCounter aFast = s_aReportingSuccess;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aReportingSuccess;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_REPORTING_SUCCESS)
                       .setDescription ("Peppol Reporting (TSR/EUSR) reports successfully sent to OpenPeppol")
                       .setUnit ("{report}")
                       .build ();
        s_aReportingSuccess = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongCounter reportingFailures ()
  {
    final LongCounter aFast = s_aReportingFailures;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aReportingFailures;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_REPORTING_FAILURES)
                       .setDescription ("Peppol Reporting (TSR/EUSR) generation/validation/sending failures")
                       .setUnit ("{report}")
                       .build ();
        s_aReportingFailures = aRet;
      }
      return aRet;
    });
  }

  // === Schedulers ===

  @NonNull
  public static DoubleHistogram schedulerCycleDuration ()
  {
    final DoubleHistogram aFast = s_aSchedulerCycleDuration;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      DoubleHistogram aRet = s_aSchedulerCycleDuration;
      if (aRet == null)
      {
        aRet = meter ().histogramBuilder (CPhossAPOtel.METRIC_SCHEDULER_CYCLE_DURATION)
                       .setDescription ("Wall-clock duration of a scheduler cycle, tagged with the scheduler name")
                       .setUnit ("s")
                       .build ();
        s_aSchedulerCycleDuration = aRet;
      }
      return aRet;
    });
  }

  @NonNull
  public static LongHistogram schedulerCycleItems ()
  {
    final LongHistogram aFast = s_aSchedulerCycleItems;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongHistogram aRet = s_aSchedulerCycleItems;
      if (aRet == null)
      {
        aRet = meter ().histogramBuilder (CPhossAPOtel.METRIC_SCHEDULER_CYCLE_ITEMS)
                       .ofLongs ()
                       .setDescription ("Number of items processed in one scheduler cycle, tagged with the scheduler name")
                       .setUnit ("{item}")
                       .build ();
        s_aSchedulerCycleItems = aRet;
      }
      return aRet;
    });
  }

  // === General ===

  @NonNull
  public static LongCounter unexpectedExceptions ()
  {
    final LongCounter aFast = s_aUnexpectedExceptions;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      LongCounter aRet = s_aUnexpectedExceptions;
      if (aRet == null)
      {
        aRet = meter ().counterBuilder (CPhossAPOtel.METRIC_UNEXPECTED_EXCEPTIONS)
                       .setDescription ("Unexpected exceptions raised inside the AP that are not covered by a more specific counter")
                       .setUnit ("{exception}")
                       .build ();
        s_aUnexpectedExceptions = aRet;
      }
      return aRet;
    });
  }
}
