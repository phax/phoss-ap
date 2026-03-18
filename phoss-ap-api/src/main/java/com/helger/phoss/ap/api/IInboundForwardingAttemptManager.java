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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.model.IInboundForwardingAttempt;

/**
 * Manager interface for creating and querying forwarding transaction attempts.
 *
 * @author Philip Helger
 */
public interface IInboundForwardingAttemptManager
{
  /**
   * Record a successful forwarding attempt for the given inbound transaction.
   *
   * @param sInboundTransactionID
   *        The parent inbound transaction ID. Never <code>null</code>.
   * @return The ID of the created attempt row, or <code>null</code> if insertion fails.
   */
  @Nullable
  String createSuccess (@NonNull String sInboundTransactionID);

  /**
   * Record a failed forwarding attempt for the given inbound transaction.
   *
   * @param sInboundTransactionID
   *        The parent inbound transaction ID. Never <code>null</code>.
   * @param sErrorCode
   *        Machine-readable error code classifying the failure. May be <code>null</code>.
   * @param sErrorDetails
   *        Human-readable error description. May be <code>null</code>.
   * @return The ID of the created attempt row, or <code>null</code> if insertion fails.
   */
  @Nullable
  String createFailure (@NonNull String sInboundTransactionID,
                        @Nullable String sErrorCode,
                        @Nullable String sErrorDetails);

  /**
   * Get all forwarding attempts for the given inbound transaction, ordered by attempt date.
   *
   * @param sInboundTransactionID
   *        The parent inbound transaction ID. Never <code>null</code>.
   * @return A list of attempts. Never <code>null</code> but may be empty.
   */
  @NonNull
  ICommonsList <IInboundForwardingAttempt> getByTransactionID (@NonNull String sInboundTransactionID);
}
