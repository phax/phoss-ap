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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.EVerificationResult;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.api.spi.IAPLifecycleEventSPI;
import com.helger.telemetry.TelemetryAttributes;

/**
 * Implementation of {@link IAPLifecycleEventSPI} that records positive lifecycle counters and
 * histograms. Records through the vendor-neutral {@code ph-telemetry} abstraction — when no meter
 * SPI is registered, every call is a no-op, so this handler is always safe to load.
 * <p>
 * Cardinality discipline: high-cardinality Peppol identifiers (sender, receiver, doctype, process,
 * SBDH instance ID, transaction ID) are intentionally <em>not</em> attached as metric attributes —
 * they would explode the cardinality of the metrics backend. They appear on spans (which can
 * tolerate high cardinality) when manual instrumentation is added to the relevant call sites.
 * <p>
 * Registered via {@code META-INF/services} and picked up by
 * {@link com.helger.phoss.ap.core.notification.LifecycleEventManager#initSPI()}.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class APMetricsLifecycleEventHandler implements IAPLifecycleEventSPI
{
  private static double _toSeconds (@NonNull final Duration aDuration)
  {
    return aDuration.toNanos () / 1_000_000_000.0;
  }

  public void onInboundDocumentReceived (@NonNull final String sTransactionID,
                                         @NonNull final String sSenderID,
                                         @NonNull final String sReceiverID,
                                         @NonNull final String sDocTypeID,
                                         @NonNull final String sProcessID,
                                         @NonNull final String sSbdhInstanceID,
                                         final boolean bIsDuplicateAS4,
                                         final boolean bIsDuplicateSBDH)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_IS_DUPLICATE_AS4, bIsDuplicateAS4)
                                                          .put (CPhossAPOtel.ATTR_IS_DUPLICATE_SBDH, bIsDuplicateSBDH)
                                                          .build ();
    APMetrics.INBOUND_RECEIVED.add (1, aAttrs);
  }

  public void onInboundVerificationAccepted (@NonNull final String sTransactionID,
                                             @NonNull final String sSbdhInstanceID)
  {
    APMetrics.INBOUND_VERIFICATION_ACCEPTED.add (1);
  }

  public void onOutboundVerificationAccepted (@NonNull final String sSbdhInstanceID)
  {
    APMetrics.OUTBOUND_VERIFICATION_ACCEPTED.add (1);
  }

  public void onInboundMLSCorrelated (@NonNull final String sMlsTransactionID,
                                      @NonNull final String sReferencedSbdhInstanceID,
                                      @NonNull final String sCorrelatedOutboundTransactionID,
                                      @NonNull final EPeppolMLSResponseCode eMlsResponseCode,
                                      @NonNull final EMlsReceptionStatus eMlsReceptionStatus,
                                      @Nullable final String sMlsDocumentID,
                                      @NonNull final OffsetDateTime aMlsReceivedDT,
                                      @Nullable final Duration aRoundTrip)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_MLS_RESPONSE_CODE,
                                                                eMlsResponseCode.getID ())
                                                          .put (CPhossAPOtel.ATTR_MLS_RECEPTION_STATUS,
                                                                eMlsReceptionStatus.getID ())
                                                          .build ();
    APMetrics.INBOUND_MLS_CORRELATED.add (1, aAttrs);
    if (aRoundTrip != null)
      APMetrics.MLS_ROUNDTRIP_DURATION.record (_toSeconds (aRoundTrip), aAttrs);
  }

  public void onInboundDocumentForwarded (@NonNull final String sTransactionID,
                                          @NonNull final String sSbdhInstanceID,
                                          @Nullable final Duration aForwardingDuration,
                                          final boolean bIsRetry,
                                          @Nullable final EVerificationResult eVerificationResult)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_IS_RETRY, bIsRetry)
                                                          .build ();
    APMetrics.INBOUND_FORWARDED.add (1, aAttrs);
    if (aForwardingDuration != null)
      APMetrics.INBOUND_FORWARDING_DURATION.record (_toSeconds (aForwardingDuration), aAttrs);

    // Only counts the mode "retry" - a "best-effort" copy never reaches this callback
    if (eVerificationResult != null && eVerificationResult.isRejected ())
      APMetrics.INBOUND_VERIFICATION_REJECTIONS_FORWARDED.add (1, aAttrs);
  }

  public void onOutboundDocumentAccepted (@NonNull final String sTransactionID,
                                          @NonNull final String sSenderID,
                                          @NonNull final String sReceiverID,
                                          @NonNull final String sDocTypeID,
                                          @NonNull final String sProcessID,
                                          @NonNull final String sSbdhInstanceID)
  {
    APMetrics.OUTBOUND_ACCEPTED.add (1);
  }

  public void onOutboundDocumentSent (@NonNull final String sTransactionID,
                                      @NonNull final String sSbdhInstanceID,
                                      @Nullable final Duration aSendingDuration,
                                      @Nonnegative final int nAttempts)
  {
    APMetrics.OUTBOUND_SENT.add (1);
    if (aSendingDuration != null)
      APMetrics.OUTBOUND_SENDING_DURATION.record (_toSeconds (aSendingDuration));
    APMetrics.OUTBOUND_SENDING_ATTEMPTS.record (nAttempts);
  }

  public void onPeppolReportingTSRSuccess (@NonNull final YearMonth aYearMonth)
  {
    APMetrics.REPORTING_SUCCESS.add (1,
                                     TelemetryAttributes.builder ()
                                                        .put (CPhossAPOtel.ATTR_REPORT_TYPE, "TSR")
                                                        .build ());
  }

  public void onPeppolReportingEUSRSuccess (@NonNull final YearMonth aYearMonth)
  {
    APMetrics.REPORTING_SUCCESS.add (1,
                                     TelemetryAttributes.builder ()
                                                        .put (CPhossAPOtel.ATTR_REPORT_TYPE, "EUSR")
                                                        .build ());
  }

  private static void _recordSchedulerCycle (@NonNull final String sSchedulerName,
                                             final boolean bIsOutbound,
                                             @Nonnegative final int nItems,
                                             @NonNull final Duration aCycleDuration)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_SCHEDULER_NAME, sSchedulerName)
                                                          .put (CPhossAPOtel.ATTR_IS_OUTBOUND, bIsOutbound)
                                                          .build ();
    APMetrics.SCHEDULER_CYCLE_DURATION.record (_toSeconds (aCycleDuration), aAttrs);
    APMetrics.SCHEDULER_CYCLE_ITEMS.record (nItems, aAttrs);
  }

  public void onRetrySchedulerCycle (final boolean bIsOutbound,
                                     @Nonnegative final int nProcessed,
                                     @NonNull final Duration aCycleDuration)
  {
    _recordSchedulerCycle ("retry", bIsOutbound, nProcessed, aCycleDuration);
  }

  public void onArchivalSchedulerCycle (final boolean bIsOutbound,
                                        @Nonnegative final int nArchived,
                                        @NonNull final Duration aCycleDuration)
  {
    _recordSchedulerCycle ("archival", bIsOutbound, nArchived, aCycleDuration);
  }

  public void onCleanupSchedulerCycle (final boolean bIsOutbound,
                                       @Nonnegative final int nDeleted,
                                       @NonNull final Duration aCycleDuration)
  {
    _recordSchedulerCycle ("cleanup", bIsOutbound, nDeleted, aCycleDuration);
  }
}
