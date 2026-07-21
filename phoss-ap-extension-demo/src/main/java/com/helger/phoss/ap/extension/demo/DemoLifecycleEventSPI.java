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

import java.time.Duration;
import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.spi.IAPLifecycleEventSPI;

/**
 * Demo implementation of {@link IAPLifecycleEventSPI} that only logs each positive lifecycle event.
 * Its sole purpose is to prove that an externally supplied SPI implementation (dropped into the
 * <code>/ext</code> extension directory) is discovered and invoked by the running Access Point.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class DemoLifecycleEventSPI implements IAPLifecycleEventSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (DemoLifecycleEventSPI.class);
  private static final String PREFIX = "[extension-demo] Lifecycle event ";

  public DemoLifecycleEventSPI ()
  {
    LOGGER.info ("[extension-demo] " + DemoLifecycleEventSPI.class.getSimpleName () + " was loaded via SPI");
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
    LOGGER.info (PREFIX +
                 "onInboundDocumentReceived: transactionID=" +
                 sTransactionID +
                 ", senderID=" +
                 sSenderID +
                 ", receiverID=" +
                 sReceiverID +
                 ", docTypeID=" +
                 sDocTypeID +
                 ", processID=" +
                 sProcessID +
                 ", sbdhInstanceID=" +
                 sSbdhInstanceID +
                 ", duplicateAS4=" +
                 bIsDuplicateAS4 +
                 ", duplicateSBDH=" +
                 bIsDuplicateSBDH);
  }

  /** {@inheritDoc} */
  public void onInboundVerificationAccepted (@NonNull final String sTransactionID,
                                             @NonNull final String sSbdhInstanceID)
  {
    LOGGER.info (PREFIX +
                 "onInboundVerificationAccepted: transactionID=" +
                 sTransactionID +
                 ", sbdhInstanceID=" +
                 sSbdhInstanceID);
  }

  /** {@inheritDoc} */
  public void onOutboundVerificationAccepted (@NonNull final String sSbdhInstanceID)
  {
    LOGGER.info (PREFIX + "onOutboundVerificationAccepted: sbdhInstanceID=" + sSbdhInstanceID);
  }

  /** {@inheritDoc} */
  public void onInboundMLSCorrelated (@NonNull final String sMlsTransactionID,
                                      @NonNull final String sReferencedSbdhInstanceID,
                                      @NonNull final EPeppolMLSResponseCode eMlsResponseCode,
                                      @Nullable final Duration aRoundTrip)
  {
    LOGGER.info (PREFIX +
                 "onInboundMLSCorrelated: mlsTransactionID=" +
                 sMlsTransactionID +
                 ", referencedSbdhInstanceID=" +
                 sReferencedSbdhInstanceID +
                 ", mlsResponseCode=" +
                 eMlsResponseCode +
                 ", roundTrip=" +
                 aRoundTrip);
  }

  /** {@inheritDoc} */
  public void onInboundDocumentForwarded (@NonNull final String sTransactionID,
                                          @NonNull final String sSbdhInstanceID,
                                          @Nullable final Duration aForwardingDuration,
                                          final boolean bIsRetry)
  {
    LOGGER.info (PREFIX +
                 "onInboundDocumentForwarded: transactionID=" +
                 sTransactionID +
                 ", sbdhInstanceID=" +
                 sSbdhInstanceID +
                 ", forwardingDuration=" +
                 aForwardingDuration +
                 ", isRetry=" +
                 bIsRetry);
  }

  /** {@inheritDoc} */
  public void onOutboundDocumentAccepted (@NonNull final String sTransactionID,
                                          @NonNull final String sSenderID,
                                          @NonNull final String sReceiverID,
                                          @NonNull final String sDocTypeID,
                                          @NonNull final String sProcessID,
                                          @NonNull final String sSbdhInstanceID)
  {
    LOGGER.info (PREFIX +
                 "onOutboundDocumentAccepted: transactionID=" +
                 sTransactionID +
                 ", senderID=" +
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
  public void onOutboundDocumentSent (@NonNull final String sTransactionID,
                                      @NonNull final String sSbdhInstanceID,
                                      @Nullable final Duration aSendingDuration,
                                      @Nonnegative final int nAttempts)
  {
    LOGGER.info (PREFIX +
                 "onOutboundDocumentSent: transactionID=" +
                 sTransactionID +
                 ", sbdhInstanceID=" +
                 sSbdhInstanceID +
                 ", sendingDuration=" +
                 aSendingDuration +
                 ", attempts=" +
                 nAttempts);
  }

  /** {@inheritDoc} */
  public void onPeppolReportingTSRSuccess (@NonNull final YearMonth aYearMonth)
  {
    LOGGER.info (PREFIX + "onPeppolReportingTSRSuccess: yearMonth=" + aYearMonth);
  }

  /** {@inheritDoc} */
  public void onPeppolReportingEUSRSuccess (@NonNull final YearMonth aYearMonth)
  {
    LOGGER.info (PREFIX + "onPeppolReportingEUSRSuccess: yearMonth=" + aYearMonth);
  }

  /** {@inheritDoc} */
  public void onRetrySchedulerCycle (final boolean bIsOutbound,
                                     @Nonnegative final int nProcessed,
                                     @NonNull final Duration aCycleDuration)
  {
    LOGGER.info (PREFIX +
                 "onRetrySchedulerCycle: outbound=" +
                 bIsOutbound +
                 ", processed=" +
                 nProcessed +
                 ", cycleDuration=" +
                 aCycleDuration);
  }

  /** {@inheritDoc} */
  public void onArchivalSchedulerCycle (final boolean bIsOutbound,
                                        @Nonnegative final int nArchived,
                                        @NonNull final Duration aCycleDuration)
  {
    LOGGER.info (PREFIX +
                 "onArchivalSchedulerCycle: outbound=" +
                 bIsOutbound +
                 ", archived=" +
                 nArchived +
                 ", cycleDuration=" +
                 aCycleDuration);
  }

  /** {@inheritDoc} */
  public void onCleanupSchedulerCycle (final boolean bIsOutbound,
                                       @Nonnegative final int nDeleted,
                                       @NonNull final Duration aCycleDuration)
  {
    LOGGER.info (PREFIX +
                 "onCleanupSchedulerCycle: outbound=" +
                 bIsOutbound +
                 ", deleted=" +
                 nDeleted +
                 ", cycleDuration=" +
                 aCycleDuration);
  }
}
