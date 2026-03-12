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
 * Lifecycle status of an inbound transaction.
 *
 * @author Philip Helger
 */
public enum EInboundStatus implements IHasID <String>
{
  /** Received via AS4 and stored in DB, awaiting processing. */
  RECEIVED ("received"),
  /**
   * Failed optional verification &mdash; no forwarding attempt will be made.
   */
  REJECTED ("rejected"),
  /** Forwarding to Receiver Backend (C3) is currently in progress. */
  FORWARDING ("forwarding"),
  /** Successfully forwarded to Receiver Backend (C3). */
  FORWARDED ("forwarded"),
  /** Last forwarding attempt failed &mdash; scheduled for retry. */
  FORWARD_FAILED ("forward_failed"),
  /** Max retries exhausted &mdash; no further attempts. */
  PERMANENTLY_FAILED ("permanently_failed");

  private final String m_sID;

  EInboundStatus (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public boolean isFinalState ()
  {
    return this == FORWARDED || this == REJECTED || this == PERMANENTLY_FAILED;
  }

  /**
   * Find the enum constant matching the given ID.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or <code>null</code> if not found.
   */
  @Nullable
  public static EInboundStatus getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EInboundStatus.class, sID);
  }
}
