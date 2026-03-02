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

import org.jspecify.annotations.Nullable;

public class SubmitResponse
{
  private String transactionID;
  private String sbdhInstanceID;
  private String status;

  public SubmitResponse ()
  {}

  public SubmitResponse (@Nullable final String sTransactionID,
                         @Nullable final String sSbdhInstanceID,
                         @Nullable final String sStatus)
  {
    transactionID = sTransactionID;
    sbdhInstanceID = sSbdhInstanceID;
    status = sStatus;
  }

  public String getTransactionID ()
  {
    return transactionID;
  }

  public void setTransactionID (final String sTransactionID)
  {
    transactionID = sTransactionID;
  }

  public String getSbdhInstanceID ()
  {
    return sbdhInstanceID;
  }

  public void setSbdhInstanceID (final String sSbdhInstanceID)
  {
    sbdhInstanceID = sSbdhInstanceID;
  }

  public String getStatus ()
  {
    return status;
  }

  public void setStatus (final String sStatus)
  {
    status = sStatus;
  }
}
