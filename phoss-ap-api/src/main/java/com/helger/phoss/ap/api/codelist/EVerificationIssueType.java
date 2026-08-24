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
package com.helger.phoss.ap.api.codelist;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * The kind of rule that a {@link com.helger.phoss.ap.api.model.VerificationIssue} violated.
 * <p>
 * Combined with {@link EVerificationIssueLevel} this reconstructs the Peppol MLS status reason
 * codes: {@link #SYNTAX} maps to "SV", {@link #BUSINESS_RULE} maps to "BV" for
 * {@link EVerificationIssueLevel#ERROR} and to "BW" for {@link EVerificationIssueLevel#WARNING}.
 * </p>
 *
 * @author Philip Helger
 * @since 0.12.0
 */
public enum EVerificationIssueType implements IHasID <String>
{
  /** A structural or syntactical violation, e.g. an XML Schema validation failure. */
  SYNTAX ("syntax"),
  /** A business rule violation, e.g. a failed Schematron assertion. */
  BUSINESS_RULE ("business_rule");

  private final String m_sID;

  EVerificationIssueType (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * Find the enum constant matching the given ID.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or <code>null</code> if not found.
   */
  @Nullable
  public static EVerificationIssueType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EVerificationIssueType.class, sID);
  }
}
