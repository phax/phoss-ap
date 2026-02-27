/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

/**
 * SPI interface for receiving notifications about permanent processing
 * failures. Implementations are loaded via {@link java.util.ServiceLoader}.
 * Multiple handlers may be registered. Concrete implementations are
 * deployment-specific (e.g., email, Slack, monitoring system webhook).
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface INotificationHandlerSPI
{
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
  void onVerificationRejection (@NonNull String sTransactionID,
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
  void onPermanentSendingFailure (@NonNull String sTransactionID,
                                  @NonNull String sSbdhInstanceID,
                                  @Nullable String sErrorDetails);

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
  void onPermanentForwardingFailure (@NonNull String sTransactionID,
                                     @NonNull String sSbdhInstanceID,
                                     @Nullable String sErrorDetails);
}
