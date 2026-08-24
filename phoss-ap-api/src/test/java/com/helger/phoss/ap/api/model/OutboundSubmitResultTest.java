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

/**
 * Test class for class {@link OutboundSubmitResult}.
 *
 * @author Philip Helger
 */
public final class OutboundSubmitResultTest
{
  @Test
  public void testVerificationRejected ()
  {
    final VerificationOutcome aOutcome = VerificationOutcome.rejected ("Invalid",
                                                                       new CommonsArrayList <> (VerificationIssue.businessRuleViolation ("R-1",
                                                                                                                                         "/a",
                                                                                                                                         "d")));
    final OutboundSubmitResult a = OutboundSubmitResult.verificationRejected (aOutcome);
    assertFalse (a.isSuccess ());
    assertTrue (a.isFailure ());
    assertTrue (a.isVerificationRejected ());
    // No transaction is created for a rejected document - it is never sent
    assertNull (a.getTransaction ());
    assertSame (aOutcome, a.getVerificationOutcome ());
    assertNull (a.getErrorMessage ());
    assertNotNull (a.toString ());
  }

  @Test
  public void testFailure ()
  {
    final OutboundSubmitResult a = OutboundSubmitResult.failure ("Cannot parse");
    assertFalse (a.isSuccess ());
    // A generic failure must not look like a verification rejection
    assertFalse (a.isVerificationRejected ());
    assertNull (a.getTransaction ());
    assertNull (a.getVerificationOutcome ());
    assertEquals ("Cannot parse", a.getErrorMessage ());
  }

  @Test
  public void testInvalidParameters ()
  {
    try
    {
      OutboundSubmitResult.verificationRejected (null);
      fail ();
    }
    catch (final NullPointerException | IllegalArgumentException ex)
    {
      // expected
    }

    try
    {
      OutboundSubmitResult.failure ("");
      fail ();
    }
    catch (final NullPointerException | IllegalArgumentException ex)
    {
      // expected
    }

    try
    {
      OutboundSubmitResult.success (null, null);
      fail ();
    }
    catch (final NullPointerException | IllegalArgumentException ex)
    {
      // expected
    }
  }
}
