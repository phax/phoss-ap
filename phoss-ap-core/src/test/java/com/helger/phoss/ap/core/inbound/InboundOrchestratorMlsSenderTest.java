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
package com.helger.phoss.ap.core.inbound;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.peppolid.peppol.PeppolIdentifierHelper;

/**
 * Test class for {@link InboundOrchestrator#isValidMlsSender(String, String, String)}.
 *
 * @author Philip Helger
 */
public final class InboundOrchestratorMlsSenderTest
{
  private static final String SCHEME = PeppolIdentifierHelper.PARTICIPANT_SCHEME_ISO6523_ACTORID_UPIS;
  // Seat ID = 3-char prefix + 6-digit Main ID
  private static final String SENDER_SEAT_ID = "PFR000920";

  @Test
  public void testValidMainIDOnly ()
  {
    assertTrue (InboundOrchestrator.isValidMlsSender (SCHEME, "0242:000920", SENDER_SEAT_ID));
  }

  @Test
  public void testValidWithUseCaseAndSuffix ()
  {
    assertTrue (InboundOrchestrator.isValidMlsSender (SCHEME, "0242:000920-mls", SENDER_SEAT_ID));
    assertTrue (InboundOrchestrator.isValidMlsSender (SCHEME, "0242:000920-MLS.svc99", SENDER_SEAT_ID));
  }

  @Test
  public void testMainIDMismatch ()
  {
    // The case reported from production: SPID of a different Service Provider
    assertFalse (InboundOrchestrator.isValidMlsSender (SCHEME, "0242:000077-mls", SENDER_SEAT_ID));
    assertFalse (InboundOrchestrator.isValidMlsSender (SCHEME, "0242:000077", SENDER_SEAT_ID));
  }

  @Test
  public void testWrongScheme ()
  {
    assertFalse (InboundOrchestrator.isValidMlsSender ("iso6523-actorid-XXX", "0242:000920", SENDER_SEAT_ID));
    assertFalse (InboundOrchestrator.isValidMlsSender (null, "0242:000920", SENDER_SEAT_ID));
  }

  @Test
  public void testNotSpis ()
  {
    assertFalse (InboundOrchestrator.isValidMlsSender (SCHEME, "0208:000920", SENDER_SEAT_ID));
  }

  @Test
  public void testSyntacticallyInvalid ()
  {
    assertFalse (InboundOrchestrator.isValidMlsSender (SCHEME, "0242:", SENDER_SEAT_ID));
    assertFalse (InboundOrchestrator.isValidMlsSender (SCHEME, "0242:00092", SENDER_SEAT_ID));
    assertFalse (InboundOrchestrator.isValidMlsSender (SCHEME, "0242:ABCDEF", SENDER_SEAT_ID));
    assertFalse (InboundOrchestrator.isValidMlsSender (SCHEME, null, SENDER_SEAT_ID));
  }

  @Test
  public void testInvalidSenderSeatID ()
  {
    assertFalse (InboundOrchestrator.isValidMlsSender (SCHEME, "0242:000920", null));
    assertFalse (InboundOrchestrator.isValidMlsSender (SCHEME, "0242:000920", ""));
    assertFalse (InboundOrchestrator.isValidMlsSender (SCHEME, "0242:000920", "AB"));
  }
}
