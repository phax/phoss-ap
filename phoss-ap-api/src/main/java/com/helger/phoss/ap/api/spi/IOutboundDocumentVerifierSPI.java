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
package com.helger.phoss.ap.api.spi;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIInterface;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.ap.api.model.VerificationOutcome;

/**
 * SPI interface for optional document verification. Implementations are loaded via
 * {@link java.util.ServiceLoader}. Multiple verifiers may be registered and are evaluated in order
 * — all must pass for the document to be accepted.
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface IOutboundDocumentVerifierSPI extends IDocumentVerifier
{
  /**
   * Verify a document's content against the given document type and process identifiers.
   *
   * @param sDocumentPath
   *        The path where the document resides. Must only be opened for reading. Never
   *        <code>null</code>.
   * @param aDocTypeID
   *        The Peppol Document Type Identifier. Never <code>null</code>.
   * @param aProcessID
   *        The Peppol Process Identifier. Never <code>null</code>.
   * @return The outcome of the verification. May not be <code>null</code>. Use
   *         {@link VerificationOutcome#passed()} if the verifier has no objection,
   *         {@link VerificationOutcome#passed(Iterable)} to accept the document but report
   *         warnings, and {@link VerificationOutcome#rejected(String, Iterable)} to reject it - the
   *         {@link com.helger.phoss.ap.api.model.VerificationIssue}s are returned to the submitter,
   *         so that a client can react to a specific rule without parsing human-readable text.
   *         {@link VerificationOutcome#serviceUnavailable(String)} is treated like a rejection
   *         here: unlike the inbound direction, outbound verification has no fail mode, so a
   *         verifier that cannot make a verdict stays fail-closed.
   * @since 0.12.0 - was previously returning {@link com.helger.base.state.ESuccess}, which could
   *        not carry any detail about <em>why</em> a document was rejected
   */
  @NonNull
  VerificationOutcome verifyOutboundDocument (@NonNull @Nonempty String sDocumentPath,
                                              @NonNull IDocumentTypeIdentifier aDocTypeID,
                                              @NonNull IProcessIdentifier aProcessID);
}
