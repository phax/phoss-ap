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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.datetime.helper.PDTFactory;
import com.helger.phoss.ap.api.codelist.ECircuitBreakerState;
import com.helger.phoss.ap.api.model.CircuitBreakerInfo;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for class {@link CircuitBreakerManager}.
 *
 * @author Philip Helger
 */
public final class CircuitBreakerManagerTest
{
  private static final String KEY = "unittest$" + CircuitBreakerManagerTest.class.getName ();

  private IConfigWithFallback m_aOldConfig;

  private static void _setSystemProperty (final String sKey, final String sValue)
  {
    if (sValue == null)
      System.clearProperty (sKey);
    else
      System.setProperty (sKey, sValue);
  }

  /**
   * Wait until the circuit breaker grants a permit again, but at most for the provided number of
   * milliseconds.
   */
  private static boolean _awaitPermit (final String sKey, final long nMaxWaitMillis) throws InterruptedException
  {
    final long nEnd = System.currentTimeMillis () + nMaxWaitMillis;
    do
    {
      if (CircuitBreakerManager.tryAcquirePermit (sKey))
        return true;
      Thread.sleep (10);
    } while (System.currentTimeMillis () < nEnd);
    return false;
  }

  @Before
  public void before ()
  {
    m_aOldConfig = APConfigProvider.getConfig ();

    // A very short open duration, so that the half-open state is reached quickly
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD, "2");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION, "50ms");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_HALF_OPEN_MAX_ATTEMPTS, "1");
    APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
    CircuitBreakerManager.removeAll ();
  }

  @After
  public void after ()
  {
    CircuitBreakerManager.removeAll ();
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD, null);
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION, null);
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_HALF_OPEN_MAX_ATTEMPTS, null);
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_EXECUTIONS, null);
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_PERIOD, null);
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_RATE, null);
    APConfigProvider.setConfig (m_aOldConfig);
  }

  @Test
  public void testRecordedFailuresOpenAndReopenTheCircuitBreaker () throws InterruptedException
  {
    // Open the circuit breaker
    for (int i = 0; i < 2; ++i)
    {
      assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
      CircuitBreakerManager.recordFailure (KEY);
    }
    assertFalse (CircuitBreakerManager.tryAcquirePermit (KEY));

    // After the open duration the half-open state grants a single permit again
    assertTrue (_awaitPermit (KEY, 5_000));
    CircuitBreakerManager.recordSuccess (KEY);

    // The circuit breaker is closed again, so every call is permitted
    assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
    CircuitBreakerManager.recordSuccess (KEY);
  }

  @Test
  public void testUnrecordedPermitInHalfOpenStateDoesNotBlockForever () throws InterruptedException
  {
    // Open the circuit breaker
    for (int i = 0; i < 2; ++i)
    {
      assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
      CircuitBreakerManager.recordFailure (KEY);
    }
    assertFalse (CircuitBreakerManager.tryAcquirePermit (KEY));

    // Wait for the half-open state and simulate a guarded block that throws a RuntimeException
    // after the permit was acquired. This is exactly what the "finally" blocks in the
    // orchestrators protect against - the permit of a half-open circuit breaker is only released
    // by recordSuccess or recordFailure.
    assertTrue (_awaitPermit (KEY, 5_000));
    boolean bResultRecorded = false;
    try
    {
      throw new IllegalStateException ("Something went wrong after the permit was acquired");
    }
    catch (final IllegalStateException ex)
    {
      // Expected
    }
    finally
    {
      if (!bResultRecorded)
      {
        CircuitBreakerManager.recordFailure (KEY);
        bResultRecorded = true;
      }
    }
    assertTrue (bResultRecorded);

    // Because the permit was released, the circuit breaker opens regularly and grants a new permit
    // after the open duration instead of rejecting every call forever
    assertTrue (_awaitPermit (KEY, 5_000));
    CircuitBreakerManager.recordSuccess (KEY);
    assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
    CircuitBreakerManager.recordSuccess (KEY);
  }

  @Test
  public void testFailureRateThresholdToleratesIsolatedFailures ()
  {
    // Open only if at least half of the last 4+ executions within 10 minutes failed
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_RATE, "50");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_EXECUTIONS, "4");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_PERIOD, "10m");
    APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
    CircuitBreakerManager.removeAll ();

    // 2 failures out of 10 executions is a rate of 20% - a short load peak on a healthy SMP
    for (int i = 0; i < 10; ++i)
    {
      assertTrue ("Permit " + i + " was not granted", CircuitBreakerManager.tryAcquirePermit (KEY));
      if (i % 5 == 0)
        CircuitBreakerManager.recordFailure (KEY);
      else
        CircuitBreakerManager.recordSuccess (KEY);
    }
    assertTrue ("Isolated failures must not open the circuit breaker",
                CircuitBreakerManager.tryAcquirePermit (KEY));
    CircuitBreakerManager.recordSuccess (KEY);
  }

  @Test
  public void testFailureRateThresholdStillOpensOnRealOutages ()
  {
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_RATE, "50");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_EXECUTIONS, "4");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_PERIOD, "10m");
    APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
    CircuitBreakerManager.removeAll ();

    // 4 executions, all of them failures - a rate of 100%
    for (int i = 0; i < 4; ++i)
    {
      assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
      CircuitBreakerManager.recordFailure (KEY);
    }
    assertFalse (CircuitBreakerManager.tryAcquirePermit (KEY));
  }

  @Test
  public void testTimeBasedFailureCountIsNotLimitedToConsecutiveFailures ()
  {
    // Without a rate, the failures inside the window do not have to be consecutive anymore - this
    // makes the circuit breaker more sensitive, not less
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD, "2");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_EXECUTIONS, "4");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_PERIOD, "10m");
    APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
    CircuitBreakerManager.removeAll ();

    // failure, success, success, failure - never two in a row
    final boolean [] aFailures = { true, false, false, true };
    for (final boolean bFailure : aFailures)
    {
      assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
      if (bFailure)
        CircuitBreakerManager.recordFailure (KEY);
      else
        CircuitBreakerManager.recordSuccess (KEY);
    }
    assertFalse (CircuitBreakerManager.tryAcquirePermit (KEY));
  }

  @Test
  public void testInvalidTimeBasedThresholdingFallsBackToConsecutiveFailures ()
  {
    // failure-executions smaller than failure-threshold is rejected by Failsafe - the circuit
    // breaker must still be created
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD, "5");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_EXECUTIONS, "2");
    APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
    CircuitBreakerManager.removeAll ();

    // 5 consecutive failures - the documented default behaviour
    for (int i = 0; i < 5; ++i)
    {
      assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
      CircuitBreakerManager.recordFailure (KEY);
    }
    assertFalse (CircuitBreakerManager.tryAcquirePermit (KEY));
  }

  @Test
  public void testRejectionMessageContainsStateOpenSinceRemainingDelayAndCause ()
  {
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD, "2");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION, "1m");
    APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
    CircuitBreakerManager.removeAll ();

    final OffsetDateTime aBeforeDT = PDTFactory.getCurrentOffsetDateTimeUTC ();
    for (int i = 0; i < 2; ++i)
    {
      assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
      CircuitBreakerManager.recordFailure (KEY, new SocketTimeoutException ("Read timed out after 10000 ms"));
    }
    assertFalse (CircuitBreakerManager.tryAcquirePermit (KEY));

    final String sMsg = CircuitBreakerManager.getRejectionMessage (KEY,
                                                                   "SMP access to 'https://smp.example.org'");
    assertTrue (sMsg, sMsg.startsWith ("SMP access to 'https://smp.example.org' suspended by circuit breaker ("));
    assertTrue (sMsg, sMsg.contains ("state OPEN since "));
    assertTrue (sMsg, sMsg.contains ("s remaining) after "));
    assertTrue (sMsg, sMsg.contains (" failures;"));
    assertTrue (sMsg, sMsg.contains ("last failure: SocketTimeoutException: Read timed out after 10000 ms"));

    // The "open since" timestamp is a UTC ISO-8601 timestamp of this test run
    final int nSinceIdx = sMsg.indexOf ("since ") + "since ".length ();
    final String sOpenSince = sMsg.substring (nSinceIdx, sMsg.indexOf (',', nSinceIdx));
    final OffsetDateTime aOpenSinceDT = OffsetDateTime.parse (sOpenSince);
    assertEquals (ZoneOffset.UTC, aOpenSinceDT.getOffset ());
    assertTrue ("Open since " + aOpenSinceDT + " is before the test started at " + aBeforeDT,
                !aOpenSinceDT.isBefore (aBeforeDT));
  }

  @Test
  public void testRejectionMessageWithoutAKnownCause ()
  {
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD, "2");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION, "1m");
    APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
    CircuitBreakerManager.removeAll ();

    for (int i = 0; i < 2; ++i)
    {
      assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
      CircuitBreakerManager.recordFailure (KEY);
    }

    final String sMsg = CircuitBreakerManager.getRejectionMessage (KEY, "Document forwarding");
    assertTrue (sMsg, sMsg.startsWith ("Document forwarding suspended by circuit breaker (state OPEN since "));
    assertFalse (sMsg, sMsg.contains ("last failure"));
  }

  @Test
  public void testTheLastFailureCauseIsTruncated ()
  {
    CircuitBreakerManager.removeAll ();
    CircuitBreakerManager.tryAcquirePermit (KEY);
    CircuitBreakerManager.recordFailure (KEY, new IllegalStateException ("x".repeat (500)));

    final String sMsg = CircuitBreakerManager.getRejectionMessage (KEY, "Something");
    final String sMarker = "last failure: ";
    assertTrue (sMsg, sMsg.contains (sMarker + "IllegalStateException: "));
    assertTrue (sMsg, sMsg.endsWith ("..."));

    // 200 characters plus the "..." marker
    final String sCause = sMsg.substring (sMsg.indexOf (sMarker) + sMarker.length ());
    assertEquals (203, sCause.length ());
  }

  @Test
  public void testGetAllInfos ()
  {
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD, "2");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION, "1m");
    APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
    CircuitBreakerManager.removeAll ();
    assertTrue (CircuitBreakerManager.getAllInfos ().isEmpty ());

    // A closed one
    final String sOtherKey = "unittest$closed";
    assertTrue (CircuitBreakerManager.tryAcquirePermit (sOtherKey));
    CircuitBreakerManager.recordSuccess (sOtherKey);

    // An open one
    for (int i = 0; i < 2; ++i)
    {
      assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
      CircuitBreakerManager.recordFailure (KEY, new SocketTimeoutException ("Read timed out"));
    }

    final var aInfos = CircuitBreakerManager.getAllInfos ();
    assertEquals (2, aInfos.size ());

    // Ordered by key - "unittest$closed" before "unittest$com..."
    final CircuitBreakerInfo aClosed = aInfos.get (0);
    assertEquals (sOtherKey, aClosed.circuitKey ());
    assertEquals (ECircuitBreakerState.CLOSED, aClosed.state ());
    assertFalse (aClosed.isOpen ());
    assertNull (aClosed.openSinceDT ());
    assertEquals (Duration.ZERO, aClosed.remainingDelay ());
    assertNull (aClosed.lastFailureCause ());

    final CircuitBreakerInfo aOpen = aInfos.get (1);
    assertEquals (KEY, aOpen.circuitKey ());
    assertEquals (ECircuitBreakerState.OPEN, aOpen.state ());
    assertTrue (aOpen.isOpen ());
    assertNotNull (aOpen.openSinceDT ());
    assertTrue (aOpen.remainingDelay ().toMillis () > 0);
    assertEquals (2, aOpen.failureCount ());
    assertEquals ("SocketTimeoutException: Read timed out", aOpen.lastFailureCause ());
  }

  @Test
  public void testReset ()
  {
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_THRESHOLD, "2");
    _setSystemProperty (APConfigurationProperties.CIRCUIT_BREAKER_OPEN_DURATION, "1m");
    APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
    CircuitBreakerManager.removeAll ();

    assertFalse ("An unknown key cannot be reset", CircuitBreakerManager.reset (KEY));

    for (int i = 0; i < 2; ++i)
    {
      assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
      CircuitBreakerManager.recordFailure (KEY, new SocketTimeoutException ("Read timed out"));
    }
    assertFalse (CircuitBreakerManager.tryAcquirePermit (KEY));

    assertTrue (CircuitBreakerManager.reset (KEY));

    // The next call creates a new, closed circuit breaker without any history
    assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
    CircuitBreakerManager.recordSuccess (KEY);
    final var aInfos = CircuitBreakerManager.getAllInfos ();
    assertEquals (1, aInfos.size ());
    assertEquals (ECircuitBreakerState.CLOSED, aInfos.get (0).state ());
    assertNull (aInfos.get (0).lastFailureCause ());
  }

  @Test
  public void testLeakedPermitInHalfOpenStateBlocksForever () throws InterruptedException
  {
    // This test documents the Failsafe behaviour that makes the "finally" blocks necessary: a
    // permit that is acquired in the half-open state and never recorded is never released again
    for (int i = 0; i < 2; ++i)
    {
      assertTrue (CircuitBreakerManager.tryAcquirePermit (KEY));
      CircuitBreakerManager.recordFailure (KEY);
    }

    // Acquire the single half-open permit and record nothing at all
    assertTrue (_awaitPermit (KEY, 5_000));

    // Waiting does not help anymore - the circuit breaker stays in the half-open state without a
    // free permit, so no further call is ever permitted
    if (_awaitPermit (KEY, 500))
      fail ("The circuit breaker unexpectedly granted a permit after a leaked one");
  }
}
