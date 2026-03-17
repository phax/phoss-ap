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

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.mls.PeppolMLSBuilder;
import com.helger.peppol.mls.PeppolMLSLineResponseBuilder;

/**
 * Immutable DTO that captures the outcome of inbound document processing, used
 * to drive MLS response creation. Carries the overall response code, an
 * optional human-readable response text, and for rejection cases a list of
 * individual issues.
 * <p>
 * JSON format:
 *
 * <pre>
 * {
 *   "responseCode": "AP",
 *   "responseText": "Optional human-readable text",
 *   "issues": [
 *     {
 *       "errorField": "XPath or NA",
 *       "statusReasonCode": "BV",
 *       "description": "Human-readable error text"
 *     }
 *   ]
 * }
 * </pre>
 *
 * @author Philip Helger
 */
@Immutable
public final class MlsOutcome
{
  private final EPeppolMLSResponseCode m_eResponseCode;
  private final String m_sResponseText;
  private final ICommonsOrderedMap <String, ICommonsList <MlsOutcomeIssue>> m_aIssues = new CommonsLinkedHashMap <> ();

  /**
   * Full constructor.
   *
   * @param eResponseCode
   *        The overall MLS response code. May not be <code>null</code>.
   * @param sResponseText
   *        Optional human-readable response text. May be <code>null</code>.
   * @param aIssues
   *        Optional list of issues. May be <code>null</code> or empty for
   *        non-rejection responses. For rejection responses at least one issue
   *        is required.
   */
  public MlsOutcome (@NonNull final EPeppolMLSResponseCode eResponseCode,
                     @Nullable final String sResponseText,
                     @Nullable final Iterable <? extends MlsOutcomeIssue> aIssues)
  {
    ValueEnforcer.notNull (eResponseCode, "ResponseCode");
    m_eResponseCode = eResponseCode;
    m_sResponseText = sResponseText;
    // Group by error field
    if (aIssues != null)
      for (final var aIssue : aIssues)
        m_aIssues.computeIfAbsent (aIssue.getErrorField (), k -> new CommonsArrayList <> ()).add (aIssue);
  }

  /**
   * @return The overall MLS response code. Never <code>null</code>.
   */
  @NonNull
  public EPeppolMLSResponseCode getResponseCode ()
  {
    return m_eResponseCode;
  }

  /**
   * @return The response code ID string (e.g. "AP", "AB", "RE"). Never
   *         <code>null</code>.
   */
  @NonNull
  @Nonempty
  public String getResponseCodeID ()
  {
    return m_eResponseCode.getID ();
  }

  /**
   * @return Optional human-readable response text. May be <code>null</code>.
   */
  @Nullable
  public String getResponseText ()
  {
    return m_sResponseText;
  }

  /**
   * @return The list of issues. Never <code>null</code> but may be empty for
   *         non-rejection responses.
   */
  @NonNull
  public List <MlsOutcomeIssue> getIssues ()
  {
    return m_aIssues.values ().stream ().flatMap (ICommonsList::stream).toList ();
  }

  /**
   * @return <code>true</code> if there is at least one issue.
   */
  public boolean hasIssues ()
  {
    return m_aIssues.isNotEmpty ();
  }

  /**
   * @return The data structures as a new {@link PeppolMLSBuilder} with the
   *         response code and the response text as well as all potentially
   *         present line responses present. Never <code>null</code>.
   */
  @NonNull
  public PeppolMLSBuilder getAsMLSBuilder ()
  {
    final PeppolMLSBuilder ret = new PeppolMLSBuilder (m_eResponseCode).responseText (m_sResponseText);
    for (final var aGroup : m_aIssues.entrySet ())
    {
      final PeppolMLSLineResponseBuilder aBuilder = new PeppolMLSLineResponseBuilder ().errorField (aGroup.getKey ());
      for (final var aIssue : aGroup.getValue ())
        aBuilder.addResponse (b -> b.statusReasonCode (aIssue.getStatusReasonCode ())
                                    .description (aIssue.getDescription ()));
      ret.addLineResponse (aBuilder);
    }
    return ret;
  }

  /** {@inheritDoc} */
  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ResponseCode", m_eResponseCode)
                                       .appendIfNotNull ("ResponseText", m_sResponseText)
                                       .append ("Issues", m_aIssues)
                                       .getToString ();
  }

  /**
   * Create an acceptance outcome (response code AP). The document was
   * successfully delivered to C4 with confirmation.
   *
   * @return A new {@link MlsOutcome} with response code
   *         {@link EPeppolMLSResponseCode#ACCEPTANCE}.
   */
  @NonNull
  public static MlsOutcome acceptance ()
  {
    return new MlsOutcome (EPeppolMLSResponseCode.ACCEPTANCE, null, null);
  }

  /**
   * Create an acknowledging outcome (response code AB). The document was
   * forwarded to C4 without confirmation of receipt.
   *
   * @return A new {@link MlsOutcome} with response code
   *         {@link EPeppolMLSResponseCode#ACKNOWLEDGING}.
   */
  @NonNull
  public static MlsOutcome acknowledging ()
  {
    return new MlsOutcome (EPeppolMLSResponseCode.ACKNOWLEDGING, null, null);
  }

  /**
   * Create a rejection outcome (response code RE) with a single issue.
   *
   * @param sResponseText
   *        Human-readable response text. May be <code>null</code>.
   * @param aIssue
   *        The rejection issue. May not be <code>null</code>.
   * @return A new {@link MlsOutcome} with response code
   *         {@link EPeppolMLSResponseCode#REJECTION}.
   */
  @NonNull
  public static MlsOutcome rejection (@Nullable final String sResponseText, @NonNull final MlsOutcomeIssue aIssue)
  {
    return rejection (sResponseText, new CommonsArrayList <> (aIssue));
  }

  /**
   * Create a rejection outcome (response code RE) with multiple issues.
   *
   * @param sResponseText
   *        Human-readable response text. May be <code>null</code>.
   * @param aIssues
   *        The rejection issues. May not be <code>null</code> or empty.
   * @return A new {@link MlsOutcome} with response code
   *         {@link EPeppolMLSResponseCode#REJECTION}.
   */
  @NonNull
  public static MlsOutcome rejection (@Nullable final String sResponseText,
                                      @NonNull @Nonempty final List <MlsOutcomeIssue> aIssues)
  {
    ValueEnforcer.notEmpty (aIssues, "Issues");
    return new MlsOutcome (EPeppolMLSResponseCode.REJECTION, sResponseText, aIssues);
  }
}
