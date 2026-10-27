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

import java.time.OffsetDateTime;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for class {@link StartupCheckPeppolAPCertificate}.
 *
 * @author Philip Helger
 */
public final class StartupCheckPeppolAPCertificateTest
{
  @NonNull
  private static StartupCheckPeppolAPCertificate _createCheckWithKeyStore ()
  {
    // The certificate in this key store is not a Peppol AP certificate
    return new StartupCheckPeppolAPCertificate ( () -> new AS4CryptoFactoryConfiguration (createConfig (StartupCheckAS4KeyStoreTest.createKeyStoreConfig ())),
                                                OffsetDateTime::now);
  }

  @Test
  public void testNotAPeppolCertificate ()
  {
    final StartupCheckPeppolAPCertificate aCheck = _createCheckWithKeyStore ();
    assertErrors (aCheck, 1, performCheck (aCheck, Map.of (APConfigurationProperties.PEPPOL_STAGE, "test")));
    assertErrors (aCheck, 1, performCheck (aCheck, Map.of (APConfigurationProperties.PEPPOL_STAGE, "prod")));
  }

  @Test
  public void testSkipPropertyDowngradesToWarning ()
  {
    final String sOld = System.getProperty (StartupCheckPeppolAPCertificate.SYSTEM_PROPERTY_SKIP);
    try
    {
      System.setProperty (StartupCheckPeppolAPCertificate.SYSTEM_PROPERTY_SKIP, "true");
      final StartupCheckPeppolAPCertificate aCheck = _createCheckWithKeyStore ();
      assertOneWarning (aCheck, performCheck (aCheck, Map.of (APConfigurationProperties.PEPPOL_STAGE, "test")));
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
  public void testNoPrivateKeyIsNotReportedTwice ()
  {
    // Reported by StartupCheckAS4KeyStore
    final StartupCheckPeppolAPCertificate aCheck = new StartupCheckPeppolAPCertificate ( () -> null,
                                                                                        OffsetDateTime::now);
    assertErrors (aCheck, 0, performCheck (aCheck, Map.of (APConfigurationProperties.PEPPOL_STAGE, "test")));
  }

  @Test
  public void testMissingStage ()
  {
    final StartupCheckPeppolAPCertificate aCheck = _createCheckWithKeyStore ();
    assertErrors (aCheck, 1, performCheck (aCheck, Map.of ()));
  }
}
