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

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppol.mls.CPeppolMLS;
import com.helger.peppol.mls.EPeppolMLSStatusReasonCode;
import com.helger.phoss.ap.api.codelist.EVerificationIssueType;

/**
 * A single issue within an MLS rejection response. Each issue references an error location, a
 * status reason code and a human-readable description.
 * <p>
 * JSON format:
 *
 * <pre>
 * {
 *   "errorField": "XPath or NA",
 *   "statusReasonCode": "BV",
 *   "description": "Human-readable error text"
 * }
 * </pre>
 *
 * @author Philip Helger
 */
@Immutable
public final class MlsOutcomeIssue
{
  private final String m_sErrorField;
  private final EPeppolMLSStatusReasonCode m_eStatusReasonCode;
  private final String m_sDescription;

  /**
   * Constructor for all fields.
   *
   * @param sErrorField
   *        The error field reference (XPath expression or
   *        {@link CPeppolMLS#LINE_ID_NOT_AVAILABLE}). May neither be <code>null</code> nor empty.
   * @param eStatusReasonCode
   *        The MLS status reason code (e.g. "BV", "BW", "FD", "SV"). May neither be
   *        <code>null</code> nor empty.
   * @param sDescription
   *        Human-readable error description. May neither be <code>null</code> nor empty.
   */
  public MlsOutcomeIssue (@NonNull @Nonempty final String sErrorField,
                          @NonNull final EPeppolMLSStatusReasonCode eStatusReasonCode,
                          @NonNull @Nonempty final String sDescription)
  {
    ValueEnforcer.notEmpty (sErrorField, "ErrorField");
    ValueEnforcer.notNull (eStatusReasonCode, "StatusReasonCode");
    ValueEnforcer.notEmpty (sDescription, "Description");
    m_sErrorField = sErrorField;
    m_eStatusReasonCode = eStatusReasonCode;
    m_sDescription = sDescription;
  }

  /**
   * @return The error field reference (XPath expression or
   *         {@link CPeppolMLS#LINE_ID_NOT_AVAILABLE}). Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getErrorField ()
  {
    return m_sErrorField;
  }

  /**
   * @return The MLS status reason code (e.g. "BV", "BW", "FD", "SV"). Never <code>null</code> nor
   *         empty.
   */
  @NonNull
  public EPeppolMLSStatusReasonCode getStatusReasonCode ()
  {
    return m_eStatusReasonCode;
  }

