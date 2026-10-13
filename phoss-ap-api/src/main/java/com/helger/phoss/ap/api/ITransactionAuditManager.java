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

import com.helger.annotation.Nonempty;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.phoss.ap.api.model.ITransactionAuditItem;

/**
 * Manager interface for transaction lifecycle history and operator action audit log.
 *
 * @author Philip Helger
 */
public interface ITransactionAuditManager
{
  /**
   * Record an operator or system action into the audit trail and emit to SIEM / ECS log.
   *
   * @param sTransactionID
   *        Internal transaction ID if available, otherwise <code>null</code>.
   * @param sSbdhInstanceID
   *        The Peppol SBDH Instance Identifier. Never <code>null</code>.
   * @param sDirection
   *        "INBOUND" or "OUTBOUND". Never <code>null</code>.
   * @param sEventType
   *        "OPERATOR_ACTION" or "STATUS_CHANGE". Never <code>null</code>.
   * @param sAction
   *        Action code (e.g. "REPLAY_INBOUND", "REVERIFY_AND_FORWARD_INBOUND", "REPLAY_OUTBOUND",
   *        "SEND_OUTBOUND_MANUAL", "C4_REPORTING_SUBMIT"). Never <code>null</code>.
   * @param sFromStatus
   *        Status before operation, or <code>null</code>.
   * @param sToStatus
   *        Status after operation, or <code>null</code>.
   * @param aAttemptCount
   *        Attempt count, or <code>null</code>.
   * @param sPerformedBy
   *        User/Operator ID, username, email, or "SYSTEM". Never <code>null</code>.
   * @param sDetails
   *        Additional context, result, or reason.
   * @return {@link ESuccess#SUCCESS} on success, {@link ESuccess#FAILURE} on error.
   */
  @NonNull
  ESuccess recordAudit (@Nullable String sTransactionID,
                        @NonNull @Nonempty String sSbdhInstanceID,
                        @NonNull @Nonempty String sDirection,
                        @NonNull @Nonempty String sEventType,
                        @NonNull @Nonempty String sAction,
                        @Nullable String sFromStatus,
                        @Nullable String sToStatus,
                        @Nullable Integer aAttemptCount,
                        @NonNull @Nonempty String sPerformedBy,
                        @Nullable String sDetails);

  /**
   * Retrieve all audit entries for a given SBDH Instance ID in chronological order.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance ID to query. Never <code>null</code>.
   * @return Non-null list of audit entries.
   */
  @NonNull
  ICommonsList <ITransactionAuditItem> getTimelineBySbdhInstanceID (@NonNull @Nonempty String sSbdhInstanceID);

  /**
   * Retrieve all audit entries for a given transaction ID in chronological order.
   *
   * @param sTransactionID
   *        The transaction ID to query. Never <code>null</code>.
   * @return Non-null list of audit entries.
   */
  @NonNull
  ICommonsList <ITransactionAuditItem> getTimelineByTransactionID (@NonNull @Nonempty String sTransactionID);

  /**
   * @return Non-null map of SBDH Instance ID to replay count for all transactions replayed.
   */
  @NonNull
  ICommonsMap <String, Integer> getReplayCountsBySbdhInstanceID ();
}
