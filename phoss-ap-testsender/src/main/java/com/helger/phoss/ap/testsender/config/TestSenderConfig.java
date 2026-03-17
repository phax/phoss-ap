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

  /** @return the target server configuration */
  public Target getTarget ()
  {
    return target;
  }

  /** @return the HTTP client configuration */
  public Http getHttp ()
  {
    return http;
  }

  /** @return the Peppol participant and document configuration */
  public Peppol getPeppol ()
  {
    return peppol;
  }

  /** @return the PDF-specific SBDH configuration */
  public Pdf getPdf ()
  {
    return pdf;
  }

  /** @return the sample document file paths configuration */
  public Samples getSamples ()
  {
    return samples;
  }

  /** @return the bulk send configuration */
  public Bulk getBulk ()
  {
    return bulk;
  }

  /** @return the status polling configuration */
  public Poll getPoll ()
  {
    return poll;
  }

  /** @return the output configuration */
  public Output getOutput ()
  {
    return output;
  }

  public static class Target
  {
    private String m_sBaseUrl = "http://localhost:8080";
    private String m_sToken;

    /** @return the base URL of the target server */
    public String getBaseUrl ()
    {
      return m_sBaseUrl;
    }

    /**
     * @param sBaseUrl
     *        the base URL of the target server
     */
    public void setBaseUrl (final String sBaseUrl)
    {
      m_sBaseUrl = sBaseUrl;
    }

    /** @return the authentication token */
    public String getToken ()
    {
      return m_sToken;
    }

    /**
     * @param sToken
     *        the authentication token
     */
    public void setToken (final String sToken)
    {
      m_sToken = sToken;
    }
  }

  public static class Http
  {
    private int m_nConnectTimeoutMs = 5000;
    private int m_nReadTimeoutMs = 60_000;

    /** @return the connection timeout in milliseconds */
    public int getConnectTimeoutMs ()
    {
      return m_nConnectTimeoutMs;
    }

    /**
     * @param nConnectTimeoutMs
     *        the connection timeout in milliseconds
     */
    public void setConnectTimeoutMs (final int nConnectTimeoutMs)
    {
      m_nConnectTimeoutMs = nConnectTimeoutMs;
    }

    /** @return the read timeout in milliseconds */
    public int getReadTimeoutMs ()
    {
      return m_nReadTimeoutMs;
    }

    /**
     * @param nReadTimeoutMs
     *        the read timeout in milliseconds
     */
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

    /** @return the Peppol sender participant ID */
    public String getSenderID ()
    {
      return m_sSenderID;
    }

    /**
     * @param sSenderID
     *        the Peppol sender participant ID
     */
    public void setSenderID (final String sSenderID)
    {
      m_sSenderID = sSenderID;
    }

    /** @return the Peppol receiver participant ID */
    public String getReceiverID ()
    {
      return m_sReceiverID;
    }

    /**
     * @param sReceiverID
     *        the Peppol receiver participant ID
     */
    public void setReceiverID (final String sReceiverID)
    {
      m_sReceiverID = sReceiverID;
    }

    /** @return the Peppol document type ID */
    public String getDocTypeID ()
    {
      return m_sDocTypeID;
    }

    /**
     * @param sDocTypeID
     *        the Peppol document type ID
     */
    public void setDocTypeID (final String sDocTypeID)
    {
      m_sDocTypeID = sDocTypeID;
    }

    /** @return the Peppol process ID */
    public String getProcessID ()
    {
      return m_sProcessID;
    }

    /**
     * @param sProcessID
     *        the Peppol process ID
     */
    public void setProcessID (final String sProcessID)
    {
      m_sProcessID = sProcessID;
    }

    /** @return the C1 country code */
    public String getC1CountryCode ()
    {
      return m_sC1CountryCode;
    }

    /**
     * @param sC1CountryCode
     *        the C1 country code
     */
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

    /** @return the SBDH standard identifier for PDF documents */
    public String getSbdhStandard ()
    {
      return m_sSbdhStandard;
    }

    /**
     * @param sSbdhStandard
     *        the SBDH standard identifier for PDF documents
     */
    public void setSbdhStandard (final String sSbdhStandard)
    {
      m_sSbdhStandard = sSbdhStandard;
    }

    /** @return the SBDH type version for PDF documents */
    public String getSbdhTypeVersion ()
    {
      return m_sSbdhTypeVersion;
    }

    /**
     * @param sSbdhTypeVersion
     *        the SBDH type version for PDF documents
     */
    public void setSbdhTypeVersion (final String sSbdhTypeVersion)
    {
      m_sSbdhTypeVersion = sSbdhTypeVersion;
    }

    /** @return the SBDH type for PDF documents */
    public String getSbdhType ()
    {
      return m_sSbdhType;
    }

    /**
     * @param sSbdhType
     *        the SBDH type for PDF documents
     */
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

    /** @return the path to the sample XML document */
    public String getXml ()
    {
      return m_sXml;
    }

    /**
     * @param sXml
     *        the path to the sample XML document
     */
    public void setXml (final String sXml)
    {
      m_sXml = sXml;
    }

    /** @return the path to the sample SBD document */
    public String getSbd ()
    {
      return m_sSbd;
    }

    /**
     * @param sSbd
     *        the path to the sample SBD document
     */
    public void setSbd (final String sSbd)
    {
      m_sSbd = sSbd;
    }

    /** @return the path to the sample PDF document */
    public String getPdf ()
    {
      return m_sPdf;
    }

    /**
     * @param sPdf
     *        the path to the sample PDF document
     */
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

    /** @return whether bulk mode is enabled */
    public boolean isEnabled ()
    {
      return m_bEnabled;
    }

    /**
     * @param bEnabled
     *        whether bulk mode is enabled
     */
    public void setEnabled (final boolean bEnabled)
    {
      m_bEnabled = bEnabled;
    }

    /** @return the number of concurrent threads */
    public int getThreads ()
    {
      return m_nThreads;
    }

    /**
     * @param nThreads
     *        the number of concurrent threads
     */
    public void setThreads (final int nThreads)
    {
      m_nThreads = nThreads;
    }

    /** @return the total number of documents to send */
    public int getCount ()
    {
      return m_nCount;
    }

    /**
     * @param nCount
     *        the total number of documents to send
     */
    public void setCount (final int nCount)
    {
      m_nCount = nCount;
    }

    /** @return the ramp-up time in milliseconds */
    public long getRampUpMs ()
    {
      return m_nRampUpMs;
    }

    /**
     * @param nRampUpMs
     *        the ramp-up time in milliseconds
     */
    public void setRampUpMs (final long nRampUpMs)
    {
      m_nRampUpMs = nRampUpMs;
    }

    /** @return the comma-separated document types to send */
    public String getDocumentTypes ()
    {
      return m_sDocumentTypes;
    }

    /**
     * @param sDocumentTypes
     *        the comma-separated document types to send
     */
    public void setDocumentTypes (final String sDocumentTypes)
    {
      m_sDocumentTypes = sDocumentTypes;
    }

    /** @return the comma-separated mix ratio for document types */
    public String getMixRatio ()
    {
      return m_sMixRatio;
    }

    /**
     * @param sMixRatio
     *        the comma-separated mix ratio for document types
     */
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

    /** @return whether status polling is enabled */
    public boolean isEnabled ()
    {
      return m_bEnabled;
    }

    /**
     * @param bEnabled
     *        whether status polling is enabled
     */
    public void setEnabled (final boolean bEnabled)
    {
      m_bEnabled = bEnabled;
    }

    /** @return the polling timeout in milliseconds */
    public long getTimeoutMs ()
    {
      return m_nTimeoutMs;
    }

    /**
     * @param nTimeoutMs
     *        the polling timeout in milliseconds
     */
    public void setTimeoutMs (final long nTimeoutMs)
    {
      m_nTimeoutMs = nTimeoutMs;
    }

    /** @return the polling interval in milliseconds */
    public long getIntervalMs ()
    {
      return m_nIntervalMs;
    }

    /**
     * @param nIntervalMs
     *        the polling interval in milliseconds
     */
    public void setIntervalMs (final long nIntervalMs)
    {
      m_nIntervalMs = nIntervalMs;
    }
  }

  public static class Output
  {
    private String m_sFile;
    private boolean m_bVerbose = false;

    /** @return the output file path */
    public String getFile ()
    {
      return m_sFile;
    }

    /**
     * @param sFile
     *        the output file path
     */
    public void setFile (final String sFile)
    {
      m_sFile = sFile;
    }

    /** @return whether verbose output is enabled */
    public boolean isVerbose ()
    {
      return m_bVerbose;
    }

    /**
     * @param bVerbose
     *        whether verbose output is enabled
     */
    public void setVerbose (final boolean bVerbose)
    {
      m_bVerbose = bVerbose;
    }
  }
}
