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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.IsSPIInterface;
import com.helger.peppol.mls.EPeppolMLSResponseCode;

/**
 * SPI interface for receiving notifications about permanent processing failures. Implementations
 * are loaded via {@link java.util.ServiceLoader}. Multiple handlers may be registered. Concrete
 * implementations are deployment-specific (e.g., email, Slack, monitoring system webhook).
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface INotificationHandlerSPI
{
  /**
   * Called when an outbound or inbound document fails optional verification and is rejected.
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
   * Called when an outbound transaction permanently fails after exhausting all sending retries.
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param sErrorDetails
   *        Optional error details. May be <code>null</code>.
   */
  void onPermanentSendingFailure (@NonNull String sTransactionID,
                                  @NonNull String sSbdhInstanceID,
                                  @Nullable String sErrorDetails);

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
   * Called when an inbound transaction permanently fails after exhausting all forwarding retries.
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param sErrorDetails
   *        Optional error details. May be <code>null</code>.
   */
  void onPermanentForwardingFailure (@NonNull String sTransactionID,
                                     @NonNull String sSbdhInstanceID,
                                     @Nullable String sErrorDetails);

  /**
   * Called when the inbound message is an MLS but could not be correlated with an outbound
   * transaction.
   *
   * @param sTxID
   *        The incoming transaction ID. May not be <code>null</code>.
   * @param sReferencedSbdhInstanceID
   *        The referenced SBDH ID from the MLS. May not be <code>null</code>.
   * @param eMlsResponseCode
   *        The response code contained in the MLS. May not be <code>null</code>.
   */
  void onInboundMLSCorrelationError (@NonNull String sTxID,
                                     @NonNull String sReferencedSbdhInstanceID,
                                     @NonNull EPeppolMLSResponseCode eMlsResponseCode);

  /**
   * Called if an inbound messages could not be forwarded properly. The database state has already
   * been updated when this is called.
   *
   * @param sTxID
   *        The inbound transaction ID. May not be <code>null</code>.
   * @param bIsRetry
   *        <code>true</code> if it is a retry, <code>false</code> if it is the original request.
   */
  void onInboundForwardingError (@NonNull String sTxID, boolean bIsRetry);
}
