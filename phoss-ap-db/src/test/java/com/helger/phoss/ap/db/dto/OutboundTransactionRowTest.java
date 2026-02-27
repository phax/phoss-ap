/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phoss.ap.db.dto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.db.testhelper.DBResultRowHelper;

/**
 * Test class for {@link OutboundTransactionRow}.
 *
 * @author Philip Helger
 */
public final class OutboundTransactionRowTest
{
  @NonNull
  private static DBResultRow _createValidRow ()
  {
    final OffsetDateTime aNow = OffsetDateTime.now ();
    final byte [] aDocBytes = { 1, 2, 3 };

    // 24 columns, matching OutboundTransactionRow constructor order
    // 0 id
    // 1 transactionType
    // 2 senderID
    // 3 receiverID
    // 4 docTypeID
    // 5 processID
    // 6 sbdhInstanceID
    // 7 sourceType
    // 8 documentBytes
    // 9 documentSize
    // 10 documentHash
    // 11 c1CountryCode
    // 12 status
    // 13 attemptCount
    // 14 createdDT
    // 15 completedDT (nullable)
    // 16 reportingStatus
    // 17 nextRetryDT (nullable)
    // 18 errorDetails (nullable)
    // 19 mlsTo (nullable)
    // 20 mlsStatus (nullable)
    // 21 mlsReceivedDT (nullable)
    // 22 mlsID (nullable)
    // 23 mlsInboundTransactionID (nullable)
    return DBResultRowHelper.createRow ("tx-001",
                                        "business_document",
                                        "iso6523-actorid-upis::sender",
                                        "iso6523-actorid-upis::recv",
                                        "busdox-docid-qns::inv",
                                        "cenbii-procid-ubl::proc",
                                        "sbdh-001",
                                        "raw_xml",
                                        aDocBytes,
                                        Long.valueOf (3L),
                                        "abc123hash",
                                        "DE",
                                        "pending",
                                        Integer.valueOf (0),
                                        aNow,
                                        null,
                                        "pending",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null);
  }

  @Test
  public void testAllMandatoryFieldsMappedCorrectly ()
  {
    final DBResultRow aRow = _createValidRow ();
    final OutboundTransactionRow aTx = new OutboundTransactionRow (aRow);

    assertEquals ("tx-001", aTx.getID ());
    assertEquals (ETransactionType.BUSINESS_DOCUMENT, aTx.getTransactionType ());
    assertEquals ("iso6523-actorid-upis::sender", aTx.getSenderID ());
    assertEquals ("iso6523-actorid-upis::recv", aTx.getReceiverID ());
    assertEquals ("busdox-docid-qns::inv", aTx.getDocTypeID ());
    assertEquals ("cenbii-procid-ubl::proc", aTx.getProcessID ());
    assertEquals ("sbdh-001", aTx.getSbdhInstanceID ());
    assertEquals (ESourceType.RAW_XML, aTx.getSourceType ());
    assertArrayEquals (new byte [] { 1, 2, 3 }, aTx.getDocumentBytes ());
    assertEquals (3L, aTx.getDocumentSize ());
    assertEquals ("abc123hash", aTx.getDocumentHash ());
    assertEquals ("DE", aTx.getC1CountryCode ());
    assertEquals (EOutboundStatus.PENDING, aTx.getStatus ());
    assertEquals (0, aTx.getAttemptCount ());
    assertNotNull (aTx.getCreatedDT ());
    assertEquals (EReportingStatus.PENDING, aTx.getReportingStatus ());
  }

  @Test
  public void testNullableFieldsReturnNull ()
  {
    final OutboundTransactionRow aTx = new OutboundTransactionRow (_createValidRow ());
    assertNull (aTx.getCompletedDT ());
    assertNull (aTx.getNextRetryDT ());
    assertNull (aTx.getErrorDetails ());
    assertNull (aTx.getMlsTo ());
    assertNull (aTx.getMlsStatus ());
    assertNull (aTx.getMlsReceivedDT ());
    assertNull (aTx.getMlsID ());
    assertNull (aTx.getMlsInboundTransactionID ());
  }

  @Test
  public void testAllFieldsPopulated ()
  {
    final OffsetDateTime aNow = OffsetDateTime.now ();
    final OffsetDateTime aCompleted = aNow.plusMinutes (5);
    final OffsetDateTime aMlsReceived = aNow.plusMinutes (10);
    final byte [] aDocBytes = { 10, 20 };

    final DBResultRow aRow = DBResultRowHelper.createRow ("tx-full",
                                                          "mls_response",
                                                          "sender-full",
                                                          "receiver-full",
                                                          "doctype-full",
                                                          "process-full",
                                                          "sbdh-full",
                                                          "prebuilt_sbd",
                                                          aDocBytes,
                                                          Long.valueOf (2L),
                                                          "hash-full",
                                                          "AT",
                                                          "sent",
                                                          Integer.valueOf (2),
                                                          aNow,
                                                          aCompleted,
                                                          "reported",
                                                          aNow.plusMinutes (1),
                                                          "some error",
                                                          "mls-to-value",
                                                          "received_ap",
                                                          aMlsReceived,
                                                          "mls-id-123",
                                                          "mls-inbound-tx-456");
    final OutboundTransactionRow aTx = new OutboundTransactionRow (aRow);

    assertEquals ("tx-full", aTx.getID ());
    assertEquals (ETransactionType.MLS_RESPONSE, aTx.getTransactionType ());
    assertEquals (ESourceType.PREBUILT_SBD, aTx.getSourceType ());
    assertEquals (EOutboundStatus.SENT, aTx.getStatus ());
    assertEquals (EReportingStatus.REPORTED, aTx.getReportingStatus ());
    assertEquals (2, aTx.getAttemptCount ());
    assertNotNull (aTx.getCompletedDT ());
    assertNotNull (aTx.getNextRetryDT ());
    assertEquals ("some error", aTx.getErrorDetails ());
    assertEquals ("mls-to-value", aTx.getMlsTo ());
    assertEquals (EMlsReceptionStatus.RECEIVED_AP, aTx.getMlsStatus ());
    assertNotNull (aTx.getMlsReceivedDT ());
    assertEquals ("mls-id-123", aTx.getMlsID ());
    assertEquals ("mls-inbound-tx-456", aTx.getMlsInboundTransactionID ());
  }
}
