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
package com.helger.phoss.ap.testsender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties (prefix = "testsender")
public class TestSenderConfig
{
  private final Target target = new Target ();
  private final Http http = new Http ();
  private final Peppol peppol = new Peppol ();
  private final Pdf pdf = new Pdf ();
  private final Samples samples = new Samples ();
  private final Bulk bulk = new Bulk ();
  private final Poll poll = new Poll ();
  private final Output output = new Output ();

  public Target getTarget ()
  {
    return target;
  }

  public Http getHttp ()
  {
    return http;
  }

  public Peppol getPeppol ()
  {
    return peppol;
  }

  public Pdf getPdf ()
  {
    return pdf;
  }

  public Samples getSamples ()
  {
    return samples;
  }

  public Bulk getBulk ()
  {
    return bulk;
  }

  public Poll getPoll ()
  {
    return poll;
  }

  public Output getOutput ()
  {
    return output;
  }

  public static class Target
  {
    private String m_sBaseUrl = "http://localhost:8080";
    private String m_sToken;

    public String getBaseUrl ()
    {
      return m_sBaseUrl;
    }

    public void setBaseUrl (final String sBaseUrl)
    {
      m_sBaseUrl = sBaseUrl;
    }

    public String getToken ()
    {
      return m_sToken;
    }

    public void setToken (final String sToken)
    {
      m_sToken = sToken;
    }
  }

  public static class Http
  {
    private int m_nConnectTimeoutMs = 5000;
    private int m_nReadTimeoutMs = 60_000;

    public int getConnectTimeoutMs ()
    {
      return m_nConnectTimeoutMs;
    }

    public void setConnectTimeoutMs (final int nConnectTimeoutMs)
    {
      m_nConnectTimeoutMs = nConnectTimeoutMs;
    }

    public int getReadTimeoutMs ()
    {
      return m_nReadTimeoutMs;
    }

    public void setReadTimeoutMs (final int nReadTimeoutMs)
    {
      m_nReadTimeoutMs = nReadTimeoutMs;
    }
  }

  public static class Peppol
  {
    private String m_sSenderID;
    private String m_sReceiverID;
    private String m_sDocTypeID;
    private String m_sProcessID;
    private String m_sC1CountryCode = "AT";

    public String getSenderID ()
    {
      return m_sSenderID;
    }

    public void setSenderID (final String sSenderID)
    {
      m_sSenderID = sSenderID;
    }

    public String getReceiverID ()
    {
      return m_sReceiverID;
    }

    public void setReceiverID (final String sReceiverID)
    {
      m_sReceiverID = sReceiverID;
    }

    public String getDocTypeID ()
    {
      return m_sDocTypeID;
    }

    public void setDocTypeID (final String sDocTypeID)
    {
      m_sDocTypeID = sDocTypeID;
    }

    public String getProcessID ()
    {
      return m_sProcessID;
    }

    public void setProcessID (final String sProcessID)
    {
      m_sProcessID = sProcessID;
    }

    public String getC1CountryCode ()
    {
      return m_sC1CountryCode;
    }

    public void setC1CountryCode (final String sC1CountryCode)
    {
      m_sC1CountryCode = sC1CountryCode;
    }
  }

  public static class Pdf
  {
    private String m_sSbdhStandard;
    private String m_sSbdhTypeVersion;
    private String m_sSbdhType;

    public String getSbdhStandard ()
    {
      return m_sSbdhStandard;
    }

    public void setSbdhStandard (final String sSbdhStandard)
    {
      m_sSbdhStandard = sSbdhStandard;
    }

    public String getSbdhTypeVersion ()
    {
      return m_sSbdhTypeVersion;
    }

    public void setSbdhTypeVersion (final String sSbdhTypeVersion)
    {
      m_sSbdhTypeVersion = sSbdhTypeVersion;
    }

    public String getSbdhType ()
    {
      return m_sSbdhType;
    }

    public void setSbdhType (final String sSbdhType)
    {
      m_sSbdhType = sSbdhType;
    }
  }

  public static class Samples
  {
    private String m_sXml;
    private String m_sSbd;
    private String m_sPdf;

    public String getXml ()
    {
      return m_sXml;
    }

    public void setXml (final String sXml)
    {
      m_sXml = sXml;
    }

    public String getSbd ()
    {
      return m_sSbd;
    }

    public void setSbd (final String sSbd)
    {
      m_sSbd = sSbd;
    }

    public String getPdf ()
    {
      return m_sPdf;
    }

    public void setPdf (final String sPdf)
    {
      m_sPdf = sPdf;
    }
  }

  public static class Bulk
  {
    private boolean m_bEnabled = false;
    private int m_nThreads = 10;
    private int m_nCount = 100;
    private long m_nRampUpMs = 0;
    private String m_sDocumentTypes = "xml";
    private String m_sMixRatio = "100";

    public boolean isEnabled ()
    {
      return m_bEnabled;
    }

    public void setEnabled (final boolean bEnabled)
    {
      m_bEnabled = bEnabled;
    }

    public int getThreads ()
    {
      return m_nThreads;
    }

    public void setThreads (final int nThreads)
    {
      m_nThreads = nThreads;
    }

    public int getCount ()
    {
      return m_nCount;
    }

    public void setCount (final int nCount)
    {
      m_nCount = nCount;
    }

    public long getRampUpMs ()
    {
      return m_nRampUpMs;
    }

    public void setRampUpMs (final long nRampUpMs)
    {
      m_nRampUpMs = nRampUpMs;
    }

    public String getDocumentTypes ()
    {
      return m_sDocumentTypes;
    }

    public void setDocumentTypes (final String sDocumentTypes)
    {
      m_sDocumentTypes = sDocumentTypes;
    }

    public String getMixRatio ()
    {
      return m_sMixRatio;
    }

    public void setMixRatio (final String sMixRatio)
    {
      m_sMixRatio = sMixRatio;
    }
  }

  public static class Poll
  {
    private boolean m_bEnabled = true;
    private long m_nTimeoutMs = 30_000;
    private long m_nIntervalMs = 1_000;

    public boolean isEnabled ()
    {
      return m_bEnabled;
    }

    public void setEnabled (final boolean bEnabled)
    {
      m_bEnabled = bEnabled;
    }

    public long getTimeoutMs ()
    {
      return m_nTimeoutMs;
    }

    public void setTimeoutMs (final long nTimeoutMs)
    {
      m_nTimeoutMs = nTimeoutMs;
    }

    public long getIntervalMs ()
    {
      return m_nIntervalMs;
    }

    public void setIntervalMs (final long nIntervalMs)
    {
      m_nIntervalMs = nIntervalMs;
    }
  }

  public static class Output
  {
    private String m_sFile;
    private boolean m_bVerbose = false;

    public String getFile ()
    {
      return m_sFile;
    }

    public void setFile (final String sFile)
    {
      m_sFile = sFile;
    }

    public boolean isVerbose ()
    {
      return m_bVerbose;
    }

    public void setVerbose (final boolean bVerbose)
    {
      m_bVerbose = bVerbose;
    }
  }
}
