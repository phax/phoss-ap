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
package com.helger.phoss.ap.webapp.dto;

import java.util.List;

import com.helger.phoss.ap.db.MlsMetricsManagerJdbc;

/**
 * JSON response DTO representing an MLS SLA compliance report with individual measurement entries
 * and aggregated statistics.
 *
 * @author Philip Helger
 */
public class MlsSlaReportResponse
{
  private int totalCount;
  private int withinSlaCount;
  private double compliancePercent;
  private double targetPercent;
  private long thresholdSeconds;
  private boolean meetingSla;
  private List <MlsSlaEntryResponse> entries;

  /**
   * Default constructor for JSON deserialization.
   */
  public MlsSlaReportResponse ()
  {}

  /**
   * Create a response DTO from a domain model MLS SLA report.
   *
   * @param aReport
   *        The MLS SLA report. May not be <code>null</code>.
   * @return A new response DTO. Never <code>null</code>.
   */
  public static MlsSlaReportResponse fromDomain (final MlsMetricsManagerJdbc.MlsSlaReport aReport)
  {
    final MlsSlaReportResponse aResp = new MlsSlaReportResponse ();
    aResp.totalCount = aReport.totalCount ();
    aResp.withinSlaCount = aReport.withinSlaCount ();
    aResp.compliancePercent = aReport.compliancePercent ();
    aResp.targetPercent = aReport.targetPercent ();
    aResp.thresholdSeconds = aReport.thresholdSeconds ();
    aResp.meetingSla = aReport.isMeetingSla ();
    aResp.entries = aReport.entries ()
                           .getAllMapped (e -> new MlsSlaEntryResponse (e.sbdhInstanceID (),
                                                                        e.m1 ().toString (),
                                                                        e.m2OrM3 ().toString (),
                                                                        e.durationSeconds (),
                                                                        e.withinSla ()));
    return aResp;
  }

  /** @return the total number of measured transactions */
  public int getTotalCount ()
  {
    return totalCount;
  }

  /**
   * @param n
   *        The total count to set.
   */
  public void setTotalCount (final int n)
  {
    totalCount = n;
  }

  /** @return the number of transactions within the SLA threshold */
  public int getWithinSlaCount ()
  {
    return withinSlaCount;
  }

  /**
   * @param n
   *        The within-SLA count to set.
   */
  public void setWithinSlaCount (final int n)
  {
    withinSlaCount = n;
  }

  /** @return the SLA compliance percentage */
  public double getCompliancePercent ()
  {
    return compliancePercent;
  }

  /**
   * @param d
   *        The compliance percentage to set.
   */
  public void setCompliancePercent (final double d)
  {
    compliancePercent = d;
  }

  /** @return the target SLA percentage */
  public double getTargetPercent ()
  {
    return targetPercent;
  }

  /**
   * @param d
   *        The target percentage to set.
   */
  public void setTargetPercent (final double d)
  {
    targetPercent = d;
  }

  /** @return the SLA threshold in seconds */
  public long getThresholdSeconds ()
  {
    return thresholdSeconds;
  }

  /**
   * @param n
   *        The threshold in seconds to set.
   */
  public void setThresholdSeconds (final long n)
  {
    thresholdSeconds = n;
  }

  /** @return <code>true</code> if the SLA target is being met */
  public boolean isMeetingSla ()
  {
    return meetingSla;
  }

  /**
   * @param b
   *        <code>true</code> if meeting the SLA target.
   */
  public void setMeetingSla (final boolean b)
  {
    meetingSla = b;
  }

  /** @return the list of individual SLA measurement entries */
  public List <MlsSlaEntryResponse> getEntries ()
  {
    return entries;
  }

  /**
   * @param aEntries
   *        The SLA measurement entries to set.
   */
  public void setEntries (final List <MlsSlaEntryResponse> aEntries)
  {
    entries = aEntries;
  }

  /**
   * JSON response DTO for an individual MLS SLA measurement data point.
   *
   * @author Philip Helger
   */
  public static class MlsSlaEntryResponse
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
    public MlsSlaEntryResponse (final String sSbdhInstanceID,
                                final String sM1,
                                final String sM2OrM3,
                                final long nDurationSeconds,
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
}
