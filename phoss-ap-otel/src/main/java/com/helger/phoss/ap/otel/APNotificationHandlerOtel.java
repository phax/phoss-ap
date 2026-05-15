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

import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.spi.IAPNotificationHandlerSPI;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;

/**
 * Implementation of {@link IAPNotificationHandlerSPI} that records OpenTelemetry counters and
 * exception events for AP-level notifications. It is not registered as an SPI provider, because it
 * is included dependent on the existence of the OpenTelemetry dependencies — wired explicitly via
 * Spring configuration in the webapp module when OTel is enabled.
 *
 * @author Philip Helger
 */
public class APNotificationHandlerOtel implements IAPNotificationHandlerSPI
{
  @NonNull
  private static AttributesBuilder _baseTxAttrs (@NonNull final String sTransactionID,
                                                 @NonNull final String sSbdhInstanceID)
  {
    return Attributes.builder ()
                     .put (CPhossAPOtel.ATTR_TRANSACTION_ID, sTransactionID)
                     .put (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID, sSbdhInstanceID);
  }

  /** {@inheritDoc} */
  public void onInboundVerificationRejection (@NonNull final String sTransactionID,
                                              @NonNull final String sSbdhInstanceID,
                                              @Nullable final String sErrorDetails)
  {
    PhossAPTelemetry.inboundVerificationRejections ().add (1, _baseTxAttrs (sTransactionID, sSbdhInstanceID).build ());
  }

  /** {@inheritDoc} */
  public void onOutboundVerificationRejection (@NonNull final String sSbdhInstanceID,
                                               @Nullable final String sErrorDetails)
  {
    PhossAPTelemetry.outboundVerificationRejections ()
                    .add (1, Attributes.of (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID, sSbdhInstanceID));
  }

  /** {@inheritDoc} */
  public void onOutboundPermanentSendingFailure (@NonNull final String sTransactionID,
                                                 @NonNull final String sSbdhInstanceID,
                                                 @Nullable final String sErrorDetails)
  {
    PhossAPTelemetry.outboundSendingPermanentFailures ()
                    .add (1, _baseTxAttrs (sTransactionID, sSbdhInstanceID).build ());
  }

  /** {@inheritDoc} */
  public void onInboundReceiverNotServiced (@NonNull final String sSenderID,
                                            @NonNull final String sReceiverID,
                                            @NonNull final String sDocTypeID,
                                            @NonNull final String sProcessID,
                                            @NonNull final String sSbdhInstanceID)
  {
    final Attributes aAttrs = Attributes.builder ()
                                        .put (CPhossAPOtel.ATTR_SENDER_ID, sSenderID)
                                        .put (CPhossAPOtel.ATTR_RECEIVER_ID, sReceiverID)
                                        .put (CPhossAPOtel.ATTR_DOCTYPE_ID, sDocTypeID)
                                        .put (CPhossAPOtel.ATTR_PROCESS_ID, sProcessID)
                                        .put (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID, sSbdhInstanceID)
                                        .build ();
    PhossAPTelemetry.inboundReceiverNotServiced ().add (1, aAttrs);
  }

  /** {@inheritDoc} */
  public void onInboundPermanentForwardingFailure (@NonNull final String sTransactionID,
                                                   @NonNull final String sSbdhInstanceID,
                                                   @Nullable final String sErrorDetails)
  {
    PhossAPTelemetry.inboundForwardingPermanentFailures ()
                    .add (1, _baseTxAttrs (sTransactionID, sSbdhInstanceID).build ());
  }

  /** {@inheritDoc} */
  public void onInboundMLSCorrelationError (@NonNull final String sTransactionID,
                                            @NonNull final String sReferencedSbdhInstanceID,
                                            @NonNull final EPeppolMLSResponseCode eMlsResponseCode)
  {
    final Attributes aAttrs = Attributes.builder ()
                                        .put (CPhossAPOtel.ATTR_TRANSACTION_ID, sTransactionID)
                                        .put (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID, sReferencedSbdhInstanceID)
                                        .put (CPhossAPOtel.ATTR_MLS_RESPONSE_CODE, eMlsResponseCode.getID ())
                                        .build ();
    PhossAPTelemetry.inboundMLSCorrelationErrors ().add (1, aAttrs);
  }

  /** {@inheritDoc} */
  public void onInboundForwardingError (@NonNull final String sTransactionID, final boolean bIsRetry)
  {
    final Attributes aAttrs = Attributes.builder ()
                                        .put (CPhossAPOtel.ATTR_TRANSACTION_ID, sTransactionID)
                                        .put (CPhossAPOtel.ATTR_IS_RETRY, Boolean.valueOf (bIsRetry))
                                        .build ();
    PhossAPTelemetry.inboundForwardingErrors ().add (1, aAttrs);
  }

  /** {@inheritDoc} */
  public void onPeppolReportingTSRFailure (@NonNull final YearMonth aYearMonth)
  {
    final Attributes aAttrs = Attributes.builder ()
                                        .put (CPhossAPOtel.ATTR_REPORT_TYPE, "TSR")
                                        .put (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH, aYearMonth.toString ())
                                        .build ();
    PhossAPTelemetry.reportingFailures ().add (1, aAttrs);
  }

  /** {@inheritDoc} */
  public void onPeppolReportingEUSRFailure (@NonNull final YearMonth aYearMonth)
  {
    final Attributes aAttrs = Attributes.builder ()
                                        .put (CPhossAPOtel.ATTR_REPORT_TYPE, "EUSR")
                                        .put (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH, aYearMonth.toString ())
                                        .build ();
    PhossAPTelemetry.reportingFailures ().add (1, aAttrs);
  }

  /** {@inheritDoc} */
  public void onUnexpectedException (@NonNull final String sContext,
                                     @NonNull final String sMessage,
                                     @NonNull final Exception aException)
  {
    final Attributes aAttrs = Attributes.builder ()
                                        .put (CPhossAPOtel.ATTR_EXCEPTION_CONTEXT, sContext)
                                        .put (CPhossAPOtel.ATTR_EXCEPTION_CLASS, aException.getClass ().getName ())
                                        .build ();
    PhossAPTelemetry.unexpectedExceptions ().add (1, aAttrs);

    // If an active span is in flight, attach the exception to it so the trace shows the error.
    final Span aSpan = Span.current ();
    if (aSpan.getSpanContext ().isValid ())
      aSpan.recordException (aException, aAttrs);
  }
}
