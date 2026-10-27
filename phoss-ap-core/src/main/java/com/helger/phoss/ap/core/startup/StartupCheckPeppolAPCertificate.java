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
import java.time.OffsetDateTime;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ETriState;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.security.certificate.ECertificateCheckResult;
import com.helger.security.certificate.TrustedCAChecker;

/**
 * Check that the certificate of the configured AS4 private key is a Peppol AP certificate of the
 * configured Peppol stage. Runs in {@link EAPStartupCheckPhase#POST_INIT} after
 * {@link StartupCheckAS4KeyStore}. If the private key cannot be loaded, this check reports nothing,
 * because {@link StartupCheckAS4KeyStore} already did.
 * <p>
 * For unit tests only, the system property {@value #SYSTEM_PROPERTY_SKIP} can be set to
 * <code>true</code> to downgrade an invalid certificate to a warning.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckPeppolAPCertificate extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "peppol-ap-certificate";
  public static final String SYSTEM_PROPERTY_SKIP = "phossap.internal.skip-peppol-certificate-check";

  private final Supplier <? extends AS4CryptoFactoryInMemoryKeyStore> m_aCryptoFactorySupplier;
  private final Supplier <OffsetDateTime> m_aNowSupplier;

  public StartupCheckPeppolAPCertificate ()
  {
    this (AS4CryptoFactoryConfiguration::getDefaultInstanceOrNull,
          () -> MetaAS4Manager.getTimestampMgr ().getCurrentDateTime ());
  }

  /**
   * @param aCryptoFactorySupplier
   *        Provides the crypto factory whose certificate is checked. The supplier may return
   *        <code>null</code>. May not be <code>null</code>.
   * @param aNowSupplier
   *        Provides the date and time to check the certificate at. May not be <code>null</code>.
   */
  @VisibleForTesting
  StartupCheckPeppolAPCertificate (@NonNull final Supplier <? extends AS4CryptoFactoryInMemoryKeyStore> aCryptoFactorySupplier,
                                   @NonNull final Supplier <OffsetDateTime> aNowSupplier)
  {
    super (CHECK_NAME, EAPStartupCheckPhase.POST_INIT);
    ValueEnforcer.notNull (aCryptoFactorySupplier, "CryptoFactorySupplier");
    ValueEnforcer.notNull (aNowSupplier, "NowSupplier");
    m_aCryptoFactorySupplier = aCryptoFactorySupplier;
    m_aNowSupplier = aNowSupplier;
  }

  /**
   * @param ePeppolStage
   *        The Peppol stage. May not be <code>null</code>.
   * @return The checker for Peppol AP certificates of the provided stage. Never <code>null</code>.
   */
  @NonNull
  public static TrustedCAChecker getAPCAChecker (@NonNull final EPeppolNetwork ePeppolStage)
  {
    return ePeppolStage.isProduction () ? PeppolTrustedCA.peppolProductionAP () : PeppolTrustedCA.peppolTestAP ();
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final EPeppolNetwork ePeppolStage = APCoreConfig.getPeppolStage ();
    if (ePeppolStage == null)
    {
      // Cannot happen after a successful PRE_INIT phase, but the check may also run on its own
      addError (aErrors, "The Peppol Stage configuration is missing or invalid");
      return;
    }

    final AS4CryptoFactoryInMemoryKeyStore aCryptoFactory = m_aCryptoFactorySupplier.get ();
    final KeyStore.PrivateKeyEntry aPKE = aCryptoFactory == null ? null : aCryptoFactory.getPrivateKeyEntry ();
    if (aPKE == null)
    {
      // Already reported by StartupCheckAS4KeyStore - a second finding would only be misleading
      return;
    }

    final X509Certificate aAPCert = (X509Certificate) aPKE.getCertificate ();

    // Check the configured Peppol AP certificate
    // * No caching
    // * Use global certificate check mode
    final ECertificateCheckResult eCheckResult = getAPCAChecker (ePeppolStage).checkCertificate (aAPCert,
                                                                                               m_aNowSupplier.get (),
                                                                                               ETriState.FALSE,
                                                                                               null);
    if (eCheckResult.isInvalid ())
    {
      if ("true".equals (System.getProperty (SYSTEM_PROPERTY_SKIP)))
        addWarning (aErrors,
                    "The provided certificate is not a Peppol AP certificate (check result: " +
                             eCheckResult +
                             ") - ignored because '" +
                             SYSTEM_PROPERTY_SKIP +
                             "' is set. For unit tests only!");
      else
        addError (aErrors, "The provided certificate is not a Peppol AP certificate. Check result: " + eCheckResult);
    }
  }
}
