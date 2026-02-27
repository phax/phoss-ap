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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.db.testhelper.DBResultRowHelper;

/**
 * Test class for {@link InboundTransactionRow}.
 *
 * @author Philip Helger
 */
public final class InboundTransactionRowTest
{
  @NonNull
  private static DBResultRow _createValidRow ()
  {
    final OffsetDateTime aNow = OffsetDateTime.now ();
    final byte [] aDocBytes = { 4, 5, 6 };

    // 29 columns, matching InboundTransactionRow constructor order
    // 0 id
    // 1 incomingID
    // 2 c2SeatID
    // 3 c3SeatID
    // 4 signingCertCN
    // 5 senderID
    // 6 receiverID
    // 7 docTypeID
    // 8 processID
    // 9 documentBytes
    // 10 documentSize
    // 11 documentHash
    // 12 as4MessageID
    // 13 as4Timestamp
    // 14 sbdhInstanceID
    // 15 c4CountryCode (nullable)
    // 16 isDuplicateAS4
    // 17 isDuplicateSBDH
    // 18 status
    // 19 attemptCount
    // 20 receivedDT
    // 21 completedDT (nullable)
    // 22 reportingStatus
    // 23 nextRetryDT (nullable)
    // 24 errorDetails (nullable)
    // 25 mlsTo (nullable)
    // 26 mlsType
    // 27 mlsResponseCode (nullable)
    // 28 mlsOutboundTransactionID (nullable)
    return DBResultRowHelper.createRow ("ib-001",
                                        "incoming-001",
                                        "POP000001",
                                        "POP000002",
                                        "CN=Test Cert",
                                        "iso6523-actorid-upis::sender",
                                        "iso6523-actorid-upis::recv",
                                        "busdox-docid-qns::inv",
                                        "cenbii-procid-ubl::proc",
                                        aDocBytes,
                                        Long.valueOf (3L),
                                        "def456hash",
                                        "as4-msg-001@sender.example",
                                        aNow,
                                        "sbdh-ib-001",
                                        null,
                                        Boolean.FALSE,
                                        Boolean.FALSE,
                                        "received",
                                        Integer.valueOf (0),
                                        aNow,
                                        null,
                                        "pending",
                                        null,
                                        null,
                                        null,
                                        "ALWAYS_SEND",
                                        null,
                                        null);
  }

  @Test
  public void testAllMandatoryFieldsMappedCorrectly ()
  {
    final InboundTransactionRow aTx = new InboundTransactionRow (_createValidRow ());

    assertEquals ("ib-001", aTx.getID ());
    assertEquals ("incoming-001", aTx.getIncomingID ());
    assertEquals ("POP000001", aTx.getC2SeatID ());
    assertEquals ("POP000002", aTx.getC3SeatID ());
    assertEquals ("CN=Test Cert", aTx.getSigningCertCN ());
    assertEquals ("iso6523-actorid-upis::sender", aTx.getSenderID ());
    assertEquals ("iso6523-actorid-upis::recv", aTx.getReceiverID ());
    assertEquals ("busdox-docid-qns::inv", aTx.getDocTypeID ());
    assertEquals ("cenbii-procid-ubl::proc", aTx.getProcessID ());
    assertArrayEquals (new byte [] { 4, 5, 6 }, aTx.getDocumentBytes ());
    assertEquals (3L, aTx.getDocumentSize ());
    assertEquals ("def456hash", aTx.getDocumentHash ());
    assertEquals ("as4-msg-001@sender.example", aTx.getAS4MessageID ());
    assertNotNull (aTx.getAS4Timestamp ());
    assertEquals ("sbdh-ib-001", aTx.getSbdhInstanceID ());
    assertFalse (aTx.isDuplicateAS4 ());
    assertFalse (aTx.isDuplicateSBDH ());
    assertEquals (EInboundStatus.RECEIVED, aTx.getStatus ());
    assertEquals (0, aTx.getAttemptCount ());
    assertNotNull (aTx.getReceivedDT ());
    assertEquals (EReportingStatus.PENDING, aTx.getReportingStatus ());
    assertEquals (EPeppolMLSType.ALWAYS_SEND, aTx.getMlsType ());
  }

  @Test
  public void testNullableFieldsReturnNull ()
  {
    final InboundTransactionRow aTx = new InboundTransactionRow (_createValidRow ());

    assertNull (aTx.getC4CountryCode ());
    assertNull (aTx.getCompletedDT ());
    assertNull (aTx.getNextRetryDT ());
    assertNull (aTx.getErrorDetails ());
    assertNull (aTx.getMlsTo ());
    assertNull (aTx.getMlsResponseCode ());
    assertNull (aTx.getMlsOutboundTransactionID ());
  }

  @Test
  public void testDuplicateFlags ()
  {
    final OffsetDateTime aNow = OffsetDateTime.now ();
    final byte [] aDocBytes = { 7 };

    final DBResultRow aRow = DBResultRowHelper.createRow ("ib-dup",
                                                          "inc-dup",
                                                          "POP000001",
                                                          "POP000002",
                                                          "CN=Dup Cert",
                                                          "sender-dup",
                                                          "recv-dup",
                                                          "doctype-dup",
                                                          "process-dup",
                                                          aDocBytes,
                                                          Long.valueOf (1L),
                                                          "hash-dup",
                                                          "as4-dup@test",
                                                          aNow,
                                                          "sbdh-dup",
                                                          null,
                                                          Boolean.TRUE,
                                                          Boolean.TRUE,
                                                          "received",
                                                          Integer.valueOf (0),
                                                          aNow,
                                                          null,
                                                          "pending",
                                                          null,
                                                          null,
                                                          null,
                                                          "ALWAYS_SEND",
                                                          null,
                                                          null);
    final InboundTransactionRow aTx = new InboundTransactionRow (aRow);
    assertTrue (aTx.isDuplicateAS4 ());
    assertTrue (aTx.isDuplicateSBDH ());
  }
}
