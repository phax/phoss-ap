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
package com.helger.phoss.ap.api.dto;

import org.jspecify.annotations.NonNull;

import com.helger.phoss.ap.api.model.CircuitBreakerInfo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON response DTO representing the current state of a single circuit breaker. Usable both for
 * server-side serialization and client-side deserialization.
 *
 * @author Philip Helger
 * @since 0.13.0
 */
@Schema (description = "State of a single circuit breaker - which remote system is guarded, " +
                       "whether it is currently suspended and why.")
public class CircuitBreakerResponse
{
  @Schema (description = "Key that identifies the circuit breaker",
           example = "smp$https://smp.example.org")
  private String circuitKey;

  @Schema (description = "Current state", allowableValues = { "closed", "open", "half_open" })
  private String state;

  @Schema (description = "When the circuit breaker was opened (ISO-8601, UTC); null if it is closed",
           example = "2026-09-14T10:12:03Z",
           nullable = true)
  private String openSinceDT;

  @Schema (description = "Remaining seconds until an open circuit breaker transitions to half-open; 0 if it is not open",
           example = "42")
  private long remainingDelaySeconds;

  @Schema (description = "Number of failures currently counted")
  private long failureCount;

  @Schema (description = "Short description of the last recorded failure cause; null if unknown",
           example = "SocketTimeoutException: Read timed out after 10000 ms",
           nullable = true)
  private String lastFailureCause;

  /**
   * Create a response DTO from the provided circuit breaker snapshot.
   *
   * @param aInfo
   *        The snapshot to convert. May not be <code>null</code>.
   * @return The new response DTO. Never <code>null</code>.
   */
  @NonNull
  public static CircuitBreakerResponse fromDomain (@NonNull final CircuitBreakerInfo aInfo)
  {
    final CircuitBreakerResponse aResp = new CircuitBreakerResponse ();
    aResp.circuitKey = aInfo.circuitKey ();
    aResp.state = aInfo.state ().getID ();
    aResp.openSinceDT = aInfo.openSinceDT () != null ? aInfo.openSinceDT ().toString () : null;
    aResp.remainingDelaySeconds = aInfo.remainingDelay ().toSeconds ();
    aResp.failureCount = aInfo.failureCount ();
    aResp.lastFailureCause = aInfo.lastFailureCause ();
    return aResp;
  }

  /** @return the circuit breaker key */
  public String getCircuitKey ()
  {
    return circuitKey;
  }

  /** @return the current state */
  public String getState ()
  {
    return state;
  }

  /** @return when the circuit breaker was opened, or <code>null</code> */
  public String getOpenSinceDT ()
  {
    return openSinceDT;
  }

  /** @return the remaining delay in seconds */
  public long getRemainingDelaySeconds ()
  {
    return remainingDelaySeconds;
  }

  /** @return the number of counted failures */
  public long getFailureCount ()
  {
    return failureCount;
  }

  /** @return the last failure cause, or <code>null</code> */
  public String getLastFailureCause ()
  {
    return lastFailureCause;
  }
}
