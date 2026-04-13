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
 * Test class for {@link InboundForwardingAttemptRow}.
 *
 * @author Philip Helger
 */
public final class InboundForwardingAttemptRowTest
{
  @Rule
  public final ScopeTestRule m_aRule = new ScopeTestRule ();

  @Test
  public void testAllFieldsMapped ()
  {
    final OffsetDateTime aNow = APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ();

    // 6 columns matching InboundForwardingAttemptRow constructor order
    // 0 id
    // 1 inboundTransactionID
    // 2 attemptDT
    // 3 attemptStatus
    // 4 errorCode (nullable)
    // 5 errorDetails (nullable)
    final DBResultRow aRow = DBResultRowHelper.createRow ("fwd-001", "ib-001", aNow, "success", null, null);
    final InboundForwardingAttemptRow aAttempt = new InboundForwardingAttemptRow (aRow);

    assertEquals ("fwd-001", aAttempt.getID ());
    assertEquals ("ib-001", aAttempt.getInboundTransactionID ());
    assertNotNull (aAttempt.getAttemptDT ());
    assertEquals (EAttemptStatus.SUCCESS, aAttempt.getAttemptStatus ());
    assertNull (aAttempt.getErrorCode ());
    assertNull (aAttempt.getErrorDetails ());
  }

  @Test
  public void testWithErrorDetails ()
  {
    final OffsetDateTime aNow = APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ();

    final DBResultRow aRow = DBResultRowHelper.createRow ("fwd-002",
                                                          "ib-002",
                                                          aNow,
                                                          "failed",
                                                          "HTTP_500",
                                                          "Internal server error");
    final InboundForwardingAttemptRow aAttempt = new InboundForwardingAttemptRow (aRow);

    assertEquals (EAttemptStatus.FAILED, aAttempt.getAttemptStatus ());
    assertEquals ("HTTP_500", aAttempt.getErrorCode ());
    assertEquals ("Internal server error", aAttempt.getErrorDetails ());
  }
}
