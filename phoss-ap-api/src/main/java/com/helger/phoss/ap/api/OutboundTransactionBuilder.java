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
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;

/**
 * Builder for a new outbound transaction. It collects all values and calls
 * {@link IOutboundTransactionManager#create(ETransactionType, String, String, String, String, String, ESourceType, String, long, String, String, OffsetDateTime, String, String, String, String, String, String, String, String, String, EPeppolMLSType, String)}
 * in {@link #create()}. Get an instance via {@link IOutboundTransactionManager#newTransactionBuilder()}.
 * See the <code>create</code> method of the manager for the meaning of the values.
 *
 * @author Philip Helger
 * @since 0.15.0
 */
public class OutboundTransactionBuilder
{
  private final IOutboundTransactionManager m_aMgr;
  private ETransactionType m_eTransactionType;
  private String m_sSenderID;
  private String m_sReceiverID;
  private String m_sDocTypeID;
  private String m_sProcessID;
  private String m_sSbdhInstanceID;
  private ESourceType m_eSourceType;
  private String m_sDocumentPath;
  private long m_nDocumentSize = -1;
  private String m_sDocumentHash;
  private String m_sC1CountryCode;
  private OffsetDateTime m_aCreationDT;
  private String m_sMlsTo;
  private String m_sMlsInboundTransactionID;
  private String m_sSbdhStandard;
  private String m_sSbdhTypeVersion;
  private String m_sSbdhType;
  private String m_sPayloadMimeType;
  private String m_sCustom1;
  private String m_sCustom2;
  private String m_sCustom3;
  private EPeppolMLSType m_eMlsType;
  private String m_sRequestedAS4MessageID;

  /**
   * Constructor.
   *
   * @param aMgr
   *        The manager to create the transaction with. May not be <code>null</code>.
   */
  public OutboundTransactionBuilder (@NonNull final IOutboundTransactionManager aMgr)
  {
    ValueEnforcer.notNull (aMgr, "Mgr");
    m_aMgr = aMgr;
  }

