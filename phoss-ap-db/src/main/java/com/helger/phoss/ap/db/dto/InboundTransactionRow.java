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
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.model.IInboundTransaction;

/**
 * Simple implementation of {@link IInboundTransaction} created from JDBC result
 * object.
 *
 * @author Philip Helger
 */
public class InboundTransactionRow implements IInboundTransaction
{
  private final String m_sID;
  private final String m_sIncomingID;
  private final String m_sC2SeatID;
  private final String m_sC3SeatID;
  private final String m_sSigningCertCN;
  private final String m_sSenderID;
  private final String m_sReceiverID;
  private final String m_sDocTypeID;
  private final String m_sProcessID;
  private final String m_sDocumentPath;
  private final long m_nDocumentSize;
  private final String m_sDocumentHash;
  private final String m_sAS4MessageID;
  private final OffsetDateTime m_aAS4Timestamp;
  private final String m_sSbdhInstanceID;
  private final String m_sC1CountryCode;
  private final String m_sC4CountryCode;
  private final boolean m_bIsDuplicateAS4;
  private final boolean m_bIsDuplicateSBDH;
  private final EInboundStatus m_eStatus;
  private final int m_nAttemptCount;
  private final OffsetDateTime m_aReceivedDT;
  private final OffsetDateTime m_aCompletedDT;
  private final EReportingStatus m_eReportingStatus;
  private final OffsetDateTime m_aNextRetryDT;
  private final String m_sErrorDetails;
  private final String m_sMlsTo;
  private final EPeppolMLSType m_eMlsType;
  private final EPeppolMLSResponseCode m_eMlsResponseCode;
  private final String m_sMlsOutboundTransactionID;

  /**
   * Construct an inbound transaction row from a JDBC result row.
   *
   * @param aRow
   *        The database result row to read from. May not be <code>null</code>.
   */
  public InboundTransactionRow (@NonNull final DBResultRow aRow)
  {
    m_sID = aRow.getAsString (0);
    m_sIncomingID = aRow.getAsString (1);
    m_sC2SeatID = aRow.getAsString (2);
    m_sC3SeatID = aRow.getAsString (3);
    m_sSigningCertCN = aRow.getAsString (4);
    m_sSenderID = aRow.getAsString (5);
    m_sReceiverID = aRow.getAsString (6);
    m_sDocTypeID = aRow.getAsString (7);
    m_sProcessID = aRow.getAsString (8);
    m_sDocumentPath = aRow.getAsString (9);
    m_nDocumentSize = aRow.getAsLong (10);
    m_sDocumentHash = aRow.getAsString (11);
    m_sAS4MessageID = aRow.getAsString (12);
    m_aAS4Timestamp = aRow.getAsOffsetDateTime (13);
    m_sSbdhInstanceID = aRow.getAsString (14);
    m_sC1CountryCode = aRow.getAsString (15);
    m_sC4CountryCode = aRow.getAsString (16);
    m_bIsDuplicateAS4 = aRow.getAsBoolean (17);
    m_bIsDuplicateSBDH = aRow.getAsBoolean (18);
    m_eStatus = EInboundStatus.getFromIDOrNull (aRow.getAsString (19));
    m_nAttemptCount = aRow.getAsInt (20);
    m_aReceivedDT = aRow.getAsOffsetDateTime (21);
    m_aCompletedDT = aRow.getAsOffsetDateTime (22);
    m_eReportingStatus = EReportingStatus.getFromIDOrNull (aRow.getAsString (23));
    m_aNextRetryDT = aRow.getAsOffsetDateTime (24);
    m_sErrorDetails = aRow.getAsString (25);
    m_sMlsTo = aRow.getAsString (26);
    m_eMlsType = EPeppolMLSType.getFromIDOrNull (aRow.getAsString (27));
    m_eMlsResponseCode = EPeppolMLSResponseCode.getFromIDOrNull (aRow.getAsString (28));
    m_sMlsOutboundTransactionID = aRow.getAsString (29);
    ValueEnforcer.notEmpty (m_sID, "ID");
    ValueEnforcer.notEmpty (m_sIncomingID, "IncomingID");
    ValueEnforcer.notEmpty (m_sC2SeatID, "C2SeatID");
    ValueEnforcer.notEmpty (m_sC3SeatID, "C3SeatID");
    ValueEnforcer.notEmpty (m_sSigningCertCN, "SigningCertCN");
    ValueEnforcer.notEmpty (m_sSenderID, "SenderID");
    ValueEnforcer.notEmpty (m_sReceiverID, "ReceiverID");
    ValueEnforcer.notEmpty (m_sDocTypeID, "DocTypeID");
    ValueEnforcer.notEmpty (m_sProcessID, "ProcessID");
    ValueEnforcer.notEmpty (m_sDocumentPath, "DocumentPath");
    ValueEnforcer.isGE0 (m_nDocumentSize, "DocumentSize");
    ValueEnforcer.notEmpty (m_sDocumentHash, "DocumentHash");
    ValueEnforcer.notEmpty (m_sAS4MessageID, "AS4MessageID");
    ValueEnforcer.notNull (m_aAS4Timestamp, "AS4Timestamp");
    ValueEnforcer.notEmpty (m_sSbdhInstanceID, "SbdhInstanceID");
    ValueEnforcer.notNull (m_eStatus, "Status");
    ValueEnforcer.isGE0 (m_nAttemptCount, "AttemptCount");
    ValueEnforcer.notNull (m_aReceivedDT, "ReceivedDT");
    ValueEnforcer.notNull (m_eReportingStatus, "ReportingStatus");
    ValueEnforcer.notNull (m_eMlsType, "MlsType");
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
  @Nonempty
  public String getIncomingID ()
  {
    return m_sIncomingID;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getC2SeatID ()
  {
    return m_sC2SeatID;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getC3SeatID ()
  {
    return m_sC3SeatID;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getSigningCertCN ()
  {
    return m_sSigningCertCN;
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
  public String getAS4MessageID ()
  {
    return m_sAS4MessageID;
  }

  /** {@inheritDoc} */
  @NonNull
  public OffsetDateTime getAS4Timestamp ()
  {
    return m_aAS4Timestamp;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getSbdhInstanceID ()
  {
    return m_sSbdhInstanceID;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getC1CountryCode ()
  {
    return m_sC1CountryCode;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getC4CountryCode ()
  {
    return m_sC4CountryCode;
  }

  /** {@inheritDoc} */
  public boolean isDuplicateAS4 ()
  {
    return m_bIsDuplicateAS4;
  }

  /** {@inheritDoc} */
  public boolean isDuplicateSBDH ()
  {
    return m_bIsDuplicateSBDH;
  }

  /** {@inheritDoc} */
  @NonNull
  public EInboundStatus getStatus ()
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
  public OffsetDateTime getReceivedDT ()
  {
    return m_aReceivedDT;
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
  @NonNull
  public EPeppolMLSType getMlsType ()
  {
    return m_eMlsType;
  }

  /** {@inheritDoc} */
  @Nullable
  public EPeppolMLSResponseCode getMlsResponseCode ()
  {
    return m_eMlsResponseCode;
  }

  /** {@inheritDoc} */
  @Nullable
  public String getMlsOutboundTransactionID ()
  {
    return m_sMlsOutboundTransactionID;
  }
}
