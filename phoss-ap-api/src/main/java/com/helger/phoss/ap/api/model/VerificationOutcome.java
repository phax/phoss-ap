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
import com.helger.base.tostring.ToStringGenerator;
import com.helger.phoss.ap.api.codelist.EVerificationOutcomeCategory;

/**
 * Immutable DTO with the outcome of a single document verification, as returned by
 * {@link com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI}.
 * <p>
 * It distinguishes a verdict about the document itself ({@link #passed()} and
 * {@link #rejected(MlsOutcome)}) from the inability to make a verdict at all, because the verifier
 * backend service was unavailable ({@link #serviceUnavailable(String)}). The latter is never an
 * implicit rejection - how it is handled, depends on the configured
 * {@link com.helger.phoss.ap.api.codelist.EVerificationFailMode}.
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
  private final MlsOutcome m_aMlsOutcome;

  private VerificationOutcome (@NonNull final EVerificationOutcomeCategory eCategory,
                               @Nullable final String sMessage,
                               @Nullable final MlsOutcome aMlsOutcome)
  {
    m_eCategory = eCategory;
    m_sMessage = sMessage;
    m_aMlsOutcome = aMlsOutcome;
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
   * @return The details to be used for the negative MLS response. Only set for a rejection that was
   *         created via {@link #rejected(MlsOutcome)}, <code>null</code> otherwise. For all other
   *         cases that end up as a rejection, the MLS response is created from the category and the
   *         message.
   */
  @Nullable
  public MlsOutcome getMlsOutcome ()
  {
    return m_aMlsOutcome;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Category", m_eCategory)
                                       .appendIfNotNull ("Message", m_sMessage)
                                       .appendIfNotNull ("MlsOutcome", m_aMlsOutcome)
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
   * Create an outcome stating that the document was inspected and rejected, including the details
   * for the negative MLS response.
   *
   * @param aMlsOutcome
   *        The MLS details of the rejection. Its issues are propagated into the MLS response. May
   *        not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static VerificationOutcome rejected (@NonNull final MlsOutcome aMlsOutcome)
  {
    ValueEnforcer.notNull (aMlsOutcome, "MlsOutcome");
    return new VerificationOutcome (EVerificationOutcomeCategory.REJECTION,
                                    aMlsOutcome.getResponseText (),
                                    aMlsOutcome);
  }

  /**
   * Create an outcome stating that the document was inspected and rejected, without MLS details. If
   * an MLS response is to be sent, it is created from the provided message.
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
