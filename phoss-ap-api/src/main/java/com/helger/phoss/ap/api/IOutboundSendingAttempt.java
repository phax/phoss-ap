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
package com.helger.phoss.ap.api;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;

/**
 * Read-only view of a single outbound sending attempt. Each attempt represents one AS4 message
 * exchange with the remote AP.
 *
 * @author Philip Helger
 */
public interface IOutboundSendingAttempt extends IHasID <String>
{
  /**
   * @return The unique attempt ID. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getID ();

  /**
   * @return The ID of the parent outbound transaction. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getOutboundTransactionID ();

  /**
   * @return The AS4 Message ID used for this attempt. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getAS4MessageID ();

  /**
   * @return The AS4 MessageInfo/Timestamp from this attempt (UTC). Never <code>null</code>.
   */
  @NonNull
  OffsetDateTime getAS4Timestamp ();

  /**
   * @return The AS4 Message ID from the synchronous receipt, or <code>null</code> on failure.
   */
  @Nullable
  String getReceiptMessageID ();

  /**
   * @return The HTTP status code from the AS4 response, or <code>null</code> on failure.
   */
  @Nullable
  Integer getHttpStatusCode ();

  /**
   * @return The date/time of this sending attempt. Never <code>null</code>.
   */
  @NonNull
  OffsetDateTime getAttemptDT ();

  /**
   * @return The outcome of this attempt. Never <code>null</code>.
   */
  @NonNull
  EAttemptStatus getAttemptStatus ();

  /**
   * @return The error message or reason for failure, or <code>null</code> on success.
   */
  @Nullable
  String getErrorDetails ();
}
