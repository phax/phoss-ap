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
package com.helger.phoss.ap.db.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.OffsetDateTime;

import org.junit.Rule;
import org.junit.Test;

import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.db.testhelper.DBResultRowHelper;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Test class for {@link OutboundSendingAttemptRow}.
 *
 * @author Philip Helger
 */
public final class OutboundSendingAttemptRowTest
{
  @Rule
  public final ScopeTestRule m_aRule = new ScopeTestRule ();

  @Test
  public void testAllFieldsMapped ()
  {
    final OffsetDateTime aNow = APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ();

    // 9 columns matching OutboundSendingAttemptRow constructor order
    // 0 id
    // 1 outboundTransactionID
    // 2 as4MessageID
    // 3 as4Timestamp
    // 4 receiptMessageID (nullable)
    // 5 httpStatusCode (nullable)
    // 6 attemptDT
    // 7 attemptStatus
    // 8 errorDetails (nullable)
    // 9 sendingReport (nullable)
    final DBResultRow aRow = DBResultRowHelper.createRow ("att-001",
                                                          "tx-001",
                                                          "as4-msg@test",
                                                          aNow,
                                                          "receipt-msg@test",
                                                          Integer.valueOf (200),
                                                          aNow,
                                                          "success",
                                                          null,
                                                          "{'a': 0}");
    final OutboundSendingAttemptRow aAttempt = new OutboundSendingAttemptRow (aRow);

    assertEquals ("att-001", aAttempt.getID ());
    assertEquals ("tx-001", aAttempt.getOutboundTransactionID ());
    assertEquals ("as4-msg@test", aAttempt.getAS4MessageID ());
    assertNotNull (aAttempt.getAS4Timestamp ());
    assertEquals ("receipt-msg@test", aAttempt.getReceiptMessageID ());
    assertEquals (Integer.valueOf (200), aAttempt.getHttpStatusCode ());
    assertNotNull (aAttempt.getAttemptDT ());
    assertEquals (EAttemptStatus.SUCCESS, aAttempt.getAttemptStatus ());
    assertNull (aAttempt.getErrorDetails ());
  }

  @Test
  public void testNullableFieldsReturnNull ()
  {
    final OffsetDateTime aNow = APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ();

    final DBResultRow aRow = DBResultRowHelper.createRow ("att-002",
                                                          "tx-002",
                                                          "as4-msg2@test",
                                                          aNow,
                                                          null,
                                                          null,
                                                          aNow,
                                                          "failed",
                                                          "Connection timeout",
                                                          "{'sendingReport'}");
    final OutboundSendingAttemptRow aAttempt = new OutboundSendingAttemptRow (aRow);

    assertNull (aAttempt.getReceiptMessageID ());
    assertNull (aAttempt.getHttpStatusCode ());
    assertEquals (EAttemptStatus.FAILED, aAttempt.getAttemptStatus ());
    assertEquals ("Connection timeout", aAttempt.getErrorDetails ());
  }
}
