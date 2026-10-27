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
package com.helger.phoss.ap.core.startup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.base.exception.InitializationException;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.spi.IAPStartupCheckSPI;

/**
 * Test class for class {@link APStartupCheckManager}.
 *
 * @author Philip Helger
 */
public final class APStartupCheckManagerTest
{
  @Before
  @After
  public void reset ()
  {
    APStartupCheckManager.reset ();
  }

  @Test
  public void testOrder ()
  {
    APStartupCheckManager.initSPI ();
    final ICommonsList <IAPStartupCheckSPI> aChecks = APStartupCheckManager.getAllChecks ();

    // Built-in checks first, in registration order
    final ICommonsList <IAPStartupCheckSPI> aBuiltIns = APBuiltInStartupChecks.getAll ();
    assertEquals (aBuiltIns.size () + 2, aChecks.size ());
    for (int i = 0; i < aBuiltIns.size (); ++i)
      assertSame (aBuiltIns.get (i).getClass (), aChecks.get (i).getClass ());

    // Then the SPI checks, ordered by name - the services file lists them in reverse order
    assertEquals (MockStartupCheckSPIA.class, aChecks.get (aBuiltIns.size ()).getClass ());
    assertEquals (MockStartupCheckSPIB.class, aChecks.get (aBuiltIns.size () + 1).getClass ());

    // Manually registered checks come last
    final IAPStartupCheckSPI aManual = new MockStartupCheckSPIA ();
    APStartupCheckManager.registerCheck (aManual);
    assertSame (aManual, APStartupCheckManager.getAllChecks ().getLastOrNull ());
  }

  @Test
  public void testManualRegistrationBeforeInitComesLast ()
  {
    final IAPStartupCheckSPI aManual = new MockStartupCheckSPIB ();
    APStartupCheckManager.registerCheck (aManual);
    APStartupCheckManager.initSPI ();
    final ICommonsList <IAPStartupCheckSPI> aChecks = APStartupCheckManager.getAllChecks ();
    assertEquals (APBuiltInStartupChecks.getAll ().size () + 3, aChecks.size ());
    assertSame (aManual, aChecks.getLastOrNull ());
  }

  @Test
  public void testInitOnlyOnce ()
  {
    APStartupCheckManager.initSPI ();
    try
    {
      APStartupCheckManager.initSPI ();
      fail ();
    }
    catch (final InitializationException ex)
    {
      assertTrue (ex.getMessage ().contains ("already initialized"));
    }
  }

  @Test
  public void testBuiltInCheckNamesAreUnique ()
  {
    final ICommonsList <IAPStartupCheckSPI> aBuiltIns = APBuiltInStartupChecks.getAll ();
    assertEquals (aBuiltIns.size (), new CommonsHashSet <> (aBuiltIns, IAPStartupCheckSPI::getCheckName).size ());
  }
}
