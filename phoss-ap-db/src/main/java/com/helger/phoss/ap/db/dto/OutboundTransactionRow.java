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
package com.helger.phoss.ap.db.dto;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.model.IOutboundTransaction;

/**
 * Simple implementation of {@link IOutboundTransaction} created from JDBC result object.
 *
 * @author Philip Helger
 */
public class OutboundTransactionRow implements IOutboundTransaction
{
  private final String m_sID;
  private final ETransactionType m_eTransactionType;
  private final String m_sSenderID;
  private final String m_sReceiverID;
  private final String m_sDocTypeID;
  private final String m_sProcessID;
  private final String m_sSbdhInstanceID;
  private final ESourceType m_eSourceType;
  private final String m_sDocumentPath;
  private final long m_nDocumentSize;
  private final String m_sDocumentHash;
  private final String m_sC1CountryCode;
  private final EOutboundStatus m_eStatus;
  private final int m_nAttemptCount;
  private final OffsetDateTime m_aCreatedDT;
  private final OffsetDateTime m_aCompletedDT;
  private final EReportingStatus m_eReportingStatus;
  private final OffsetDateTime m_aNextRetryDT;
  private final String m_sErrorDetails;
  private final String m_sMlsTo;
  private final EMlsReceptionStatus m_eMlsStatus;
  private final OffsetDateTime m_aMlsReceivedDT;
  private final String m_sMlsID;
  private final String m_sMlsInboundTransactionID;
  private final String m_sSbdhStandard;
  private final String m_sSbdhTypeVersion;
  private final String m_sSbdhType;
  private final String m_sPayloadMimeType;

