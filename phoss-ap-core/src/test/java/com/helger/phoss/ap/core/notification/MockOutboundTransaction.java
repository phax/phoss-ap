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
package com.helger.phoss.ap.core.notification;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.model.IOutboundTransaction;

/**
 * A minimal outbound business document transaction for the notification handler tests.
 *
 * @author Philip Helger
 */
final class MockOutboundTransaction implements IOutboundTransaction
{
  private static final OffsetDateTime CREATED_DT = OffsetDateTime.parse ("2026-08-30T11:20:00+02:00");

  private final String m_sID;
  private final String m_sSbdhInstanceID;

  MockOutboundTransaction (@NonNull final String sID, @NonNull final String sSbdhInstanceID)
  {
    m_sID = sID;
    m_sSbdhInstanceID = sSbdhInstanceID;
  }

  @NonNull
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  public ETransactionType getTransactionType ()
  {
    return ETransactionType.BUSINESS_DOCUMENT;
  }

  @NonNull
  public String getSenderID ()
  {
    return "iso6523-actorid-upis::9915:sender";
  }

  @NonNull
  public String getReceiverID ()
  {
    return "iso6523-actorid-upis::9915:receiver";
  }

  @NonNull
  public String getDocTypeID ()
  {
    return "busdox-docid-qns::doctype";
  }

  @NonNull
  public String getProcessID ()
  {
    return "cenbii-procid-ubl::process";
  }

  @NonNull
  public String getSbdhInstanceID ()
  {
    return m_sSbdhInstanceID;
  }

  @NonNull
  public ESourceType getSourceType ()
  {
    return ESourceType.PAYLOAD_ONLY;
  }

  @NonNull
  public String getDocumentPath ()
  {
    return "/var/phoss-ap/outbound/" + m_sSbdhInstanceID + ".xml";
  }

  public long getDocumentSize ()
  {
    return 4711;
  }

  @NonNull
  public String getDocumentHash ()
  {
    return "0123456789abcdef";
  }

  @NonNull
  public String getC1CountryCode ()
  {
    return "AT";
  }

  @NonNull
  public EOutboundStatus getStatus ()
  {
    return EOutboundStatus.PENDING;
  }

  public int getAttemptCount ()
  {
    return 0;
  }

  @NonNull
  public OffsetDateTime getCreatedDT ()
  {
    return CREATED_DT;
  }

  @Nullable
  public OffsetDateTime getCompletedDT ()
  {
    return null;
  }

  @NonNull
  public EReportingStatus getReportingStatus ()
  {
    return EReportingStatus.PENDING;
  }

  @Nullable
  public OffsetDateTime getNextRetryDT ()
  {
    return null;
  }

  @Nullable
  public String getErrorDetails ()
  {
    return null;
  }

  @Nullable
  public String getMlsTo ()
  {
    return null;
  }

  @Nullable
  public EMlsReceptionStatus getMlsStatus ()
  {
    return null;
  }

  @Nullable
  public OffsetDateTime getMlsReceivedDT ()
  {
    return null;
  }

  @Nullable
  public String getMlsID ()
  {
    return null;
  }

  @Nullable
  public String getMlsInboundTransactionID ()
  {
    return null;
  }

  @Nullable
  public String getSbdhStandard ()
  {
    return null;
  }

  @Nullable
  public String getSbdhTypeVersion ()
  {
    return null;
  }

  @Nullable
  public String getSbdhType ()
  {
    return null;
  }

  @Nullable
  public String getPayloadMimeType ()
  {
    return null;
  }

  @Nullable
  public String getCustom1 ()
  {
    return "custom-1";
  }

  @Nullable
  public String getCustom2 ()
  {
    return null;
  }

  @Nullable
  public String getCustom3 ()
  {
    return null;
  }
}
