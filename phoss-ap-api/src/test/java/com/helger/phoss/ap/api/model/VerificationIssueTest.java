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

import com.helger.json.IJsonObject;
import com.helger.phoss.ap.api.codelist.EVerificationIssueLevel;
import com.helger.phoss.ap.api.codelist.EVerificationIssueType;

/**
 * Test class for class {@link VerificationIssue}.
 *
 * @author Philip Helger
 */
public final class VerificationIssueTest
{
  @Test
  public void testAllFields ()
  {
    final VerificationIssue a = new VerificationIssue (EVerificationIssueLevel.ERROR,
                                                        EVerificationIssueType.BUSINESS_RULE,
                                                        "PEPPOL-EN16931-R001",
                                                        "/Invoice/cbc:ID",
                                                        "Missing ID");
    assertSame (EVerificationIssueLevel.ERROR, a.getLevel ());
    assertSame (EVerificationIssueType.BUSINESS_RULE, a.getType ());
    assertTrue (a.isError ());
    assertEquals ("PEPPOL-EN16931-R001", a.getCode ());
    assertEquals ("/Invoice/cbc:ID", a.getLocation ());
    assertEquals ("Missing ID", a.getDescription ());
    assertNotNull (a.toString ());
  }

  @Test
  public void testFactories ()
  {
    final VerificationIssue aSyntax = VerificationIssue.syntaxViolation ("X-1", "/a", "d");
    assertSame (EVerificationIssueType.SYNTAX, aSyntax.getType ());
    assertTrue (aSyntax.isError ());

    final VerificationIssue aFatal = VerificationIssue.businessRuleViolation ("B-1", "/a", "d");
    assertSame (EVerificationIssueType.BUSINESS_RULE, aFatal.getType ());
    assertTrue (aFatal.isError ());

    final VerificationIssue aWarn = VerificationIssue.businessRuleWarning ("B-2", "/a", "d");
    assertSame (EVerificationIssueType.BUSINESS_RULE, aWarn.getType ());
    assertFalse (aWarn.isError ());
  }

  @Test
  public void testGetAsJsonFull ()
  {
    final IJsonObject aJson = VerificationIssue.businessRuleViolation ("R-1", "/Invoice", "Nope").getAsJson ();
    assertEquals ("error", aJson.getAsString ("level"));
    assertEquals ("business_rule", aJson.getAsString ("type"));
    assertEquals ("R-1", aJson.getAsString ("code"));
    assertEquals ("/Invoice", aJson.getAsString ("location"));
    assertEquals ("Nope", aJson.getAsString ("description"));
  }

  @Test
  public void testGetAsJsonOptionalsOmitted ()
  {
    final IJsonObject aJson = VerificationIssue.businessRuleWarning (null, null, "Nope").getAsJson ();
    assertEquals ("warning", aJson.getAsString ("level"));
    assertNull (aJson.get ("code"));
    assertNull (aJson.get ("location"));
    assertEquals ("Nope", aJson.getAsString ("description"));
  }

  @Test
  public void testInvalidParameters ()
  {
    try
    {
      new VerificationIssue (null, EVerificationIssueType.SYNTAX, null, null, "d");
      fail ();
    }
    catch (final NullPointerException | IllegalArgumentException ex)
    {
      // expected
    }

    try
    {
      new VerificationIssue (EVerificationIssueLevel.ERROR, EVerificationIssueType.SYNTAX, null, null, "");
      fail ();
    }
    catch (final NullPointerException | IllegalArgumentException ex)
    {
      // expected
    }
  }
}
