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
import com.helger.phoss.ap.api.codelist.EForwardableKind;
import com.helger.phoss.ap.api.codelist.EVerificationResult;

/**
 * Everything a {@link com.helger.phoss.ap.api.mgr.IDocumentForwarder} needs to hand a document to
 * C4, without being tied to the kind of transaction the document came from. An inbound business
 * document, an inbound MLS and a copy of a self-generated outbound MLS all look the same here.
 * <p>
 * This exists so that forwarding is no longer hard-wired to {@link IInboundTransaction}: the thing
 * to forward may just as well be an {@link IOutboundTransaction}.
 * </p>
 *
 * @author Philip Helger
 * @since 0.12.0
 */
public interface IForwardableDocument
{
  /**
   * @return The ID of the transaction this document belongs to. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String id ();

  /**
   * @return What kind of document this is. Never <code>null</code>. This is the one thing that the
   *         document itself cannot tell the receiver.
   */
  @NonNull
  EForwardableKind kind ();

  /**
   * @return The SBDH Instance Identifier. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String sbdhInstanceID ();

  /**
   * @return The absolute path to the document payload, to be opened with
   *         {@link com.helger.phoss.ap.api.mgr.IDocumentPayloadManager#openDocumentStreamForRead(String)}.
   *         Never <code>null</code>. Because the path is absolute, the same forwarder works for the
   *         inbound and for the outbound storage root, without having to know which one it is.
   */
  @NonNull
  @Nonempty
  String documentPath ();

  /**
   * @return The size of the document payload in bytes.
   */
  long documentSize ();

  /**
   * @return The Peppol Document Type Identifier, URI encoded. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String docTypeID ();

  /**
   * @return The Peppol Process Identifier, URI encoded. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String processID ();

  /**
   * @return The Peppol sender participant ID (C1), URI encoded. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String senderID ();

  /**
   * @return The Peppol receiver participant ID (C4), URI encoded. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String receiverID ();

  /**
   * @return When this document entered the AP - the AS4 reception time for a received document, the
   *         creation time for a self-generated one. Never <code>null</code>.
   */
  @NonNull
  OffsetDateTime timestamp ();

  /**
   * @return An AP local unique identifier of this document, suitable as a file name component -
   *         the phase4 Incoming ID for a received document, the transaction ID for a self-generated
   *         one. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String localID ();

  /**
   * @return The verdict of the inbound document verification, or <code>null</code> if no
   *         verification was performed - which is always the case for a self-generated document.
   */
  @Nullable
  EVerificationResult verificationResult ();

  /**
   * @return The findings of the verification as a JSON array of {@link VerificationIssue}, or
   *         <code>null</code> if there are none.
   */
  @Nullable
  String verificationDetails ();

  /**
   * @return A supplier of the metadata JSON that a forwarder may write as a sidecar next to the
   *         document, or <code>null</code> if this document has no metadata to offer. It is
   *         deliberately a supplier and not a plain value: rendering the JSON is only worth doing
   *         for the forwarders that actually write a sidecar.
   */
  @Nullable
  Supplier <String> metadataJson ();
}
