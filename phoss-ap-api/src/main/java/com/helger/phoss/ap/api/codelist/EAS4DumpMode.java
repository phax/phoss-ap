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
 * AS4 message dump file layout mode. {@link #DIRECTION} places dumps into separate
 * {@code incoming/} and {@code outgoing/} directories (the existing default). {@link #GROUPED}
 * correlates related messages (UserMessage + SignalMessage) of a single exchange into a shared
 * directory.
 *
 * @author Philip Helger
 */
public enum EAS4DumpMode implements IHasID <String>
{
  /** Existing behavior: incoming/ and outgoing/ directories. */
  DIRECTION ("direction"),

  /** Correlated exchange directories grouping request + response. */
  GROUPED ("grouped");

  public static final EAS4DumpMode DEFAULT = DIRECTION;

  private final String m_sID;

  EAS4DumpMode (@NonNull @Nonempty final String sID)
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
  public static EAS4DumpMode getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EAS4DumpMode.class, sID);
  }

  /**
   * Find the enum constant matching the given ID.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or {@link #DEFAULT} if not found.
   */
  @NonNull
  public static EAS4DumpMode getFromIDOrDefault (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrDefault (EAS4DumpMode.class, sID, DEFAULT);
  }
}
