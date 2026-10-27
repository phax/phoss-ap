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

import static com.helger.phoss.ap.core.startup.StartupCheckTestHelper.assertErrors;
import static com.helger.phoss.ap.core.startup.StartupCheckTestHelper.assertOneWarning;
import static com.helger.phoss.ap.core.startup.StartupCheckTestHelper.createConfig;
import static com.helger.phoss.ap.core.startup.StartupCheckTestHelper.performCheck;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for class {@link StartupCheckPeppolSeatIDCertificate}.
 *
 * @author Philip Helger
 */
public final class StartupCheckPeppolSeatIDCertificateTest
{
  @NonNull
  private static StartupCheckPeppolSeatIDCertificate _createCheck (@NonNull final Map <String, String> aKeyStoreConfig)
  {
    return new StartupCheckPeppolSeatIDCertificate ( () -> new AS4CryptoFactoryConfiguration (createConfig (aKeyStoreConfig)));
  }

  @Test
  public void testMatch ()
  {
    final StartupCheckPeppolSeatIDCertificate aCheck = _createCheck (StartupCheckAS4KeyStoreTest.createPopKeyStoreConfig ());
    assertErrors (aCheck, 0, performCheck (aCheck, Map.of (APConfigurationProperties.PEPPOL_OWNER_SEATID, "POP000001")));
  }

  @Test
  public void testMismatch ()
  {
    final StartupCheckPeppolSeatIDCertificate aCheck = _createCheck (StartupCheckAS4KeyStoreTest.createPopKeyStoreConfig ());
    assertErrors (aCheck, 1, performCheck (aCheck, Map.of (APConfigurationProperties.PEPPOL_OWNER_SEATID, "POP000002")));

    // CN is not a Seat ID at all
    final StartupCheckPeppolSeatIDCertificate aCheck2 = _createCheck (StartupCheckAS4KeyStoreTest.createKeyStoreConfig ());
    assertErrors (aCheck2, 1, performCheck (aCheck2, Map.of (APConfigurationProperties.PEPPOL_OWNER_SEATID, "POP000001")));
  }

  @Test
  public void testSkipPropertyDowngradesToWarning ()
  {
    final String sOld = System.getProperty (StartupCheckPeppolAPCertificate.SYSTEM_PROPERTY_SKIP);
    try
    {
      System.setProperty (StartupCheckPeppolAPCertificate.SYSTEM_PROPERTY_SKIP, "true");
      final StartupCheckPeppolSeatIDCertificate aCheck = _createCheck (StartupCheckAS4KeyStoreTest.createPopKeyStoreConfig ());
      assertOneWarning (aCheck,
                        performCheck (aCheck, Map.of (APConfigurationProperties.PEPPOL_OWNER_SEATID, "POP000002")));
    }
    finally
    {
      if (sOld == null)
        System.clearProperty (StartupCheckPeppolAPCertificate.SYSTEM_PROPERTY_SKIP);
      else
        System.setProperty (StartupCheckPeppolAPCertificate.SYSTEM_PROPERTY_SKIP, sOld);
    }
  }

  @Test
  public void testNotReportedTwice ()
  {
    // Invalid Seat ID - reported by StartupCheckPeppolSeatID
    final StartupCheckPeppolSeatIDCertificate aCheck = _createCheck (StartupCheckAS4KeyStoreTest.createPopKeyStoreConfig ());
    assertErrors (aCheck, 0, performCheck (aCheck, Map.of (APConfigurationProperties.PEPPOL_OWNER_SEATID, "XYZ")));

    // No private key - reported by StartupCheckAS4KeyStore
    final StartupCheckPeppolSeatIDCertificate aCheck2 = new StartupCheckPeppolSeatIDCertificate ( () -> null);
    assertErrors (aCheck2, 0, performCheck (aCheck2, Map.of (APConfigurationProperties.PEPPOL_OWNER_SEATID, "POP000001")));
  }
}
