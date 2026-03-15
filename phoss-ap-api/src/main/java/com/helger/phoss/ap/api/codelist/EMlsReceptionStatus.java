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
 * Status of MLS response reception for an outbound business document (as C2).
 *
 * @author Philip Helger
 */
public enum EMlsReceptionStatus implements IHasID <String>
{
  /** MLS response has not yet been received. */
  PENDING ("pending"),
  /** MLS response received with code AP (Approved). */
  RECEIVED_AP ("received_ap"),
  /** MLS response received with code AB (Accepted Blind). */
  RECEIVED_AB ("received_ab"),
  /** MLS response received with code RE (Rejection). */
  RECEIVED_RE ("received_re"),
  /**
   * MLS is not expected for this transaction (e.g., MLS response transactions).
   */
  NOT_APPLICABLE ("not_applicable");

  private final String m_sID;

  EMlsReceptionStatus (@NonNull @Nonempty final String sID)
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
  public static EMlsReceptionStatus getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EMlsReceptionStatus.class, sID);
  }
}
