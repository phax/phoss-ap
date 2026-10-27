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
package com.helger.phoss.ap.core.forwarding;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phoss.ap.api.codelist.EForwardingMode;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;

/**
 * The configuration of one document forwarder: the primary one, a secondary one or the MLS copy
 * one. Used by the startup check and by the initialization, so that both read the forwarder
 * configuration the same way.
 *
 * @param description
 *        A human readable description for messages, e.g. "secondary forwarder #2". Never
 *        <code>null</code>.
 * @param configPrefix
 *        The configuration key prefix of this forwarder. Never <code>null</code>.
 * @param modeConfigKey
 *        The configuration key the mode was read from. Never <code>null</code>.
 * @param configuredMode
 *        The configured mode as is. May be <code>null</code>.
 * @param mode
 *        The parsed mode, or <code>null</code> if the configured mode is missing or invalid.
 * @author Philip Helger
 * @since 0.13.3
 */
public record ForwarderConfigSlot (@NonNull @Nonempty String description,
                                   @NonNull @Nonempty String configPrefix,
                                   @NonNull @Nonempty String modeConfigKey,
                                   @Nullable String configuredMode,
                                   @Nullable EForwardingMode mode)
{
  @NonNull
  private static ForwarderConfigSlot _create (@NonNull final IConfigWithFallback aConfig,
                                              @NonNull @Nonempty final String sDescription,
                                              @NonNull @Nonempty final String sConfigPrefix,
                                              @NonNull @Nonempty final String sModeConfigKey)
  {
    final String sConfiguredMode = aConfig.getAsString (sModeConfigKey);
    return new ForwarderConfigSlot (sDescription,
                                    sConfigPrefix,
                                    sModeConfigKey,
                                    sConfiguredMode,
                                    EForwardingMode.getFromIDOrNull (sConfiguredMode));
  }

  /**
   * @param aConfig
   *        The configuration to read from. May not be <code>null</code>.
   * @return The slot of the primary forwarder. Never <code>null</code>.
   */
  @NonNull
  public static ForwarderConfigSlot getPrimary (@NonNull final IConfigWithFallback aConfig)
  {
    return _create (aConfig,
                    "primary forwarder",
                    IDocumentForwarder.DEFAULT_CONFIG_KEY_PREFIX,
                    APConfigurationProperties.FORWARDING_MODE);
  }

  /**
   * @param aConfig
   *        The configuration to read from. May not be <code>null</code>.
   * @return The slots of all configured secondary forwarders, in index order, starting at index 1
   *         and ending before the first index without a mode. This may be more than
   *         {@link com.helger.phoss.ap.core.APCoreMetaManager#MAX_SECONDARY_FORWARDERS}. Never
   *         <code>null</code> but maybe empty.
   */
  @NonNull
  public static ICommonsList <ForwarderConfigSlot> getAllSecondaries (@NonNull final IConfigWithFallback aConfig)
  {
    final ICommonsList <ForwarderConfigSlot> ret = new CommonsArrayList <> ();
    for (int nIndex = 1;; nIndex++)
    {
      final String sPrefix = APConfigurationProperties.FORWARDING_SECONDARY_PREFIX + nIndex + ".";
      final ForwarderConfigSlot aSlot = _create (aConfig,
                                                 "secondary forwarder #" + nIndex,
                                                 sPrefix,
                                                 sPrefix + APConfigurationProperties.FORWARDING_SECONDARY_MODE_SUFFIX);
      if (aSlot.configuredMode () == null)
        break;
      ret.add (aSlot);
    }
    return ret;
  }

  /**
   * @param aConfig
   *        The configuration to read from. May not be <code>null</code>.
   * @return The slot of the MLS copy forwarder, or <code>null</code> if it is disabled. Without an
   *         own mode, the whole primary forwarder configuration is reused.
   */
  @Nullable
  public static ForwarderConfigSlot getMlsCopy (@NonNull final IConfigWithFallback aConfig)
  {
    if (!aConfig.getAsBoolean (APConfigurationProperties.FORWARDING_MLS_COPY_ENABLED,
                               APConfigurationProperties.FORWARDING_MLS_COPY_ENABLED_DEFAULT))
      return null;

    if (StringHelper.isNotEmpty (aConfig.getAsString (APConfigurationProperties.FORWARDING_MLS_COPY_MODE)))
      return _create (aConfig,
                      "MLS copy forwarder",
                      APConfigurationProperties.FORWARDING_MLS_COPY_PREFIX,
                      APConfigurationProperties.FORWARDING_MLS_COPY_MODE);

    return _create (aConfig,
                    "MLS copy forwarder",
                    IDocumentForwarder.DEFAULT_CONFIG_KEY_PREFIX,
                    APConfigurationProperties.FORWARDING_MODE);
  }

  /**
   * @return <code>true</code> if this slot reuses the primary forwarder configuration.
   */
  public boolean isUsingPrimaryConfig ()
  {
    return IDocumentForwarder.DEFAULT_CONFIG_KEY_PREFIX.equals (configPrefix);
  }
}
