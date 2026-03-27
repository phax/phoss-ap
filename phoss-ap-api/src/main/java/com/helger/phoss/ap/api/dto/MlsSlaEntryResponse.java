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

import com.helger.annotation.Nonnegative;

/**
 * JSON response DTO for an individual MLS SLA measurement data point.
 *
 * @author Philip Helger
 */
public class MlsSlaEntryResponse
{
  private String sbdhInstanceID;
  private String m1;
  private String m2OrM3;
  private long durationSeconds;
  private boolean withinSla;

  /**
   * Default constructor for JSON deserialization.
   */
  public MlsSlaEntryResponse ()
  {}

  /**
   * Constructor with all fields.
   *
   * @param sSbdhInstanceID
   *        The SBDH instance ID.
   * @param sM1
   *        The M1 timestamp as a string.
   * @param sM2OrM3
   *        The M2 or M3 timestamp as a string.
   * @param nDurationSeconds
   *        The duration in seconds between the two timestamps.
   * @param bWithinSla
   *        <code>true</code> if the duration is within the SLA threshold.
   */
  public MlsSlaEntryResponse (@NonNull final String sSbdhInstanceID,
                              @NonNull final String sM1,
                              @NonNull final String sM2OrM3,
                              @Nonnegative final long nDurationSeconds,
                              final boolean bWithinSla)
  {
    sbdhInstanceID = sSbdhInstanceID;
    m1 = sM1;
    m2OrM3 = sM2OrM3;
    durationSeconds = nDurationSeconds;
    withinSla = bWithinSla;
  }

  /** @return the SBDH instance ID */
  public String getSbdhInstanceID ()
  {
    return sbdhInstanceID;
  }

  /**
   * @param s
   *        The SBDH instance ID to set.
   */
  public void setSbdhInstanceID (final String s)
  {
    sbdhInstanceID = s;
  }

  /** @return the M1 timestamp as a string */
  public String getM1 ()
  {
    return m1;
  }

  /**
   * @param s
   *        The M1 timestamp to set.
   */
  public void setM1 (final String s)
  {
    m1 = s;
  }

  /** @return the M2 or M3 timestamp as a string */
  public String getM2OrM3 ()
  {
    return m2OrM3;
  }

  /**
   * @param s
   *        The M2 or M3 timestamp to set.
   */
  public void setM2OrM3 (final String s)
  {
    m2OrM3 = s;
  }

  /** @return the duration in seconds between M1 and M2/M3 */
  public long getDurationSeconds ()
  {
    return durationSeconds;
  }

  /**
   * @param n
   *        The duration in seconds to set.
   */
  public void setDurationSeconds (final long n)
  {
    durationSeconds = n;
  }

  /** @return <code>true</code> if the duration is within the SLA threshold */
  public boolean isWithinSla ()
  {
    return withinSla;
  }

  /**
   * @param b
   *        <code>true</code> if within the SLA threshold.
   */
  public void setWithinSla (final boolean b)
  {
    withinSla = b;
  }
}
