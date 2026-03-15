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
package com.helger.phoss.ap.api;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;
import com.helger.phoss.ap.api.model.IOutboundSendingAttempt;

/**
 * Manager interface for creating and querying outbound transaction attempts.
 *
 * @author Philip Helger
 */
public interface IOutboundSendingAttemptManager
{
  /**
   * Record a sending attempt with a specific status.
   *
   * @param sOutboundTransactionID
   *        The parent outbound transaction ID. Never <code>null</code>.
   * @param sAS4MessageID
   *        The AS4 Message ID used for this attempt. Never <code>null</code>.
   * @param aAS4Timestamp
   *        The AS4 MessageInfo/Timestamp (UTC). Never <code>null</code>.
   * @param sReceiptMessageID
   *        The AS4 Message ID from the synchronous receipt. May be
   *        <code>null</code> on failure.
   * @param aHttpStatusCode
   *        The HTTP status code from the AS4 response. May be <code>null</code>
   *        on failure.
   * @param eAttemptStatus
   *        The outcome of this attempt. Never <code>null</code>.
   * @param sErrorDetails
   *        Error description on failure. May be <code>null</code>.
   * @param sSendingReport
   *        The Phase4 Peppol sending report as JSON. May be <code>null</code>.
   * @return The ID of the created attempt row, or <code>null</code> if
   *         insertion fails.
   */
  @Nullable
  String create (@NonNull String sOutboundTransactionID,
                 @NonNull String sAS4MessageID,
                 @NonNull OffsetDateTime aAS4Timestamp,
                 @Nullable String sReceiptMessageID,
                 @Nullable Integer aHttpStatusCode,
                 @NonNull EAttemptStatus eAttemptStatus,
                 @Nullable String sErrorDetails,
                 @Nullable String sSendingReport);

  /**
   * Convenience method to record a successful sending attempt.
   *
   * @param sOutboundTransactionID
   *        The parent outbound transaction ID. Never <code>null</code>.
   * @param sAS4MessageID
   *        The AS4 Message ID used for this attempt. Never <code>null</code>.
   * @param aAS4Timestamp
   *        The AS4 MessageInfo/Timestamp (UTC). Never <code>null</code>.
   * @param sReceiptMessageID
   *        The AS4 Message ID from the synchronous receipt. Never
   *        <code>null</code>.
   * @param sSendingReport
   *        The Phase4 Peppol sending report as JSON. May be <code>null</code>.
   * @return The ID of the created attempt row, or <code>null</code> if
   *         insertion fails.
   */
  @Nullable
  String createSuccess (@NonNull String sOutboundTransactionID,
                        @NonNull String sAS4MessageID,
                        @NonNull OffsetDateTime aAS4Timestamp,
                        @NonNull String sReceiptMessageID,
                        @Nullable String sSendingReport);

  /**
   * Get all sending attempts for the given outbound transaction, ordered by
   * attempt date.
   *
   * @param sOutboundTransactionID
   *        The parent outbound transaction ID. Never <code>null</code>.
   * @return A list of attempts. Never <code>null</code> but may be empty.
   */
  @NonNull
  ICommonsList <IOutboundSendingAttempt> getByTransactionID (@NonNull String sOutboundTransactionID);
}
