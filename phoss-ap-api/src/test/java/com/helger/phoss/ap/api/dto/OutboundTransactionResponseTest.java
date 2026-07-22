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
package com.helger.phoss.ap.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.model.IOutboundTransaction;

/**
 * Test class for class {@link OutboundTransactionResponse}.
 *
 * @author Philip Helger
 */
public final class OutboundTransactionResponseTest
{
  @Test
  public void testDefaultConstructor ()
  {
    final OutboundTransactionResponse a = new OutboundTransactionResponse ();
    assertNull (a.getID ());
    assertNull (a.getTransactionType ());
    assertNull (a.getSenderID ());
    assertNull (a.getReceiverID ());
    assertNull (a.getDocTypeID ());
    assertNull (a.getProcessID ());
    assertNull (a.getSbdhInstanceID ());
    assertNull (a.getStatus ());
    assertEquals (0, a.getAttemptCount ());
    assertNull (a.getCreatedDT ());
    assertNull (a.getCompletedDT ());
    assertNull (a.getReportingStatus ());
    assertNull (a.getNextRetryDT ());
    assertNull (a.getErrorDetails ());
    assertNull (a.getMlsStatus ());
    assertNull (a.getCustom1 ());
    assertNull (a.getCustom2 ());
    assertNull (a.getCustom3 ());
  }

  @Test
  public void testSettersAndGetters ()
  {
    final OutboundTransactionResponse a = new OutboundTransactionResponse ();
    a.setID ("id1");
    a.setTransactionType ("business_document");
    a.setSenderID ("sender1");
    a.setReceiverID ("receiver1");
    a.setDocTypeID ("docType1");
    a.setProcessID ("process1");
    a.setSbdhInstanceID ("sbdh-1");
    a.setStatus ("sent");
    a.setAttemptCount (2);
    a.setCreatedDT ("2026-03-29T10:00:00Z");
    a.setCompletedDT ("2026-03-29T10:01:00Z");
    a.setReportingStatus ("reported");
    a.setNextRetryDT ("2026-03-29T11:00:00Z");
    a.setErrorDetails ("some error");
    a.setMlsStatus ("received_ap");
    a.setCustom1 ("c1");
    a.setCustom2 ("c2");
    a.setCustom3 ("c3");

    assertEquals ("id1", a.getID ());
    assertEquals ("business_document", a.getTransactionType ());
    assertEquals ("sender1", a.getSenderID ());
    assertEquals ("receiver1", a.getReceiverID ());
    assertEquals ("docType1", a.getDocTypeID ());
    assertEquals ("process1", a.getProcessID ());
    assertEquals ("sbdh-1", a.getSbdhInstanceID ());
    assertEquals ("sent", a.getStatus ());
    assertEquals (2, a.getAttemptCount ());
    assertEquals ("2026-03-29T10:00:00Z", a.getCreatedDT ());
    assertEquals ("2026-03-29T10:01:00Z", a.getCompletedDT ());
    assertEquals ("reported", a.getReportingStatus ());
    assertEquals ("2026-03-29T11:00:00Z", a.getNextRetryDT ());
    assertEquals ("some error", a.getErrorDetails ());
    assertEquals ("received_ap", a.getMlsStatus ());
    assertEquals ("c1", a.getCustom1 ());
    assertEquals ("c2", a.getCustom2 ());
    assertEquals ("c3", a.getCustom3 ());
  }

  @Test
  public void testFromDomainAllFieldsPopulated ()
  {
    final OffsetDateTime aCreated = OffsetDateTime.of (2026, 3, 29, 10, 0, 0, 0, ZoneOffset.UTC);
    final OffsetDateTime aCompleted = aCreated.plusMinutes (5);
    final OffsetDateTime aNextRetry = aCreated.plusHours (1);

    final IOutboundTransaction aTx = _createMock ("tx-1",
                                                   ETransactionType.BUSINESS_DOCUMENT,
                                                   "sender-1",
                                                   "receiver-1",
                                                   "docType-1",
                                                   "process-1",
                                                   "sbdh-1",
                                                   ESourceType.PAYLOAD_ONLY,
                                                   "/path/doc.xml",
                                                   5678L,
                                                   "hash123",
                                                   "AT",
                                                   EOutboundStatus.SENT,
                                                   1,
                                                   aCreated,
                                                   aCompleted,
                                                   EReportingStatus.REPORTED,
                                                   aNextRetry,
                                                   "err details",
                                                   "mls-to-1",
                                                   EMlsReceptionStatus.RECEIVED_AP,
                                                   aCompleted,
                                                   "mls-id-1",
                                                   "mls-inbound-1",
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   "custom-a",
                                                   "custom-b",
                                                   "custom-c");

    final OutboundTransactionResponse aResp = OutboundTransactionResponse.fromDomain (aTx);
    assertNotNull (aResp);
    assertEquals ("tx-1", aResp.getID ());
    assertEquals ("business_document", aResp.getTransactionType ());
    assertEquals ("sender-1", aResp.getSenderID ());
    assertEquals ("receiver-1", aResp.getReceiverID ());
    assertEquals ("docType-1", aResp.getDocTypeID ());
    assertEquals ("process-1", aResp.getProcessID ());
    assertEquals ("sbdh-1", aResp.getSbdhInstanceID ());
    assertEquals ("sent", aResp.getStatus ());
    assertEquals (1, aResp.getAttemptCount ());
    assertNotNull (aResp.getCreatedDT ());
    assertNotNull (aResp.getCompletedDT ());
    assertEquals ("reported", aResp.getReportingStatus ());
    assertNotNull (aResp.getNextRetryDT ());
    assertEquals ("err details", aResp.getErrorDetails ());
    assertEquals ("received_ap", aResp.getMlsStatus ());
    assertEquals ("custom-a", aResp.getCustom1 ());
    assertEquals ("custom-b", aResp.getCustom2 ());
    assertEquals ("custom-c", aResp.getCustom3 ());
  }

