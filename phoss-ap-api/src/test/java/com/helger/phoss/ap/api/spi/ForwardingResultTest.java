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
package com.helger.phoss.ap.api.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.phoss.ap.api.model.ForwardingResult;

/**
 * Test class for class {@link ForwardingResult}.
 *
 * @author Philip Helger
 */
public final class ForwardingResultTest
{
  @Test
  public void testSuccess ()
  {
    final ForwardingResult aResult = ForwardingResult.success ();
    assertTrue (aResult.isSuccess ());
    assertFalse (aResult.isFailure ());
    assertNull (aResult.getErrorCode ());
    assertNull (aResult.getErrorDetails ());
  }

  @Test
  public void testSuccessSingleton ()
  {
    assertSame (ForwardingResult.success (), ForwardingResult.success ());
  }

  @Test
  public void testFailure ()
  {
    final ForwardingResult aResult = ForwardingResult.failure ("err_code", "Something went wrong");
    assertFalse (aResult.isSuccess ());
    assertTrue (aResult.isFailure ());
    assertEquals ("err_code", aResult.getErrorCode ());
    assertEquals ("Something went wrong", aResult.getErrorDetails ());
  }

  @Test
  public void testFailureWithNulls ()
  {
    final ForwardingResult aResult = ForwardingResult.failure (null, null);
    assertFalse (aResult.isSuccess ());
    assertTrue (aResult.isFailure ());
    assertNull (aResult.getErrorCode ());
    assertNull (aResult.getErrorDetails ());
  }
}
