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
package com.helger.phoss.ap.api.spi;

import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.IsSPIInterface;
import com.helger.peppol.mls.EPeppolMLSResponseCode;

/**
 * SPI interface for receiving notifications about permanent processing
 * failures. Implementations are loaded via {@link java.util.ServiceLoader}.
 * Multiple handlers may be registered. Concrete implementations are
 * deployment-specific (e.g., email, Slack, monitoring system webhook).
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface IAPNotificationHandlerSPI
{
  /**
   * Called when an inbound receiver is not serviced.
   *
   * @param sSenderID
   *        Peppol sender ID (C1). Never <code>null</code>.
   * @param sReceiverID
   *        Peppol receiver ID (C4). Never <code>null</code>.
   * @param sDocTypeID
   *        Peppol document type ID. Never <code>null</code>.
   * @param sProcessID
   *        Peppol process ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        SBDH Instance Identifier. Never <code>null</code>.
   */
  void onInboundReceiverNotServiced (@NonNull String sSenderID,
                                     @NonNull String sReceiverID,
                                     @NonNull String sDocTypeID,
                                     @NonNull String sProcessID,
                                     @NonNull String sSbdhInstanceID);

  /**
   * Called when an outbound or inbound document fails optional verification and
   * is rejected.
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param sErrorDetails
   *        Optional error details. May be <code>null</code>.
   */
  void onInboundVerificationRejection (@NonNull String sTransactionID,
                                       @NonNull String sSbdhInstanceID,
                                       @Nullable String sErrorDetails);

  /**
   * Called when the inbound message is an MLS but could not be correlated with
   * an outbound transaction.
   *
   * @param sTransactionID
   *        The incoming transaction ID. May not be <code>null</code>.
   * @param sReferencedSbdhInstanceID
   *        The referenced SBDH ID from the MLS. May not be <code>null</code>.
   * @param eMlsResponseCode
   *        The response code contained in the MLS. May not be
   *        <code>null</code>.
   */
  void onInboundMLSCorrelationError (@NonNull String sTransactionID,
                                     @NonNull String sReferencedSbdhInstanceID,
                                     @NonNull EPeppolMLSResponseCode eMlsResponseCode);

  /**
   * Called if an inbound messages could not be forwarded properly. The database
   * state has already been updated when this is called.
   *
   * @param sTransactionID
   *        The inbound transaction ID. May not be <code>null</code>.
   * @param bIsRetry
   *        <code>true</code> if it is a retry, <code>false</code> if it is the
   *        original request.
   */
  void onInboundForwardingError (@NonNull String sTransactionID, boolean bIsRetry);

  /**
   * Called when an inbound transaction permanently fails after exhausting all
   * forwarding retries.
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param sErrorDetails
   *        Optional error details. May be <code>null</code>.
   */
  void onInboundPermanentForwardingFailure (@NonNull String sTransactionID,
                                            @NonNull String sSbdhInstanceID,
                                            @Nullable String sErrorDetails);

  /**
   * Called when an outbound transaction permanently fails after exhausting all
   * sending retries.
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param sErrorDetails
   *        Optional error details. May be <code>null</code>.
   */
  void onOutboundPermanentSendingFailure (@NonNull String sTransactionID,
                                          @NonNull String sSbdhInstanceID,
                                          @Nullable String sErrorDetails);

  /**
   * Called when creating, validating or sending a Peppol Reporting TSR report
   * failed.
   *
   * @param aYearMonth
   *        The year and month for which the reporting should be performed never
   *        <code>null</code>.
   */
  void onPeppolReportingTSRFailure (@NonNull YearMonth aYearMonth);

  /**
   * Called when creating, validating or sending a Peppol Reporting EUSR report
   * failed.
   *
   * @param aYearMonth
   *        The year and month for which the reporting should be performed never
   *        <code>null</code>.
   */
  void onPeppolReportingEUSRFailure (@NonNull YearMonth aYearMonth);

  /**
   * Called when an unexpected exception occurs during processing that is not
   * covered by more specific notification methods.
   *
   * @param sContext
   *        A short description of where the exception occurred (e.g. class and
   *        method name). Never <code>null</code>.
   * @param sMessage
   *        A human-readable description of what went wrong. Never
   *        <code>null</code>.
   * @param aException
   *        The caught exception. Never <code>null</code>.
   */
  void onUnexpectedException (@NonNull String sContext, @NonNull String sMessage, @NonNull Exception aException);
}
