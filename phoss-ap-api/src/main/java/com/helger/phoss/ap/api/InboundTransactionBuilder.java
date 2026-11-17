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

import com.helger.base.enforce.ValueEnforcer;
import com.helger.peppol.sbdh.EPeppolMLSType;

/**
 * Builder for a new inbound transaction. It collects all values and calls
 * {@link IInboundTransactionManager#create(String, String, String, String, String, String, String, String, String, long, String, String, OffsetDateTime, String, String, boolean, boolean, String, EPeppolMLSType)}
 * in {@link #create()}. Get an instance via {@link IInboundTransactionManager#newTransactionBuilder()}.
 * See the <code>create</code> method of the manager for the meaning of the values.
 *
 * @author Philip Helger
 * @since 0.15.0
 */
public class InboundTransactionBuilder
{
  private final IInboundTransactionManager m_aMgr;
  private String m_sIncomingID;
  private String m_sC2SeatID;
  private String m_sC3SeatID;
  private String m_sSigningCertCN;
  private String m_sSenderID;
  private String m_sReceiverID;
  private String m_sDocTypeID;
  private String m_sProcessID;
  private String m_sDocumentPath;
  private long m_nDocumentSize = -1;
  private String m_sDocumentHash;
  private String m_sAS4MessageID;
  private OffsetDateTime m_aAS4Timestamp;
  private String m_sSbdhInstanceID;
  private String m_sC1CountryCode;
  private boolean m_bIsDuplicateAS4;
  private boolean m_bIsDuplicateSBDH;
  private String m_sMlsTo;
  private EPeppolMLSType m_eMlsType;

  /**
   * Constructor.
   *
   * @param aMgr
   *        The manager to create the transaction with. May not be <code>null</code>.
   */
  public InboundTransactionBuilder (@NonNull final IInboundTransactionManager aMgr)
  {
    ValueEnforcer.notNull (aMgr, "Mgr");
    m_aMgr = aMgr;
  }

