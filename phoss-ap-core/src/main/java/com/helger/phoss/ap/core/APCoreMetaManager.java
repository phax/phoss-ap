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
package com.helger.phoss.ap.core;

import java.util.ServiceLoader;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.exception.InitializationException;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.peppol.apsupport.BusinessCardCache;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.phoss.ap.api.codelist.EC4CountryCodeMode;
import com.helger.phoss.ap.api.codelist.EForwardingMode;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.spi.IAPLifecycleEventSPI;
import com.helger.phoss.ap.api.spi.IAPNotificationHandlerSPI;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IOutboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IPeppolReceiverCheckSPI;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.phoss.ap.core.forwarding.DocumentForwarderFactory;
import com.helger.phoss.ap.core.notification.LifecycleEventManager;
import com.helger.phoss.ap.core.notification.NotificationHandlerManager;
import com.helger.smpclient.httpclient.SMPHttpClientSettings;

/**
 * Central manager for core AP components including the document forwarder, inbound/outbound
 * document verifiers, receiver checks, and notification handlers. Components are initialized from
 * configuration and via {@link ServiceLoader} SPI.
 *
 * @author Philip Helger
 */
public final class APCoreMetaManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APCoreMetaManager.class);

  private static EForwardingMode s_eForwardingMode;
  private static IDocumentForwarder s_aForwarder;
  private static final ICommonsList <IDocumentForwarder> s_aSecondaryForwarders = new CommonsArrayList <> ();
  private static BusinessCardCache s_aBusinessCardCache;
  private static final ICommonsList <IInboundDocumentVerifierSPI> s_aInboundVerifiers = new CommonsArrayList <> ();
  private static final ICommonsList <IOutboundDocumentVerifierSPI> s_aOutboundVerifiers = new CommonsArrayList <> ();
  private static final ICommonsList <IPeppolReceiverCheckSPI> s_aReceiverChecks = new CommonsArrayList <> ();

  private APCoreMetaManager ()
  {}

  /**
   * Initialize the core meta manager by creating the document forwarder from configuration and
   * loading all SPI-based verifiers, receiver checks, and notification handlers.
   */
  public static void init ()
  {
    LOGGER.info ("Initializing APMetaManager");

    final IConfigWithFallback aConfig = APConfigProvider.getConfig ();

    // Create primary forwarder based on configuration
    {
      final String sForwardingMode = aConfig.getAsString (APConfigurationProperties.FORWARDING_MODE);
      final EForwardingMode eForwardingMode = EForwardingMode.getFromIDOrNull (sForwardingMode);
      if (eForwardingMode == null)
        throw new InitializationException ("The configured Forwarding Mode '" + sForwardingMode + "' is invalid");

      final IDocumentForwarder aForwarder = DocumentForwarderFactory.create (eForwardingMode,
                                                                             aConfig,
                                                                             IDocumentForwarder.DEFAULT_CONFIG_KEY_PREFIX);
      if (aForwarder.initFromConfiguration (aConfig, IDocumentForwarder.DEFAULT_CONFIG_KEY_PREFIX).isFailure ())
        throw new InitializationException ("Failed to init forwarder configuration - see logs for details");

      s_eForwardingMode = eForwardingMode;
      s_aForwarder = aForwarder;
      LOGGER.info ("Loaded document forwarder: " + aForwarder.toString ());
    }

    // Create secondary forwarders (fire-and-forget, no retry, no SLA) based on configuration
    {
      for (int nIndex = 1;; nIndex++)
      {
        final String sSecondaryPrefix = APConfigurationProperties.FORWARDING_SECONDARY_PREFIX + nIndex + ".";
        final String sSecondaryMode = aConfig.getAsString (sSecondaryPrefix +
                                                           APConfigurationProperties.FORWARDING_SECONDARY_MODE_SUFFIX);
        if (sSecondaryMode == null)
          break;

        final EForwardingMode eSecondaryMode = EForwardingMode.getFromIDOrNull (sSecondaryMode);
        if (eSecondaryMode == null)
          throw new InitializationException ("The configured Forwarding Mode for secondary forwarder at index " +
                                             nIndex +
                                             " ('" +
                                             sSecondaryMode +
                                             "') is invalid");

        if (nIndex > 10)
          throw new InitializationException ("No more than 10 secondary Forwarders are allowed.");

        final IDocumentForwarder aSecondary = DocumentForwarderFactory.create (eSecondaryMode, aConfig, sSecondaryPrefix);
        if (aSecondary.initFromConfiguration (aConfig, sSecondaryPrefix).isFailure ())
          throw new InitializationException ("Failed to init secondary forwarder #" +
                                             nIndex +
                                             " configuration - see logs for details");

        s_aSecondaryForwarders.add (aSecondary);
        LOGGER.info ("Loaded secondary document forwarder #" + nIndex + ": " + aSecondary.toString ());
      }
      if (s_aSecondaryForwarders.isEmpty ())
        LOGGER.info ("No secondary document forwarders configured");
      else
        LOGGER.info ("Loaded " + s_aSecondaryForwarders.size () + " secondary document forwarder(s)");
    }

    // Initialize Business Card Cache if configured
    {
      final ICommonsOrderedSet <EC4CountryCodeMode> aModes = APCoreConfig.getC4CountryCodeModes ();
      if (aModes.contains (EC4CountryCodeMode.BUSINESS_CARD_CACHE))
      {
        final EPeppolNetwork ePeppolStage = APCoreConfig.getPeppolStage ();
        if (ePeppolStage == null)
          throw new InitializationException ("Peppol stage must be configured when using C4 country code mode 'business_card'");

        final SMPHttpClientSettings aHCS = new SMPHttpClientSettings ();
        APBasicConfig.applyHttpProxySettings (aHCS);
        s_aBusinessCardCache = new BusinessCardCache (ePeppolStage.getSMLInfo (), aHCS);
        LOGGER.info ("Initialized BusinessCardCache for C4 country code determination");
      }
      if (aModes.isNotEmpty ())
        LOGGER.info ("C4 country code determination modes: " + aModes);
    }

    for (final IInboundDocumentVerifierSPI aVerifier : ServiceLoader.load (IInboundDocumentVerifierSPI.class))
    {
      s_aInboundVerifiers.add (aVerifier);
      LOGGER.info ("Loaded inbound document verifier: " + aVerifier.getClass ().getName ());
    }

    for (final IOutboundDocumentVerifierSPI aVerifier : ServiceLoader.load (IOutboundDocumentVerifierSPI.class))
    {
      s_aOutboundVerifiers.add (aVerifier);
      LOGGER.info ("Loaded outbound document verifier: " + aVerifier.getClass ().getName ());
    }

    for (final IPeppolReceiverCheckSPI aCheck : ServiceLoader.load (IPeppolReceiverCheckSPI.class))
    {
      s_aReceiverChecks.add (aCheck);
      LOGGER.info ("Loaded receiver check: " + aCheck.getClass ().getName ());
    }

    NotificationHandlerManager.initSPI ();
    LifecycleEventManager.initSPI ();

    LOGGER.info ("APMetaManager initialized successfully");
  }

  /**
   * Shutdown the core meta manager and release all resources.
   */
  public static void shutdown ()
  {
    LOGGER.info ("Shutting down APMetaManager");
  }

  /**
   * @return The configured forwarding mode. Never <code>null</code>.
   */
  @NonNull
  public static EForwardingMode getForwardingMode ()
  {
    return s_eForwardingMode;
  }

  /**
   * @return The configured document forwarder instance. Never <code>null</code>.
   */
  @NonNull
  public static IDocumentForwarder getForwarder ()
  {
    return s_aForwarder;
  }

  /**
   * @return A mutable copy of all configured secondary document forwarders. Never <code>null</code>
   *         but may be empty. Secondary forwarders are dispatched on a fire-and-forget basis after
   *         the primary forwarder succeeded; they have no retries and their failure does not affect
   *         the inbound transaction status.
   * @since 0.9.0
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IDocumentForwarder> getAllSecondaryForwarders ()
  {
    return s_aSecondaryForwarders.getClone ();
  }

  /**
   * @return The Business Card Cache for C4 country code lookup, or <code>null</code> if not
   *         configured.
   * @since v0.1.3
   */
  @Nullable
  public static BusinessCardCache getBusinessCardCache ()
  {
    return s_aBusinessCardCache;
  }

  /**
   * @return A mutable copy of all registered inbound document verifiers. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IInboundDocumentVerifierSPI> getAllInboundVerifiers ()
  {
    return s_aInboundVerifiers.getClone ();
  }

  /**
   * @return A mutable copy of all registered outbound document verifiers. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IOutboundDocumentVerifierSPI> getAllOutboundVerifiers ()
  {
    return s_aOutboundVerifiers.getClone ();
  }

  /**
   * @return A mutable copy of all registered Peppol receiver checks. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IPeppolReceiverCheckSPI> getAllPeppolReceiverChecks ()
  {
    return s_aReceiverChecks.getClone ();
  }

  /**
   * @return A mutable copy of all registered notification handlers. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IAPNotificationHandlerSPI> getAllNotificationHandlers ()
  {
    return NotificationHandlerManager.getAllNotificationHandlers ();
  }

  /**
   * @return A mutable copy of all registered lifecycle event handlers. Never <code>null</code>.
   * @since 0.9.0
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IAPLifecycleEventSPI> getAllLifecycleHandlers ()
  {
    return LifecycleEventManager.getAllLifecycleHandlers ();
  }
}
