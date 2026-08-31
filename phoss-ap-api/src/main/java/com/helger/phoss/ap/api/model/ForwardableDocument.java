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
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.codelist.EForwardableKind;
import com.helger.phoss.ap.api.codelist.EVerificationResult;
import com.helger.phoss.ap.api.dto.InboundTransactionResponse;

/**
 * Default implementation of {@link IForwardableDocument}.
 *
 * @param id
 *        The ID of the transaction this document belongs to. May not be <code>null</code>.
 * @param kind
 *        What kind of document this is. May not be <code>null</code>.
 * @param sbdhInstanceID
 *        The SBDH Instance Identifier. May not be <code>null</code>.
 * @param documentPath
 *        The absolute path to the document payload. May not be <code>null</code>.
 * @param documentSize
 *        The size of the document payload in bytes.
 * @param docTypeID
 *        The Peppol Document Type Identifier, URI encoded. May not be <code>null</code>.
 * @param processID
 *        The Peppol Process Identifier, URI encoded. May not be <code>null</code>.
 * @param senderID
 *        The Peppol sender participant ID (C1), URI encoded. May not be <code>null</code>.
 * @param receiverID
 *        The Peppol receiver participant ID (C4), URI encoded. May not be <code>null</code>.
 * @param timestamp
 *        When this document entered the AP. May not be <code>null</code>.
 * @param localID
 *        An AP local unique identifier, suitable as a file name component. May not be
 *        <code>null</code>.
 * @param verificationResult
 *        The verdict of the inbound document verification. May be <code>null</code>.
 * @param verificationDetails
 *        The findings of the verification as a JSON array. May be <code>null</code>.
 * @param metadataJson
 *        A supplier of the metadata JSON sidecar content. May be <code>null</code>.
 * @author Philip Helger
 * @since 0.12.0
 */
public record ForwardableDocument (@NonNull @Nonempty String id,
                                   @NonNull EForwardableKind kind,
                                   @NonNull @Nonempty String sbdhInstanceID,
                                   @NonNull @Nonempty String documentPath,
                                   long documentSize,
                                   @NonNull @Nonempty String docTypeID,
                                   @NonNull @Nonempty String processID,
                                   @NonNull @Nonempty String senderID,
                                   @NonNull @Nonempty String receiverID,
                                   @NonNull OffsetDateTime timestamp,
                                   @NonNull @Nonempty String localID,
                                   @Nullable EVerificationResult verificationResult,
                                   @Nullable String verificationDetails,
                                   @Nullable Supplier <String> metadataJson) implements IForwardableDocument
{
  /**
   * Create a forwardable document from a received inbound transaction. The kind is derived from the
   * document type and process identifiers, and the metadata JSON is the very same
   * {@link InboundTransactionResponse} the forwarders rendered themselves before 0.12.0.
   *
   * @param aTransaction
   *        The inbound transaction to adapt. May not be <code>null</code>.
   * @return The adapted document. Never <code>null</code>.
   */
  @NonNull
  public static ForwardableDocument fromInbound (@NonNull final IInboundTransaction aTransaction)
  {
    final EForwardableKind eKind = CPhossAP.isMLS (aTransaction.getDocTypeID (), aTransaction.getProcessID ())
                                                                                                              ? EForwardableKind.INBOUND_MLS
                                                                                                              : EForwardableKind.INBOUND_DOCUMENT;
    return new ForwardableDocument (aTransaction.getID (),
                                    eKind,
                                    aTransaction.getSbdhInstanceID (),
                                    aTransaction.getDocumentPath (),
                                    aTransaction.getDocumentSize (),
                                    aTransaction.getDocTypeID (),
                                    aTransaction.getProcessID (),
                                    aTransaction.getSenderID (),
                                    aTransaction.getReceiverID (),
                                    aTransaction.getReceivedDT (),
                                    aTransaction.getIncomingID (),
                                    aTransaction.getVerificationResult (),
                                    aTransaction.getVerificationDetails (),
                                    () -> InboundTransactionResponse.fromDomain (aTransaction)
                                                                    .toJson ()
                                                                    .getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED));
  }

  /**
   * Create a forwardable document from an outbound MLS transaction, to hand C4 a copy of an MLS
   * that this AP generated and sent itself. The payload is the bare <code>ApplicationResponse</code>
   * XML as it was stored - the AP never materializes the SBDH envelope, phase4 adds it during the
   * AS4 transmission - so C4 tells this apart from a received MLS by {@link #kind()}.
   * <p>
   * No metadata sidecar is offered: the MLS <code>ApplicationResponse</code> already references the
   * document it responds to, which is the correlation key a downstream reporter needs.
   * </p>
   *
   * @param aTransaction
   *        The outbound MLS transaction to adapt. May not be <code>null</code>.
   * @return The adapted document. Never <code>null</code>.
   */
  @NonNull
  public static ForwardableDocument fromOutboundMlsCopy (@NonNull final IOutboundTransaction aTransaction)
  {
    return new ForwardableDocument (aTransaction.getID (),
                                    EForwardableKind.OUTBOUND_MLS_COPY,
                                    aTransaction.getSbdhInstanceID (),
                                    aTransaction.getDocumentPath (),
                                    aTransaction.getDocumentSize (),
                                    aTransaction.getDocTypeID (),
                                    aTransaction.getProcessID (),
                                    aTransaction.getSenderID (),
                                    aTransaction.getReceiverID (),
                                    aTransaction.getCreatedDT (),
                                    aTransaction.getID (),
                                    // A self-generated document is never verified
                                    null,
                                    null,
                                    // No sidecar: OutboundTransactionResponse has no JSON
                                    // rendering, and the MLS ApplicationResponse already carries
                                    // the reference to the document it responds to
                                    null);
  }
}
