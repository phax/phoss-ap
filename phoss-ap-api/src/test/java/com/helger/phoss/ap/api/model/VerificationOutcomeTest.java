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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

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
    assertNull (a.getMlsOutcome ());
    // Always the same instance
    assertSame (a, VerificationOutcome.passed ());
  }

  @Test
  public void testRejectedWithMlsOutcome ()
  {
    final MlsOutcome aMls = MlsOutcome.rejection ("Invalid document",
                                                   MlsOutcomeIssue.businessRuleViolation ("NA", "Rule failed"));
    final VerificationOutcome a = VerificationOutcome.rejected (aMls);
    assertSame (EVerificationOutcomeCategory.REJECTION, a.getCategory ());
    assertTrue (a.isRejected ());
    assertFalse (a.isPassed ());
    assertFalse (a.isServiceUnavailable ());
    assertSame (aMls, a.getMlsOutcome ());
    // The message is taken from the MLS response text
    assertEquals ("Invalid document", a.getMessage ());
  }

  @Test
  public void testRejectedWithMessage ()
  {
    final VerificationOutcome a = VerificationOutcome.rejected ("Malware found");
    assertSame (EVerificationOutcomeCategory.REJECTION, a.getCategory ());
    assertTrue (a.isRejected ());
    assertEquals ("Malware found", a.getMessage ());
    assertNull (a.getMlsOutcome ());
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
    assertNull (a.getMlsOutcome ());
  }

  @Test
  public void testInvalidParameters ()
  {
    try
    {
      VerificationOutcome.rejected ((MlsOutcome) null);
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
