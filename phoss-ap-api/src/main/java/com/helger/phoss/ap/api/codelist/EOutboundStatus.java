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
 * Lifecycle status of an outbound transaction (business document or MLS
 * response).
 *
 * @author Philip Helger
 */
public enum EOutboundStatus implements IHasID <String>
{
  /** Stored in DB, awaiting processing. */
  PENDING ("pending"),
  /** Failed optional verification &mdash; no sending attempt will be made. */
  REJECTED ("rejected"),
  /** AS4 sending is currently in progress. */
  SENDING ("sending"),
  /** Successfully sent via AS4 and receipt confirmed. */
  SENT ("sent"),
  /** Last sending attempt failed &mdash; scheduled for retry. */
  FAILED ("failed"),
  /** Max retries exhausted &mdash; no further attempts. */
  PERMANENTLY_FAILED ("permanently_failed");

  private final String m_sID;

  EOutboundStatus (@NonNull @Nonempty final String sID)
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
   * @return <code>true</code> if this status represents a terminal state where
   *         no further processing will occur.
   */
  public boolean isFinalState ()
  {
    return this == REJECTED || this == SENT || this == PERMANENTLY_FAILED;
  }

  /**
   * Find the enum constant matching the given ID.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or <code>null</code> if not found.
   */
  @Nullable
  public static EOutboundStatus getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EOutboundStatus.class, sID);
  }
}
