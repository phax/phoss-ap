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
package com.helger.phoss.ap.api.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.mls.EPeppolMLSStatusReasonCode;
import com.helger.peppol.mls.PeppolMLSBuilder;

import oasis.names.specification.ubl.schema.xsd.applicationresponse_21.ApplicationResponseType;

/**
 * Test class for class {@link MlsOutcome}.
 *
 * @author Philip Helger
 */
public final class MlsOutcomeTest
{
  @Test
  public void testAcceptance ()
  {
    final MlsOutcome a = MlsOutcome.acceptance ();
    assertSame (EPeppolMLSResponseCode.ACCEPTANCE, a.getResponseCode ());
    assertEquals ("AP", a.getResponseCodeID ());
    assertNull (a.getResponseText ());
    assertFalse (a.hasIssues ());
  }

  @Test
  public void testAcknowledging ()
  {
    final MlsOutcome a = MlsOutcome.acknowledging ();
    assertSame (EPeppolMLSResponseCode.ACKNOWLEDGING, a.getResponseCode ());
    assertEquals ("AB", a.getResponseCodeID ());
    assertFalse (a.hasIssues ());
  }

  @Test
  public void testRejectionSingleIssue ()
  {
    final MlsOutcomeIssue aIssue = MlsOutcomeIssue.failureOfDelivery ("Cannot deliver");
    final MlsOutcome a = MlsOutcome.rejection ("Rejected", aIssue);
    assertSame (EPeppolMLSResponseCode.REJECTION, a.getResponseCode ());
    assertEquals ("RE", a.getResponseCodeID ());
    assertEquals ("Rejected", a.getResponseText ());
    assertTrue (a.hasIssues ());
    assertEquals (1, a.getIssues ().size ());
    assertSame (EPeppolMLSStatusReasonCode.FAILURE_OF_DELIVERY, a.getIssues ().get (0).getStatusReasonCode ());
    assertEquals ("Cannot deliver", a.getIssues ().get (0).getDescription ());
  }

  @Test
  public void testRejectionMultipleIssues ()
  {
    final MlsOutcomeIssue aIssue1 = MlsOutcomeIssue.businessRuleViolation ("/Invoice/ID", "Missing invoice ID");
    final MlsOutcomeIssue aIssue2 = MlsOutcomeIssue.syntaxViolation ("/Invoice/IssueDate", "Invalid date format");
    final MlsOutcome a = MlsOutcome.rejection ("Multiple errors", List.of (aIssue1, aIssue2));
    assertSame (EPeppolMLSResponseCode.REJECTION, a.getResponseCode ());
    assertEquals ("Multiple errors", a.getResponseText ());
    assertTrue (a.hasIssues ());
    assertEquals (2, a.getIssues ().size ());
  }

  @Test
  public void testRejectionIssuesGroupedByErrorField ()
  {
    // Two issues with the same error field should be grouped
    final MlsOutcomeIssue aIssue1 = MlsOutcomeIssue.businessRuleViolation ("/Invoice/ID", "Rule 1 failed");
    final MlsOutcomeIssue aIssue2 = MlsOutcomeIssue.businessRuleWarning ("/Invoice/ID", "Rule 2 warning");
    final MlsOutcomeIssue aIssue3 = MlsOutcomeIssue.syntaxViolation ("/Invoice/Amount", "Bad amount");
    final MlsOutcome a = MlsOutcome.rejection (null, List.of (aIssue1, aIssue2, aIssue3));
    assertNull (a.getResponseText ());
    assertTrue (a.hasIssues ());
    // All 3 issues accessible via flat list
    assertEquals (3, a.getIssues ().size ());
  }

  @Test
  public void testAcceptanceGetIssuesEmpty ()
  {
    final MlsOutcome a = MlsOutcome.acceptance ();
    assertNotNull (a.getIssues ());
    assertTrue (a.getIssues ().isEmpty ());
  }

  @Test
  public void testGetAsMLSBuilder ()
  {
    final MlsOutcomeIssue aIssue = MlsOutcomeIssue.failureOfDelivery ("Delivery failed");
    final MlsOutcome a = MlsOutcome.rejection ("Failed", aIssue);
    final PeppolMLSBuilder aBuilder = a.getAsMLSBuilder ();
    assertNotNull (aBuilder);

    final ApplicationResponseType aMls = aBuilder.randomID ()
                                                 .issueDateTimeNow ()
                                                 .senderParticipantID ("9915:phoss-ap-test-sender")
                                                 .receiverParticipantID ("0242:123456")
                                                 .referenceId (UUID.randomUUID ().toString ())
                                                 .build ();
    assertNotNull (aMls);
  }

  @Test
  public void testGetAsMLSBuilderAcceptance ()
  {
    final MlsOutcome a = MlsOutcome.acceptance ();
    final PeppolMLSBuilder aBuilder = a.getAsMLSBuilder ();
    assertNotNull (aBuilder);
  }
}
