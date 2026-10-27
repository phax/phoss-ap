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
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.ap.api.codelist.EC4CountryCodeMode;
import com.helger.phoss.ap.api.codelist.EForwardingMode;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.spi.IAPLifecycleEventSPI;
import com.helger.phoss.ap.api.spi.IAPNotificationHandlerSPI;
import com.helger.phoss.ap.api.spi.IDocumentForwarderProviderSPI;
import com.helger.phoss.ap.api.spi.IDocumentVerifier;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IOutboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IPeppolReceiverCheckSPI;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.phoss.ap.core.forwarding.DocumentForwarderFactory;
import com.helger.phoss.ap.core.forwarding.ForwarderConfigSlot;
import com.helger.phoss.ap.core.notification.LifecycleEventManager;
import com.helger.phoss.ap.core.reporting.APPeppolReportingHelper;
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
  public static final int MAX_SECONDARY_FORWARDERS = 10;
  private static final Logger LOGGER = LoggerFactory.getLogger (APCoreMetaManager.class);

  private static EForwardingMode s_eForwardingMode;
  private static IDocumentForwarder s_aForwarder;
  private static final ICommonsList <IDocumentForwarder> s_aSecondaryForwarders = new CommonsArrayList <> ();
  private static IDocumentForwarder s_aMlsCopyForwarder;
  private static BusinessCardCache s_aBusinessCardCache;
  private static final ICommonsList <IInboundDocumentVerifierSPI> s_aInboundVerifiers = new CommonsArrayList <> ();
  private static final ICommonsList <IOutboundDocumentVerifierSPI> s_aOutboundVerifiers = new CommonsArrayList <> ();
  private static final ICommonsList <IPeppolReceiverCheckSPI> s_aReceiverChecks = new CommonsArrayList <> ();
  private static boolean s_bVerifiersInitialized = false;

  private APCoreMetaManager ()
  {}

  /**
   * Load all inbound and outbound document verifier SPIs. Called during startup before the
   * {@link com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase#POST_INIT} startup checks, so that
   * {@link com.helger.phoss.ap.core.startup.StartupCheckDocumentVerifiers} can check them before
   * anything else is initialized. May only be called once.
   *
   * @since 0.13.3
   */
  public static void initVerifierSPIs ()
  {
    if (s_bVerifiersInitialized)
      throw new InitializationException ("The document verifiers were already initialized");
    s_bVerifiersInitialized = true;

    for (final IInboundDocumentVerifierSPI aVerifier : ServiceLoader.load (IInboundDocumentVerifierSPI.class))
    {
      s_aInboundVerifiers.add (aVerifier);
      LOGGER.info ("Loaded inbound document verifier '" +
                   aVerifier.getID () +
                   "': " +
                   aVerifier.getClass ().getName ());
    }

    for (final IOutboundDocumentVerifierSPI aVerifier : ServiceLoader.load (IOutboundDocumentVerifierSPI.class))
    {
      s_aOutboundVerifiers.add (aVerifier);
      LOGGER.info ("Loaded outbound document verifier '" +
                   aVerifier.getID () +
                   "': " +
                   aVerifier.getClass ().getName ());
    }
  }

  /**
   * Initialize the core meta manager by creating the document forwarder from configuration and
   * loading the SPI-based receiver checks and notification handlers. The document verifiers must
   * have been loaded via {@link #initVerifierSPIs()} before.
   */
  public static void init ()
  {
    LOGGER.info ("Initializing APMetaManager");

    final IConfigWithFallback aConfig = APConfigProvider.getConfig ();

    // The forwarding modes, the number of secondary forwarders and the SPI provider IDs were
    // verified by StartupCheckForwarding

    // Create primary forwarder based on configuration
    {
      final ForwarderConfigSlot aSlot = ForwarderConfigSlot.getPrimary (aConfig);
      final EForwardingMode eForwardingMode = aSlot.mode ();
      final IDocumentForwarder aForwarder = DocumentForwarderFactory.create (eForwardingMode,
                                                                             aConfig,
                                                                             aSlot.configPrefix ());
      if (aForwarder.initFromConfiguration (aConfig, aSlot.configPrefix ()).isFailure ())
        throw new InitializationException ("Failed to init forwarder configuration - see logs for details");

      s_eForwardingMode = eForwardingMode;
      s_aForwarder = aForwarder;
      LOGGER.info ("Loaded document forwarder: " + aForwarder.toString ());
    }

    // Create secondary forwarders (fire-and-forget, no retry, no SLA) based on configuration
    {
      int nIndex = 1;
      for (final ForwarderConfigSlot aSlot : ForwarderConfigSlot.getAllSecondaries (aConfig))
      {
        final IDocumentForwarder aSecondary = DocumentForwarderFactory.create (aSlot.mode (),
                                                                               aConfig,
                                                                               aSlot.configPrefix ());
        if (aSecondary.initFromConfiguration (aConfig, aSlot.configPrefix ()).isFailure ())
          throw new InitializationException ("Failed to init secondary forwarder #" +
                                             nIndex +
                                             " configuration - see logs for details");

        s_aSecondaryForwarders.add (aSecondary);
        LOGGER.info ("Loaded secondary document forwarder #" + nIndex + ": " + aSecondary.toString ());
        nIndex++;
      }
      if (s_aSecondaryForwarders.isEmpty ())
        LOGGER.info ("No secondary document forwarders configured");
      else
        LOGGER.info ("Loaded " + s_aSecondaryForwarders.size () + " secondary document forwarder(s)");
    }

    // Create the sink for the copies of the self-generated MLS documents (fire-and-forget, no
    // retry, no SLA). Disabled by default - stay completely silent in that case.
    final ForwarderConfigSlot aMlsCopySlot = ForwarderConfigSlot.getMlsCopy (aConfig);
    if (aMlsCopySlot != null)
    {
      final IDocumentForwarder aMlsCopy = DocumentForwarderFactory.create (aMlsCopySlot.mode (),
                                                                           aConfig,
                                                                           aMlsCopySlot.configPrefix ());
      if (aMlsCopy.initFromConfiguration (aConfig, aMlsCopySlot.configPrefix ()).isFailure ())
        throw new InitializationException ("Failed to init the MLS copy forwarder configuration - see logs for details");

      s_aMlsCopyForwarder = aMlsCopy;
      LOGGER.info ("Loaded MLS copy document forwarder" +
                   (aMlsCopySlot.isUsingPrimaryConfig () ? " (reusing the primary forwarder configuration)" : "") +
                   ": " +
                   aMlsCopy.toString ());
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

    // Log the effective Peppol Reporting exclusions, so that configuration errors are visible on
    // startup and not only when the first matching transaction is handled
    {
      final ICommonsList <IParticipantIdentifier> aExcludedPIDs = APPeppolReportingHelper.getAllExcludedParticipantIDs ();
      if (aExcludedPIDs.isNotEmpty ())
        LOGGER.info ("Excluding " +
                     aExcludedPIDs.size () +
                     " participant ID(s) from Peppol Reporting: " +
                     aExcludedPIDs.getAllMapped (IParticipantIdentifier::getURIEncoded));
    }

    // The document verifiers were loaded in initVerifierSPIs and their IDs were verified by
    // StartupCheckDocumentVerifiers. The IDs of the forwarder providers were verified by
    // StartupCheckForwarding
    for (final IDocumentForwarderProviderSPI aProvider : ServiceLoader.load (IDocumentForwarderProviderSPI.class))
      LOGGER.info ("Loaded document forwarder provider '" +
                   aProvider.getID () +
                   "': " +
                   aProvider.getClass ().getName ());

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
  /**
   * Close every loaded document verifier that holds a resource. A verifier that talks to a backend
   * service usually keeps an HTTP connection pool, and the verifier SPIs themselves carry no
   * lifecycle - see {@link IDocumentVerifier}.
   */
  private static void _closeVerifiers ()
  {
    final ICommonsList <IDocumentVerifier> aAllVerifiers = new CommonsArrayList <> ();
    aAllVerifiers.addAll (s_aInboundVerifiers);
    aAllVerifiers.addAll (s_aOutboundVerifiers);
    for (final IDocumentVerifier aVerifier : aAllVerifiers)
      if (aVerifier instanceof final AutoCloseable aClosable)
        try
        {
          aClosable.close ();
          LOGGER.info ("Closed document verifier '" + aVerifier.getID () + "'");
        }
        catch (final Exception ex)
        {
          // A failing verifier must not abort the shutdown of everything after it
          LOGGER.error ("Failed to close document verifier '" + aVerifier.getID () + "'", ex);
        }
  }

  public static void shutdown ()
  {
    LOGGER.info ("Shutting down APMetaManager");

    _closeVerifiers ();
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
   * @return The forwarder that receives a copy of every MLS this AP generates itself, or
   *         <code>null</code> if <code>forwarding.mls-copy.enabled</code> is not set. It is
   *         dispatched on a fire-and-forget basis; it has no retries and its failure affects
   *         neither the MLS sending to C2 nor the inbound transaction status.
   * @since 0.12.0
   */
  @Nullable
  public static IDocumentForwarder getMlsCopyForwarderOrNull ()
  {
    return s_aMlsCopyForwarder;
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
