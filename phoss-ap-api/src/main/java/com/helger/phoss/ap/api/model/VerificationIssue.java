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
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.phoss.ap.api.codelist.EVerificationIssueLevel;
import com.helger.phoss.ap.api.codelist.EVerificationIssueType;

/**
 * A single finding of a document verification, independent of how it is later reported to the
 * outside world. For an inbound document the issues are mapped to Peppol MLS line responses (see
 * {@link MlsOutcomeIssue#fromVerificationIssue(VerificationIssue)}); for an outbound document they
 * are returned to the submitter as JSON.
 * <p>
 * The separation of {@link #getLevel()} and {@link #getType()} is deliberate: the Peppol MLS status
 * reason codes connects severity and rule kind into a single value, so a verifier that returns MLS
 * data directly cannot express "an XSD warning". Keeping the two apart also means that a verifier
 * does not need to know anything about MLS at all.
 * </p>
 * <p>
 * JSON format:
 *
 * <pre>
 * {
 *   "level": "error",
 *   "type": "business_rule",
 *   "code": "PEPPOL-EN16931-R001",
 *   "location": "/Invoice/cbc:ID",
 *   "description": "Human-readable error text"
 * }
 * </pre>
 *
 * The "code" and "location" entries are only present if known.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
@Immutable
public final class VerificationIssue
{
  private final EVerificationIssueLevel m_eLevel;
  private final EVerificationIssueType m_eType;
  private final String m_sCode;
  private final String m_sLocation;
  private final String m_sDescription;

  /**
   * Constructor for all fields.
   *
   * @param eLevel
   *        The severity of the issue. May not be <code>null</code>.
   * @param eType
   *        The kind of rule that was violated. May not be <code>null</code>.
   * @param sCode
   *        The machine-readable identifier of the violated rule, e.g. a Schematron assertion ID
   *        like <code>PEPPOL-EN16931-R001</code>. May be <code>null</code> if the verifier does not
   *        provide one.
   * @param sLocation
   *        The location of the issue in the document, e.g. an XPath expression. May be
   *        <code>null</code> if unknown.
   * @param sDescription
   *        The human-readable description. May neither be <code>null</code> nor empty.
   */
  public VerificationIssue (@NonNull final EVerificationIssueLevel eLevel,
                            @NonNull final EVerificationIssueType eType,
                            @Nullable final String sCode,
                            @Nullable final String sLocation,
                            @NonNull @Nonempty final String sDescription)
  {
    ValueEnforcer.notNull (eLevel, "Level");
    ValueEnforcer.notNull (eType, "Type");
    ValueEnforcer.notEmpty (sDescription, "Description");
    m_eLevel = eLevel;
    m_eType = eType;
    m_sCode = sCode;
    m_sLocation = sLocation;
    m_sDescription = sDescription;
  }

  /**
   * @return The severity of the issue. Never <code>null</code>.
   */
  @NonNull
  public EVerificationIssueLevel getLevel ()
  {
    return m_eLevel;
  }

  /**
   * @return <code>true</code> if this issue prevents the document from being accepted.
   */
  public boolean isError ()
  {
    return m_eLevel.isError ();
  }

  /**
   * @return The kind of rule that was violated. Never <code>null</code>.
   */
  @NonNull
  public EVerificationIssueType getType ()
  {
    return m_eType;
  }

  public boolean hasCode ()
  {
    return StringHelper.isNotEmpty (m_sCode);
  }

  /**
   * @return The machine-readable identifier of the violated rule, or <code>null</code> if the
   *         verifier does not provide one. This is what a client should branch on - never the
   *         description.
   */
  @Nullable
  public String getCode ()
  {
    return m_sCode;
  }

  public boolean hasLocation ()
  {
    return StringHelper.isNotEmpty (m_sLocation);
  }

  /**
   * @return The location of the issue in the document, e.g. an XPath expression, or
   *         <code>null</code> if unknown.
   */
  @Nullable
  public String getLocation ()
  {
    return m_sLocation;
  }

  /**
   * @return The human-readable description. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getDescription ()
  {
    return m_sDescription;
  }

  /**
   * @return This issue in the JSON format documented on this class. Never <code>null</code>.
   */
  @NonNull
  public IJsonObject getAsJson ()
  {
    final IJsonObject ret = new JsonObject ().add ("level", m_eLevel.getID ()).add ("type", m_eType.getID ());
    if (hasCode ())
      ret.add ("code", m_sCode);
    if (hasLocation ())
      ret.add ("location", m_sLocation);
    ret.add ("description", m_sDescription);
    return ret;
  }

  /** {@inheritDoc} */
  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Level", m_eLevel)
                                       .append ("Type", m_eType)
                                       .appendIfNotNull ("Code", m_sCode)
                                       .appendIfNotNull ("Location", m_sLocation)
                                       .append ("Description", m_sDescription)
                                       .getToString ();
  }

  /**
   * Factory method for a fatal syntax violation, e.g. an XML Schema validation failure.
   *
   * @param sCode
   *        The machine-readable rule identifier. May be <code>null</code>.
   * @param sLocation
   *        The location in the document. May be <code>null</code>.
   * @param sDescription
   *        The human-readable description. May neither be <code>null</code> nor empty.
   * @return A new {@link VerificationIssue}. Never <code>null</code>.
   */
  @NonNull
  public static VerificationIssue syntaxViolation (@Nullable final String sCode,
                                                   @Nullable final String sLocation,
                                                   @NonNull @Nonempty final String sDescription)
  {
    return new VerificationIssue (EVerificationIssueLevel.ERROR,
                                  EVerificationIssueType.SYNTAX,
                                  sCode,
                                  sLocation,
                                  sDescription);
  }

  /**
   * Factory method for a fatal business rule violation, e.g. a failed Schematron assertion.
   *
   * @param sCode
   *        The machine-readable rule identifier. May be <code>null</code>.
   * @param sLocation
   *        The location in the document. May be <code>null</code>.
   * @param sDescription
   *        The human-readable description. May neither be <code>null</code> nor empty.
   * @return A new {@link VerificationIssue}. Never <code>null</code>.
   */
  @NonNull
  public static VerificationIssue businessRuleViolation (@Nullable final String sCode,
                                                         @Nullable final String sLocation,
                                                         @NonNull @Nonempty final String sDescription)
  {
    return new VerificationIssue (EVerificationIssueLevel.ERROR,
                                  EVerificationIssueType.BUSINESS_RULE,
                                  sCode,
                                  sLocation,
                                  sDescription);
  }

  /**
   * Factory method for a non-fatal business rule warning.
   *
   * @param sCode
   *        The machine-readable rule identifier. May be <code>null</code>.
   * @param sLocation
   *        The location in the document. May be <code>null</code>.
   * @param sDescription
   *        The human-readable description. May neither be <code>null</code> nor empty.
   * @return A new {@link VerificationIssue}. Never <code>null</code>.
   */
  @NonNull
  public static VerificationIssue businessRuleWarning (@Nullable final String sCode,
                                                       @Nullable final String sLocation,
                                                       @NonNull @Nonempty final String sDescription)
  {
    return new VerificationIssue (EVerificationIssueLevel.WARNING,
                                  EVerificationIssueType.BUSINESS_RULE,
                                  sCode,
                                  sLocation,
                                  sDescription);
  }
}
