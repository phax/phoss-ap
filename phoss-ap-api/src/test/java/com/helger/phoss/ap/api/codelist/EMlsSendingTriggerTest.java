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
package com.helger.phoss.ap.api.codelist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link EMlsSendingTrigger}.
 *
 * @author Philip Helger
 */
public final class EMlsSendingTriggerTest
{
  @Test
  public void testBasic ()
  {
    for (final EMlsSendingTrigger e : EMlsSendingTrigger.values ())
    {
      assertTrue (StringHelper.isNotEmpty (e.getID ()));
      assertSame (e, EMlsSendingTrigger.getFromIDOrNull (e.getID ()));
    }
    assertNull (EMlsSendingTrigger.getFromIDOrNull (null));
    assertNull (EMlsSendingTrigger.getFromIDOrNull ("bla"));

    // The default must reproduce the behaviour of all versions before 0.13.0
    assertSame (EMlsSendingTrigger.AUTO, EMlsSendingTrigger.DEFAULT);
  }

  @Test
  public void testIDs ()
  {
    // The IDs are part of the configuration surface and must stay stable
    assertEquals ("auto", EMlsSendingTrigger.AUTO.getID ());
    assertEquals ("api", EMlsSendingTrigger.API.getID ());
  }
}
