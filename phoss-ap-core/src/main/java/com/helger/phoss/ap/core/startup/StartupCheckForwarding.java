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

import java.util.ServiceLoader;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.codelist.EForwardingMode;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.spi.APStartupCheckError;
import com.helger.phoss.ap.api.spi.IDocumentForwarderProviderSPI;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.forwarding.ForwarderConfigSlot;

/**
 * Check the forwarding configuration: the modes of the primary, the secondary and the MLS copy
 * forwarder are valid, there are not too many secondary forwarders, the IDs of the forwarder
 * provider SPIs are unique, and every forwarder with mode <code>spi</code> references an existing
 * provider. The forwarder specific settings (endpoint URL, bucket, ...) are verified when the
 * forwarders are initialized.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckForwarding extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "forwarding";

  public StartupCheckForwarding ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  /**
   * Check that the IDs of all document forwarder provider SPIs are set and unique, because the
   * configuration selects a provider by its ID.
   *
   * @param aAllProviders
   *        All loaded forwarder providers. May not be <code>null</code>.
   * @param aErrors
   *        The error list to add to. May not be <code>null</code>.
   */
  @VisibleForTesting
  static void checkForwarderProviderIDsAreUnique (@NonNull final Iterable <? extends IDocumentForwarderProviderSPI> aAllProviders,
                                                  @NonNull final ErrorList aErrors)
  {
    final ICommonsMap <String, Class <?>> aIDToClass = new CommonsHashMap <> ();
    for (final IDocumentForwarderProviderSPI aProvider : aAllProviders)
    {
      final String sID = aProvider.getID ();
      if (StringHelper.isEmpty (sID))
      {
        aErrors.add (APStartupCheckError.create (CHECK_NAME,
                                                 "The document forwarder provider " +
                                                             aProvider.getClass ().getName () +
                                                             " provides an empty ID"));
        continue;
      }

      final Class <?> aExistingClass = aIDToClass.put (sID, aProvider.getClass ());
      if (aExistingClass != null)
        aErrors.add (APStartupCheckError.create (CHECK_NAME,
                                                 "The document forwarder provider ID '" +
                                                             sID +
                                                             "' is used by " +
                                                             aExistingClass.getName () +
                                                             " and by " +
                                                             aProvider.getClass ().getName () +
                                                             ". Forwarder provider IDs must be unique over all providers."));
    }
  }

  private void _checkSlot (@NonNull final IConfigWithFallback aConfig,
                           @NonNull final ForwarderConfigSlot aSlot,
                           @NonNull final ICommonsList <IDocumentForwarderProviderSPI> aAllProviders,
                           @NonNull final ErrorList aErrors)
  {
    final EForwardingMode eMode = aSlot.mode ();
    if (eMode == null)
    {
      addError (aErrors,
                "The configured forwarding mode '" +
                         aSlot.configuredMode () +
                         "' of the " +
                         aSlot.description () +
                         " ('" +
                         aSlot.modeConfigKey () +
                         "') is missing or invalid");
      return;
    }

    if (eMode == EForwardingMode.SPI)
    {
      final String sIDKey = aSlot.configPrefix () + APConfigurationProperties.FORWARDING_SPI_ID_SUFFIX;
      final String sProviderID = aConfig.getAsString (sIDKey);
      if (StringHelper.isEmpty (sProviderID))
        addError (aErrors,
                  "Forwarding mode 'spi' of the " +
                           aSlot.description () +
                           " requires configuration property '" +
                           sIDKey +
                           "' to be set");
      else
        if (!aAllProviders.containsAny (x -> sProviderID.equals (x.getID ())))
          addError (aErrors,
                    "No document forwarder provider SPI found for ID '" +
                             sProviderID +
                             "' from configuration property '" +
                             sIDKey +
                             "'");
    }
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final IConfigWithFallback aConfig = APConfigProvider.getConfig ();

    final ICommonsList <IDocumentForwarderProviderSPI> aAllProviders = new CommonsArrayList <> ();
    for (final IDocumentForwarderProviderSPI aProvider : ServiceLoader.load (IDocumentForwarderProviderSPI.class))
      aAllProviders.add (aProvider);
    checkForwarderProviderIDsAreUnique (aAllProviders, aErrors);

    _checkSlot (aConfig, ForwarderConfigSlot.getPrimary (aConfig), aAllProviders, aErrors);

    final ICommonsList <ForwarderConfigSlot> aSecondaries = ForwarderConfigSlot.getAllSecondaries (aConfig);
    for (final ForwarderConfigSlot aSlot : aSecondaries)
      _checkSlot (aConfig, aSlot, aAllProviders, aErrors);
    if (aSecondaries.size () > APCoreMetaManager.MAX_SECONDARY_FORWARDERS)
      addError (aErrors,
                "No more than " +
                         APCoreMetaManager.MAX_SECONDARY_FORWARDERS +
                         " secondary Forwarders are allowed, but " +
                         aSecondaries.size () +
                         " are configured.");

    final ForwarderConfigSlot aMlsCopySlot = ForwarderConfigSlot.getMlsCopy (aConfig);
    // If the primary configuration is reused, it was already checked
    if (aMlsCopySlot != null && !aMlsCopySlot.isUsingPrimaryConfig ())
      _checkSlot (aConfig, aMlsCopySlot, aAllProviders, aErrors);
  }
}
