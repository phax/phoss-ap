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
package com.helger.phoss.ap.api.model;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;

/**
 * Read-only view of a single transaction audit log event (lifecycle status change or operator action).
 *
 * @author Philip Helger
 */
public interface ITransactionAuditItem
{
  /**
   * @return The auto-incremented audit log ID.
   */
  long getID ();

  /**
   * @return The internal transaction ID, or <code>null</code> if not known.
   */
  @Nullable
  String getTransactionID ();

  /**
   * @return The Peppol SBDH Instance Identifier. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getSbdhInstanceID ();

  /**
   * @return The direction: "INBOUND" or "OUTBOUND". Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getDirection ();

  /**
   * @return Event classification: "STATUS_CHANGE" or "OPERATOR_ACTION". Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getEventType ();

  /**
   * @return Action name, e.g. "STATUS_UPDATE", "REPLAY_INBOUND", "REVERIFY_AND_FORWARD_INBOUND",
   *         "REPLAY_OUTBOUND", "SEND_OUTBOUND_MANUAL", "C4_REPORTING_SUBMIT". Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getAction ();

  /**
   * @return Previous status before transition, or <code>null</code> for action events without state shift.
   */
  @Nullable
  String getFromStatus ();

  /**
   * @return New status after transition, or <code>null</code>.
   */
  @Nullable
  String getToStatus ();

  /**
   * @return Attempt count at event time, or <code>null</code>.
   */
  @Nullable
  Integer getAttemptCount ();

  /**
   * @return The operator ID, username, email, or "SYSTEM". Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getPerformedBy ();

  /**
   * @return Timestamp when event occurred. Never <code>null</code>.
   */
  @NonNull
  OffsetDateTime getPerformedDT ();

  /**
   * @return Additional details or error message.
   */
  @Nullable
  String getDetails ();
}
