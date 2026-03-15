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
 * Defines which Peppol identifier factory to use. The strict mode enforces all
 * Peppol identifier rules, while the lax mode is more lenient.
 *
 * @author Philip Helger
 */
public enum EPeppolIdentifierMode implements IHasID <String>
{
  /** Strict Peppol identifier validation (default). */
  STRICT ("strict"),
  /** Lax Peppol identifier validation. */
  LAX ("lax");

  public static final EPeppolIdentifierMode DEFAULT = STRICT;

  private final String m_sID;

  EPeppolIdentifierMode (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

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
  public static EPeppolIdentifierMode getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EPeppolIdentifierMode.class, sID);
  }

  /**
   * Find the enum constant matching the given ID or return the default.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or {@link #DEFAULT} if not found.
   */
  @NonNull
  public static EPeppolIdentifierMode getFromIDOrDefault (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrDefault (EPeppolIdentifierMode.class, sID, DEFAULT);
  }
}
