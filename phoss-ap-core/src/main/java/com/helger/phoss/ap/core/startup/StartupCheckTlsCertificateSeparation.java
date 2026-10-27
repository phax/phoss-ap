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

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.security.certificate.CertificateHelper;

/**
 * Check that the Peppol AP certificate (or its key) is not used as the TLS server certificate.
 * Peppol requires the separation. The TLS certificates are provided by the caller, because only the
 * hosting environment knows them. If TLS is terminated in front of the AP (e.g. by a reverse proxy),
 * the rule cannot be checked and a warning says so, because an unchecked rule must not look like a
 * passed one.
 * <p>
 * This check is not a built-in check. It is registered by the hosting environment that knows its TLS
 * configuration.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckTlsCertificateSeparation extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "tls-certificate-separation";

  private final Supplier <? extends AS4CryptoFactoryInMemoryKeyStore> m_aCryptoFactorySupplier;
  private final Supplier <? extends List <X509Certificate>> m_aTlsCertificatesSupplier;

  /**
   * @param aTlsCertificatesSupplier
   *        Provides the TLS server certificates of this installation. The supplier may return
   *        <code>null</code> or an empty list if TLS is not terminated by this application. May
   *        not be <code>null</code>.
   */
  public StartupCheckTlsCertificateSeparation (@NonNull final Supplier <? extends List <X509Certificate>> aTlsCertificatesSupplier)
  {
    this (AS4CryptoFactoryConfiguration::getDefaultInstanceOrNull, aTlsCertificatesSupplier);
  }

  @VisibleForTesting
  StartupCheckTlsCertificateSeparation (@NonNull final Supplier <? extends AS4CryptoFactoryInMemoryKeyStore> aCryptoFactorySupplier,
                                        @NonNull final Supplier <? extends List <X509Certificate>> aTlsCertificatesSupplier)
  {
    super (CHECK_NAME, EAPStartupCheckPhase.POST_INIT);
    ValueEnforcer.notNull (aCryptoFactorySupplier, "CryptoFactorySupplier");
    ValueEnforcer.notNull (aTlsCertificatesSupplier, "TlsCertificatesSupplier");
    m_aCryptoFactorySupplier = aCryptoFactorySupplier;
    m_aTlsCertificatesSupplier = aTlsCertificatesSupplier;
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final AS4CryptoFactoryInMemoryKeyStore aCryptoFactory = m_aCryptoFactorySupplier.get ();
    final KeyStore.PrivateKeyEntry aPKE = aCryptoFactory == null ? null : aCryptoFactory.getPrivateKeyEntry ();
    if (aPKE == null)
    {
      // Already reported by StartupCheckAS4KeyStore
      return;
    }
    final X509Certificate aAPCert = (X509Certificate) aPKE.getCertificate ();

    final List <X509Certificate> aTlsCerts = m_aTlsCertificatesSupplier.get ();
    if (aTlsCerts == null || aTlsCerts.isEmpty ())
    {
      addWarning (aErrors,
                  "The separation of the Peppol AP certificate and the TLS certificate could not be checked, because TLS is not terminated by phoss AP. Make sure the TLS endpoint in front of it does not use the Peppol AP certificate or its key.");
      return;
    }

    for (final X509Certificate aTlsCert : aTlsCerts)
      // Same public key covers the same certificate as well as a re-issued one with the same key
      if (aAPCert.getPublicKey ().equals (aTlsCert.getPublicKey ()))
      {
        addError (aErrors,
                  "The TLS server certificate (subject CN '" +
                           CertificateHelper.getSubjectCN (aTlsCert) +
                           "') uses the key of the Peppol AP certificate (subject CN '" +
                           CertificateHelper.getSubjectCN (aAPCert) +
                           "'). Peppol requires a separate TLS certificate.");
        return;
      }
  }
}
