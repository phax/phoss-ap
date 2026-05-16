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

import java.time.Duration;
import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.IsSPIInterface;
import com.helger.peppol.mls.EPeppolMLSResponseCode;

/**
 * SPI interface for receiving notifications about positive lifecycle events of the AP — e.g.
 * documents received, forwarded, sent; reports submitted successfully; scheduler cycle completion.
 * <p>
 * This SPI is the counterpart of {@link IAPNotificationHandlerSPI}, which carries
 * <em>failure</em>-side callbacks. The split is deliberate: handlers that target error trackers
 * (e.g. Sentry) implement only {@link IAPNotificationHandlerSPI}, while observability handlers
 * (e.g. OpenTelemetry) implement this interface to feed counters and histograms for throughput
 * and SLA dashboards.
 * <p>
 * Implementations are loaded via {@link java.util.ServiceLoader}. Multiple handlers may be
 * registered.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
@IsSPIInterface
public interface IAPLifecycleEventSPI
{
  /**
   * Called when an inbound AS4 message has been received and persisted in the database.
   *
   * @param sTransactionID
   *        The newly created inbound transaction ID. Never <code>null</code>.
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
   * @param bIsDuplicateAS4
   *        <code>true</code> if an AS4 message with the same ID was previously received.
   * @param bIsDuplicateSBDH
   *        <code>true</code> if an SBDH with the same Instance ID was previously received.
   */
  void onInboundDocumentReceived (@NonNull String sTransactionID,
                                  @NonNull String sSenderID,
                                  @NonNull String sReceiverID,
                                  @NonNull String sDocTypeID,
                                  @NonNull String sProcessID,
                                  @NonNull String sSbdhInstanceID,
                                  boolean bIsDuplicateAS4,
                                  boolean bIsDuplicateSBDH);

  /**
   * Called when an inbound document passes optional verification.
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   */
  void onInboundVerificationAccepted (@NonNull String sTransactionID, @NonNull String sSbdhInstanceID);

  /**
   * Called when an outbound document passes optional verification before sending.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   */
  void onOutboundVerificationAccepted (@NonNull String sSbdhInstanceID);

  /**
   * Called when an inbound MLS message has been successfully correlated with the original outbound
   * transaction it refers to.
   *
   * @param sMlsTransactionID
   *        The inbound MLS transaction ID. Never <code>null</code>.
   * @param sReferencedSbdhInstanceID
   *        The SBDH Instance Identifier of the original outbound document. Never <code>null</code>.
   * @param eMlsResponseCode
   *        The response code carried by the MLS. Never <code>null</code>.
   * @param aRoundTrip
   *        The wall-clock duration between the original outbound send completion and the MLS
   *        reception, or <code>null</code> if either timestamp was not available. When present,
   *        this value powers MLS-1 / MLS-2 SLA histograms.
   */
  void onInboundMLSCorrelated (@NonNull String sMlsTransactionID,
                               @NonNull String sReferencedSbdhInstanceID,
                               @NonNull EPeppolMLSResponseCode eMlsResponseCode,
                               @Nullable Duration aRoundTrip);

  /**
   * Called when an inbound document has been successfully forwarded to the Receiver Backend (C4).
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param aForwardingDuration
   *        The wall-clock duration from the original AS4 reception to the successful forwarding,
   *        or <code>null</code> if the receive timestamp was not available.
   * @param bIsRetry
   *        <code>true</code> if the successful forwarding happened on a retry attempt,
   *        <code>false</code> on the first attempt.
   */
  void onInboundDocumentForwarded (@NonNull String sTransactionID,
                                   @NonNull String sSbdhInstanceID,
                                   @Nullable Duration aForwardingDuration,
                                   boolean bIsRetry);

  /**
   * Called when an outbound transaction has been accepted (document persisted, in
   * {@code PENDING} state).
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
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
  void onOutboundDocumentAccepted (@NonNull String sTransactionID,
                                   @NonNull String sSenderID,
                                   @NonNull String sReceiverID,
                                   @NonNull String sDocTypeID,
                                   @NonNull String sProcessID,
                                   @NonNull String sSbdhInstanceID);

  /**
   * Called when an outbound transaction has been successfully transmitted via AS4 and the receipt
   * has been confirmed.
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param aSendingDuration
   *        The wall-clock duration from the outbound transaction creation to the successful send,
   *        or <code>null</code> if the creation timestamp was not available.
   * @param nAttempts
   *        The total number of attempts that were made before the message was confirmed. Always
   *        &gt;= 1.
   */
  void onOutboundDocumentSent (@NonNull String sTransactionID,
                               @NonNull String sSbdhInstanceID,
                               @Nullable Duration aSendingDuration,
                               @Nonnegative int nAttempts);

  /**
   * Called when a Peppol TSR report for the given year-month has been successfully generated,
   * validated and sent to OpenPeppol.
   *
   * @param aYearMonth
   *        The year and month covered by the report. Never <code>null</code>.
   */
  void onPeppolReportingTSRSuccess (@NonNull YearMonth aYearMonth);

  /**
   * Called when a Peppol EUSR report for the given year-month has been successfully generated,
   * validated and sent to OpenPeppol.
   *
   * @param aYearMonth
   *        The year and month covered by the report. Never <code>null</code>.
   */
  void onPeppolReportingEUSRSuccess (@NonNull YearMonth aYearMonth);

  /**
   * Called once per direction at the end of a retry scheduler cycle (after both outbound and
   * inbound batches have been processed).
   *
   * @param bIsOutbound
   *        <code>true</code> for the outbound retry cycle, <code>false</code> for the inbound
   *        retry cycle.
   * @param nProcessed
   *        Number of transactions actually attempted in this cycle. Always &gt;= 0.
   * @param aCycleDuration
   *        Wall-clock duration of the cycle. Never <code>null</code>.
   */
  void onRetrySchedulerCycle (boolean bIsOutbound, @Nonnegative int nProcessed, @NonNull Duration aCycleDuration);

  /**
   * Called once per direction at the end of an archival scheduler cycle.
   *
   * @param bIsOutbound
   *        <code>true</code> for the outbound archival cycle, <code>false</code> for the inbound
   *        archival cycle.
   * @param nArchived
   *        Number of transactions archived in this cycle. Always &gt;= 0.
   * @param aCycleDuration
   *        Wall-clock duration of the cycle. Never <code>null</code>.
   */
  void onArchivalSchedulerCycle (boolean bIsOutbound, @Nonnegative int nArchived, @NonNull Duration aCycleDuration);

  /**
   * Called once per direction at the end of a cleanup scheduler cycle (deletion of archived
   * transactions whose retention has expired).
   *
   * @param bIsOutbound
   *        <code>true</code> for the outbound cleanup cycle, <code>false</code> for the inbound
   *        cleanup cycle.
   * @param nDeleted
   *        Number of archived transactions deleted in this cycle. Always &gt;= 0.
   * @param aCycleDuration
   *        Wall-clock duration of the cycle. Never <code>null</code>.
   */
  void onCleanupSchedulerCycle (boolean bIsOutbound, @Nonnegative int nDeleted, @NonNull Duration aCycleDuration);
}
