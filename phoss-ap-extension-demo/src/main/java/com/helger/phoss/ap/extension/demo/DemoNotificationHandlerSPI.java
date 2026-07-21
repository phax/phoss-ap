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
package com.helger.phoss.ap.extension.demo;

import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.spi.IAPNotificationHandlerSPI;

/**
 * Demo implementation of {@link IAPNotificationHandlerSPI} that only logs each failure-side
 * notification. Its sole purpose is to prove that an externally supplied SPI implementation
 * (dropped into the <code>/ext</code> extension directory) is discovered and invoked by the running
 * Access Point.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class DemoNotificationHandlerSPI implements IAPNotificationHandlerSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (DemoNotificationHandlerSPI.class);
  private static final String PREFIX = "[extension-demo] Notification ";

  public DemoNotificationHandlerSPI ()
  {
    LOGGER.info ("[extension-demo] " + DemoNotificationHandlerSPI.class.getSimpleName () + " was loaded via SPI");
  }

  /** {@inheritDoc} */
  public void onInboundReceiverNotServiced (@NonNull final String sSenderID,
                                            @NonNull final String sReceiverID,
                                            @NonNull final String sDocTypeID,
                                            @NonNull final String sProcessID,
                                            @NonNull final String sSbdhInstanceID)
  {
    LOGGER.info (PREFIX +
                 "onInboundReceiverNotServiced: senderID=" +
                 sSenderID +
                 ", receiverID=" +
                 sReceiverID +
                 ", docTypeID=" +
                 sDocTypeID +
                 ", processID=" +
                 sProcessID +
                 ", sbdhInstanceID=" +
                 sSbdhInstanceID);
  }

  /** {@inheritDoc} */
  public void onInboundVerificationRejection (@NonNull final String sTransactionID,
                                              @NonNull final String sSbdhInstanceID,
                                              @Nullable final String sErrorDetails)
  {
    LOGGER.info (PREFIX +
                 "onInboundVerificationRejection: transactionID=" +
                 sTransactionID +
                 ", sbdhInstanceID=" +
                 sSbdhInstanceID +
                 ", errorDetails=" +
                 sErrorDetails);
  }

  /** {@inheritDoc} */
  public void onOutboundVerificationRejection (@NonNull final String sSbdhInstanceID,
                                               @Nullable final String sErrorDetails)
  {
    LOGGER.info (PREFIX +
                 "onOutboundVerificationRejection: sbdhInstanceID=" +
                 sSbdhInstanceID +
                 ", errorDetails=" +
                 sErrorDetails);
  }

  /** {@inheritDoc} */
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
    LOGGER.info (PREFIX +
                 "onInboundDuplicateRejected: senderID=" +
                 sSenderID +
                 ", receiverID=" +
                 sReceiverID +
                 ", docTypeID=" +
                 sDocTypeID +
                 ", processID=" +
                 sProcessID +
                 ", senderProviderID=" +
                 sSenderProviderID +
                 ", as4MessageID=" +
                 sAS4MessageID +
                 ", sbdhInstanceID=" +
                 sSbdhInstanceID +
                 ", duplicateAS4=" +
                 bIsDuplicateAS4 +
                 ", duplicateSBDH=" +
                 bIsDuplicateSBDH +
                 ", errorDetails=" +
                 sErrorDetails);
  }

  /** {@inheritDoc} */
  public void onInboundMLSCorrelationError (@NonNull final String sTransactionID,
                                            @NonNull final String sReferencedSbdhInstanceID,
                                            @NonNull final EPeppolMLSResponseCode eMlsResponseCode)
  {
    LOGGER.info (PREFIX +
                 "onInboundMLSCorrelationError: transactionID=" +
                 sTransactionID +
                 ", referencedSbdhInstanceID=" +
                 sReferencedSbdhInstanceID +
                 ", mlsResponseCode=" +
                 eMlsResponseCode);
  }

  /** {@inheritDoc} */
  public void onInboundForwardingError (@NonNull final String sTransactionID, final boolean bIsRetry)
  {
    LOGGER.info (PREFIX + "onInboundForwardingError: transactionID=" + sTransactionID + ", isRetry=" + bIsRetry);
  }

  /** {@inheritDoc} */
  public void onInboundPermanentForwardingFailure (@NonNull final String sTransactionID,
                                                   @NonNull final String sSbdhInstanceID,
                                                   @Nullable final String sErrorDetails)
  {
    LOGGER.info (PREFIX +
                 "onInboundPermanentForwardingFailure: transactionID=" +
                 sTransactionID +
                 ", sbdhInstanceID=" +
                 sSbdhInstanceID +
                 ", errorDetails=" +
                 sErrorDetails);
  }

  /** {@inheritDoc} */
  public void onOutboundPermanentSendingFailure (@NonNull final String sTransactionID,
                                                 @NonNull final String sSbdhInstanceID,
                                                 @Nullable final String sErrorDetails)
  {
    LOGGER.info (PREFIX +
                 "onOutboundPermanentSendingFailure: transactionID=" +
                 sTransactionID +
                 ", sbdhInstanceID=" +
                 sSbdhInstanceID +
                 ", errorDetails=" +
                 sErrorDetails);
  }

  /** {@inheritDoc} */
  public void onPeppolReportingTSRFailure (@NonNull final YearMonth aYearMonth)
  {
    LOGGER.info (PREFIX + "onPeppolReportingTSRFailure: yearMonth=" + aYearMonth);
  }

  /** {@inheritDoc} */
  public void onPeppolReportingEUSRFailure (@NonNull final YearMonth aYearMonth)
  {
    LOGGER.info (PREFIX + "onPeppolReportingEUSRFailure: yearMonth=" + aYearMonth);
  }

  /** {@inheritDoc} */
  public void onUnexpectedException (@NonNull final String sContext,
                                     @NonNull final String sMessage,
                                     @NonNull final Exception aException)
  {
    LOGGER.info (PREFIX + "onUnexpectedException: context=" + sContext + ", message=" + sMessage, aException);
  }
}
