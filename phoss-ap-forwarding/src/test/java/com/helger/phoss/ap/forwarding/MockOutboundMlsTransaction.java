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
package com.helger.phoss.ap.forwarding;

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
 * An outbound MLS transaction as {@code MlsHandler} creates it - a bare {@code ApplicationResponse}
 * stored as {@link ESourceType#PAYLOAD_ONLY} - so that the adaptation to a forwardable MLS copy can
 * be verified field by field.
 *
 * @author Philip Helger
 */
public final class MockOutboundMlsTransaction implements IOutboundTransaction
{
  public static final String ID = "mls-tx-1";
  public static final String SBDH_INSTANCE_ID = "mls-sbdh-1";
  public static final String DOCUMENT_PATH = "/var/phoss-ap/outbound/mls-sbdh-1.mls";
  public static final long DOCUMENT_SIZE = 815;
  public static final String DOCTYPE_ID = "busdox-docid-qns::mls-doctype";
  public static final String PROCESS_ID = "cenbii-procid-ubl::urn:peppol:edec:mls";
  public static final String SENDER_ID = "iso6523-actorid-upis::0242:spis-sender";
  public static final String RECEIVER_ID = "iso6523-actorid-upis::0242:spis-receiver";
  public static final OffsetDateTime CREATED_DT = OffsetDateTime.parse ("2026-08-30T11:20:00+02:00");

  @NonNull
  public String getID ()
  {
    return ID;
  }

  @NonNull
  public ETransactionType getTransactionType ()
  {
    return ETransactionType.MLS_RESPONSE;
  }

  @NonNull
  public String getSenderID ()
  {
    return SENDER_ID;
  }

  @NonNull
  public String getReceiverID ()
  {
    return RECEIVER_ID;
  }

  @NonNull
  public String getDocTypeID ()
  {
    return DOCTYPE_ID;
  }

  @NonNull
  public String getProcessID ()
  {
    return PROCESS_ID;
  }

  @NonNull
  public String getSbdhInstanceID ()
  {
    return SBDH_INSTANCE_ID;
  }

  @NonNull
  public ESourceType getSourceType ()
  {
    return ESourceType.PAYLOAD_ONLY;
  }

  @NonNull
  public String getDocumentPath ()
  {
    return DOCUMENT_PATH;
  }

  public long getDocumentSize ()
  {
    return DOCUMENT_SIZE;
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
    return "tx-1";
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
    return null;
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
