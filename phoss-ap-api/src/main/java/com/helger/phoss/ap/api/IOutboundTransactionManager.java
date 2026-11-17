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
import com.helger.peppol.sbdh.EPeppolMLSType;
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
   * @param sCustom1
   *        First optional custom field (max 255 characters). May be <code>null</code>.
   * @param sCustom2
   *        Second optional custom field (max 255 characters). May be <code>null</code>.
   * @param sCustom3
   *        Third optional custom field (max 255 characters). May be <code>null</code>.
   * @param eMlsType
   *        Optional MLS_TYPE of this transaction. May be <code>null</code> to use the configured
   *        default at sending time. For a pre-built SBD this is the MLS_TYPE contained in the SBDH.
   *        Since 0.15.0.
   * @param sRequestedAS4MessageID
   *        Optional AS4 message ID requested by the submitter for the first sending attempt. May be
   *        <code>null</code> to use a random one. Since 0.15.0.
   * @return The ID of the created transaction. Only <code>null</code> if insertion fails.
   * @see #newTransactionBuilder()
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
                 @Nullable String sPayloadMimeType,
                 @Nullable String sCustom1,
                 @Nullable String sCustom2,
                 @Nullable String sCustom3,
                 @Nullable EPeppolMLSType eMlsType,
                 @Nullable String sRequestedAS4MessageID);

  /**
   * Create a new builder for an outbound transaction. The builder calls
   * {@link #create(ETransactionType, String, String, String, String, String, ESourceType, String, long, String, String, OffsetDateTime, String, String, String, String, String, String, String, String, String, EPeppolMLSType, String)}
   * of this manager.
   *
   * @return A new builder. Never <code>null</code>.
   * @since 0.15.0
   */
  @NonNull
  default OutboundTransactionBuilder newTransactionBuilder ()
  {
    return new OutboundTransactionBuilder (this);
  }

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
   * Look up a transaction by its unique ID, searching both the active {@code outbound_transaction}
   * table and the {@code outbound_transaction_archive} table. The active table is queried first;
   * the archive table is only consulted if no match is found there.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @return The transaction (from the active table if present, otherwise from the archive), or
   *         <code>null</code> if not found in either.
   * @since 0.14.0
   */
  @Nullable
  IOutboundTransaction getByIDIncludingArchive (@NonNull String sID);

  /**
   * Check if the transaction with the provided ID exists in the DB or not.
   *
   * @param sID
   *        The ID to check. May not be <code>null</code>.
   * @return <code>true</code> if the transaction exists, <code>false</code> if not.
   */
  boolean containsTransactionWithID (@NonNull String sID);

  /**
   * Check if an outbound transaction in the active {@code outbound_transaction} table or in the
   * {@code outbound_transaction_archive} table already requested the provided AS4 message ID.
   *
   * @param sAS4MessageID
   *        The AS4 message ID to check. May not be <code>null</code>.
   * @return <code>true</code> if the AS4 message ID was already requested, <code>false</code> if
   *         not.
   * @since 0.15.0
   */
  boolean containsRequestedAS4MessageIDIncludingArchive (@NonNull String sAS4MessageID);

  /**
   * Look up a transaction by its SBDH Instance Identifier. Searches the active
   * {@code outbound_transaction} table only.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @return The transaction, or <code>null</code> if not found.
   */
  @Nullable
  IOutboundTransaction getBySbdhInstanceID (@NonNull String sSbdhInstanceID);

  /**
   * Look up a transaction by its SBDH Instance Identifier, searching both the active
   * {@code outbound_transaction} table and the {@code outbound_transaction_archive} table. The
   * active table is queried first; the archive table is only consulted if no match is found there.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @return The transaction (from the active table if present, otherwise from the archive), or
   *         <code>null</code> if not found in either.
   * @since 0.9.0
   */
  @Nullable
  IOutboundTransaction getBySbdhInstanceIDIncludingArchive (@NonNull String sSbdhInstanceID);

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
   * Atomically claim a transaction returned by {@link #getAllForRetry(int)}: change its status from
   * {@link EOutboundStatus#FAILED} or {@link EOutboundStatus#RECOVERING} to
   * {@link EOutboundStatus#SENDING}, if it is still in that status and due for a retry.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @return {@link ESuccess#SUCCESS} if this call claimed the transaction.
   * @since 0.14.0
   */
  @NonNull
  ESuccess claimForRetry (@NonNull String sID);

  /**
   * Mark all transactions in {@link EOutboundStatus#PENDING} or {@link EOutboundStatus#SENDING} as
   * {@link EOutboundStatus#RECOVERING}, due for a retry at the provided date time. A transaction
   * that is still being processed leaves that status again with its next regular status update; one
   * that was left behind by an instance that stopped unexpectedly is retried when it is due.
   *
   * @param aRecoveryDueDT
   *        The date time at which the marked transactions become due for a retry. Never
   *        <code>null</code>.
   * @return The number of transactions marked. Always &ge; 0.
   * @since 0.14.0
   */
  @Nonnegative
  long markAllInFlightAsRecovering (@NonNull OffsetDateTime aRecoveryDueDT);

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
   * Update MLS reception status for an outbound business document. Only the first MLS received for
   * a transaction is stored: the update is only performed while the MLS reception status is still
   * {@link EMlsReceptionStatus#PENDING}, because the Peppol specifications only require the first
   * MLS to be considered. The check and the update are a single atomic statement, so that two MLS
   * received at the same time cannot both win.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @param eMlsStatus
   *        The new MLS reception status. Never <code>null</code>.
   * @param aMlsReceivedDT
   *        When the MLS response was received. May be <code>null</code>.
   * @param sMlsID
   *        The MLS message ID. May be <code>null</code>.
   * @param sMlsInboundTransactionID
   *        The ID of the inbound transaction representing the received MLS message. May be
   *        <code>null</code>.
   * @return {@link ESuccess#SUCCESS} if the MLS status was stored, {@link ESuccess#FAILURE} if the
   *         transaction does not exist or if an MLS was already received for it.
   * @since 0.14.0 - only the first MLS is stored
   */
  @NonNull
  ESuccess updateMlsStatus (@NonNull String sID,
                            @NonNull EMlsReceptionStatus eMlsStatus,
                            @Nullable OffsetDateTime aMlsReceivedDT,
                            @Nullable String sMlsID,
                            @Nullable String sMlsInboundTransactionID);

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
   * Get failed and recovering outbound transactions eligible for retry. They are not locked - claim
   * each one with {@link #claimForRetry(String)} before processing it.
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

  /**
   * Get historical transactions with pagination.
   *
   * @param nOffset
   *        Offset to start from. Must be &ge; 0.
   * @param nLimit
   *        Maximum number of transactions to return. Must be &ge; 0.
   * @return The list of transactions. Never <code>null</code>.
   */
  @NonNull
  ICommonsList <IOutboundTransaction> getAllTransactions (@Nonnegative int nOffset, @Nonnegative int nLimit);

  /**
   * @return The total count of active (non-archived) outbound transactions. Must be &ge; 0.
   */
  @Nonnegative
  long getTransactionCount ();
}
