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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Result of a single document send attempt.
 */
public class SendResult
{
  private final String m_sDocumentType;
  private final String m_sSbdhInstanceID;
  private final boolean m_bSuccess;
  private final int m_nHttpStatus;
  private final long m_nDurationMs;
  private final String m_sResponseBody;
  private final String m_sErrorMessage;

  private SendResult (final String sDocumentType,
                      final String sSbdhInstanceID,
                      final boolean bSuccess,
                      final int nHttpStatus,
                      final long nDurationMs,
                      @Nullable final String sResponseBody,
                      @Nullable final String sErrorMessage)
  {
    m_sDocumentType = sDocumentType;
    m_sSbdhInstanceID = sSbdhInstanceID;
    m_bSuccess = bSuccess;
    m_nHttpStatus = nHttpStatus;
    m_nDurationMs = nDurationMs;
    m_sResponseBody = sResponseBody;
    m_sErrorMessage = sErrorMessage;
  }

  /** @return the document type (e.g. "xml", "pdf", "sbd") */
  public String getDocumentType ()
  {
    return m_sDocumentType;
  }

  /** @return the SBDH instance ID */
  public String getSbdhInstanceID ()
  {
    return m_sSbdhInstanceID;
  }

  /** @return {@code true} if the send was successful */
  public boolean isSuccess ()
  {
    return m_bSuccess;
  }

  /** @return {@code true} if the send failed */
  public boolean isFailure ()
  {
    return !m_bSuccess;
  }

  /** @return the HTTP status code, or 0 if no HTTP response was received */
  public int getHttpStatus ()
  {
    return m_nHttpStatus;
  }

  /** @return the send duration in milliseconds */
  public long getDurationMs ()
  {
    return m_nDurationMs;
  }

  /** @return the HTTP response body, or {@code null} */
  @Nullable
  public String getResponseBody ()
  {
    return m_sResponseBody;
  }

  /** @return the error message, or {@code null} if successful */
  @Nullable
  public String getErrorMessage ()
  {
    return m_sErrorMessage;
  }

  /**
   * Create a successful send result.
   *
   * @param sDocumentType
   *        the document type
   * @param sSbdhInstanceID
   *        the SBDH instance ID
   * @param nHttpStatus
   *        the HTTP status code
   * @param nDurationMs
   *        the send duration in milliseconds
   * @param sResponseBody
   *        the HTTP response body, or {@code null}
   * @return a new successful {@link SendResult}. Never {@code null}.
   */
  @NonNull
  public static SendResult success (final String sDocumentType,
                                    final String sSbdhInstanceID,
                                    final int nHttpStatus,
                                    final long nDurationMs,
                                    @Nullable final String sResponseBody)
  {
    return new SendResult (sDocumentType, sSbdhInstanceID, true, nHttpStatus, nDurationMs, sResponseBody, null);
  }

  /**
   * Create a failed send result.
   *
   * @param sDocumentType
   *        the document type
   * @param sSbdhInstanceID
   *        the SBDH instance ID
   * @param nHttpStatus
   *        the HTTP status code, or 0 if no response was received
   * @param nDurationMs
   *        the send duration in milliseconds
   * @param sResponseBody
   *        the HTTP response body, or {@code null}
   * @param sErrorMessage
   *        the error message, or {@code null}
   * @return a new failed {@link SendResult}. Never {@code null}.
   */
  @NonNull
  public static SendResult failure (final String sDocumentType,
                                    final String sSbdhInstanceID,
                                    final int nHttpStatus,
                                    final long nDurationMs,
                                    @Nullable final String sResponseBody,
                                    @Nullable final String sErrorMessage)
  {
    return new SendResult (sDocumentType,
                           sSbdhInstanceID,
                           false,
                           nHttpStatus,
                           nDurationMs,
                           sResponseBody,
                           sErrorMessage);
  }
}
