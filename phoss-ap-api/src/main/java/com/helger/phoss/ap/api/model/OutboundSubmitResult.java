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

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ISuccessIndicator;
import com.helger.base.tostring.ToStringGenerator;

/**
 * Immutable result of submitting an outbound document.
 * <p>
 * Before 0.12.0 the submit methods returned a <code>@Nullable IOutboundTransaction</code>, where
 * <code>null</code> meant "the document was rejected by a verifier" <em>or</em> "the document could
 * not be processed" <em>or</em> "an internal error occurred" - indistinguishable to the caller, and
 * therefore reported to the submitter as a single generic message. This type separates the three
 * cases and carries the verification detail.
 * </p>
 * <p>
 * A verification rejection deliberately does <b>not</b> create an outbound transaction: the
 * document is never sent, so there is nothing to track, retry or report to the Peppol network.
 * </p>
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class OutboundSubmitResult implements ISuccessIndicator
{
  private final IOutboundTransaction m_aTransaction;
  private final VerifierResult m_aVerifierResult;
  private final String m_sErrorMessage;

  private OutboundSubmitResult (@Nullable final IOutboundTransaction aTransaction,
                                @Nullable final VerifierResult aVerifierResult,
                                @Nullable final String sErrorMessage)
  {
    m_aTransaction = aTransaction;
    m_aVerifierResult = aVerifierResult;
    m_sErrorMessage = sErrorMessage;
  }

  /** {@inheritDoc} */
  public boolean isSuccess ()
  {
    return m_aTransaction != null;
  }

  /**
   * @return <code>true</code> if the submission failed because a document verifier rejected the
   *         document. In that case {@link #getVerificationOutcome()} carries the details.
   */
  public boolean isVerificationRejected ()
  {
    return m_aTransaction == null && m_aVerifierResult != null;
  }

  /**
   * @return The created outbound transaction, or <code>null</code> if the submission failed.
   */
  @Nullable
  public IOutboundTransaction getTransaction ()
  {
    return m_aTransaction;
  }

  /**
   * @return The result of the document verification including the name of the decisive verifier, or
   *         <code>null</code> if no verification was performed. On success its outcome may carry
   *         warnings.
   */
  @Nullable
  public VerifierResult getVerifierResult ()
  {
    return m_aVerifierResult;
  }

  /**
   * @return The outcome of the document verification, or <code>null</code> if no verification was
   *         performed. Shorthand for {@link #getVerifierResult()}.
   */
  @Nullable
  public VerificationOutcome getVerificationOutcome ()
  {
    return m_aVerifierResult != null ? m_aVerifierResult.outcome () : null;
  }

  /**
   * @return The human-readable error message, or <code>null</code> on success and on a
   *         verification rejection - use {@link #getVerificationOutcome()} for the latter.
   */
  @Nullable
  public String getErrorMessage ()
  {
    return m_sErrorMessage;
  }

  /** {@inheritDoc} */
  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).appendIfNotNull ("Transaction", m_aTransaction)
                                       .appendIfNotNull ("VerifierResult", m_aVerifierResult)
                                       .appendIfNotNull ("ErrorMessage", m_sErrorMessage)
                                       .getToString ();
  }

  /**
   * Create a successful result.
   *
   * @param aTransaction
   *        The created outbound transaction. May not be <code>null</code>.
   * @param aVerifierResult
   *        The result of the verification, if one was performed. May be <code>null</code>. If its
   *        outcome carries issues, those are warnings that did not prevent the submission.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static OutboundSubmitResult success (@NonNull final IOutboundTransaction aTransaction,
                                              @Nullable final VerifierResult aVerifierResult)
  {
    ValueEnforcer.notNull (aTransaction, "Transaction");
    return new OutboundSubmitResult (aTransaction, aVerifierResult, null);
  }

  /**
   * Create a result stating that a document verifier rejected the document. No outbound transaction
   * is created in this case.
   *
   * @param aVerifierResult
   *        The rejecting result, carrying the individual findings and the name of the verifier that
   *        produced them. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static OutboundSubmitResult verificationRejected (@NonNull final VerifierResult aVerifierResult)
  {
    ValueEnforcer.notNull (aVerifierResult, "VerifierResult");
    return new OutboundSubmitResult (null, aVerifierResult, null);
  }

  /**
   * Create a result stating that the document could not be submitted for a reason unrelated to the
   * document verification, e.g. an unparsable payload or an internal error.
   *
   * @param sErrorMessage
   *        The human-readable error message. May neither be <code>null</code> nor empty.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static OutboundSubmitResult failure (@NonNull @Nonempty final String sErrorMessage)
  {
    ValueEnforcer.notEmpty (sErrorMessage, "ErrorMessage");
    return new OutboundSubmitResult (null, null, sErrorMessage);
  }
}
