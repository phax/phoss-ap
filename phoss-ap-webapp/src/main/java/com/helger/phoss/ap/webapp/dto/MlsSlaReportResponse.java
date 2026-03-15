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

public class MlsSlaReportResponse
{
  private int totalCount;
  private int withinSlaCount;
  private double compliancePercent;
  private double targetPercent;
  private long thresholdSeconds;
  private boolean meetingSla;
  private List <MlsSlaEntryResponse> entries;

  public MlsSlaReportResponse ()
  {}

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

  public int getTotalCount ()
  {
    return totalCount;
  }

  public void setTotalCount (final int n)
  {
    totalCount = n;
  }

  public int getWithinSlaCount ()
  {
    return withinSlaCount;
  }

  public void setWithinSlaCount (final int n)
  {
    withinSlaCount = n;
  }

  public double getCompliancePercent ()
  {
    return compliancePercent;
  }

  public void setCompliancePercent (final double d)
  {
    compliancePercent = d;
  }

  public double getTargetPercent ()
  {
    return targetPercent;
  }

  public void setTargetPercent (final double d)
  {
    targetPercent = d;
  }

  public long getThresholdSeconds ()
  {
    return thresholdSeconds;
  }

  public void setThresholdSeconds (final long n)
  {
    thresholdSeconds = n;
  }

  public boolean isMeetingSla ()
  {
    return meetingSla;
  }

  public void setMeetingSla (final boolean b)
  {
    meetingSla = b;
  }

  public List <MlsSlaEntryResponse> getEntries ()
  {
    return entries;
  }

  public void setEntries (final List <MlsSlaEntryResponse> aEntries)
  {
    entries = aEntries;
  }

  public static class MlsSlaEntryResponse
  {
    private String sbdhInstanceID;
    private String m1;
    private String m2OrM3;
    private long durationSeconds;
    private boolean withinSla;

    public MlsSlaEntryResponse ()
    {}

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

    public String getSbdhInstanceID ()
    {
      return sbdhInstanceID;
    }

    public void setSbdhInstanceID (final String s)
    {
      sbdhInstanceID = s;
    }

    public String getM1 ()
    {
      return m1;
    }

    public void setM1 (final String s)
    {
      m1 = s;
    }

    public String getM2OrM3 ()
    {
      return m2OrM3;
    }

    public void setM2OrM3 (final String s)
    {
      m2OrM3 = s;
    }

    public long getDurationSeconds ()
    {
      return durationSeconds;
    }

    public void setDurationSeconds (final long n)
    {
      durationSeconds = n;
    }

    public boolean isWithinSla ()
    {
      return withinSla;
    }

    public void setWithinSla (final boolean b)
    {
      withinSla = b;
    }
  }
}
