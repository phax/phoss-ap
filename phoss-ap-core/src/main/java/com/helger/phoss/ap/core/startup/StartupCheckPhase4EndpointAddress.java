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

import java.net.URI;
import java.net.URISyntaxException;

import org.jspecify.annotations.NonNull;

import com.helger.base.string.StringHelper;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.core.APCoreConfig;

/**
 * Check that the configured AS4 endpoint address is an absolute URL using <code>https</code> on
 * port 443, as required by the Peppol AS4 profile (section 4.2, "Port 443 MUST be used for TLS").
 * A violation is an error in the Peppol production network and a warning otherwise. If no endpoint
 * address is configured, nothing is reported - {@link StartupCheckPeppolReceiverCheck} reports it
 * where it is required.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckPhase4EndpointAddress extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "phase4-endpoint-address";

  public StartupCheckPhase4EndpointAddress ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final String sEndpointAddress = APCoreConfig.getPhase4EndpointAddress ();
    if (StringHelper.isEmpty (sEndpointAddress))
      return;

    final URI aURI;
    try
    {
      aURI = new URI (sEndpointAddress);
    }
    catch (final URISyntaxException ex)
    {
      addError (aErrors,
                "The configured AS4 endpoint address '" +
                         sEndpointAddress +
                         "' (" +
                         APConfigurationProperties.PHASE4_ENDPOINT_ADDRESS +
                         ") is not a valid URL: " +
                         ex.getMessage ());
      return;
    }
    if (!aURI.isAbsolute () || StringHelper.isEmpty (aURI.getHost ()))
    {
      addError (aErrors,
                "The configured AS4 endpoint address '" +
                         sEndpointAddress +
                         "' (" +
                         APConfigurationProperties.PHASE4_ENDPOINT_ADDRESS +
                         ") is not an absolute URL with a host name");
      return;
    }

    final EPeppolNetwork ePeppolStage = APCoreConfig.getPeppolStage ();
    final boolean bIsProduction = ePeppolStage != null && ePeppolStage.isProduction ();
    if (!"https".equalsIgnoreCase (aURI.getScheme ()))
    {
      final String sMsg = "The configured AS4 endpoint address '" +
                          sEndpointAddress +
                          "' (" +
                          APConfigurationProperties.PHASE4_ENDPOINT_ADDRESS +
                          ") does not use https, which the Peppol AS4 profile requires";
      if (bIsProduction)
        addError (aErrors, sMsg);
      else
        addWarning (aErrors, sMsg + " - accepted outside of the Peppol production network");
    }
    else
      if (aURI.getPort () != -1 && aURI.getPort () != 443)
      {
        final String sMsg = "The configured AS4 endpoint address '" +
                            sEndpointAddress +
                            "' (" +
                            APConfigurationProperties.PHASE4_ENDPOINT_ADDRESS +
                            ") uses port " +
                            aURI.getPort () +
                            ", but the Peppol AS4 profile requires port 443";
        if (bIsProduction)
          addError (aErrors, sMsg);
        else
          addWarning (aErrors, sMsg + " - accepted outside of the Peppol production network");
      }
  }
}
