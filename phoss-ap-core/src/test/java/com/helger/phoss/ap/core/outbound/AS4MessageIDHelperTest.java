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
package com.helger.phoss.ap.core.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Test class for {@link AS4MessageIDHelper}.
 */
public final class AS4MessageIDHelperTest
{
  @Test
  public void testTrimmedOrNull ()
  {
    assertNull (AS4MessageIDHelper.getTrimmedOrNull (null));
    assertNull (AS4MessageIDHelper.getTrimmedOrNull ("  "));
    assertEquals ("326f1827-c7fe@ap.example.org", AS4MessageIDHelper.getTrimmedOrNull (" 326f1827-c7fe@ap.example.org "));
  }

  @Test
  public void testValidID ()
  {
    assertNull (AS4MessageIDHelper.getValidationError ("326f1827-c7fe@ap.example.org"));
  }

  @Test
  public void testInvalidIDs ()
  {
    assertNotNull (AS4MessageIDHelper.getValidationError ("no-at-sign"));
    assertNotNull (AS4MessageIDHelper.getValidationError ("@ap.example.org"));
    assertNotNull (AS4MessageIDHelper.getValidationError ("id@"));
    assertNotNull (AS4MessageIDHelper.getValidationError ("a@b@c"));
    assertNotNull (AS4MessageIDHelper.getValidationError ("id with space@ap.example.org"));
    assertNotNull (AS4MessageIDHelper.getValidationError ("x".repeat (250) + "@ap.example.org"));
  }
}
