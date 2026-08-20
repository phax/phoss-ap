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
 * Category of a document verification outcome. It distinguishes a verdict about the document
 * itself from the inability of a verifier to make a verdict at all, because its backend service
 * was not reachable. See {@link EVerificationFailMode} for the resulting behaviour.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
public enum EVerificationOutcomeCategory implements IHasID <String>
{
  /** The document was inspected and accepted. */
  PASSED ("passed"),
  /** The document was inspected and rejected because of its content (e.g. malware, invalid XML). */
  REJECTION ("rejection"),
  /**
   * The document could not be inspected, because the verifier backend service was unavailable
   * (e.g. connection refused, HTTP 5xx, timeout).
   */
  SERVICE_UNAVAILABLE ("service_unavailable");

  private final String m_sID;

  EVerificationOutcomeCategory (@NonNull @Nonempty final String sID)
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
  public static EVerificationOutcomeCategory getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EVerificationOutcomeCategory.class, sID);
  }
}
