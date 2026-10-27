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
import static com.helger.phoss.ap.core.startup.StartupCheckTestHelper.createConfig;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;

/**
 * Test class for class {@link StartupCheckAS4KeyStore}.
 *
 * @author Philip Helger
 */
public final class StartupCheckAS4KeyStoreTest
{
  static final String KEYSTORE_PREFIX = "org.apache.wss4j.crypto.merlin.keystore.";

  @NonNull
  static ICommonsMap <String, String> createKeyStoreConfig ()
  {
    final ICommonsMap <String, String> ret = new CommonsHashMap <> ();
    ret.put (KEYSTORE_PREFIX + "type", "pkcs12");
    ret.put (KEYSTORE_PREFIX + "file", "startup-check/invalid-keystore-pw-peppol.p12");
    ret.put (KEYSTORE_PREFIX + "password", "peppol");
    ret.put (KEYSTORE_PREFIX + "alias", "private_key_for_pkcs12_certificate");
    ret.put (KEYSTORE_PREFIX + "private.password", "peppol");
    return ret;
  }

  /**
   * @return The configuration of a self-signed test key store whose certificate has the subject CN
   *         <code>POP000001</code>.
   */
  @NonNull
  static ICommonsMap <String, String> createPopKeyStoreConfig ()
  {
    final ICommonsMap <String, String> ret = new CommonsHashMap <> ();
    ret.put (KEYSTORE_PREFIX + "type", "pkcs12");
    ret.put (KEYSTORE_PREFIX + "file", "startup-check/pop000001.p12");
    ret.put (KEYSTORE_PREFIX + "password", "password");
    ret.put (KEYSTORE_PREFIX + "alias", "ap");
    ret.put (KEYSTORE_PREFIX + "private.password", "password");
    return ret;
  }

  @NonNull
  private static ErrorList _check (@NonNull final Map <String, String> aValues)
  {
    final StartupCheckAS4KeyStore aCheck = new StartupCheckAS4KeyStore ( () -> new AS4CryptoFactoryConfiguration (createConfig (aValues)));
    final ErrorList ret = new ErrorList ();
    aCheck.performCheck (ret);
    return ret;
  }

  @Test
  public void testValid ()
  {
    assertErrors (new StartupCheckAS4KeyStore (), 0, _check (createKeyStoreConfig ()));
  }

  @Test
  public void testMissingConfiguration ()
  {
    assertErrors (new StartupCheckAS4KeyStore (), 1, _check (Map.of ()));
  }

  @Test
  public void testWrongKeyStorePassword ()
  {
    final ICommonsMap <String, String> aValues = createKeyStoreConfig ();
    aValues.put (KEYSTORE_PREFIX + "password", "wrong");
    assertErrors (new StartupCheckAS4KeyStore (), 1, _check (aValues));
  }

  @Test
  public void testWrongAlias ()
  {
    final ICommonsMap <String, String> aValues = createKeyStoreConfig ();
    aValues.put (KEYSTORE_PREFIX + "alias", "wrong");
    assertErrors (new StartupCheckAS4KeyStore (), 1, _check (aValues));
  }
}
