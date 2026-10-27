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

import org.jspecify.annotations.NonNull;

import com.helger.base.string.StringHelper;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.codelist.EReceiverCheckMode;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.core.APCoreConfig;

/**
 * Check that the configuration required by the Peppol receiver check mode is present. Mode
 * <code>smp</code> requires the SMP URL and the AP endpoint address, mode <code>sml</code> requires
 * the AP endpoint address. Mode <code>none</code> results in a warning.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckPeppolReceiverCheck extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "peppol-receiver-check";

  public StartupCheckPeppolReceiverCheck ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final EReceiverCheckMode eReceiverCheckMode = APCoreConfig.getReceiverCheckMode ();
    switch (eReceiverCheckMode)
    {
      case SMP:
      {
        if (StringHelper.isEmpty (APCoreConfig.getPeppolSmpUrl ()))
          addError (aErrors,
                    "Receiver check mode 'smp' requires configuration property '" +
                             APConfigurationProperties.PEPPOL_SMP_URL +
                             "' to be set");
        if (StringHelper.isEmpty (APCoreConfig.getPhase4EndpointAddress ()))
          addError (aErrors,
                    "Receiver check mode 'smp' requires configuration property '" +
                             APConfigurationProperties.PHASE4_ENDPOINT_ADDRESS +
                             "' to be set");
        break;
      }
      case SML:
      {
        if (StringHelper.isEmpty (APCoreConfig.getPhase4EndpointAddress ()))
          addError (aErrors,
                    "Receiver check mode 'sml' requires configuration property '" +
                             APConfigurationProperties.PHASE4_ENDPOINT_ADDRESS +
                             "' to be set");
        break;
      }
      case NONE:
      {
        addWarning (aErrors,
                    "phase4 Peppol receiver checks are disabled - configure '" +
                             APConfigurationProperties.PEPPOL_RECEIVER_CHECK_MODE +
                             "' to enable them");
        break;
      }
      default:
        addError (aErrors, "Unsupported receiver check mode: " + eReceiverCheckMode);
    }
  }
}
