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

import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.EVerificationResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;

/**
 * A fully populated inbound transaction for the forwarder tests, so that the adaptation to an
 * {@link com.helger.phoss.ap.api.model.IForwardableDocument} can be verified field by field.
 *
 * @author Philip Helger
 */
public final class MockInboundTransaction implements IInboundTransaction
{
  public static final String ID = "tx-1";
  public static final String INCOMING_ID = "incoming-1";
  public static final String SBDH_INSTANCE_ID = "sbdh-1";
  public static final String DOCUMENT_PATH = "/var/phoss-ap/inbound/tx-1.xml";
  public static final long DOCUMENT_SIZE = 4711;
  public static final String DOCTYPE_ID = "busdox-docid-qns::doctype";
  public static final String PROCESS_ID = "cenbii-procid-ubl::process";
  public static final String SENDER_ID = "iso6523-actorid-upis::9915:sender";
  public static final String RECEIVER_ID = "iso6523-actorid-upis::9915:receiver";
  public static final OffsetDateTime RECEIVED_DT = OffsetDateTime.parse ("2026-08-30T10:15:30+02:00");

  private final EVerificationResult m_eVerificationResult;
  private final String m_sVerificationDetails;

  public MockInboundTransaction (@Nullable final EVerificationResult eVerificationResult,
                                 @Nullable final String sVerificationDetails)
  {
    m_eVerificationResult = eVerificationResult;
    m_sVerificationDetails = sVerificationDetails;
  }

  @NonNull
  public String getID ()
  {
    return ID;
  }

  @NonNull
  public String getIncomingID ()
  {
    return INCOMING_ID;
  }

  @Nullable
  public String getC2SeatID ()
  {
    return "POP000306";
  }

  @Nullable
  public String getC3SeatID ()
  {
    return "POP000307";
  }

  @Nullable
  public String getSigningCertCN ()
  {
    return null;
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
  public String getDocumentPath ()
  {
    return DOCUMENT_PATH;
  }

  public long getDocumentSize ()
  {
    return DOCUMENT_SIZE;
  }

  @Nullable
  public String getDocumentHash ()
  {
    return null;
  }

  @Nullable
  public String getAS4MessageID ()
  {
    return "as4-1";
  }

  @Nullable
  public OffsetDateTime getAS4Timestamp ()
  {
    return RECEIVED_DT;
  }

  @NonNull
  public String getSbdhInstanceID ()
  {
    return SBDH_INSTANCE_ID;
  }

  @Nullable
  public String getC1CountryCode ()
  {
    return "AT";
  }

  @Nullable
  public String getC4CountryCode ()
  {
    return "AE";
  }

  public boolean isDuplicateAS4 ()
  {
    return false;
  }

  public boolean isDuplicateSBDH ()
  {
    return false;
  }

  @NonNull
  public EInboundStatus getStatus ()
  {
    return EInboundStatus.RECEIVED;
  }

  public int getAttemptCount ()
  {
    return 0;
  }

  @Nullable
  public OffsetDateTime getReceivedDT ()
  {
    return RECEIVED_DT;
  }

  @Nullable
  public OffsetDateTime getCompletedDT ()
  {
    return null;
  }

  @Nullable
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
  public EPeppolMLSType getMlsType ()
  {
    return null;
  }

  @Nullable
  public EPeppolMLSResponseCode getMlsResponseCode ()
  {
    return null;
  }

  @Nullable
  public String getMlsOutboundTransactionID ()
  {
    return null;
  }

  @Nullable
  public EVerificationResult getVerificationResult ()
  {
    return m_eVerificationResult;
  }

  @Nullable
  public String getVerificationDetails ()
  {
    return m_sVerificationDetails;
  }
}
