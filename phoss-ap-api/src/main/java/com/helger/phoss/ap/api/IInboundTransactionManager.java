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
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.model.IInboundTransaction;

/**
 * Manager interface for creating and querying inbound transactions.
 *
 * @author Philip Helger
 */
public interface IInboundTransactionManager
{
  /**
   * Create a new inbound transaction in state 'pending'.
   *
   * @param sIncomingID
   *        The phase4 Incoming ID. Never <code>null</code>.
   * @param sC2SeatID
   *        Peppol Seat ID of the sending AP (C2). Never <code>null</code>.
   * @param sC3SeatID
   *        Peppol Seat ID of the receiving AP (C3). Never <code>null</code>.
   * @param sSigningCertCN
   *        Subject CN of the signing certificate. Never <code>null</code>.
   * @param sSenderID
   *        Peppol Participant ID of the sender. Never <code>null</code>.
   * @param sReceiverID
   *        Peppol Participant ID of the receiver. Never <code>null</code>.
   * @param sDocTypeID
   *        Peppol Document Type Identifier. Never <code>null</code>.
   * @param sProcessID
   *        Peppol Process Identifier. Never <code>null</code>.
   * @param sDocumentPath
   *        Absolute path to the document file on disk. Never <code>null</code>.
   * @param nDocumentSize
   *        Size of the document in bytes. Must be &ge; 0.
   * @param sDocumentHash
   *        SHA-256 hash of the document. Never <code>null</code>.
   * @param sAS4MessageID
   *        The AS4 Message ID. Never <code>null</code>.
   * @param aAS4Timestamp
   *        The AS4 MessageInfo/Timestamp (UTC). Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param sC1CountryCode
   *        The C1 country code. Never <code>null</code>.
   * @param bIsDuplicateAS4
   *        Whether this is a duplicate on the AS4 Message ID level.
   * @param bIsDuplicateSBDH
   *        Whether this is a duplicate on the SBDH Instance Identifier level.
   * @param sMlsTo
   *        Optional MLS_TO target participant ID. In the form scheme::value. May be
   *        <code>null</code>.
   * @param eMlsType
   *        The MLS sending strategy. Never <code>null</code>.
   * @return The ID of the created transaction. Only <code>null</code> if insertion fails.
   */
  @Nullable
  String create (@NonNull String sIncomingID,
                 @NonNull String sC2SeatID,
                 @NonNull String sC3SeatID,
                 @NonNull String sSigningCertCN,
                 @NonNull String sSenderID,
                 @NonNull String sReceiverID,
                 @NonNull String sDocTypeID,
                 @NonNull String sProcessID,
                 @NonNull String sDocumentPath,
                 @Nonnegative long nDocumentSize,
                 @NonNull String sDocumentHash,
                 @NonNull String sAS4MessageID,
                 @NonNull OffsetDateTime aAS4Timestamp,
                 @NonNull String sSbdhInstanceID,
                 @NonNull String sC1CountryCode,
                 boolean bIsDuplicateAS4,
                 boolean bIsDuplicateSBDH,
                 @Nullable String sMlsTo,
                 @NonNull EPeppolMLSType eMlsType);

  /**
   * Look up a transaction by its unique ID.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @return The transaction, or <code>null</code> if not found.
   */
  @Nullable
  IInboundTransaction getByID (@NonNull String sID);

  /**
   * Check if the transaction with the provided ID exists in the DB or not.
   *
   * @param sID
   *        The ID to check. May not be <code>null</code>.
   * @return <code>true</code> if the transaction exists, <code>false</code> if not.
   */
  boolean containsTransactionWithID (@NonNull String sID);

  /**
   * Check if a transaction with the provided AS4 Message ID exists.
   *
   * @param sAS4MessageID
   *        The AS4 Message ID. Never <code>null</code>.
   * @return <code>true</code> if found, <code>false</code> otherwise.
   */
  boolean containsByAS4MessageID (@NonNull String sAS4MessageID);

  /**
   * Look up a transaction by its AS4 Message ID.
   *
   * @param sAS4MessageID
   *        The AS4 Message ID. Never <code>null</code>.
   * @return The transaction, or <code>null</code> if not found.
   */
  @Nullable
  IInboundTransaction getByAS4MessageID (@NonNull String sAS4MessageID);

  /**
   * Check if a transaction with the provided SBDH Instance Identifier exists.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @return <code>true</code> if found, <code>false</code> otherwise.
   */
  boolean containsBySbdhInstanceID (@NonNull String sSbdhInstanceID);

  /**
   * Look up a transaction by its SBDH Instance Identifier.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @return The transaction, or <code>null</code> if not found.
   */
  @Nullable
  IInboundTransaction getBySbdhInstanceID (@NonNull String sSbdhInstanceID);

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
  ESuccess updateStatus (@NonNull String sID, @NonNull EInboundStatus eStatus);

  /**
   * Update the status after a failed attempt with retry information.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @param eStatus
   *        The new status. Never <code>null</code>.
   * @param nAttemptCount
   *        The updated attempt count. Must be &ge; 0.
   * @param aNextRetryDT
   *        The next retry date/time. May be <code>null</code> if we're on permanently failed.
   * @param sErrorDetails
   *        Error details. May be <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  ESuccess updateStatusAndRetry (@NonNull String sID,
                                 @NonNull EInboundStatus eStatus,
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
  ESuccess updateStatusCompleted (@NonNull String sID, @NonNull EInboundStatus eStatus);

  /**
   * Update the C4 country code for reporting.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @param sC4CountryCode
   *        The C4 country code. Never <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  ESuccess updateC4CountryCode (@NonNull String sID, @NonNull String sC4CountryCode);

  /**
   * Update MLS-related fields after an MLS response has been determined or sent.
   *
   * @param sID
   *        The transaction ID. Never <code>null</code>.
   * @param eMlsResponseCode
   *        The MLS response code. May be <code>null</code>.
   * @param sMlsOutboundTransactionID
   *        The ID of the MLS outbound transaction. May be <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  ESuccess updateMlsFields (@NonNull String sID,
                            @Nullable EPeppolMLSResponseCode eMlsResponseCode,
                            @Nullable String sMlsOutboundTransactionID);

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
   * @return All inbound transactions that are not yet in a final state. Never <code>null</code>.
   */
  @NonNull
  ICommonsList <IInboundTransaction> getAllInProcessing ();

  /**
   * Get failed inbound transactions eligible for retry.
   *
   * @param nBatchSize
   *        Maximum number of transactions to return. Must be &gt; 0.
   * @return The list of transactions. Never <code>null</code>.
   */
  @NonNull
  ICommonsList <IInboundTransaction> getAllForRetry (@Nonnegative int nBatchSize);

  /**
   * Get completed inbound transactions eligible for archival.
   *
   * @param nBatchSize
   *        Maximum number of transactions to return. Must be &gt; 0.
   * @return The list of transactions. Never <code>null</code>.
   */
  @NonNull
  ICommonsList <IInboundTransaction> getAllForArchival (@Nonnegative int nBatchSize);

  /**
   * Get all inbound business document transactions (excluding MLS messages) for which no MLS
   * response has been sent yet (i.e. {@code mls_response_code IS NULL}).
   *
   * @return The list of transactions without an MLS response. Never <code>null</code>.
   */
  @NonNull
  ICommonsList <IInboundTransaction> getAllWithoutMlsResponse ();
}
