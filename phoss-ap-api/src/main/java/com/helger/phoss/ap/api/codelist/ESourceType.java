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
 * Indicates how the outbound document was submitted by the Sender Backend.
 * <ul>
 * <li>{@link #PAYLOAD_ONLY} &mdash; Raw business document; the AP creates the
 * SBDH envelope.</li>
 * <li>{@link #PREBUILT_SBD} &mdash; Complete Standard Business Document with
 * SBDH already present.</li>
 * </ul>
 *
 * @author Philip Helger
 */
public enum ESourceType implements IHasID <String>
{
  /** Raw business document &mdash; the AP creates the SBDH envelope. */
  PAYLOAD_ONLY ("payload_only"),
  /** Complete Standard Business Document with SBDH already present. */
  PREBUILT_SBD ("prebuilt_sbd");

  private final String m_sID;

  ESourceType (@NonNull @Nonempty final String sID)
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
  public static ESourceType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESourceType.class, sID);
  }
}
