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

import java.time.Duration;
import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.phoss.ap.api.codelist.ECircuitBreakerState;

/**
 * An immutable snapshot of a single circuit breaker, as needed for operational monitoring. The
 * snapshot is taken at a single point in time - a circuit breaker may already have changed its
 * state when the snapshot is evaluated.
 *
 * @param circuitKey
 *        The key that identifies the circuit breaker, e.g.
 *        <code>smp$https://smp.example.org</code>. May neither be <code>null</code> nor empty.
 * @param state
 *        The current state. May not be <code>null</code>.
 * @param openSinceDT
 *        When the circuit breaker was opened, in UTC. <code>null</code> if it is closed.
 * @param remainingDelay
 *        The remaining delay until an open circuit breaker transitions to half-open.
 *        {@link Duration#ZERO} if it is not open. May not be <code>null</code>.
 * @param failureCount
 *        The number of failures currently counted.
 * @param lastFailureCause
 *        A short description of the last recorded failure cause. <code>null</code> if no failure
 *        with a known cause was recorded yet.
 * @author Philip Helger
 * @since 0.13.0
 */
public record CircuitBreakerInfo (@NonNull @Nonempty String circuitKey,
                                  @NonNull ECircuitBreakerState state,
                                  @Nullable OffsetDateTime openSinceDT,
                                  @NonNull Duration remainingDelay,
                                  long failureCount,
                                  @Nullable String lastFailureCause)
{
  /**
   * @return <code>true</code> if the circuit breaker currently rejects every call.
   */
  public boolean isOpen ()
  {
    return state.isOpen ();
  }
}
