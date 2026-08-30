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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.string.StringHelper;

/**
 * The aggregated result of running the registered document verifiers of one direction: the decisive
 * {@link VerificationOutcome} together with the name of the verifier that produced it.
 * <p>
 * The name is not part of the outcome itself, because a verifier does not name itself in its
 * verdict - the orchestrator knows which verifier it just asked. It is used in log messages, in the
 * transaction error details, in the MLS response sent to C2 and in the rejection reported back to
 * an outbound submitter, so that an operator can tell <em>which</em> of several registered
 * verifiers objected.
 * </p>
 *
 * @param outcome
 *        The decisive outcome. May not be <code>null</code>.
 * @param verifierName
 *        The name of the verifier that led to this result, as returned by
 *        {@link com.helger.phoss.ap.api.spi.IDocumentVerifier#getVerifierName()}. <code>null</code>
 *        if and only if all verifiers accepted the document, because then no single verifier is
 *        decisive.
 * @author Philip Helger
 * @since 0.12.0
 */
public record VerifierResult (@NonNull VerificationOutcome outcome, @Nullable String verifierName)
{
  /**
   * @return <code>true</code> if a decisive verifier is known.
   */
  public boolean hasVerifierName ()
  {
    return StringHelper.isNotEmpty (verifierName);
  }

  /**
   * @return The outcome that all verifiers accepted the document, without a decisive verifier name.
   *         Never <code>null</code>.
   * @param aOutcome
   *        The passed outcome, potentially carrying warnings. May not be <code>null</code>.
   */
  @NonNull
  public static VerifierResult passed (@NonNull final VerificationOutcome aOutcome)
  {
    return new VerifierResult (aOutcome, null);
  }
}
