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
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.phoss.ap.api.codelist.ECircuitBreakerState;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.model.CircuitBreakerInfo;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.core.metrics.APMetrics;
import com.helger.telemetry.TelemetryAttributes;

import dev.failsafe.CircuitBreaker;
import dev.failsafe.CircuitBreakerBuilder;

/**
 * This class manages the different circuit breakers used by phoss AP.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class CircuitBreakerManager
{
  /**
   * The per-key data that Failsafe itself does not keep: when the circuit breaker was opened and
   * what the last failure was. Both are needed to turn a rejection into a message an operator can
   * act on.
   */
  private static final class BreakerState
  {
    private volatile OffsetDateTime m_aOpenSinceDT;
    private volatile String m_sLastFailureCause;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (CircuitBreakerManager.class);

  /** The minimum failure thresholding period accepted by Failsafe. */
  private static final long MIN_FAILURE_PERIOD_MILLIS = 10;
  /** The maximum length of the remembered last failure cause. */
  private static final int MAX_FAILURE_CAUSE_LENGTH = 200;

  private static final ConcurrentHashMap <String, CircuitBreaker <Void>> BREAKERS = new ConcurrentHashMap <> ();
  private static final ConcurrentHashMap <String, BreakerState> STATES = new ConcurrentHashMap <> ();

  private CircuitBreakerManager ()
  {}

  @NonNull
  private static BreakerState _getState (@NonNull final String sCircuitKey)
  {
    return STATES.computeIfAbsent (sCircuitKey, k -> new BreakerState ());
  }

  /**
   * Build the short representation of a failure cause: the simple exception class name plus its
   * message, truncated to {@link #MAX_FAILURE_CAUSE_LENGTH} characters.
   *
   * @param aCause
   *        The causing exception. May be <code>null</code>.
   * @return <code>null</code> if no cause was provided.
   */
  @Nullable
  private static String _getShortFailureCause (@Nullable final Throwable aCause)
  {
    if (aCause == null)
      return null;

    final String sMessage = aCause.getMessage ();
    final String sRet = StringHelper.isEmpty (sMessage) ? aCause.getClass ().getSimpleName () : aCause.getClass ()
                                                                                                      .getSimpleName () +
                                                                                                ": " +
                                                                                                sMessage;
    return sRet.length () > MAX_FAILURE_CAUSE_LENGTH ? sRet.substring (0, MAX_FAILURE_CAUSE_LENGTH) + "..." : sRet;
  }

  /**
   * Apply the configured failure thresholding to the provided circuit breaker builder. Three modes
   * are supported:
   * <ol>
   * <li>a rolling time window with a failure rate, if {@code circuit-breaker.failure-period} and
   * {@code circuit-breaker.failure-rate} are set</li>
   * <li>a rolling time window with an absolute failure count, if only
   * {@code circuit-breaker.failure-period} is set</li>
   * <li>an absolute failure count over the last N executions, if only
   * {@code circuit-breaker.failure-executions} is set</li>
   * </ol>
   * If neither of them is set, the default applies: N <b>consecutive</b> failures open the circuit
   * breaker. An invalid combination is logged as an error and falls back to that default, because a
   * circuit breaker that cannot be built would take the whole sending down.
   *
   * @param aBuilder
   *        The builder to apply the thresholding to. May not be <code>null</code>.
   * @param sCircuitKey
   *        The circuit breaker key, for logging only. May not be <code>null</code>.
   * @return The provided builder for chaining. Never <code>null</code>.
   */
  @NonNull
  private static CircuitBreakerBuilder <Void> _applyFailureThreshold (@NonNull final CircuitBreakerBuilder <Void> aBuilder,
                                                                      @NonNull final String sCircuitKey)
  {
    final int nFailureThreshold = APCoreConfig.getCircuitBreakerFailureThreshold ();
    final int nFailureExecutions = APCoreConfig.getCircuitBreakerFailureExecutions ();
    final int nFailureRate = APCoreConfig.getCircuitBreakerFailureRate ();
    final Duration aFailurePeriod = APCoreConfig.getCircuitBreakerFailurePeriod ();

    if (aFailurePeriod != null)
    {
      if (aFailurePeriod.toMillis () < MIN_FAILURE_PERIOD_MILLIS)
      {
        LOGGER.error ("The configuration property '" +
                      APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_PERIOD +
                      "' must be at least " +
                      MIN_FAILURE_PERIOD_MILLIS +
                      " ms - ignoring the time based thresholding of circuit breaker '" +
                      sCircuitKey +
                      "'");
      }
      else
        if (nFailureRate > 0)
        {
          if (nFailureRate <= 100)
          {
            // Minimum number of executions before the rate is evaluated at all
            final int nExecutionThreshold = nFailureExecutions > 0 ? nFailureExecutions : nFailureThreshold;
            LOGGER.info ("The circuit breaker for '" +
                         sCircuitKey +
                         "' opens at a failure rate of " +
                         nFailureRate +
                         "% over at least " +
                         nExecutionThreshold +
                         " executions within " +
                         aFailurePeriod);
            return aBuilder.withFailureRateThreshold (nFailureRate, nExecutionThreshold, aFailurePeriod);
          }
          LOGGER.error ("The configuration property '" +
                        APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_RATE +
                        "' must be between 1 and 100 - ignoring the time based thresholding of circuit breaker '" +
                        sCircuitKey +
                        "'");
        }
        else
        {
          final int nExecutionThreshold = nFailureExecutions > 0 ? nFailureExecutions : nFailureThreshold;
          if (nExecutionThreshold >= nFailureThreshold)
          {
            LOGGER.info ("The circuit breaker for '" +
                         sCircuitKey +
                         "' opens at " +
                         nFailureThreshold +
                         " failures over at least " +
                         nExecutionThreshold +
                         " executions within " +
                         aFailurePeriod);
            return aBuilder.withFailureThreshold (nFailureThreshold, nExecutionThreshold, aFailurePeriod);
          }
          LOGGER.error ("The configuration property '" +
                        APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_EXECUTIONS +
                        "' must not be smaller than '" +
                        APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD +
                        "' - ignoring the time based thresholding of circuit breaker '" +
                        sCircuitKey +
                        "'");
        }
    }
    else
      if (nFailureExecutions > 0)
      {
        if (nFailureExecutions >= nFailureThreshold)
        {
          LOGGER.info ("The circuit breaker for '" +
                       sCircuitKey +
                       "' opens at " +
                       nFailureThreshold +
                       " failures over the last " +
                       nFailureExecutions +
                       " executions");
          return aBuilder.withFailureThreshold (nFailureThreshold, nFailureExecutions);
        }
        LOGGER.error ("The configuration property '" +
                      APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_EXECUTIONS +
                      "' must not be smaller than '" +
                      APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD +
                      "' - ignoring the count based thresholding of circuit breaker '" +
                      sCircuitKey +
                      "'");
      }

    // Default: consecutive failures
    return aBuilder.withFailureThreshold (nFailureThreshold);
  }

  /**
   * Map the state of the underlying Failsafe circuit breaker onto the project's own code list, so
   * that the Failsafe dependency stays confined to this module.
   *
   * @param aBreaker
   *        The circuit breaker to read the state from. May not be <code>null</code>.
   * @return The matching code list value. Never <code>null</code>.
   */
  @NonNull
  private static ECircuitBreakerState _getMappedState (@NonNull final CircuitBreaker <Void> aBreaker)
  {
    return switch (aBreaker.getState ())
    {
      case CLOSED -> ECircuitBreakerState.CLOSED;
      case OPEN -> ECircuitBreakerState.OPEN;
      case HALF_OPEN -> ECircuitBreakerState.HALF_OPEN;
    };
  }

  @NonNull
  private static TelemetryAttributes _getMetricAttrs (@NonNull final String sCircuitKey,
                                                      @NonNull final ECircuitBreakerState eState)
  {
    return TelemetryAttributes.builder ()
                              .put (CPhossAPOtel.ATTR_CIRCUIT_BREAKER_KEY, sCircuitKey)
                              .put (CPhossAPOtel.ATTR_CIRCUIT_BREAKER_STATE, eState.getID ())
                              .build ();
  }

  private static void _onStateChange (@NonNull final String sCircuitKey, @NonNull final ECircuitBreakerState eNewState)
  {
    APMetrics.CIRCUIT_BREAKER_STATE_CHANGES.add (1, _getMetricAttrs (sCircuitKey, eNewState));
  }

  private static void _onOpen (@NonNull final String sCircuitKey)
  {
    final BreakerState aState = _getState (sCircuitKey);
    aState.m_aOpenSinceDT = PDTFactory.getCurrentOffsetDateTimeUTC ();

    _onStateChange (sCircuitKey, ECircuitBreakerState.OPEN);

    final String sLastFailureCause = aState.m_sLastFailureCause;
    LOGGER.warn ("The circuit breaker for '" +
                 sCircuitKey +
                 "' was opened for " +
                 APCoreConfig.getCircuitBreakerOpenDuration () +
                 (sLastFailureCause != null ? "; last failure: " + sLastFailureCause : ""));
  }

  private static void _onClose (@NonNull final String sCircuitKey)
  {
    _getState (sCircuitKey).m_aOpenSinceDT = null;
    _onStateChange (sCircuitKey, ECircuitBreakerState.CLOSED);
    LOGGER.info ("The circuit breaker for '" + sCircuitKey + "' was closed");
  }

  private static void _onHalfOpen (@NonNull final String sCircuitKey)
  {
    _onStateChange (sCircuitKey, ECircuitBreakerState.HALF_OPEN);
    LOGGER.info ("The circuit breaker for '" + sCircuitKey + "' was half-opened");
  }

  @NonNull
  private static CircuitBreaker <Void> _getOrCreate (@NonNull final String sCircuitKey)
  {
    return BREAKERS.computeIfAbsent (sCircuitKey, k -> {
      LOGGER.info ("Creating circuit breaker for '" + k + "'");
      return _applyFailureThreshold (CircuitBreaker.<Void> builder (), k).withDelay (APCoreConfig
                                                                                                 .getCircuitBreakerOpenDuration ())
                                                                         .withSuccessThreshold (APCoreConfig.getCircuitBreakerHalfOpenMaxAttempts ())
                                                                         .onOpen (e -> _onOpen (k))
                                                                         .onClose (e -> _onClose (k))
                                                                         .onHalfOpen (e -> _onHalfOpen (k))
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
    final CircuitBreaker <Void> aBreaker = _getOrCreate (sCircuitKey);
    if (aBreaker.tryAcquirePermit ())
      return true;

    APMetrics.CIRCUIT_BREAKER_REJECTIONS.add (1, _getMetricAttrs (sCircuitKey, _getMappedState (aBreaker)));
    return false;
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
    recordFailure (sCircuitKey, null);
  }

  /**
   * Record a failed operation for the circuit breaker identified by the given key, remembering the
   * causing exception. If the failure threshold is reached, the circuit breaker will open and the
   * cause becomes part of the rejection message of every subsequent call.
   *
   * @param sCircuitKey
   *        The circuit breaker key to record the failure for. May not be <code>null</code>.
   * @param aCause
   *        The exception that caused the failure. May be <code>null</code>, in which case a
   *        previously remembered cause is kept.
   * @since 0.13.0
   */
  public static void recordFailure (@NonNull final String sCircuitKey, @Nullable final Throwable aCause)
  {
    final String sShortFailureCause = _getShortFailureCause (aCause);
    if (sShortFailureCause != null)
      _getState (sCircuitKey).m_sLastFailureCause = sShortFailureCause;

    _getOrCreate (sCircuitKey).recordFailure ();
  }

  /**
   * Build an actionable message for a call that was rejected by a circuit breaker. It names the
   * current state, since when the circuit breaker is open, how long it stays open, how many
   * failures were counted and what the last failure was - everything an operator needs to decide
   * whether to wait or to look at the remote system.
   *
   * @param sCircuitKey
   *        The circuit breaker key that rejected the call. May not be <code>null</code>.
   * @param sWhatIsSuspended
   *        Description of what is suspended, e.g.
   *        <code>"SMP access to 'https://smp.example.org'"</code>. May not be <code>null</code>.
   * @return The rejection message. Never <code>null</code>.
   * @since 0.13.0
   */
  @NonNull
  public static String getRejectionMessage (@NonNull final String sCircuitKey, @NonNull final String sWhatIsSuspended)
  {
    final CircuitBreaker <Void> aBreaker = _getOrCreate (sCircuitKey);
    final BreakerState aState = _getState (sCircuitKey);
    final OffsetDateTime aOpenSinceDT = aState.m_aOpenSinceDT;
    final String sLastFailureCause = aState.m_sLastFailureCause;

    // The human readable enum name is used deliberately - the lower case ID is for the REST API
    // and the metric attributes
    final StringBuilder aSB = new StringBuilder (sWhatIsSuspended).append (" suspended by circuit breaker (state ")
                                                                  .append (_getMappedState (aBreaker).name ());
    if (aOpenSinceDT != null)
      aSB.append (" since ").append (aOpenSinceDT);
    aSB.append (", ").append (aBreaker.getRemainingDelay ().toSeconds ()).append ("s remaining) after ");
    aSB.append (aBreaker.getFailureCount ()).append (" failures");
    if (sLastFailureCause != null)
      aSB.append ("; last failure: ").append (sLastFailureCause);
    return aSB.toString ();
  }

  /**
   * Get the remaining delay until the circuit breaker identified by the given key transitions from
   * the open state to the half-open state. A circuit breaker that is not open returns
   * {@link Duration#ZERO}.
   *
   * @param sCircuitKey
   *        The circuit breaker key to query. May not be <code>null</code>.
   * @return The remaining delay. Never <code>null</code>.
   * @since 0.13.0
   */
  @NonNull
  public static Duration getRemainingDelay (@NonNull final String sCircuitKey)
  {
    return _getOrCreate (sCircuitKey).getRemainingDelay ();
  }

  /**
   * Get a snapshot of all known circuit breakers, ordered by their key. A circuit breaker only
   * becomes known once it was used for the first time, so a key that never rejected or recorded
   * anything is not contained.
   *
   * @return A list of snapshots. Never <code>null</code>.
   * @since 0.13.0
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <CircuitBreakerInfo> getAllInfos ()
  {
    final ICommonsList <CircuitBreakerInfo> ret = new CommonsArrayList <> ();
    for (final var aEntry : BREAKERS.entrySet ())
    {
      final String sCircuitKey = aEntry.getKey ();
      final CircuitBreaker <Void> aBreaker = aEntry.getValue ();
      final BreakerState aState = _getState (sCircuitKey);
      ret.add (new CircuitBreakerInfo (sCircuitKey,
                                       _getMappedState (aBreaker),
                                       aState.m_aOpenSinceDT,
                                       aBreaker.getRemainingDelay (),
                                       aBreaker.getFailureCount (),
                                       aState.m_sLastFailureCause));
    }
    ret.sort (Comparator.comparing (CircuitBreakerInfo::circuitKey));
    return ret;
  }

  /**
   * Reset the circuit breaker identified by the given key: it is forgotten entirely, so that the
   * next usage of the key creates a new, closed circuit breaker from the current configuration.
   * This is the operational escape hatch for a circuit breaker that suspends a remote system which
   * is known to be healthy again.
   *
   * @param sCircuitKey
   *        The circuit breaker key to reset. May not be <code>null</code>.
   * @return {@code true} if a circuit breaker with that key existed, {@code false} if not.
   * @since 0.13.0
   */
  public static boolean reset (@NonNull final String sCircuitKey)
  {
    STATES.remove (sCircuitKey);
    final boolean bExisted = BREAKERS.remove (sCircuitKey) != null;
    if (bExisted)
      LOGGER.info ("The circuit breaker for '" + sCircuitKey + "' was manually reset");
    else
      LOGGER.warn ("Cannot reset the unknown circuit breaker '" + sCircuitKey + "'");
    return bExisted;
  }

  /**
   * Remove all known circuit breakers, so that the next usage of a key creates a new circuit
   * breaker from the current configuration. Only intended for testing.
   */
  @VisibleForTesting
  public static void removeAll ()
  {
    BREAKERS.clear ();
    STATES.clear ();
  }
}