  /**
   * Construct an outbound transaction row from a JDBC result row.
   *
   * @param aRow
   *        The database result row to read from. May not be <code>null</code>.
   */
  public OutboundTransactionRow (@NonNull final DBResultRow aRow)
  {
    m_sID = aRow.getAsString (0);
    m_eTransactionType = ETransactionType.getFromIDOrNull (aRow.getAsString (1));
    m_sSenderID = aRow.getAsString (2);
    m_sReceiverID = aRow.getAsString (3);
    m_sDocTypeID = aRow.getAsString (4);
    m_sProcessID = aRow.getAsString (5);
    m_sSbdhInstanceID = aRow.getAsString (6);
    m_eSourceType = ESourceType.getFromIDOrNull (aRow.getAsString (7));
    m_sDocumentPath = aRow.getAsString (8);
    m_nDocumentSize = aRow.getAsLong (9);
    m_sDocumentHash = aRow.getAsString (10);
    m_sC1CountryCode = aRow.getAsString (11);
    m_eStatus = EOutboundStatus.getFromIDOrNull (aRow.getAsString (12));
    m_nAttemptCount = aRow.getAsInt (13);
    m_aCreatedDT = aRow.getAsOffsetDateTime (14);
    m_aCompletedDT = aRow.getAsOffsetDateTime (15);
    m_eReportingStatus = EReportingStatus.getFromIDOrNull (aRow.getAsString (16));
    m_aNextRetryDT = aRow.getAsOffsetDateTime (17);
    m_sErrorDetails = aRow.getAsString (18);
    m_sMlsTo = aRow.getAsString (19);
    m_eMlsStatus = EMlsReceptionStatus.getFromIDOrNull (aRow.getAsString (20));
    m_aMlsReceivedDT = aRow.getAsOffsetDateTime (21);
    m_sMlsID = aRow.getAsString (22);
    m_sMlsInboundTransactionID = aRow.getAsString (23);
    m_sSbdhStandard = aRow.getAsString (24);
    m_sSbdhTypeVersion = aRow.getAsString (25);
    m_sSbdhType = aRow.getAsString (26);
    m_sPayloadMimeType = aRow.getAsString (27);
    ValueEnforcer.notEmpty (m_sID, "ID");
    ValueEnforcer.notNull (m_eTransactionType, "TransactionType");
    ValueEnforcer.notEmpty (m_sSenderID, "SenderID");
    ValueEnforcer.notEmpty (m_sReceiverID, "ReceiverID");
    ValueEnforcer.notEmpty (m_sDocTypeID, "DocTypeID");
    ValueEnforcer.notEmpty (m_sProcessID, "ProcessID");
    ValueEnforcer.notEmpty (m_sSbdhInstanceID, "SbdhInstanceID");
    ValueEnforcer.notNull (m_eSourceType, "SourceType");
    ValueEnforcer.notEmpty (m_sDocumentPath, "DocumentPath");
    ValueEnforcer.isGE0 (m_nDocumentSize, "DocumentSize");
    ValueEnforcer.notEmpty (m_sDocumentHash, "DocumentHash");
    ValueEnforcer.notEmpty (m_sC1CountryCode, "C1CountryCode");
    ValueEnforcer.notNull (m_eStatus, "Status");
    ValueEnforcer.isGE0 (m_nAttemptCount, "AttemptCount");
    ValueEnforcer.notNull (m_aCreatedDT, "CreatedDT");
    ValueEnforcer.notNull (m_eReportingStatus, "ReportingStatus");
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /** {@inheritDoc} */
  @NonNull
  public ETransactionType getTransactionType ()
  {
    return m_eTransactionType;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getSenderID ()
  {
    return m_sSenderID;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getReceiverID ()
  {
    return m_sReceiverID;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getDocTypeID ()
  {
    return m_sDocTypeID;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getProcessID ()
  {
    return m_sProcessID;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getSbdhInstanceID ()
  {
    return m_sSbdhInstanceID;
  }

  /** {@inheritDoc} */
  @NonNull
  public ESourceType getSourceType ()
  {
    return m_eSourceType;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getDocumentPath ()
  {
    return m_sDocumentPath;
  }

  /** {@inheritDoc} */
  @Nonnegative
  public long getDocumentSize ()
  {
    return m_nDocumentSize;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getDocumentHash ()
  {
    return m_sDocumentHash;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getC1CountryCode ()
  {
    return m_sC1CountryCode;
  }

  /** {@inheritDoc} */
  @NonNull
  public EOutboundStatus getStatus ()
  {
    return m_eStatus;
  }

  /** {@inheritDoc} */
  @Nonnegative
  public int getAttemptCount ()
  {
    return m_nAttemptCount;
  }

  /** {@inheritDoc} */
  @NonNull
  public OffsetDateTime getCreatedDT ()
  {
    return m_aCreatedDT;
  }

  /** {@inheritDoc} */
  @Nullable
  public OffsetDateTime getCompletedDT ()
  {
    return m_aCompletedDT;
  }

  /** {@inheritDoc} */
  @NonNull
  public EReportingStatus getReportingStatus ()
  {
    return m_eReportingStatus;
  }

  /** {@inheritDoc} */
  @Nullable
  public OffsetDateTime getNextRetryDT ()
  {
    return m_aNextRetryDT;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getErrorDetails ()
  {
    return m_sErrorDetails;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getMlsTo ()
  {
    return m_sMlsTo;
  }

  /** {@inheritDoc} */
  @Nullable
  public EMlsReceptionStatus getMlsStatus ()
  {
    return m_eMlsStatus;
  }

  /** {@inheritDoc} */
  @Nullable
  public OffsetDateTime getMlsReceivedDT ()
  {
    return m_aMlsReceivedDT;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getMlsID ()
  {
    return m_sMlsID;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getMlsInboundTransactionID ()
  {
    return m_sMlsInboundTransactionID;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getSbdhStandard ()
  {
    return m_sSbdhStandard;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getSbdhTypeVersion ()
  {
    return m_sSbdhTypeVersion;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getSbdhType ()
  {
    return m_sSbdhType;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getPayloadMimeType ()
  {
    return m_sPayloadMimeType;
  }
}
