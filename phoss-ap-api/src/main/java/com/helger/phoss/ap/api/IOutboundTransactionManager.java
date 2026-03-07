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

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.model.IOutboundTransaction;

/**
 * Manager interface for creating and querying outbound transactions.
 *
 * @author Philip Helger
 */
public interface IOutboundTransactionManager
{
  /**
   * Create a new outbound transaction.
   *
   * @param eTransactionType
   *        The transaction type. Never <code>null</code>.
   * @param sSenderID
   *        Peppol Participant ID of the sender. Never <code>null</code>.
   * @param sReceiverID
   *        Peppol Participant ID of the receiver. Never <code>null</code>.
   * @param sDocTypeID
   *        Peppol Document Type Identifier. Never <code>null</code>.
   * @param sProcessID
   *        Peppol Process Identifier. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        SBDH Instance Identifier. Never <code>null</code>.
   * @param eSourceType
   *        Source type indicating how the document was submitted (raw XML or pre-built SBD). Never
   *        <code>null</code>.
   * @param sDocumentPath
   *        Absolute path to the document file on disk. Never <code>null</code>.
   * @param nDocumentSize
   *        Size of the document in bytes.
   * @param sDocumentHash
   *        SHA-256 hash of the document. Never <code>null</code>.
   * @param sC1CountryCode
   *        Country code of the sender (C1). Never <code>null</code>.
   * @param aCreationTD
   *        The date and time when the message was created. Never <code>null</code>.
   * @param sMlsTo
   *        Optional MLS_TO override. May be <code>null</code>.
   * @param sMlsInboundTransactionID
   *        ID of the inbound transaction for MLS responses. May be <code>null</code>.
   * @param sSbdhStandard
   *        SBDH Standard override for non-XML payloads (e.g. {@code urn:peppol:doctype:pdf+xml}).
   *        May be <code>null</code> to auto-derive from document type.
   * @param sSbdhTypeVersion
   *        SBDH TypeVersion override for non-XML payloads (e.g. {@code 0}). May be
   *        <code>null</code> to auto-derive from document type.
   * @param sSbdhType
   *        SBDH Type override for non-XML payloads (e.g. {@code factur-x}). May be
   *        <code>null</code> to auto-derive from document type.
   * @param sPayloadMimeType
   *        MIME type for binary payloads (e.g. {@code application/pdf}). May be <code>null</code>
   *        for XML payloads.
   * @return The ID of the created transaction. Only <code>null</code> if insertion fails.
   */
  @Nullable
  String create (@NonNull ETransactionType eTransactionType,
                 @NonNull String sSenderID,
                 @NonNull String sReceiverID,
                 @NonNull String sDocTypeID,
                 @NonNull String sProcessID,
                 @NonNull String sSbdhInstanceID,
                 @NonNull ESourceType eSourceType,
                 @NonNull String sDocumentPath,
                 @Nonnegative long nDocumentSize,
                 @NonNull String sDocumentHash,
                 @NonNull String sC1CountryCode,
                 @NonNull OffsetDateTime aCreationTD,
                 @Nullable String sMlsTo,
                 @Nullable String sMlsInboundTransactionID,
                 @Nullable String sSbdhStandard,
                 @Nullable String sSbdhTypeVersion,
                 @Nullable String sSbdhType,
                 @Nullable String sPayloadMimeType);

  /**
   * Look up a transaction by its unique ID.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @return The transaction, or <code>null</code> if not found.
   */
  @Nullable
  IOutboundTransaction getByID (@NonNull String sID);

  /**
   * Look up a transaction by its SBDH Instance Identifier.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @return The transaction, or <code>null</code> if not found.
   */
  @Nullable
  IOutboundTransaction getBySbdhInstanceID (@NonNull String sSbdhInstanceID);

  /**
   * Update the status of a transaction.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @param eStatus
   *        The new status. Never <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  ESuccess updateStatus (@NonNull String sID, @NonNull EOutboundStatus eStatus);

  /**
   * Update the status after a failed attempt with retry information.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @param eStatus
   *        The new status. Never <code>null</code>.
   * @param nAttemptCount
   *        The updated attempt count.
   * @param aNextRetryDT
   *        The next retry date/time. May be <code>null</code>.
   * @param sErrorDetails
   *        Error details. May be <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  ESuccess updateStatusAndRetry (@NonNull String sID,
                                 @NonNull EOutboundStatus eStatus,
                                 @Nonnegative int nAttemptCount,
                                 @Nullable OffsetDateTime aNextRetryDT,
                                 @Nullable String sErrorDetails);

  /**
   * Mark a transaction as completed.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @param eStatus
   *        The final status. Never <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  ESuccess updateStatusCompleted (@NonNull String sID, @NonNull EOutboundStatus eStatus);

  /**
   * Update MLS reception status for an outbound business document.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @param eMlsStatus
   *        The new MLS reception status. Never <code>null</code>.
   * @param aMlsReceivedDT
   *        When the MLS response was received. May be <code>null</code>.
   * @param sMlsID
   *        The MLS message ID. May be <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  ESuccess updateMlsStatus (@NonNull String sID,
                            @NonNull EMlsReceptionStatus eMlsStatus,
                            @Nullable OffsetDateTime aMlsReceivedDT,
                            @Nullable String sMlsID);

  /**
   * Update the reporting status for a transaction.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @param eReportingStatus
   *        The new reporting status. Never <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  ESuccess updateReportingStatus (@NonNull String sID, @NonNull EReportingStatus eReportingStatus);

  /**
   * @return All outbound transactions that are not yet in a final state. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <IOutboundTransaction> getAllInTransmission ();

  /**
   * Get failed outbound transactions eligible for retry.
   *
   * @param nBatchSize
   *        Maximum number of transactions to return.
   * @return The list of transactions. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <IOutboundTransaction> getAllForRetry (@Nonnegative int nBatchSize);

  /**
   * Get completed outbound transactions eligible for archival.
   *
   * @param nBatchSize
   *        Maximum number of transactions to return.
   * @return The list of transactions. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <IOutboundTransaction> getAllForArchival (@Nonnegative int nBatchSize);
}
