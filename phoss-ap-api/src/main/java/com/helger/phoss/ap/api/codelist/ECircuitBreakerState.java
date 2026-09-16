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
 * The state of a circuit breaker. This mirrors the state of the underlying Failsafe circuit
 * breaker, so that the Failsafe dependency stays confined to phoss-ap-core.
 *
 * @author Philip Helger
 * @since 0.13.0
 */
public enum ECircuitBreakerState implements IHasID <String>
{
  /** The circuit breaker is closed and every call is permitted. */
  CLOSED ("closed"),
  /** The circuit breaker is open and every call is rejected. */
  OPEN ("open"),
  /**
   * The circuit breaker temporarily permits a limited number of probe calls, to find out whether
   * the guarded remote system has recovered.
   */
  HALF_OPEN ("half_open");

  private final String m_sID;

  ECircuitBreakerState (@NonNull @Nonempty final String sID)
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
   * @return <code>true</code> if the circuit breaker currently rejects every call.
   */
  public boolean isOpen ()
  {
    return this == OPEN;
  }

  /**
   * Find the enum constant matching the given ID.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or <code>null</code> if not found.
   */
  @Nullable
  public static ECircuitBreakerState getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ECircuitBreakerState.class, sID);
  }
}
