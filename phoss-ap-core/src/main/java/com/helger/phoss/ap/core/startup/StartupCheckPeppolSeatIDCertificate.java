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
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.security.certificate.CertificateHelper;

/**
 * Check that the configured Peppol Seat ID equals the subject CN of the AP certificate. The Seat ID
 * is sent as the AS4 <code>eb:From/eb:PartyId</code>, and the Peppol AS4 profile (section 4.5,
 * "Party Identification") requires the PartyId to be the subject CN of the Peppol AP certificate. If
 * the two differ, the counterpart rejects the messages while this AP looks fine from the inside.
 * <p>
 * If the private key cannot be loaded or the Seat ID is syntactically invalid, this check reports
 * nothing, because {@link StartupCheckAS4KeyStore} and {@link StartupCheckPeppolSeatID} already did.
 * If the system property {@value StartupCheckPeppolAPCertificate#SYSTEM_PROPERTY_SKIP} is set to
 * <code>true</code>, a mismatch is only a warning, because the certificate is then not a Peppol AP
 * certificate anyway.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckPeppolSeatIDCertificate extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "peppol-seat-id-certificate";

  private final Supplier <? extends AS4CryptoFactoryInMemoryKeyStore> m_aCryptoFactorySupplier;

  public StartupCheckPeppolSeatIDCertificate ()
  {
    this (AS4CryptoFactoryConfiguration::getDefaultInstanceOrNull);
  }

  /**
   * @param aCryptoFactorySupplier
   *        Provides the crypto factory whose certificate is checked. The supplier may return
   *        <code>null</code>. May not be <code>null</code>.
   */
  @VisibleForTesting
  StartupCheckPeppolSeatIDCertificate (@NonNull final Supplier <? extends AS4CryptoFactoryInMemoryKeyStore> aCryptoFactorySupplier)
  {
    super (CHECK_NAME, EAPStartupCheckPhase.POST_INIT);
    ValueEnforcer.notNull (aCryptoFactorySupplier, "CryptoFactorySupplier");
    m_aCryptoFactorySupplier = aCryptoFactorySupplier;
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final String sSeatID = APCoreConfig.getPeppolOwnerSeatID ();
    if (!CPhossAP.isPeppolSeatID (sSeatID))
    {
      // Already reported by StartupCheckPeppolSeatID
      return;
    }

    final AS4CryptoFactoryInMemoryKeyStore aCryptoFactory = m_aCryptoFactorySupplier.get ();
    final KeyStore.PrivateKeyEntry aPKE = aCryptoFactory == null ? null : aCryptoFactory.getPrivateKeyEntry ();
    if (aPKE == null)
    {
      // Already reported by StartupCheckAS4KeyStore
      return;
    }

    final String sCN = CertificateHelper.getSubjectCN ((X509Certificate) aPKE.getCertificate ());
    if (!sSeatID.equals (sCN))
    {
      final String sMsg = "The configured Peppol Seat ID '" +
                          sSeatID +
                          "' (" +
                          APConfigurationProperties.PEPPOL_OWNER_SEATID +
                          ") differs from the subject CN '" +
                          sCN +
                          "' of the AP certificate. The Peppol AS4 profile requires the AS4 PartyId to be the subject CN of the AP certificate, so the other APs would reject the messages.";
      if ("true".equals (System.getProperty (StartupCheckPeppolAPCertificate.SYSTEM_PROPERTY_SKIP)))
        addWarning (aErrors,
                    sMsg +
                             " Ignored because '" +
                             StartupCheckPeppolAPCertificate.SYSTEM_PROPERTY_SKIP +
                             "' is set. For unit tests only!");
      else
        addError (aErrors, sMsg);
    }
  }
}
