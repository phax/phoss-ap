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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;

import org.junit.Test;

import com.helger.diagnostics.error.list.ErrorList;

/**
 * Test class for class {@link StartupCheckOutboundTlsTrustStore}.
 *
 * @author Philip Helger
 */
public final class StartupCheckOutboundTlsTrustStoreTest
{
  private static final String PROP_TRUSTSTORE = "javax.net.ssl.trustStore";
  private static final String PROP_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";
  private static final String PROP_TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";

  private final StartupCheckOutboundTlsTrustStore m_aCheck = new StartupCheckOutboundTlsTrustStore ();

  @Test
  public void testJreDefault ()
  {
    final String sOld = System.getProperty (PROP_TRUSTSTORE);
    try
    {
      System.clearProperty (PROP_TRUSTSTORE);
      final ErrorList aFindings = new ErrorList ();
      m_aCheck.performCheck (aFindings);
      assertErrors (m_aCheck, 0, aFindings);
    }
    finally
    {
      if (sOld != null)
        System.setProperty (PROP_TRUSTSTORE, sOld);
    }
  }

  private static void _restore (final String sKey, final String sOldValue)
  {
    if (sOldValue == null)
      System.clearProperty (sKey);
    else
      System.setProperty (sKey, sOldValue);
  }

  @Test
  public void testEmptyTrustStore () throws Exception
  {
    // A valid trust store without any certificate
    final File aFile = new File ("target/startup-check-empty-truststore.p12").getAbsoluteFile ();
    final KeyStore aKS = KeyStore.getInstance ("PKCS12");
    aKS.load (null, null);
    try (final OutputStream aOS = new FileOutputStream (aFile))
    {
      aKS.store (aOS, "password".toCharArray ());
    }

    final String sOld = System.getProperty (PROP_TRUSTSTORE);
    final String sOldPassword = System.getProperty (PROP_TRUSTSTORE_PASSWORD);
    final String sOldType = System.getProperty (PROP_TRUSTSTORE_TYPE);
    try
    {
      System.setProperty (PROP_TRUSTSTORE, aFile.getAbsolutePath ());
      System.setProperty (PROP_TRUSTSTORE_PASSWORD, "password");
      System.setProperty (PROP_TRUSTSTORE_TYPE, "PKCS12");
      final ErrorList aFindings = new ErrorList ();
      m_aCheck.performCheck (aFindings);
      assertErrors (m_aCheck, 1, aFindings);
    }
    finally
    {
      _restore (PROP_TRUSTSTORE, sOld);
      _restore (PROP_TRUSTSTORE_PASSWORD, sOldPassword);
      _restore (PROP_TRUSTSTORE_TYPE, sOldType);
    }
  }
}