  /**
   * @return Human-readable error description. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getDescription ()
  {
    return m_sDescription;
  }

  /** {@inheritDoc} */
  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ErrorField", m_sErrorField)
                                       .append ("StatusReasonCode", m_eStatusReasonCode)
                                       .append ("Description", m_sDescription)
                                       .getToString ();
  }

  /**
   * Factory method for a business rule violation (fatal).
   *
   * @param sErrorField
   *        The error field reference. May neither be <code>null</code> nor empty.
   * @param sDescription
   *        Human-readable error description. May neither be <code>null</code> nor empty.
   * @return A new {@link MlsOutcomeIssue} with status reason code "BV".
   */
  @NonNull
  public static MlsOutcomeIssue businessRuleViolation (@NonNull @Nonempty final String sErrorField,
                                                       @NonNull @Nonempty final String sDescription)
  {
    return new MlsOutcomeIssue (sErrorField, EPeppolMLSStatusReasonCode.BUSINESS_RULE_VIOLATION_FATAL, sDescription);
  }

  /**
   * Factory method for a business rule warning.
   *
   * @param sErrorField
   *        The error field reference. May neither be <code>null</code> nor empty.
   * @param sDescription
   *        Human-readable error description. May neither be <code>null</code> nor empty.
   * @return A new {@link MlsOutcomeIssue} with status reason code "BW".
   */
  @NonNull
  public static MlsOutcomeIssue businessRuleWarning (@NonNull @Nonempty final String sErrorField,
                                                     @NonNull @Nonempty final String sDescription)
  {
    return new MlsOutcomeIssue (sErrorField, EPeppolMLSStatusReasonCode.BUSINESS_RULE_VIOLATION_WARNING, sDescription);
  }

  /**
   * Factory method for a failure of delivery.
   *
   * @param sDescription
   *        Human-readable error description. May neither be <code>null</code> nor empty.
   * @return A new {@link MlsOutcomeIssue} with status reason code "FD" and error field "NA".
   */
  @NonNull
  public static MlsOutcomeIssue failureOfDelivery (@NonNull @Nonempty final String sDescription)
  {
    return new MlsOutcomeIssue (CPeppolMLS.LINE_ID_NOT_AVAILABLE,
                                EPeppolMLSStatusReasonCode.FAILURE_OF_DELIVERY,
                                sDescription);
  }

  /**
   * Factory method for a syntax violation.
   *
   * @param sErrorField
   *        The error field reference. May neither be <code>null</code> nor empty.
   * @param sDescription
   *        Human-readable error description. May neither be <code>null</code> nor empty.
   * @return A new {@link MlsOutcomeIssue} with status reason code "SV".
   */
  @NonNull
  public static MlsOutcomeIssue syntaxViolation (@NonNull @Nonempty final String sErrorField,
                                                 @NonNull @Nonempty final String sDescription)
  {
    return new MlsOutcomeIssue (sErrorField, EPeppolMLSStatusReasonCode.SYNTAX_VIOLATION, sDescription);
  }

  /**
   * Map a transport-neutral {@link VerificationIssue} to its Peppol MLS representation.
   * <p>
   * MLS conflates severity and rule kind into a single status reason code, so the mapping is
   * severity-first: a {@link com.helger.phoss.ap.api.codelist.EVerificationIssueLevel#WARNING}
   * always becomes "BW", because reporting a warning as a fatal violation would tell C2 that the
   * document is unusable. Only a fatal issue distinguishes "SV" (syntax) from "BV" (business rule).
   * </p>
   * <p>
   * MLS has no field for a machine-readable rule identifier, so a present
   * {@link VerificationIssue#getCode()} is prefixed to the description as <code>[code] text</code>
   * - which is exactly the MLS text that was produced before the neutral issue model existed.
   * </p>
   *
   * @param aIssue
   *        The issue to map. May not be <code>null</code>.
   * @return A new {@link MlsOutcomeIssue}. Never <code>null</code>.
   * @since 0.12.0
   */
  @NonNull
  public static MlsOutcomeIssue fromVerificationIssue (@NonNull final VerificationIssue aIssue)
  {
    ValueEnforcer.notNull (aIssue, "Issue");

    // MLS requires a non-empty error field
    final String sErrorField = aIssue.hasLocation () ? aIssue.getLocation () : CPeppolMLS.LINE_ID_NOT_AVAILABLE;

    final EPeppolMLSStatusReasonCode eReason;
    if (!aIssue.isError ())
      eReason = EPeppolMLSStatusReasonCode.BUSINESS_RULE_VIOLATION_WARNING;
    else
      eReason = aIssue.getType () == EVerificationIssueType.SYNTAX ? EPeppolMLSStatusReasonCode.SYNTAX_VIOLATION
                                                                   : EPeppolMLSStatusReasonCode.BUSINESS_RULE_VIOLATION_FATAL;

    // MLS has no dedicated field for the rule identifier
    final String sDescription = (aIssue.hasCode () ? "[" + aIssue.getCode () + "] " : "") + aIssue.getDescription ();

    return new MlsOutcomeIssue (sErrorField, eReason, sDescription);
  }

  /**
   * Factory method using {@link CPeppolMLS#LINE_ID_NOT_AVAILABLE} as the error field.
   *
   * @param eStatusReasonCode
   *        The MLS status reason code. May neither be <code>null</code> nor empty.
   * @param sDescription
   *        Human-readable error description. May neither be <code>null</code> nor empty.
   * @return A new {@link MlsOutcomeIssue} with error field "NA".
   */
  @NonNull
  public static MlsOutcomeIssue ofNA (@NonNull final EPeppolMLSStatusReasonCode eStatusReasonCode,
                                      @NonNull @Nonempty final String sDescription)
  {
    return new MlsOutcomeIssue (CPeppolMLS.LINE_ID_NOT_AVAILABLE, eStatusReasonCode, sDescription);
  }
}
