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

  public String getDocumentType ()
  {
    return m_sDocumentType;
  }

  public String getSbdhInstanceID ()
  {
    return m_sSbdhInstanceID;
  }

  public boolean isSuccess ()
  {
    return m_bSuccess;
  }

  public boolean isFailure ()
  {
    return !m_bSuccess;
  }

  public int getHttpStatus ()
  {
    return m_nHttpStatus;
  }

  public long getDurationMs ()
  {
    return m_nDurationMs;
  }

  @Nullable
  public String getResponseBody ()
  {
    return m_sResponseBody;
  }

  @Nullable
  public String getErrorMessage ()
  {
    return m_sErrorMessage;
  }

  @NonNull
  public static SendResult success (final String sDocumentType,
                                    final String sSbdhInstanceID,
                                    final int nHttpStatus,
                                    final long nDurationMs,
                                    @Nullable final String sResponseBody)
  {
    return new SendResult (sDocumentType, sSbdhInstanceID, true, nHttpStatus, nDurationMs, sResponseBody, null);
  }

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
