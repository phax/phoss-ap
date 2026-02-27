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
import com.helger.annotation.Nonnegative;
import com.helger.base.id.IHasID;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;

/**
 * Read-only view of an outbound transaction. Outbound transactions represent documents sent from
 * this AP (C2) to a remote AP (C3), including both regular business documents and MLS responses.
 *
 * @author Philip Helger
 */
public interface IOutboundTransaction extends IHasID <String>
{
  /**
   * @return The unique transaction ID. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getID ();

  /**
   * @return The transaction type (business document or MLS response). Never <code>null</code>.
   */
  @NonNull
  ETransactionType getTransactionType ();

  /**
   * @return The Peppol Participant ID of the sender (C1). Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getSenderID ();

  /**
   * @return The Peppol Participant ID of the receiver (C4). Never <code>null</code>.
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
   * @return The SBDH Instance Identifier. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getSbdhInstanceID ();

  /**
   * @return The source type indicating how the document was submitted (raw XML or pre-built SBD).
   *         Never <code>null</code>.
   */
  @NonNull
  ESourceType getSourceType ();

  /**
   * @return The raw document bytes. Never <code>null</code>.
   */
  byte @NonNull [] getDocumentBytes ();

  /**
   * @return The size of the document in bytes. Must be &ge; 0.
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
   * @return The country code of the sender (C1). Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getC1CountryCode ();

  /**
   * @return The current lifecycle status. Never <code>null</code>.
   */
  @NonNull
  EOutboundStatus getStatus ();

  /**
   * @return The total number of sending attempts so far. Always ge; 0.
   */
  @Nonnegative
  int getAttemptCount ();

  /**
   * @return When the transaction was created. Never <code>null</code>.
   */
  @NonNull
  OffsetDateTime getCreatedDT ();

  /**
   * @return When the transaction was completed, or <code>null</code> if not yet completed.
   */
  @Nullable
  OffsetDateTime getCompletedDT ();

  /**
   * @return The Peppol Reporting status. Never <code>null</code>.
   */
  @NonNull
  EReportingStatus getReportingStatus ();

  /**
   * @return The planned date/time of the next sending retry, or <code>null</code> if not scheduled.
   */
  @Nullable
  OffsetDateTime getNextRetryDT ();

  /**
   * @return The error details from the last failed attempt, or <code>null</code> on success.
   */
  @Nullable
  String getErrorDetails ();

  /**
   * @return The MLS_TO override participant ID, or <code>null</code> if not set.
   */
  @Nullable
  String getMlsTo ();

  /**
   * @return The MLS response reception status, or <code>null</code> if not applicable.
   */
  @Nullable
  EMlsReceptionStatus getMlsStatus ();

  /**
   * @return When the MLS response was received, or <code>null</code> if not yet received.
   */
  @Nullable
  OffsetDateTime getMlsReceivedDT ();

  /**
   * @return The MLS message ID, or <code>null</code> if not yet received.
   */
  @Nullable
  String getMlsID ();

  /**
   * @return The ID of the inbound transaction that triggered this MLS response, or
   *         <code>null</code> if this is a business document.
   */
  @Nullable
  String getMlsInboundTransactionID ();
}
