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

import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.util.Phase4RuntimeException;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.spi.APStartupCheckError;

/**
 * Check that the configured AS4 key store and the private key in it can be loaded. Needs the AS4
 * configuration to be set, so it runs in {@link EAPStartupCheckPhase#POST_INIT}.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckAS4KeyStore extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "as4-keystore";

  private static final Logger LOGGER = LoggerFactory.getLogger (StartupCheckAS4KeyStore.class);

  private final Supplier <? extends AS4CryptoFactoryInMemoryKeyStore> m_aCryptoFactorySupplier;

  public StartupCheckAS4KeyStore ()
  {
    this (AS4CryptoFactoryConfiguration::getDefaultInstance);
  }

  /**
   * @param aCryptoFactorySupplier
   *        Provides the crypto factory to check. May throw a {@link Phase4RuntimeException} if the
   *        configuration is incomplete. May not be <code>null</code>.
   */
  @VisibleForTesting
  StartupCheckAS4KeyStore (@NonNull final Supplier <? extends AS4CryptoFactoryInMemoryKeyStore> aCryptoFactorySupplier)
  {
    super (CHECK_NAME, EAPStartupCheckPhase.POST_INIT);
    ValueEnforcer.notNull (aCryptoFactorySupplier, "CryptoFactorySupplier");
    m_aCryptoFactorySupplier = aCryptoFactorySupplier;
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final AS4CryptoFactoryInMemoryKeyStore aCryptoFactory;
    try
    {
      // Throws an exception if configuration parameters are missing
      aCryptoFactory = m_aCryptoFactorySupplier.get ();
    }
    catch (final Phase4RuntimeException ex)
    {
      aErrors.add (APStartupCheckError.create (getCheckName (),
                                               "The AS4 crypto configuration is incomplete - fix the configuration: " +
                                                                ex.getMessage (),
                                               ex));
      return;
    }

    if (aCryptoFactory.getKeyStore () == null)
    {
      addError (aErrors, "Failed to load configured AS4 Key store - fix the configuration");
      return;
    }
    LOGGER.info ("Successfully loaded configured AS4 key store from the crypto factory");

    if (aCryptoFactory.getPrivateKeyEntry () == null)
    {
      addError (aErrors,
                "Failed to load configured AS4 private key with the alias '" +
                         aCryptoFactory.getKeyAlias () +
                         "' - fix the configuration");
      return;
    }
    LOGGER.info ("Successfully loaded configured AS4 private key (alias '" +
                 aCryptoFactory.getKeyAlias () +
                 "') from the crypto factory");
  }
}
