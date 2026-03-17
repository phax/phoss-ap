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
package com.helger.phoss.ap.core;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.ThreadSafe;

import dev.failsafe.CircuitBreaker;

/**
 * This class manages the different circuit breakers used by phoss AP.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class CircuitBreakerManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CircuitBreakerManager.class);

  private static final ConcurrentHashMap <String, CircuitBreaker <Void>> BREAKERS = new ConcurrentHashMap <> ();

  private CircuitBreakerManager ()
  {}

  @NonNull
  private static CircuitBreaker <Void> _getOrCreate (@NonNull final String sCircuitKey)
  {
    return BREAKERS.computeIfAbsent (sCircuitKey, k -> {
      LOGGER.info ("Creating circuit breaker for '" + k + "'");
      return CircuitBreaker.<Void> builder ()
                           .withFailureThreshold (APCoreConfig.getCircuitBreakerFailureThreshold ())
                           .withDelay (Duration.ofMillis (APCoreConfig.getCircuitBreakerOpenDurationMs ()))
                           .withSuccessThreshold (APCoreConfig.getCircuitBreakerHalfOpenMaxAttempts ())
                           .onOpen (e -> LOGGER.info ("The circuit breaker for '" + k + "' was opened"))
                           .onClose (e -> LOGGER.info ("The circuit breaker for '" + k + "' was closed"))
                           .onHalfOpen (e -> LOGGER.info ("The circuit breaker for '" + k + "' was half-opened"))
                           .build ();
    });
  }

  /**
   * Try to acquire a permit from the circuit breaker identified by the given key. If the circuit
   * breaker is open, no permit will be granted.
   *
   * @param sCircuitKey
   *        The circuit breaker key to acquire a permit for. May not be <code>null</code>.
   * @return {@code true} if the permit was acquired, {@code false} if the circuit is open.
   */
  public static boolean tryAcquirePermit (@NonNull final String sCircuitKey)
  {
    return _getOrCreate (sCircuitKey).tryAcquirePermit ();
  }

  /**
   * Record a successful operation for the circuit breaker identified by the given key.
   *
   * @param sCircuitKey
   *        The circuit breaker key to record the success for. May not be <code>null</code>.
   */
  public static void recordSuccess (@NonNull final String sCircuitKey)
  {
    _getOrCreate (sCircuitKey).recordSuccess ();
  }

  /**
   * Record a failed operation for the circuit breaker identified by the given key. If the failure
   * threshold is reached, the circuit breaker will open.
   *
   * @param sCircuitKey
   *        The circuit breaker key to record the failure for. May not be <code>null</code>.
   */
  public static void recordFailure (@NonNull final String sCircuitKey)
  {
    _getOrCreate (sCircuitKey).recordFailure ();
  }
}
