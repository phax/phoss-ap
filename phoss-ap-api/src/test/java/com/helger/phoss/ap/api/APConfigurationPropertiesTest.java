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
package com.helger.phoss.ap.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;

/**
 * Test class for class {@link APConfigurationProperties}.
 *
 * @author Philip Helger
 */
public final class APConfigurationPropertiesTest
{
  @Test
  public void testAllConstantsAreNonEmptyAndUnique () throws Exception
  {
    final ICommonsSet <String> aAllValues = new CommonsHashSet <> ();

    for (final Field aField : APConfigurationProperties.class.getDeclaredFields ())
    {
      // Only check public static final String fields
      final int nMods = aField.getModifiers ();
      if (Modifier.isPublic (nMods) &&
        Modifier.isStatic (nMods) &&
        Modifier.isFinal (nMods) &&
        aField.getType () == String.class)
      {
        final String sValue = (String) aField.get (null);
        assertNotNull ("Field " + aField.getName () + " has a null value", sValue);
        assertTrue ("Field " + aField.getName () + " has an empty value", StringHelper.isNotEmpty (sValue));
        assertTrue ("Duplicate configuration key '" + sValue + "' in field " + aField.getName (),
                    aAllValues.add (sValue));
      }
    }

    // Ensure we actually found some constants
    assertTrue ("No configuration property constants found", aAllValues.size () > 10);
  }

  @Test
  public void testSpecificKeys ()
  {
    assertEquals ("peppol.seatid", APConfigurationProperties.PEPPOL_SEATID);
    assertEquals ("jdbc.url", APConfigurationProperties.JDBC_URL);
    assertEquals ("forwarding.mode", APConfigurationProperties.FORWARDING_MODE);
    assertEquals ("retry.sending.max-attempts", APConfigurationProperties.RETRY_SENDING_MAX_ATTEMPTS);
    assertEquals ("mls.type", APConfigurationProperties.MLS_TYPE);
  }
}
