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
import com.helger.annotation.Nonnegative;
import com.helger.base.id.IHasID;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;

/**
 * Read-only view of an inbound transaction. Inbound transactions represent
 * documents received at this AP (C3) from a remote AP (C2), stored in the
 * database and forwarded to the Receiver Backend (C4).
 *
 * @author Philip Helger
 */
public interface IInboundTransaction extends IHasID <String>
{
  /**
   * @return The unique transaction ID. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getID ();

  /**
   * @return The phase4 Incoming ID. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getIncomingID ();

  /**
   * @return The Peppol Seat ID of the sending AP (C2). Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getC2SeatID ();

  /**
   * @return The Peppol Seat ID of the receiving AP (C3). Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getC3SeatID ();

  /**
   * @return The Subject CN of the signing certificate. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getSigningCertCN ();

  /**
   * @return The Peppol Participant ID of the sender (C1). Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getSenderID ();

  /**
   * @return The Peppol Participant ID of the receiver (C4). Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getReceiverID ();

  /**
   * @return The Peppol Document Type Identifier. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getDocTypeID ();

  /**
   * @return The Peppol Process Identifier. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getProcessID ();

  /**
   * @return The absolute path to the document file on disk. Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getDocumentPath ();

  /**
   * @return The size of the document in bytes.
   */
  @Nonnegative
  long getDocumentSize ();

  /**
   * @return The SHA-256 hash of the document payload. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getDocumentHash ();

  /**
   * @return The AS4 Message ID from the inbound message. Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getAS4MessageID ();

  /**
   * @return The AS4 MessageInfo/Timestamp from the incoming AS4 message (UTC).
   *         Never <code>null</code>.
   */
  @NonNull
  OffsetDateTime getAS4Timestamp ();

  /**
   * @return The SBDH Instance Identifier. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getSbdhInstanceID ();

  /**
   * @return The C1 country code, or <code>null</code> if not yet set.
   */
  @Nullable
  String getC1CountryCode ();

  /**
   * @return The C4 country code, or <code>null</code> if not yet reported.
   */
  @Nullable
  String getC4CountryCode ();

  /**
   * @return <code>true</code> if this message was detected as a duplicate on
   *         the AS4 Message ID level.
   */
  boolean isDuplicateAS4 ();

  /**
   * @return <code>true</code> if this message was detected as a duplicate on
   *         the SBDH Instance Identifier level.
   */
  boolean isDuplicateSBDH ();

  /**
   * @return The current lifecycle status. Never <code>null</code>.
   */
  @NonNull
  EInboundStatus getStatus ();

  /**
   * @return The total number of forwarding attempts so far.
   */
  @Nonnegative
  int getAttemptCount ();

  /**
   * @return When the message was received. Never <code>null</code>.
   */
  @NonNull
  OffsetDateTime getReceivedDT ();

  /**
   * @return When the transaction was completed, or <code>null</code> if not yet
   *         completed.
   */
  @Nullable
  OffsetDateTime getCompletedDT ();

  /**
   * @return The Peppol Reporting status. Never <code>null</code>.
   */
  @NonNull
  EReportingStatus getReportingStatus ();

  /**
   * @return The planned date/time of the next forwarding retry, or
   *         <code>null</code> if not scheduled.
   */
  @Nullable
  OffsetDateTime getNextRetryDT ();

  /**
   * @return The error details from the last failed attempt, or
   *         <code>null</code> on success.
   */
  @Nullable
  String getErrorDetails ();

  /**
   * @return The MLS_TO target participant ID (from SBDH extension), or
   *         <code>null</code> if not set. Only present if the MLS_TO was valid
   *         and according to the rules.
   */
  @Nullable
  String getMlsTo ();

  /**
   * @return The MLS sending strategy captured at reception time. Never
   *         <code>null</code>.
   */
  @NonNull
  EPeppolMLSType getMlsType ();

  /**
   * @return The MLS response code sent or to be sent, or <code>null</code> if
   *         not yet determined.
   */
  @Nullable
  EPeppolMLSResponseCode getMlsResponseCode ();

  /**
   * @return The ID of the outbound transaction representing the MLS sending, or
   *         <code>null</code> if not yet created.
   */
  @Nullable
  String getMlsOutboundTransactionID ();
}
