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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;

/**
 * Test class for class {@link StartupCheckTlsCertificateSeparation}.
 *
 * @author Philip Helger
 */
public final class StartupCheckTlsCertificateSeparationTest
{
  @NonNull
  private static AS4CryptoFactoryConfiguration _ap ()
  {
    return new AS4CryptoFactoryConfiguration (createConfig (StartupCheckAS4KeyStoreTest.createPopKeyStoreConfig ()));
  }

  @NonNull
  private static X509Certificate _otherCert ()
  {
    return (X509Certificate) new AS4CryptoFactoryConfiguration (createConfig (StartupCheckAS4KeyStoreTest.createKeyStoreConfig ())).getPrivateKeyEntry ()
                                                                                                                            .getCertificate ();
  }

  @NonNull
  private static ErrorList _check (@NonNull final StartupCheckTlsCertificateSeparation aCheck)
  {
    final ErrorList ret = new ErrorList ();
    aCheck.performCheck (ret);
    return ret;
  }

  @NonNull
  private static StartupCheckTlsCertificateSeparation _create (@NonNull final Supplier <? extends List <X509Certificate>> aTls)
  {
    return new StartupCheckTlsCertificateSeparation (StartupCheckTlsCertificateSeparationTest::_ap, aTls);
  }

  @Test
  public void testSeparate ()
  {
    final StartupCheckTlsCertificateSeparation aCheck = _create ( () -> new CommonsArrayList <> (_otherCert ()));
    assertErrors (aCheck, 0, _check (aCheck));
  }

  @Test
  public void testSameCertificate ()
  {
    final X509Certificate aAPCert = (X509Certificate) _ap ().getPrivateKeyEntry ().getCertificate ();
    final StartupCheckTlsCertificateSeparation aCheck = _create ( () -> new CommonsArrayList <> (_otherCert (),
                                                                                                 aAPCert));
    assertErrors (aCheck, 1, _check (aCheck));
  }

  @Test
  public void testTlsNotTerminatedHere ()
  {
    StartupCheckTlsCertificateSeparation aCheck = _create ( () -> null);
    assertOneWarning (aCheck, _check (aCheck));

    aCheck = _create (CommonsArrayList::new);
    assertOneWarning (aCheck, _check (aCheck));
  }

  @Test
  public void testNoPrivateKey ()
  {
    // Reported by StartupCheckAS4KeyStore
    final StartupCheckTlsCertificateSeparation aCheck = new StartupCheckTlsCertificateSeparation ( () -> null,
                                                                                                  () -> null);
    assertErrors (aCheck, 0, _check (aCheck));
  }
}
