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
package com.helger.phoss.ap.core.helper;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.phoss.ap.basic.APBasicMetaManager;

/**
 * Utility class for computing exponential backoff retry timestamps.
 *
 * @author Philip Helger
 */
@Immutable
public final class BackoffCalculator
{
  private BackoffCalculator ()
  {}

  /**
   * Calculate the next retry timestamp using exponential backoff. The backoff duration is computed
   * as {@code nInitialBackoffMs * dMultiplier^(nAttemptCount-1)}, capped at {@code nMaxBackoffMs}.
   *
   * @param nAttemptCount
   *        The current attempt number (1-based). Must be &gt;= 0.
   * @param nInitialBackoffMs
   *        The initial backoff duration in milliseconds. Must be &gt;= 0.
   * @param dMultiplier
   *        The multiplier applied for each subsequent attempt. Must be &gt;= 0.
   * @param nMaxBackoffMs
   *        The maximum backoff duration in milliseconds. Must be &gt;= 0.
   * @return The calculated next retry timestamp. Never <code>null</code>.
   */
  @NonNull
  public static OffsetDateTime calculateNextRetry (@Nonnegative final int nAttemptCount,
                                                   @Nonnegative final long nInitialBackoffMs,
                                                   @Nonnegative final double dMultiplier,
                                                   @Nonnegative final long nMaxBackoffMs)
  {
    long nBackoffMs = nInitialBackoffMs;
    for (int i = 1; i < nAttemptCount; i++)
      nBackoffMs = (long) (nBackoffMs * dMultiplier);
    nBackoffMs = Math.min (nBackoffMs, nMaxBackoffMs);
    return APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ().plusNanos (nBackoffMs * 1_000_000L);
  }
}