  @Test
  public void testFromDomainNullableFieldsNull ()
  {
    final OffsetDateTime aCreated = OffsetDateTime.of (2026, 3, 29, 10, 0, 0, 0, ZoneOffset.UTC);

    final IOutboundTransaction aTx = _createMock ("tx-2",
                                                   ETransactionType.MLS_RESPONSE,
                                                   "sender-2",
                                                   "receiver-2",
                                                   "docType-2",
                                                   "process-2",
                                                   "sbdh-2",
                                                   ESourceType.PREBUILT_SBD,
                                                   "/path/doc2.xml",
                                                   0L,
                                                   "hash456",
                                                   "DE",
                                                   EOutboundStatus.PENDING,
                                                   0,
                                                   aCreated,
                                                   null,
                                                   EReportingStatus.PENDING,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null,
                                                   null);

    final OutboundTransactionResponse aResp = OutboundTransactionResponse.fromDomain (aTx);
    assertNotNull (aResp);
    assertEquals ("tx-2", aResp.getID ());
    assertEquals ("mls_response", aResp.getTransactionType ());
    assertEquals ("pending", aResp.getStatus ());
    assertNull (aResp.getCompletedDT ());
    assertNull (aResp.getNextRetryDT ());
    assertNull (aResp.getErrorDetails ());
    assertNull (aResp.getMlsStatus ());
    assertEquals ("pending", aResp.getReportingStatus ());
  }

  @NonNull
  private static IOutboundTransaction _createMock (@NonNull final String sID,
                                                    @NonNull final ETransactionType eTransactionType,
                                                    @NonNull final String sSenderID,
                                                    @NonNull final String sReceiverID,
                                                    @NonNull final String sDocTypeID,
                                                    @NonNull final String sProcessID,
                                                    @NonNull final String sSbdhInstanceID,
                                                    @NonNull final ESourceType eSourceType,
                                                    @NonNull final String sDocumentPath,
                                                    final long nDocumentSize,
                                                    @NonNull final String sDocumentHash,
                                                    @NonNull final String sC1CountryCode,
                                                    @NonNull final EOutboundStatus eStatus,
                                                    final int nAttemptCount,
                                                    @NonNull final OffsetDateTime aCreatedDT,
                                                    @Nullable final OffsetDateTime aCompletedDT,
                                                    @NonNull final EReportingStatus eReportingStatus,
                                                    @Nullable final OffsetDateTime aNextRetryDT,
                                                    @Nullable final String sErrorDetails,
                                                    @Nullable final String sMlsTo,
                                                    @Nullable final EMlsReceptionStatus eMlsStatus,
                                                    @Nullable final OffsetDateTime aMlsReceivedDT,
                                                    @Nullable final String sMlsID,
                                                    @Nullable final String sMlsInboundTransactionID,
                                                    @Nullable final String sSbdhStandard,
                                                    @Nullable final String sSbdhTypeVersion,
                                                    @Nullable final String sSbdhType,
                                                    @Nullable final String sPayloadMimeType,
                                                    @Nullable final String sCustom1,
                                                    @Nullable final String sCustom2,
                                                    @Nullable final String sCustom3)
  {
    return new IOutboundTransaction ()
    {
      public String getID () { return sID; }
      public ETransactionType getTransactionType () { return eTransactionType; }
      public String getSenderID () { return sSenderID; }
      public String getReceiverID () { return sReceiverID; }
      public String getDocTypeID () { return sDocTypeID; }
      public String getProcessID () { return sProcessID; }
      public String getSbdhInstanceID () { return sSbdhInstanceID; }
      public ESourceType getSourceType () { return eSourceType; }
      public String getDocumentPath () { return sDocumentPath; }
      public long getDocumentSize () { return nDocumentSize; }
      public String getDocumentHash () { return sDocumentHash; }
      public String getC1CountryCode () { return sC1CountryCode; }
      public EOutboundStatus getStatus () { return eStatus; }
      public int getAttemptCount () { return nAttemptCount; }
      public OffsetDateTime getCreatedDT () { return aCreatedDT; }
      public OffsetDateTime getCompletedDT () { return aCompletedDT; }
      public EReportingStatus getReportingStatus () { return eReportingStatus; }
      public OffsetDateTime getNextRetryDT () { return aNextRetryDT; }
      public String getErrorDetails () { return sErrorDetails; }
      public String getMlsTo () { return sMlsTo; }
      public EMlsReceptionStatus getMlsStatus () { return eMlsStatus; }
      public OffsetDateTime getMlsReceivedDT () { return aMlsReceivedDT; }
      public String getMlsID () { return sMlsID; }
      public String getMlsInboundTransactionID () { return sMlsInboundTransactionID; }
      public String getSbdhStandard () { return sSbdhStandard; }
      public String getSbdhTypeVersion () { return sSbdhTypeVersion; }
      public String getSbdhType () { return sSbdhType; }
      public String getPayloadMimeType () { return sPayloadMimeType; }
      public String getCustom1 () { return sCustom1; }
      public String getCustom2 () { return sCustom2; }
      public String getCustom3 () { return sCustom3; }
    };
  }
}
