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

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.string.StringHelper;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.spi.APStartupCheckError;

/**
 * Verify that the TLS trust store used for outbound HTTPS connections (SMP lookup and AS4 sending)
 * actually contains trust anchors. Since phase4 4.5.0 the AP HTTP client uses the JVM default trust
 * store, honouring the <code>javax.net.ssl.trustStore</code> system property. If that trust store
 * is empty - e.g. because it was overridden with an empty or invalid file - all outbound TLS
 * handshakes fail with "PKIX path building failed" and the underlying cause is easily masked. Fail
 * at startup instead of on every transmission.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckOutboundTlsTrustStore extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "outbound-tls-truststore";

  private static final Logger LOGGER = LoggerFactory.getLogger (StartupCheckOutboundTlsTrustStore.class);

  public StartupCheckOutboundTlsTrustStore ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final String sConfiguredTrustStore = System.getProperty ("javax.net.ssl.trustStore");
    final String sTrustStoreDesc = StringHelper.isNotEmpty (sConfiguredTrustStore) ? " (configured via javax.net.ssl.trustStore='" +
                                                                                     sConfiguredTrustStore +
                                                                                     "')" : " (JRE default)";
    try
    {
      final TrustManagerFactory aTMF = TrustManagerFactory.getInstance (TrustManagerFactory.getDefaultAlgorithm ());
      // Passing null loads the JVM default trust store following the standard JSSE algorithm:
      // javax.net.ssl.trustStore, else jssecacerts, else the bundled cacerts. This is exactly the
      // trust material phase4 uses for outbound HTTPS.
      aTMF.init ((KeyStore) null);

      int nTrustAnchorCount = 0;
      for (final TrustManager aTM : aTMF.getTrustManagers ())
        if (aTM instanceof final X509TrustManager aX509TM)
          nTrustAnchorCount += aX509TM.getAcceptedIssuers ().length;

      if (nTrustAnchorCount == 0)
      {
        addError (aErrors,
                  "The TLS trust store used for outbound HTTPS connections is empty" +
                           sTrustStoreDesc +
                           ". Without trust anchors all outbound SMP lookups and AS4 transmissions fail with 'PKIX path building failed'. " +
                           "Fix the configuration - either remove the javax.net.ssl.trustStore override to use the JRE default, or point it to a valid trust store containing the required CA certificates.");
        return;
      }

      LOGGER.info ("The outbound TLS trust store contains " + nTrustAnchorCount + " trust anchor(s)" + sTrustStoreDesc);
    }
    catch (final GeneralSecurityException ex)
    {
      aErrors.add (APStartupCheckError.create (getCheckName (),
                                               "Failed to load the TLS trust store used for outbound HTTPS connections" +
                                                                sTrustStoreDesc +
                                                                " - fix the configuration: " +
                                                                ex.getMessage (),
                                               ex));
    }
  }
}
