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
package com.helger.phoss.ap.basic.mgr;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;

import org.junit.Test;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.phoss.ap.basic.mgr.APTimestampManager;

/**
 * Test class for class {@link APTimestampManager}.
 *
 * @author Philip Helger
 */
public final class APTimestampManagerTest
{
  @Test
  public void testGetCurrentDateTime ()
  {
    final var aTimestampMgr = new APTimestampManager ();
    final OffsetDateTime aDT1 = aTimestampMgr.getCurrentDateTime ();
    assertNotNull (aDT1);
    ThreadHelper.sleep (10);
    final OffsetDateTime aDT2 = aTimestampMgr.getCurrentDateTime ();
    assertNotNull (aDT2);
    assertTrue (aDT2.isAfter (aDT1));
  }

  @Test
  public void testGetCurrentDateTimeUTC ()
  {
    final var aTimestampMgr = new APTimestampManager ();
    final OffsetDateTime aDT1 = aTimestampMgr.getCurrentDateTimeUTC ();
    assertNotNull (aDT1);
    ThreadHelper.sleep (10);
    final OffsetDateTime aDT2 = aTimestampMgr.getCurrentDateTimeUTC ();
    assertNotNull (aDT2);
    assertTrue (aDT2.isAfter (aDT1));
  }
}
