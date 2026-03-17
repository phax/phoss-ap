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
package com.helger.phoss.ap.testsender.sender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated result of a bulk send operation.
 */
public class BulkSendResult
{
  private final List <SendResult> m_aResults;
  private final long m_nOverallDurationMs;

  /**
   * Construct a new aggregated bulk send result.
   *
   * @param aResults
   *        the list of individual send results
   * @param nOverallDurationMs
   *        the total wall-clock duration in milliseconds
   */
  public BulkSendResult (final List <SendResult> aResults, final long nOverallDurationMs)
  {
    m_aResults = List.copyOf (aResults);
    m_nOverallDurationMs = nOverallDurationMs;
  }

  /** @return all individual send results */
  public List <SendResult> getAllResults ()
  {
    return m_aResults;
  }

  /** @return the total wall-clock duration in milliseconds */
  public long getOverallDurationMs ()
  {
    return m_nOverallDurationMs;
  }

  /** @return the total number of send attempts */
  public int getTotalCount ()
  {
    return m_aResults.size ();
  }

  /** @return the number of successful sends */
  public long getSuccessCount ()
  {
    return m_aResults.stream ().filter (SendResult::isSuccess).count ();
  }

  /** @return the number of failed sends */
  public long getFailureCount ()
  {
    return m_aResults.stream ().filter (SendResult::isFailure).count ();
  }

  /** @return the throughput in documents per second */
  public double getThroughputPerSecond ()
  {
    if (m_nOverallDurationMs <= 0)
      return 0;
    return m_aResults.size () * 1000.0 / m_nOverallDurationMs;
  }

  /** @return the minimum individual send duration in milliseconds */
  public long getMinDurationMs ()
  {
    return m_aResults.stream ().mapToLong (SendResult::getDurationMs).min ().orElse (0);
  }

  /** @return the maximum individual send duration in milliseconds */
  public long getMaxDurationMs ()
  {
    return m_aResults.stream ().mapToLong (SendResult::getDurationMs).max ().orElse (0);
  }

  /** @return the average individual send duration in milliseconds */
  public double getAvgDurationMs ()
  {
    return m_aResults.stream ().mapToLong (SendResult::getDurationMs).average ().orElse (0);
  }

  /** @return the 95th percentile send duration in milliseconds */
  public long getP95DurationMs ()
  {
    if (m_aResults.isEmpty ())
      return 0;
    final List <Long> aSorted = new ArrayList <> (m_aResults.stream ().map (SendResult::getDurationMs).toList ());
    Collections.sort (aSorted);
    final int nIndex = (int) Math.ceil (aSorted.size () * 0.95) - 1;
    return aSorted.get (Math.max (0, nIndex)).longValue ();
  }

  /** @return a map of error messages to their occurrence counts */
  public Map <String, Long> getErrorBreakdown ()
  {
    final Map <String, Long> aMap = new LinkedHashMap <> ();
    for (final SendResult r : m_aResults)
    {
      if (r.isFailure () && r.getErrorMessage () != null)
      {
        aMap.merge (r.getErrorMessage (), Long.valueOf (1), Long::sum);
      }
    }
    return aMap;
  }

  /** @return a map of document types to their send counts */
  public Map <String, Long> getCountByDocumentType ()
  {
    final Map <String, Long> aMap = new LinkedHashMap <> ();
    for (final SendResult r : m_aResults)
    {
      aMap.merge (r.getDocumentType (), Long.valueOf (1), Long::sum);
    }
    return aMap;
  }
}
