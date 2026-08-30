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
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.json.IJsonArray;
import com.helger.json.JsonArray;
import com.helger.phoss.ap.api.codelist.EVerificationOutcomeCategory;

/**
 * Immutable DTO with the outcome of a single document verification, as returned by
 * {@link com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI} and
 * {@link com.helger.phoss.ap.api.spi.IOutboundDocumentVerifierSPI}.
 * <p>
 * It distinguishes a verdict about the document itself ({@link #passed()} and
 * {@link #rejected(String, Iterable)}) from the inability to make a verdict at all, because the
 * verifier backend service was unavailable ({@link #serviceUnavailable(String)}). The latter is
 * never an implicit rejection - for an inbound document it is handled according to the configured
 * {@link com.helger.phoss.ap.api.codelist.EVerificationFailMode}, for an outbound document it is
 * always treated as a rejection.
 * </p>
 * <p>
 * The findings are carried as {@link VerificationIssue}s, which are deliberately independent of
 * Peppol MLS: a verifier states <em>what is wrong with the document</em>, and the AP decides how to
 * report it - as an MLS response to C2 for an inbound document, and as JSON to the submitter for an
 * outbound document. A passed outcome may carry issues as well; those are then warnings.
 * </p>
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class VerificationOutcome
{
  private static final VerificationOutcome PASSED = new VerificationOutcome (EVerificationOutcomeCategory.PASSED,
                                                                             null,
                                                                             null);

  private final EVerificationOutcomeCategory m_eCategory;
  private final String m_sMessage;
  private final ICommonsList <VerificationIssue> m_aIssues;

  private VerificationOutcome (@NonNull final EVerificationOutcomeCategory eCategory,
                               @Nullable final String sMessage,
                               @Nullable final Iterable <? extends VerificationIssue> aIssues)
  {
    m_eCategory = eCategory;
    m_sMessage = sMessage;
    m_aIssues = new CommonsArrayList <> (aIssues);
  }

  /**
   * @return The category of this outcome. Never <code>null</code>.
   */
  @NonNull
  public EVerificationOutcomeCategory getCategory ()
  {
    return m_eCategory;
  }

  /**
   * @return <code>true</code> if the document was inspected and accepted.
   */
  public boolean isPassed ()
  {
    return m_eCategory == EVerificationOutcomeCategory.PASSED;
  }

  /**
   * @return <code>true</code> if the document was inspected and rejected.
   */
  public boolean isRejected ()
  {
    return m_eCategory == EVerificationOutcomeCategory.REJECTION;
  }

  /**
   * @return <code>true</code> if the document was not inspected at all, because the verifier
   *         backend service was unavailable.
   */
  public boolean isServiceUnavailable ()
  {
    return m_eCategory == EVerificationOutcomeCategory.SERVICE_UNAVAILABLE;
  }

  /**
   * @return The human readable reason of this outcome, used for logging and for the transaction
   *         error details. <code>null</code> for a passed verification.
   */
  @Nullable
  public String getMessage ()
  {
    return m_sMessage;
  }

  /**
   * @return All individual findings of the verification. Never <code>null</code> but maybe empty -
   *         a verifier is not required to provide details. On a passed outcome these are warnings.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <VerificationIssue> getAllIssues ()
  {
    return m_aIssues.getClone ();
  }

  /**
   * @return <code>true</code> if there is at least one issue.
   */
  public boolean hasIssues ()
  {
    return m_aIssues.isNotEmpty ();
  }

  /**
   * @return All findings as a JSON array, each element in the format documented on
   *         {@link VerificationIssue}. Never <code>null</code> but maybe empty. This is what ends
   *         up in the <code>verification_details</code> column of an inbound transaction.
   */
  @NonNull
  @ReturnsMutableCopy
  public IJsonArray getAllIssuesAsJson ()
  {
    final JsonArray ret = new JsonArray ();
    for (final VerificationIssue aIssue : m_aIssues)
      ret.add (aIssue.getAsJson ());
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Category", m_eCategory)
                                       .appendIfNotNull ("Message", m_sMessage)
                                       .append ("Issues", m_aIssues)
                                       .getToString ();
  }

  /**
   * @return The outcome stating that the document was inspected and accepted. Never
   *         <code>null</code>.
   */
  @NonNull
  public static VerificationOutcome passed ()
  {
    return PASSED;
  }

  /**
   * Create an outcome stating that the document was inspected and accepted, but that non-fatal
   * findings were made.
   *
   * @param aIssues
   *        The warnings. May be <code>null</code> or empty, in which case this is equivalent to
   *        {@link #passed()}.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static VerificationOutcome passed (@Nullable final Iterable <? extends VerificationIssue> aIssues)
  {
    return new VerificationOutcome (EVerificationOutcomeCategory.PASSED, null, aIssues);
  }

  /**
   * Create an outcome stating that the document was inspected and rejected, without individual
   * findings. If an MLS response is to be sent, it is created from the provided message.
   *
   * @param sMessage
   *        The human readable reason of the rejection. May neither be <code>null</code> nor empty.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static VerificationOutcome rejected (@NonNull @Nonempty final String sMessage)
  {
    ValueEnforcer.notEmpty (sMessage, "Message");
    return new VerificationOutcome (EVerificationOutcomeCategory.REJECTION, sMessage, null);
  }

  /**
   * Create an outcome stating that the document was inspected and rejected, including the
   * individual findings. For an inbound document the issues are propagated into the MLS response,
   * for an outbound document they are returned to the submitter.
   *
   * @param sMessage
   *        The human readable reason of the rejection. May neither be <code>null</code> nor empty.
   * @param aIssues
   *        The findings that led to the rejection. May neither be <code>null</code> nor empty.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static VerificationOutcome rejected (@NonNull @Nonempty final String sMessage,
                                              @NonNull @Nonempty final Iterable <? extends VerificationIssue> aIssues)
  {
    ValueEnforcer.notEmpty (sMessage, "Message");
    ValueEnforcer.notEmpty (aIssues, "Issues");
    return new VerificationOutcome (EVerificationOutcomeCategory.REJECTION, sMessage, aIssues);
  }

  /**
   * Create an outcome stating that the document could not be inspected at all, because the verifier
   * backend service was unavailable. This is never an implicit rejection of the document - see
   * {@link com.helger.phoss.ap.api.codelist.EVerificationFailMode}.
   *
   * @param sMessage
   *        The human readable reason of the unavailability. May neither be <code>null</code> nor
   *        empty.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static VerificationOutcome serviceUnavailable (@NonNull @Nonempty final String sMessage)
  {
    ValueEnforcer.notEmpty (sMessage, "Message");
    return new VerificationOutcome (EVerificationOutcomeCategory.SERVICE_UNAVAILABLE, sMessage, null);
  }
}
