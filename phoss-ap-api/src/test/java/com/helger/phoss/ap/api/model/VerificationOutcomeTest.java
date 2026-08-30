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
import static org.junit.Assert.fail;

import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.phoss.ap.api.codelist.EVerificationOutcomeCategory;

/**
 * Test class for class {@link VerificationOutcome}.
 *
 * @author Philip Helger
 */
public final class VerificationOutcomeTest
{
  @Test
  public void testPassed ()
  {
    final VerificationOutcome a = VerificationOutcome.passed ();
    assertSame (EVerificationOutcomeCategory.PASSED, a.getCategory ());
    assertTrue (a.isPassed ());
    assertFalse (a.isRejected ());
    assertFalse (a.isServiceUnavailable ());
    assertNull (a.getMessage ());
    assertFalse (a.hasIssues ());
    assertTrue (a.getAllIssues ().isEmpty ());
    // Always the same instance
    assertSame (a, VerificationOutcome.passed ());
  }

  @Test
  public void testPassedWithWarnings ()
  {
    final VerificationIssue aIssue = VerificationIssue.businessRuleWarning ("R-042", "/Invoice", "Better avoid this");
    final VerificationOutcome a = VerificationOutcome.passed (new CommonsArrayList <> (aIssue));
    assertTrue (a.isPassed ());
    assertNull (a.getMessage ());
    // A passed outcome may carry warnings - MLS cannot express those, but an outbound submitter
    // can be told about them
    assertTrue (a.hasIssues ());
    assertEquals (1, a.getAllIssues ().size ());
    assertSame (aIssue, a.getAllIssues ().getFirstOrNull ());
  }

  @Test
  public void testPassedWithNullIssues ()
  {
    final VerificationOutcome a = VerificationOutcome.passed (null);
    assertTrue (a.isPassed ());
    assertFalse (a.hasIssues ());
  }

  @Test
  public void testRejectedWithIssues ()
  {
    final VerificationIssue aIssue = VerificationIssue.businessRuleViolation ("PEPPOL-EN16931-R001",
                                                                              "/Invoice/cbc:ID",
                                                                              "Rule failed");
    final VerificationOutcome a = VerificationOutcome.rejected ("Invalid document", new CommonsArrayList <> (aIssue));
    assertSame (EVerificationOutcomeCategory.REJECTION, a.getCategory ());
    assertTrue (a.isRejected ());
    assertFalse (a.isPassed ());
    assertFalse (a.isServiceUnavailable ());
    assertEquals ("Invalid document", a.getMessage ());
    assertTrue (a.hasIssues ());
    assertSame (aIssue, a.getAllIssues ().getFirstOrNull ());
  }

  @Test
  public void testRejectedWithMessage ()
  {
    final VerificationOutcome a = VerificationOutcome.rejected ("Malware found");
    assertSame (EVerificationOutcomeCategory.REJECTION, a.getCategory ());
    assertTrue (a.isRejected ());
    assertEquals ("Malware found", a.getMessage ());
    assertFalse (a.hasIssues ());
  }

  @Test
  public void testGetAllIssuesAsJson ()
  {
    // This is what ends up in the "verification_details" column of an inbound transaction
    final VerificationOutcome a = VerificationOutcome.rejected ("Invalid",
                                                                new CommonsArrayList <> (VerificationIssue.businessRuleViolation ("PEPPOL-EN16931-R001",
                                                                                                                                  "/Invoice/cbc:ID",
                                                                                                                                  "Missing ID"),
                                                                                         VerificationIssue.businessRuleWarning (null,
                                                                                                                                null,
                                                                                                                                "Just a warning")));
    final IJsonArray aJson = a.getAllIssuesAsJson ();
    assertEquals (2, aJson.size ());

    final IJsonObject aFirst = aJson.getObjectAtIndex (0);
    assertEquals ("error", aFirst.getAsString ("level"));
    assertEquals ("PEPPOL-EN16931-R001", aFirst.getAsString ("code"));

    // Round-trips through the DB column as a string
    final String sJson = aJson.getAsJsonString ();
    assertNotNull (JsonReader.builder ().source (sJson).read ());
  }

  @Test
  public void testGetAllIssuesAsJsonEmpty ()
  {
    assertTrue (VerificationOutcome.passed ().getAllIssuesAsJson ().isEmpty ());
  }

  @Test
  public void testIssuesAreACopy ()
  {
    final VerificationIssue aIssue = VerificationIssue.businessRuleViolation (null, null, "Rule failed");
    final VerificationOutcome a = VerificationOutcome.rejected ("Nope", new CommonsArrayList <> (aIssue));
    a.getAllIssues ().clear ();
    assertTrue (a.hasIssues ());
  }

  @Test
  public void testServiceUnavailable ()
  {
    final VerificationOutcome a = VerificationOutcome.serviceUnavailable ("Connection refused");
    assertSame (EVerificationOutcomeCategory.SERVICE_UNAVAILABLE, a.getCategory ());
    assertTrue (a.isServiceUnavailable ());
    assertFalse (a.isPassed ());
    assertFalse (a.isRejected ());
    assertEquals ("Connection refused", a.getMessage ());
    assertFalse (a.hasIssues ());
  }

  @Test
  public void testInvalidParameters ()
  {
    try
    {
      VerificationOutcome.rejected ((String) null);
      fail ();
    }
    catch (final NullPointerException | IllegalArgumentException ex)
    {
      // expected
    }

    try
    {
      // A rejection with an explicitly empty issue list makes no sense
      VerificationOutcome.rejected ("Nope", new CommonsArrayList <> ());
      fail ();
    }
    catch (final NullPointerException | IllegalArgumentException ex)
    {
      // expected
    }

    try
    {
      VerificationOutcome.serviceUnavailable ("");
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }
}