  /**
   * @param s
   *        The phase4 Incoming ID. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder incomingID (@Nullable final String s)
  {
    m_sIncomingID = s;
    return this;
  }

  /**
   * @param s
   *        Peppol Seat ID of the sending AP (C2). Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder c2SeatID (@Nullable final String s)
  {
    m_sC2SeatID = s;
    return this;
  }

  /**
   * @param s
   *        Peppol Seat ID of the receiving AP (C3). Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder c3SeatID (@Nullable final String s)
  {
    m_sC3SeatID = s;
    return this;
  }

  /**
   * @param s
   *        Subject CN of the signing certificate. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder signingCertCN (@Nullable final String s)
  {
    m_sSigningCertCN = s;
    return this;
  }

  /**
   * @param s
   *        Peppol Participant ID of the sender. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder senderID (@Nullable final String s)
  {
    m_sSenderID = s;
    return this;
  }

  /**
   * @param s
   *        Peppol Participant ID of the receiver. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder receiverID (@Nullable final String s)
  {
    m_sReceiverID = s;
    return this;
  }

  /**
   * @param s
   *        Peppol Document Type Identifier. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder docTypeID (@Nullable final String s)
  {
    m_sDocTypeID = s;
    return this;
  }

  /**
   * @param s
   *        Peppol Process Identifier. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder processID (@Nullable final String s)
  {
    m_sProcessID = s;
    return this;
  }

  /**
   * @param s
   *        Absolute path to the document file on disk. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder documentPath (@Nullable final String s)
  {
    m_sDocumentPath = s;
    return this;
  }

  /**
   * @param n
   *        Size of the document in bytes. Mandatory for {@link #create()} and must be &ge; 0.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder documentSize (final long n)
  {
    m_nDocumentSize = n;
    return this;
  }

  /**
   * @param s
   *        SHA-256 hash of the document. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder documentHash (@Nullable final String s)
  {
    m_sDocumentHash = s;
    return this;
  }

  /**
   * @param s
   *        The AS4 Message ID. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder as4MessageID (@Nullable final String s)
  {
    m_sAS4MessageID = s;
    return this;
  }

  /**
   * @param a
   *        The AS4 MessageInfo/Timestamp (UTC). Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder as4Timestamp (@Nullable final OffsetDateTime a)
  {
    m_aAS4Timestamp = a;
    return this;
  }

  /**
   * @param s
   *        The SBDH Instance Identifier. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder sbdhInstanceID (@Nullable final String s)
  {
    m_sSbdhInstanceID = s;
    return this;
  }

  /**
   * @param s
   *        The C1 country code. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder c1CountryCode (@Nullable final String s)
  {
    m_sC1CountryCode = s;
    return this;
  }

  /**
   * @param b
   *        Whether this is a duplicate on the AS4 Message ID level. Defaults to <code>false</code>.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder duplicateAS4 (final boolean b)
  {
    m_bIsDuplicateAS4 = b;
    return this;
  }

  /**
   * @param b
   *        Whether this is a duplicate on the SBDH Instance Identifier level. Defaults to
   *        <code>false</code>.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder duplicateSBDH (final boolean b)
  {
    m_bIsDuplicateSBDH = b;
    return this;
  }

  /**
   * @param s
   *        Optional MLS_TO target participant ID in the form scheme::value. May be
   *        <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder mlsTo (@Nullable final String s)
  {
    m_sMlsTo = s;
    return this;
  }

  /**
   * @param e
   *        The MLS sending strategy. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public InboundTransactionBuilder mlsType (@Nullable final EPeppolMLSType e)
  {
    m_eMlsType = e;
    return this;
  }

  /**
   * Check all mandatory values and create the inbound transaction.
   *
   * @return The ID of the created transaction. Only <code>null</code> if insertion fails.
   * @throws NullPointerException
   *         If a mandatory value is missing.
   * @throws IllegalArgumentException
   *         If the document size was not set or is negative.
   */
  @Nullable
  public String create ()
  {
    ValueEnforcer.notNull (m_sIncomingID, "IncomingID");
    ValueEnforcer.notNull (m_sC2SeatID, "C2SeatID");
    ValueEnforcer.notNull (m_sC3SeatID, "C3SeatID");
    ValueEnforcer.notNull (m_sSigningCertCN, "SigningCertCN");
    ValueEnforcer.notNull (m_sSenderID, "SenderID");
    ValueEnforcer.notNull (m_sReceiverID, "ReceiverID");
    ValueEnforcer.notNull (m_sDocTypeID, "DocTypeID");
    ValueEnforcer.notNull (m_sProcessID, "ProcessID");
    ValueEnforcer.notNull (m_sDocumentPath, "DocumentPath");
    ValueEnforcer.isGE0 (m_nDocumentSize, "DocumentSize");
    ValueEnforcer.notNull (m_sDocumentHash, "DocumentHash");
    ValueEnforcer.notNull (m_sAS4MessageID, "AS4MessageID");
    ValueEnforcer.notNull (m_aAS4Timestamp, "AS4Timestamp");
    ValueEnforcer.notNull (m_sSbdhInstanceID, "SbdhInstanceID");
    ValueEnforcer.notNull (m_sC1CountryCode, "C1CountryCode");
    ValueEnforcer.notNull (m_eMlsType, "MlsType");

    return m_aMgr.create (m_sIncomingID,
                          m_sC2SeatID,
                          m_sC3SeatID,
                          m_sSigningCertCN,
                          m_sSenderID,
                          m_sReceiverID,
                          m_sDocTypeID,
                          m_sProcessID,
                          m_sDocumentPath,
                          m_nDocumentSize,
                          m_sDocumentHash,
                          m_sAS4MessageID,
                          m_aAS4Timestamp,
                          m_sSbdhInstanceID,
                          m_sC1CountryCode,
                          m_bIsDuplicateAS4,
                          m_bIsDuplicateSBDH,
                          m_sMlsTo,
                          m_eMlsType);
  }
}