  /**
   * @param e
   *        The transaction type. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder transactionType (@Nullable final ETransactionType e)
  {
    m_eTransactionType = e;
    return this;
  }

  /**
   * @param s
   *        Peppol Participant ID of the sender. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder senderID (@Nullable final String s)
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
  public OutboundTransactionBuilder receiverID (@Nullable final String s)
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
  public OutboundTransactionBuilder docTypeID (@Nullable final String s)
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
  public OutboundTransactionBuilder processID (@Nullable final String s)
  {
    m_sProcessID = s;
    return this;
  }

  /**
   * @param s
   *        SBDH Instance Identifier. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder sbdhInstanceID (@Nullable final String s)
  {
    m_sSbdhInstanceID = s;
    return this;
  }

  /**
   * @param e
   *        Source type indicating how the document was submitted. Mandatory for
   *        {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder sourceType (@Nullable final ESourceType e)
  {
    m_eSourceType = e;
    return this;
  }

  /**
   * @param s
   *        Absolute path to the document file on disk. Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder documentPath (@Nullable final String s)
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
  public OutboundTransactionBuilder documentSize (final long n)
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
  public OutboundTransactionBuilder documentHash (@Nullable final String s)
  {
    m_sDocumentHash = s;
    return this;
  }

  /**
   * @param s
   *        Country code of the sender (C1). Mandatory for {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder c1CountryCode (@Nullable final String s)
  {
    m_sC1CountryCode = s;
    return this;
  }

  /**
   * @param a
   *        The date and time when the message was created. Mandatory for
   *        {@link #create()}.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder creationDT (@Nullable final OffsetDateTime a)
  {
    m_aCreationDT = a;
    return this;
  }

  /**
   * @param s
   *        Optional MLS_TO override. May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder mlsTo (@Nullable final String s)
  {
    m_sMlsTo = s;
    return this;
  }

  /**
   * @param s
   *        ID of the inbound transaction for MLS responses. May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder mlsInboundTransactionID (@Nullable final String s)
  {
    m_sMlsInboundTransactionID = s;
    return this;
  }

  /**
   * @param s
   *        SBDH Standard override for non-XML payloads. May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder sbdhStandard (@Nullable final String s)
  {
    m_sSbdhStandard = s;
    return this;
  }

  /**
   * @param s
   *        SBDH TypeVersion override for non-XML payloads. May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder sbdhTypeVersion (@Nullable final String s)
  {
    m_sSbdhTypeVersion = s;
    return this;
  }

  /**
   * @param s
   *        SBDH Type override for non-XML payloads. May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder sbdhType (@Nullable final String s)
  {
    m_sSbdhType = s;
    return this;
  }

  /**
   * @param s
   *        MIME type for binary payloads. May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder payloadMimeType (@Nullable final String s)
  {
    m_sPayloadMimeType = s;
    return this;
  }

  /**
   * @param s
   *        First optional custom field (max 255 characters). May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder custom1 (@Nullable final String s)
  {
    m_sCustom1 = s;
    return this;
  }

  /**
   * @param s
   *        Second optional custom field (max 255 characters). May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder custom2 (@Nullable final String s)
  {
    m_sCustom2 = s;
    return this;
  }

  /**
   * @param s
   *        Third optional custom field (max 255 characters). May be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder custom3 (@Nullable final String s)
  {
    m_sCustom3 = s;
    return this;
  }

  /**
   * @param e
   *        Optional MLS_TYPE. May be <code>null</code> to use the configured default at sending
   *        time.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder mlsType (@Nullable final EPeppolMLSType e)
  {
    m_eMlsType = e;
    return this;
  }

  /**
   * @param s
   *        Optional AS4 message ID for the first sending attempt. May be <code>null</code> to use a
   *        random one.
   * @return this for chaining
   */
  @NonNull
  public OutboundTransactionBuilder requestedAS4MessageID (@Nullable final String s)
  {
    m_sRequestedAS4MessageID = s;
    return this;
  }

  /**
   * Check all mandatory values and create the outbound transaction.
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
    ValueEnforcer.notNull (m_eTransactionType, "TransactionType");
    ValueEnforcer.notNull (m_sSenderID, "SenderID");
    ValueEnforcer.notNull (m_sReceiverID, "ReceiverID");
    ValueEnforcer.notNull (m_sDocTypeID, "DocTypeID");
    ValueEnforcer.notNull (m_sProcessID, "ProcessID");
    ValueEnforcer.notNull (m_sSbdhInstanceID, "SbdhInstanceID");
    ValueEnforcer.notNull (m_eSourceType, "SourceType");
    ValueEnforcer.notNull (m_sDocumentPath, "DocumentPath");
    ValueEnforcer.isGE0 (m_nDocumentSize, "DocumentSize");
    ValueEnforcer.notNull (m_sDocumentHash, "DocumentHash");
    ValueEnforcer.notNull (m_sC1CountryCode, "C1CountryCode");
    ValueEnforcer.notNull (m_aCreationDT, "CreationDT");

    return m_aMgr.create (m_eTransactionType,
                          m_sSenderID,
                          m_sReceiverID,
                          m_sDocTypeID,
                          m_sProcessID,
                          m_sSbdhInstanceID,
                          m_eSourceType,
                          m_sDocumentPath,
                          m_nDocumentSize,
                          m_sDocumentHash,
                          m_sC1CountryCode,
                          m_aCreationDT,
                          m_sMlsTo,
                          m_sMlsInboundTransactionID,
                          m_sSbdhStandard,
                          m_sSbdhTypeVersion,
                          m_sSbdhType,
                          m_sPayloadMimeType,
                          m_sCustom1,
                          m_sCustom2,
                          m_sCustom3,
                          m_eMlsType,
                          m_sRequestedAS4MessageID);
  }
}
