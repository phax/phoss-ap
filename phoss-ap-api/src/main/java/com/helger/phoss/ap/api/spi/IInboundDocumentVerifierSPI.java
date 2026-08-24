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
public interface IInboundDocumentVerifierSPI extends IDocumentVerifier
{
  /**
   * Verify a document's content against the given document type and process identifiers.
   *
   * @param sDocumentPath
   *        The absolute path where the document is stored. Never <code>null</code>.
   * @param aDocTypeID
   *        The Peppol Document Type Identifier. Never <code>null</code>.
   * @param aProcessID
   *        The Peppol Process Identifier. Never <code>null</code>.
   * @return The outcome of the verification. May not be <code>null</code>. Use
   *         {@link VerificationOutcome#passed()} if the verifier has no objection,
   *         {@link VerificationOutcome#rejected(String, Iterable)} to reject the document - its
   *         {@link com.helger.phoss.ap.api.model.VerificationIssue}s are mapped into the MLS
   *         response - and {@link VerificationOutcome#serviceUnavailable(String)} if the document
   *         could not be verified at all, because the verifier backend service was unavailable. The
   *         latter is never an implicit rejection; it is handled according to the configured
   *         {@link com.helger.phoss.ap.api.codelist.EVerificationFailMode}.
   */
  @NonNull
  VerificationOutcome verifyInboundDocument (@NonNull @Nonempty String sDocumentPath,
                                             @NonNull IDocumentTypeIdentifier aDocTypeID,
                                             @NonNull IProcessIdentifier aProcessID);
}
