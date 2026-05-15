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

import java.time.Duration;
import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.spi.IAPLifecycleEventSPI;

import io.opentelemetry.api.common.Attributes;

/**
 * Implementation of {@link IAPLifecycleEventSPI} that records OpenTelemetry counters and histograms
 * for positive AP lifecycle events. Wired explicitly via Spring configuration in the webapp module
 * when OTel is enabled.
 * <p>
 * Cardinality discipline: high-cardinality Peppol identifiers (sender, receiver, doctype, process,
 * SBDH instance ID, transaction ID) are intentionally <em>not</em> attached as metric attributes —
 * they would explode the cardinality of the metrics backend. They appear on spans (which can
 * tolerate high cardinality) when manual instrumentation is added to the relevant call sites.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
public class APLifecycleEventHandlerOtel implements IAPLifecycleEventSPI
{
  private static double _toSeconds (@NonNull final Duration aDuration)
  {
    return aDuration.toNanos () / 1_000_000_000.0;
  }

  /** {@inheritDoc} */
  public void onInboundDocumentReceived (@NonNull final String sTransactionID,
                                         @NonNull final String sSenderID,
                                         @NonNull final String sReceiverID,
                                         @NonNull final String sDocTypeID,
                                         @NonNull final String sProcessID,
                                         @NonNull final String sSbdhInstanceID,
                                         final boolean bIsDuplicateAS4,
                                         final boolean bIsDuplicateSBDH)
  {
    final Attributes aAttrs = Attributes.of (CPhossAPOtel.ATTR_IS_DUPLICATE_AS4,
                                             Boolean.valueOf (bIsDuplicateAS4),
                                             CPhossAPOtel.ATTR_IS_DUPLICATE_SBDH,
                                             Boolean.valueOf (bIsDuplicateSBDH));
    PhossAPTelemetry.inboundReceived ().add (1, aAttrs);
  }

  /** {@inheritDoc} */
  public void onInboundVerificationAccepted (@NonNull final String sTransactionID,
                                             @NonNull final String sSbdhInstanceID)
  {
    PhossAPTelemetry.inboundVerificationAccepted ().add (1);
  }

  /** {@inheritDoc} */
  public void onOutboundVerificationAccepted (@NonNull final String sSbdhInstanceID)
  {
    PhossAPTelemetry.outboundVerificationAccepted ().add (1);
  }

  /** {@inheritDoc} */
  public void onInboundMLSCorrelated (@NonNull final String sMlsTransactionID,
                                      @NonNull final String sReferencedSbdhInstanceID,
                                      @NonNull final EPeppolMLSResponseCode eMlsResponseCode,
                                      @Nullable final Duration aRoundTrip)
  {
    final Attributes aAttrs = Attributes.of (CPhossAPOtel.ATTR_MLS_RESPONSE_CODE, eMlsResponseCode.getID ());
    PhossAPTelemetry.inboundMLSCorrelated ().add (1, aAttrs);
    if (aRoundTrip != null)
      PhossAPTelemetry.mlsRoundtripDuration ().record (_toSeconds (aRoundTrip), aAttrs);
  }

  /** {@inheritDoc} */
  public void onInboundDocumentForwarded (@NonNull final String sTransactionID,
                                          @NonNull final String sSbdhInstanceID,
                                          @Nullable final Duration aForwardingDuration,
                                          final boolean bIsRetry)
  {
    final Attributes aAttrs = Attributes.of (CPhossAPOtel.ATTR_IS_RETRY, Boolean.valueOf (bIsRetry));
    PhossAPTelemetry.inboundForwarded ().add (1, aAttrs);
    if (aForwardingDuration != null)
      PhossAPTelemetry.inboundForwardingDuration ().record (_toSeconds (aForwardingDuration), aAttrs);
  }

  /** {@inheritDoc} */
  public void onOutboundDocumentAccepted (@NonNull final String sTransactionID,
                                          @NonNull final String sSenderID,
                                          @NonNull final String sReceiverID,
                                          @NonNull final String sDocTypeID,
                                          @NonNull final String sProcessID,
                                          @NonNull final String sSbdhInstanceID)
  {
    PhossAPTelemetry.outboundAccepted ().add (1);
  }

  /** {@inheritDoc} */
  public void onOutboundDocumentSent (@NonNull final String sTransactionID,
                                      @NonNull final String sSbdhInstanceID,
                                      @Nullable final Duration aSendingDuration,
                                      @Nonnegative final int nAttempts)
  {
    PhossAPTelemetry.outboundSent ().add (1);
    if (aSendingDuration != null)
      PhossAPTelemetry.outboundSendingDuration ().record (_toSeconds (aSendingDuration));
    PhossAPTelemetry.outboundSendingAttempts ().record (nAttempts);
  }

  /** {@inheritDoc} */
  public void onPeppolReportingTSRSuccess (@NonNull final YearMonth aYearMonth)
  {
    PhossAPTelemetry.reportingSuccess ().add (1, Attributes.of (CPhossAPOtel.ATTR_REPORT_TYPE, "TSR"));
  }

  /** {@inheritDoc} */
  public void onPeppolReportingEUSRSuccess (@NonNull final YearMonth aYearMonth)
  {
    PhossAPTelemetry.reportingSuccess ().add (1, Attributes.of (CPhossAPOtel.ATTR_REPORT_TYPE, "EUSR"));
  }

  private static void _recordSchedulerCycle (@NonNull final String sSchedulerName,
                                             final boolean bIsOutbound,
                                             @Nonnegative final int nItems,
                                             @NonNull final Duration aCycleDuration)
  {
    final Attributes aAttrs = Attributes.of (CPhossAPOtel.ATTR_SCHEDULER_NAME,
                                             sSchedulerName,
                                             CPhossAPOtel.ATTR_IS_OUTBOUND,
                                             Boolean.valueOf (bIsOutbound));
    PhossAPTelemetry.schedulerCycleDuration ().record (_toSeconds (aCycleDuration), aAttrs);
    PhossAPTelemetry.schedulerCycleItems ().record (nItems, aAttrs);
  }

  /** {@inheritDoc} */
  public void onRetrySchedulerCycle (final boolean bIsOutbound,
                                     @Nonnegative final int nProcessed,
                                     @NonNull final Duration aCycleDuration)
  {
    _recordSchedulerCycle ("retry", bIsOutbound, nProcessed, aCycleDuration);
  }

  /** {@inheritDoc} */
  public void onArchivalSchedulerCycle (final boolean bIsOutbound,
                                        @Nonnegative final int nArchived,
                                        @NonNull final Duration aCycleDuration)
  {
    _recordSchedulerCycle ("archival", bIsOutbound, nArchived, aCycleDuration);
  }

  /** {@inheritDoc} */
  public void onCleanupSchedulerCycle (final boolean bIsOutbound,
                                       @Nonnegative final int nDeleted,
                                       @NonNull final Duration aCycleDuration)
  {
    _recordSchedulerCycle ("cleanup", bIsOutbound, nDeleted, aCycleDuration);
  }
}
