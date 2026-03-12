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
package com.helger.phoss.ap.sentry;

import java.time.YearMonth;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.spi.IAPNotificationHandlerSPI;

import io.sentry.Sentry;
import io.sentry.SentryAttributes;
import io.sentry.SentryLogLevel;
import io.sentry.logger.SentryLogParameters;

/**
 * Special implementation of {@link IAPNotificationHandlerSPI} for Sentry log events. It is not
 * registered as an SPI provider, because it is included dependent on the existence of the Sentry
 * dependencies.
 *
 * @author Philip Helger
 */
public class APNotificationHandlerSentry implements IAPNotificationHandlerSPI
{
  private static void _logError (@NonNull final String sMsg, @NonNull final Map <String, Object> aParams)
  {
    Sentry.logger ().log (SentryLogLevel.ERROR, SentryLogParameters.create (SentryAttributes.fromMap (aParams)), sMsg);
  }

  public void onInboundVerificationRejection (@NonNull final String sTransactionID,
                                              @NonNull final String sSbdhInstanceID,
                                              @Nullable final String sErrorDetails)
  {
    _logError ("onInboundVerificationRejection",
               Map.of ("transactionID",
                       sTransactionID,
                       "sbdhInstanceID",
                       sSbdhInstanceID,
                       "errorDetails",
                       sErrorDetails));
  }

  public void onOutboundPermanentSendingFailure (@NonNull final String sTransactionID,
                                                 @NonNull final String sSbdhInstanceID,
                                                 @Nullable final String sErrorDetails)
  {
    _logError ("onPermanentSendingFailure",
               Map.of ("transactionID",
                       sTransactionID,
                       "sbdhInstanceID",
                       sSbdhInstanceID,
                       "errorDetails",
                       sErrorDetails));
  }

  public void onInboundReceiverNotServiced (@NonNull final String sSenderID,
                                            @NonNull final String sReceiverID,
                                            @NonNull final String sDocTypeID,
                                            @NonNull final String sProcessID,
                                            @NonNull final String sSbdhInstanceID)
  {
    _logError ("onInboundReceiverNotServiced",
               Map.of ("senderID",
                       sSenderID,
                       "receiverID",
                       sReceiverID,
                       "docTypeID",
                       sDocTypeID,
                       "processID",
                       sProcessID,
                       "sbdhInstanceID",
                       sSbdhInstanceID));
  }

  public void onInboundPermanentForwardingFailure (@NonNull final String sTransactionID,
                                                   @NonNull final String sSbdhInstanceID,
                                                   @Nullable final String sErrorDetails)
  {
    _logError ("onPermanentForwardingFailure",
               Map.of ("transactionID",
                       sTransactionID,
                       "sbdhInstanceID",
                       sSbdhInstanceID,
                       "errorDetails",
                       sErrorDetails));
  }

  public void onInboundMLSCorrelationError (@NonNull final String sTransactionID,
                                            @NonNull final String sReferencedSbdhInstanceID,
                                            @NonNull final EPeppolMLSResponseCode eMlsResponseCode)
  {
    _logError ("onInboundMLSCorrelationError",
               Map.of ("transactionID",
                       sTransactionID,
                       "referencedSbdhInstanceID",
                       sReferencedSbdhInstanceID,
                       "mlsResponseCode",
                       eMlsResponseCode.getID ()));
  }

  public void onInboundForwardingError (@NonNull final String sTransactionID, final boolean bIsRetry)
  {
    _logError ("onInboundForwardingError",
               Map.of ("transactionID", sTransactionID, "isRetry", Boolean.valueOf (bIsRetry)));
  }

  public void onPeppolReportingTSRFailure (@NonNull final YearMonth aYearMonth)
  {
    _logError ("onPeppolReportingTSRFailure",
               Map.of ("year",
                       Integer.valueOf (aYearMonth.getYear ()),
                       "month",
                       Integer.valueOf (aYearMonth.getMonthValue ())));
  }

  public void onPeppolReportingEUSRFailure (@NonNull final YearMonth aYearMonth)
  {
    _logError ("onPeppolReportingEUSRFailure",
               Map.of ("year",
                       Integer.valueOf (aYearMonth.getYear ()),
                       "month",
                       Integer.valueOf (aYearMonth.getMonthValue ())));
  }
}
