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
package com.helger.phoss.ap.testbackend.model;

import java.time.OffsetDateTime;

/**
 * Immutable model representing a document received by the test backend via any
 * forwarding channel (HTTP, SFTP, S3).
 *
 * @author Philip Helger
 */
public class ReceivedDocument
{
  private final String m_sID;
  private final String m_sChannel;
  private final String m_sFilename;
  private final long m_nSizeBytes;
  private final OffsetDateTime m_aReceivedDT;
  private final String m_sStoragePath;
  private boolean m_bCallbackSent;

  /**
   * Constructor for a received document.
   *
   * @param sID
   *        The unique document ID.
   * @param sChannel
   *        The forwarding channel name (e.g. "http-sync", "sftp", "s3").
   * @param sFilename
   *        The original filename.
   * @param nSizeBytes
   *        The document size in bytes.
   * @param aReceivedDT
   *        The date and time the document was received.
   * @param sStoragePath
   *        The absolute path where the document is stored on disk.
   */
  public ReceivedDocument (final String sID,
                           final String sChannel,
                           final String sFilename,
                           final long nSizeBytes,
                           final OffsetDateTime aReceivedDT,
                           final String sStoragePath)
  {
    m_sID = sID;
    m_sChannel = sChannel;
    m_sFilename = sFilename;
    m_nSizeBytes = nSizeBytes;
    m_aReceivedDT = aReceivedDT;
    m_sStoragePath = sStoragePath;
  }

  /** @return the unique document ID */
  public String getID ()
  {
    return m_sID;
  }

  /** @return the forwarding channel name */
  public String getChannel ()
  {
    return m_sChannel;
  }

  /** @return the original filename */
  public String getFilename ()
  {
    return m_sFilename;
  }

  /** @return the document size in bytes */
  public long getSizeBytes ()
  {
    return m_nSizeBytes;
  }

  /** @return the date and time the document was received */
  public OffsetDateTime getReceivedDT ()
  {
    return m_aReceivedDT;
  }

  /** @return the absolute storage path on disk */
  public String getStoragePath ()
  {
    return m_sStoragePath;
  }

  /** @return {@code true} if the C4 callback has been sent for this document */
  public boolean isCallbackSent ()
  {
    return m_bCallbackSent;
  }

  /**
   * Set whether the C4 callback has been sent for this document.
   *
   * @param bCallbackSent
   *        {@code true} if the callback was sent.
   */
  public void setCallbackSent (final boolean bCallbackSent)
  {
    m_bCallbackSent = bCallbackSent;
  }
}
