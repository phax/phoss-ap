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
package com.helger.phoss.ap.testsender.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.helger.phoss.ap.testsender.config.TestSenderConfig;
import com.helger.phoss.ap.testsender.scenario.ITestScenario;
import com.helger.phoss.ap.testsender.sender.BulkSendResult;
import com.helger.phoss.ap.testsender.sender.DocumentSender;
import com.helger.phoss.ap.testsender.sender.SendResult;

/**
 * Runs multiple document scenarios concurrently to test throughput and discover race conditions.
 */
@Component
public class BulkRunner
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BulkRunner.class);

  private final DocumentSender m_aSender;
  private final TestSenderConfig m_aConfig;

  /**
   * Constructor for the bulk runner.
   *
   * @param aSender
   *        the document sender to use. May not be {@code null}.
   * @param aConfig
   *        the test sender configuration. May not be {@code null}.
   */
  public BulkRunner (@NonNull final DocumentSender aSender, @NonNull final TestSenderConfig aConfig)
  {
    m_aSender = aSender;
    m_aConfig = aConfig;
  }

  /**
   * Run all configured scenarios concurrently and collect aggregated results.
   *
   * @param aScenarios
   *        the list of test scenarios to execute. May not be {@code null}.
   * @return the aggregated bulk send result. Never {@code null}.
   */
  @NonNull
  public BulkSendResult run (@NonNull final List <ITestScenario> aScenarios)
  {
    final TestSenderConfig.Bulk aBulkConfig = m_aConfig.getBulk ();
    final int nTotal = aBulkConfig.getCount ();
    final int nThreads = aBulkConfig.getThreads ();
    final long nRampUpMs = aBulkConfig.getRampUpMs ();

    LOGGER.info ("Starting bulk send: " +
                 nTotal +
                 " documents, " +
                 nThreads +
                 " threads, " +
                 nRampUpMs +
                 " ms ramp-up");

    final ExecutorService aExecutor = Executors.newFixedThreadPool (nThreads);
    final AtomicInteger aCompletedCount = new AtomicInteger (0);
    final long nDelayPerDoc = nTotal > 1 && nRampUpMs > 0 ? nRampUpMs / (nTotal - 1) : 0;

    final List <CompletableFuture <SendResult>> aFutures = new ArrayList <> (nTotal);
    final long nOverallStart = System.nanoTime ();

    for (int i = 0; i < nTotal; i++)
    {
      final ITestScenario aScenario = aScenarios.get (i % aScenarios.size ());
      final int nIteration = i;

      // Stagger submissions if ramp-up is configured
      if (nDelayPerDoc > 0 && i > 0)
      {
        try
        {
          Thread.sleep (nDelayPerDoc);
        }
        catch (final InterruptedException ex)
        {
          Thread.currentThread ().interrupt ();
          break;
        }
      }

      final CompletableFuture <SendResult> aFuture = CompletableFuture.supplyAsync ( () -> {
        final SendResult aResult = aScenario.execute (m_aSender, nIteration);
        final int nDone = aCompletedCount.incrementAndGet ();
        if (nDone % 10 == 0 || nDone == nTotal)
          LOGGER.info ("Progress: " + nDone + "/" + nTotal + " completed");
        return aResult;
      }, aExecutor);

      aFutures.add (aFuture);
    }

    // Wait for all futures
    final List <SendResult> aResults = new ArrayList <> (nTotal);
    for (final CompletableFuture <SendResult> aFuture : aFutures)
    {
      try
      {
        aResults.add (aFuture.join ());
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Unexpected error in bulk send", ex);
      }
    }

    final long nOverallDurationMs = (System.nanoTime () - nOverallStart) / 1_000_000;

    aExecutor.shutdown ();

    // Optionally poll for final statuses to detect stuck transactions
    if (m_aConfig.getPoll ().isEnabled ())
    {
      LOGGER.info ("Polling final statuses for " +
                   aResults.stream ().filter (SendResult::isSuccess).count () +
                   " successful sends...");
      _pollAllStatuses (aResults);
    }

    return new BulkSendResult (aResults, nOverallDurationMs);
  }

  private void _pollAllStatuses (final List <SendResult> aResults)
  {
    int nStuck = 0;
    int nPolled = 0;
    for (final SendResult aResult : aResults)
    {
      if (aResult.isSuccess ())
      {
        final String sStatus = m_aSender.pollStatus (aResult.getSbdhInstanceID (),
                                                     m_aConfig.getPoll ().getTimeoutMs (),
                                                     m_aConfig.getPoll ().getIntervalMs ());
        nPolled++;
        if (sStatus == null)
        {
          nStuck++;
          LOGGER.warn ("Transaction '" + aResult.getSbdhInstanceID () + "' stuck in non-terminal state");
        }
      }
    }
    if (nStuck > 0)
      LOGGER.warn (nStuck + "/" + nPolled + " transactions stuck in non-terminal state (potential race condition)");
    else
      if (nPolled > 0)
        LOGGER.info ("All " + nPolled + " polled transactions reached terminal state");
  }
}
