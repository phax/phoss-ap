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

import java.time.OffsetDateTime;
import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.model.VerifierResult;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.api.spi.IAPNotificationHandlerSPI;
import com.helger.telemetry.TelemetryAttributes;

/**
 * Implementation of {@link IAPNotificationHandlerSPI} that records failure-side telemetry counters.
 * Records through the vendor-neutral {@code ph-telemetry} abstraction — when no meter SPI is
 * registered, every call is a no-op, so this handler is always safe to load.
 * <p>
 * Registered via {@code META-INF/services} and picked up by
 * {@link com.helger.phoss.ap.core.notification.NotificationHandlerManager#initSPI()}.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class APMetricsNotificationHandler implements IAPNotificationHandlerSPI
{
  @NonNull
  private static TelemetryAttributes _baseTxAttrs (@NonNull final String sTransactionID,
                                                   @NonNull final String sSbdhInstanceID)
  {
    return TelemetryAttributes.builder ()
                              .put (CPhossAPOtel.ATTR_TRANSACTION_ID, sTransactionID)
                              .put (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID, sSbdhInstanceID)
                              .build ();
  }

  public void onInboundVerificationRejection (@NonNull final String sTransactionID,
                                              @NonNull final String sSbdhInstanceID,
                                              @Nullable final String sErrorDetails)
  {
    APMetrics.INBOUND_VERIFICATION_REJECTIONS.add (1, _baseTxAttrs (sTransactionID, sSbdhInstanceID));
  }

  public void onInboundVerificationDeferred (@NonNull final String sTransactionID,
                                             @NonNull final String sSbdhInstanceID,
                                             @NonNull final String sVerifierName,
                                             @NonNull final OffsetDateTime aNextRetryDT,
                                             @Nullable final String sErrorDetails)
  {
    APMetrics.INBOUND_VERIFICATION_DEFERRED.add (1, _baseTxAttrs (sTransactionID, sSbdhInstanceID));
  }

  public void onOutboundVerificationRejection (@NonNull final String sSbdhInstanceID,
                                               @NonNull final VerifierResult aVerifierResult)
  {
    APMetrics.OUTBOUND_VERIFICATION_REJECTIONS.add (1,
                                                    TelemetryAttributes.builder ()
                                                                       .put (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID,
                                                                             sSbdhInstanceID)
                                                                       .build ());
  }

  public void onOutboundPermanentSendingFailure (@NonNull final String sTransactionID,
                                                 @NonNull final String sSbdhInstanceID,
                                                 @Nullable final String sErrorDetails)
  {
    APMetrics.OUTBOUND_SENDING_PERMANENT_FAILURES.add (1, _baseTxAttrs (sTransactionID, sSbdhInstanceID));
  }

  public void onInboundReceiverNotServiced (@NonNull final String sSenderID,
                                            @NonNull final String sReceiverID,
                                            @NonNull final String sDocTypeID,
                                            @NonNull final String sProcessID,
                                            @NonNull final String sSbdhInstanceID)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_SENDER_ID, sSenderID)
                                                          .put (CPhossAPOtel.ATTR_RECEIVER_ID, sReceiverID)
                                                          .put (CPhossAPOtel.ATTR_DOCTYPE_ID, sDocTypeID)
                                                          .put (CPhossAPOtel.ATTR_PROCESS_ID, sProcessID)
                                                          .put (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID, sSbdhInstanceID)
                                                          .build ();
    APMetrics.INBOUND_RECEIVER_NOT_SERVICED.add (1, aAttrs);
  }

  public void onInboundDuplicateRejected (@NonNull final String sSenderID,
                                          @NonNull final String sReceiverID,
                                          @NonNull final String sDocTypeID,
                                          @NonNull final String sProcessID,
                                          @Nullable final String sSenderProviderID,
                                          @Nullable final String sAS4MessageID,
                                          @NonNull final String sSbdhInstanceID,
                                          final boolean bIsDuplicateAS4,
                                          final boolean bIsDuplicateSBDH,
                                          @NonNull final String sErrorDetails)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_SENDER_ID, sSenderID)
                                                          .put (CPhossAPOtel.ATTR_RECEIVER_ID, sReceiverID)
                                                          .put (CPhossAPOtel.ATTR_DOCTYPE_ID, sDocTypeID)
                                                          .put (CPhossAPOtel.ATTR_PROCESS_ID, sProcessID)
                                                          .put (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID, sSbdhInstanceID)
                                                          .put (CPhossAPOtel.ATTR_IS_DUPLICATE_AS4, bIsDuplicateAS4)
                                                          .put (CPhossAPOtel.ATTR_IS_DUPLICATE_SBDH, bIsDuplicateSBDH)
                                                          .build ();
    APMetrics.INBOUND_DUPLICATE_REJECTIONS.add (1, aAttrs);
  }

  public void onInboundPermanentForwardingFailure (@NonNull final String sTransactionID,
                                                   @NonNull final String sSbdhInstanceID,
                                                   @Nullable final String sErrorDetails)
  {
    APMetrics.INBOUND_FORWARDING_PERMANENT_FAILURES.add (1, _baseTxAttrs (sTransactionID, sSbdhInstanceID));
  }

  public void onInboundMLSCorrelationError (@NonNull final String sTransactionID,
                                            @NonNull final String sReferencedSbdhInstanceID,
                                            @NonNull final EPeppolMLSResponseCode eMlsResponseCode)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_TRANSACTION_ID, sTransactionID)
                                                          .put (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID,
                                                                sReferencedSbdhInstanceID)
                                                          .put (CPhossAPOtel.ATTR_MLS_RESPONSE_CODE,
                                                                eMlsResponseCode.getID ())
                                                          .build ();
    APMetrics.INBOUND_MLS_CORRELATION_ERRORS.add (1, aAttrs);
  }

  public void onSpecialMlsToNotReachable (@NonNull final String sOutboundTransactionID,
                                          @NonNull final String sReferencedSbdhInstanceID,
                                          @NonNull final String sAttemptedMlsToParticipantID,
                                          @NonNull final String sFallbackDefaultSpidParticipantID)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_TRANSACTION_ID,
                                                                sOutboundTransactionID)
                                                          .put (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID,
                                                                sReferencedSbdhInstanceID)
                                                          .build ();
    APMetrics.OUTBOUND_MLS_SPECIAL_TO_NOT_REACHABLE.add (1, aAttrs);
  }

  public void onInboundForwardingError (@NonNull final String sTransactionID, final boolean bIsRetry)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_TRANSACTION_ID, sTransactionID)
                                                          .put (CPhossAPOtel.ATTR_IS_RETRY, bIsRetry)
                                                          .build ();
    APMetrics.INBOUND_FORWARDING_ERRORS.add (1, aAttrs);
  }

  public void onPeppolReportingTSRFailure (@NonNull final YearMonth aYearMonth)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_REPORT_TYPE, "TSR")
                                                          .put (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH,
                                                                aYearMonth.toString ())
                                                          .build ();
    APMetrics.REPORTING_FAILURES.add (1, aAttrs);
  }

  public void onPeppolReportingEUSRFailure (@NonNull final YearMonth aYearMonth)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_REPORT_TYPE, "EUSR")
                                                          .put (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH,
                                                                aYearMonth.toString ())
                                                          .build ();
    APMetrics.REPORTING_FAILURES.add (1, aAttrs);
  }

  public void onUnexpectedException (@NonNull final String sContext,
                                     @NonNull final String sMessage,
                                     @NonNull final Exception aException)
  {
    final TelemetryAttributes aAttrs = TelemetryAttributes.builder ()
                                                          .put (CPhossAPOtel.ATTR_EXCEPTION_CONTEXT, sContext)
                                                          .put (CPhossAPOtel.ATTR_EXCEPTION_CLASS,
                                                                aException.getClass ().getName ())
                                                          .build ();
    APMetrics.UNEXPECTED_EXCEPTIONS.add (1, aAttrs);
  }
}
