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
package com.helger.phoss.ap.core.servlet;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.base.debug.GlobalDebug;
import com.helger.base.exception.InitializationException;
import com.helger.base.state.ETriState;
import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.httpclient.HttpDebugger;
import com.helger.mime.CMimeType;
import com.helger.peppol.reporting.api.PeppolReportingHelper;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.incoming.AS4ServerInitializer;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.peppol.servlet.Phase4PeppolDefaultReceiverConfiguration;
import com.helger.phase4.profile.peppol.AS4PeppolProfileRegistarSPI;
import com.helger.phase4.profile.peppol.PeppolCRLDownloader;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.codelist.EReceiverCheckMode;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.StartupRecovery;
import com.helger.phoss.ap.core.dump.AS4GroupedExchangeDumper;
import com.helger.phoss.ap.core.dump.AS4IncomingDumperWithMetadata;
import com.helger.phoss.ap.core.job.ArchivalScheduler;
import com.helger.phoss.ap.core.job.RetryScheduler;
import com.helger.phoss.ap.core.phase4.APAS4ManagerFactory;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.photon.io.WebFileIO;
import com.helger.security.certificate.ECertificateCheckResult;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.security.revocation.CertificateRevocationCheckerDefaults;
import com.helger.security.revocation.ERevocationCheckMode;
import com.helger.servlet.ServletHelper;
import com.helger.smpclient.config.SMPClientConfiguration;
import com.helger.smpclient.peppol.CachingSMPClientReadOnly;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.smpclient.url.PeppolNaptrURLProvider;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

import jakarta.activation.CommandMap;
import jakarta.servlet.ServletContext;

/**
 * Central servlet initialization and shutdown handler for the phoss AP application. Configures
 * global settings, AS4 and Peppol AS4, initializes all managers, and starts background schedulers
 * on startup. Performs orderly shutdown of all components on application stop.
 *
 * @author Philip Helger
 */
public class APServletInit
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APServletInit.class);
  private static OffsetDateTime s_aStartupDT;

  private APServletInit ()
  {}

  /**
   * @return The date time when the application was initialized, or <code>null</code> if not yet
   *         initialized.
   * @since 0.1.3
   */
  @Nullable
  public static OffsetDateTime getStartupDateTime ()
  {
    return s_aStartupDT;
  }

  private static void _initGlobalSettings (@NonNull final ServletContext aSC)
  {
    // Logging: JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    // Order matters
    GlobalDebug.setProductionModeDirect (true);
    GlobalDebug.setDebugModeDirect (false);

    if (GlobalDebug.isDebugMode ())
    {
      RequestTrackerSettings.setLongRunningRequestsCheckEnabled (false);
      RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (false);
    }

    HttpDebugger.setEnabled (false);

    // Sanity check
    if (CommandMap.getDefaultCommandMap ()
                  .createDataContentHandler (CMimeType.MULTIPART_RELATED.getAsString ()) == null)
    {
      throw new IllegalStateException ("No DataContentHandler for MIME Type '" +
                                       CMimeType.MULTIPART_RELATED.getAsString () +
                                       "' is available. There seems to be a problem with the dependencies/packaging");
    }

    // Init the data path
    {
      // Get the ServletContext base path
      final String sServletContextPath = ServletHelper.getServletContextBasePath (aSC);
      // Get the data path
      final String sDataPath = AS4Configuration.getDataPath ();
      if (StringHelper.isEmpty (sDataPath))
        throw new InitializationException ("No data path was provided!");
      final boolean bFileAccessCheck = false;
      // Init the IO layer
      WebFileIO.initPaths (new File (sDataPath).getAbsoluteFile (), sServletContextPath, bFileAccessCheck);
    }

    if (APCoreConfig.isOfflineMode ())
    {
      LOGGER.warn ("Offline mode enabled - for development purposes only!");
      // Special setup for offline mode
      CertificateRevocationCheckerDefaults.setRevocationCheckMode (ERevocationCheckMode.NONE);
    }
  }

  private static void _initAS4 ()
  {
    // Explicitly set the configuration so that the Spring stuff gets propagated
    AS4Configuration.setConfig (APConfigProvider.getConfig ());
    SMPClientConfiguration.setConfig (APConfigProvider.getConfig ());

    // Enforce Peppol profile usage
    // This is the programmatic way to enforce exactly this one profile
    // In a multi-profile environment, that will not work
    AS4ProfileSelector.setCustomDefaultAS4ProfileID (AS4PeppolProfileRegistarSPI.AS4_PROFILE_ID);

    // Install the manager factory that wires the phase4 AS4 duplicate manager
    // to the JDBC-backed implementation. Must happen before MetaAS4Manager is
    // first instantiated by AS4ServerInitializer.initAS4Server() below.
    MetaAS4Manager.setFactory (new APAS4ManagerFactory ());

    AS4ServerInitializer.initAS4Server ();

    final String sDumpPath = AS4Configuration.getDumpBasePath ();
    if (StringHelper.isNotEmpty (sDumpPath))
    {
      switch (APCoreConfig.getPhase4DumpMode ())
      {
        case DIRECTION ->
        {
          AS4DumpManager.setIncomingDumper (new AS4IncomingDumperWithMetadata ());
          AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());
          LOGGER.info ("AS4 message dumping enabled to '" + sDumpPath + "'");
        }
        case GROUPED ->
        {
          final AS4GroupedExchangeDumper aDumper = new AS4GroupedExchangeDumper (new File (sDumpPath));
          AS4DumpManager.setIncomingDumper (aDumper);
          AS4DumpManager.setOutgoingDumper (aDumper);
          LOGGER.info ("AS4 grouped message dumping enabled to '" + sDumpPath + "'");
        }
      }
    }

    MessageHelperMethods.setCustomMessageIDSuffix ("phoss-ap");
  }

  private static void _initPeppolAS4 ()
  {
    // Configure global custom DNS servers for Peppol NAPTR lookup if provided
    {
      final String sDnsServers = APCoreConfig.getPeppolDnsServers ();
      if (StringHelper.isNotEmpty (sDnsServers))
      {
        // Configure the DNS resolver for NAPTR lookups using sDnsServers
        LOGGER.info ("Using custom DNS servers '" + sDnsServers + "' for Peppol NAPTR lookup");
        for (final String sDnsServer : StringHelper.getExploded (',', sDnsServers))
          try
          {
            PeppolNaptrURLProvider.MUTABLE_INSTANCE.customDNSServers ()
                                                   .addAll (InetAddress.getAllByName (sDnsServer.trim ()));
          }
          catch (final UnknownHostException ex)
          {
            LOGGER.error ("Failed to resolve custom DNS server '" + sDnsServer + "': " + ex.getMessage ());
          }
      }
    }

    // Make sure the download of CRL is using Apache HttpClient and that the
    // provided settings are used. If e.g. a proxy is needed to access outbound
    // resources, it can be configured here
    {
      final Phase4PeppolHttpClientSettings aHCS = new Phase4PeppolHttpClientSettings ();
      APBasicConfig.applyHttpProxySettings (aHCS);
      PeppolCRLDownloader.setAsDefaultCRLCache (aHCS);
    }

    // Throws an exception if configuration parameters are missing
    final AS4CryptoFactoryInMemoryKeyStore aCryptoFactory = AS4CryptoFactoryConfiguration.getDefaultInstance ();

    // Check if crypto factory configuration is valid
    final KeyStore aKS = aCryptoFactory.getKeyStore ();
    if (aKS == null)
      throw new InitializationException ("Failed to load configured AS4 Key store - fix the configuration");
    LOGGER.info ("Successfully loaded configured AS4 key store from the crypto factory");

    final KeyStore.PrivateKeyEntry aPKE = aCryptoFactory.getPrivateKeyEntry ();
    if (aPKE == null)
      throw new InitializationException ("Failed to load configured AS4 private key with the alias '" +
                                         aCryptoFactory.getKeyAlias () +
                                         "' - fix the configuration");
    LOGGER.info ("Successfully loaded configured AS4 private key (alias '" +
                 aCryptoFactory.getKeyAlias () +
                 "') from the crypto factory");

    // Configure the stage correctly
    final EPeppolNetwork ePeppolStage = APCoreConfig.getPeppolStage ();
    if (ePeppolStage == null)
      throw new InitializationException ("The Peppol Stage configuration is missing or invalid");

    // Check if the private key is a proper Peppol AP certificate
    final X509Certificate aAPCert = (X509Certificate) aPKE.getCertificate ();
    {
      final TrustedCAChecker aAPCAChecker = ePeppolStage.isProduction () ? PeppolTrustedCA.peppolProductionAP ()
                                                                         : PeppolTrustedCA.peppolTestAP ();

      // Check the configured Peppol AP certificate
      // * No caching
      // * Use global certificate check mode
      final ECertificateCheckResult eCheckResult = aAPCAChecker.checkCertificate (aAPCert,
                                                                                  MetaAS4Manager.getTimestampMgr ()
                                                                                                .getCurrentDateTime (),
                                                                                  ETriState.FALSE,
                                                                                  null);
      if (eCheckResult.isInvalid ())
      {
        throw new InitializationException ("The provided certificate is not a Peppol AP certificate. Check result: " +
                                           eCheckResult);
      }
      LOGGER.info ("Successfully checked that the provided Peppol AP certificate is from the correct CA");

      // Must be set independent on the enabled/disable status
      Phase4PeppolDefaultReceiverConfiguration.setAPCAChecker (aAPCAChecker);
    }

    // Check Seat ID configuration
    final String sSeatID = APCoreConfig.getPeppolOwnerSeatID ();
    if (!CPhossAP.isPeppolSeatID (sSeatID))
    {
      throw new InitializationException ("The configured Peppol Seat ID '" +
                                         sSeatID +
                                         "' does not match the syntactial requirements.");
    }

    // Check owner country
    final String sOwnerCountry = APCoreConfig.getPeppolOwnerCountryCode ();
    if (!PeppolReportingHelper.isValidCountryCode (sOwnerCountry))
    {
      throw new InitializationException ("The configured Peppol Owner Country Code '" +
                                         sOwnerCountry +
                                         "' does not match the syntactial requirements.");
    }

    // Eventually enable the receiver check, so that for each incoming request
    // the validity is crosscheck against the owning SMP
    final EReceiverCheckMode eReceiverCheckMode = APCoreConfig.getReceiverCheckMode ();
    switch (eReceiverCheckMode)
    {
      case SMP:
      {
        final String sSMPURL = APCoreConfig.getPeppolSmpUrl ();
        if (StringHelper.isEmpty (sSMPURL))
          throw new InitializationException ("Receiver check mode 'smp' requires configuration property '" +
                                             APConfigurationProperties.PEPPOL_SMP_URL +
                                             "' to be set");

        final String sAPURL = AS4Configuration.getThisEndpointAddress ();
        if (StringHelper.isEmpty (sAPURL))
          throw new InitializationException ("Receiver check mode 'smp' requires configuration property '" +
                                             APConfigurationProperties.PHASE4_ENDPOINT_ADDRESS +
                                             "' to be set");

        Phase4PeppolDefaultReceiverConfiguration.setReceiverCheckEnabled (true);

        final SMPClientReadOnly aReceiverCheckSMPClient = new CachingSMPClientReadOnly (URLHelper.getAsURI (sSMPURL));
        APBasicConfig.applyHttpProxySettings (aReceiverCheckSMPClient.httpClientSettings ());
        Phase4PeppolDefaultReceiverConfiguration.setSMPClient (aReceiverCheckSMPClient);

        Phase4PeppolDefaultReceiverConfiguration.setAS4EndpointURL (sAPURL);
        Phase4PeppolDefaultReceiverConfiguration.setAPCertificate (aAPCert);
        LOGGER.info ("phase4 Peppol receiver checks are enabled using fixed SMP URL '" + sSMPURL + "'");
        break;
      }
      case SML:
      {
        final String sAPURL = AS4Configuration.getThisEndpointAddress ();
        if (StringHelper.isEmpty (sAPURL))
          throw new InitializationException ("Receiver check mode 'sml' requires configuration property '" +
                                             APConfigurationProperties.PHASE4_ENDPOINT_ADDRESS +
                                             "' to be set");

        Phase4PeppolDefaultReceiverConfiguration.setReceiverCheckEnabled (true);
        Phase4PeppolDefaultReceiverConfiguration.setSMLInfo (ePeppolStage.getSMLInfo ());
        Phase4PeppolDefaultReceiverConfiguration.setAS4EndpointURL (sAPURL);
        Phase4PeppolDefaultReceiverConfiguration.setAPCertificate (aAPCert);
        LOGGER.info ("phase4 Peppol receiver checks are enabled using SML '" +
                     ePeppolStage.getSMLInfo ().getDisplayName () +
                     "'");
        break;
      }
      case NONE:
      {
        Phase4PeppolDefaultReceiverConfiguration.setReceiverCheckEnabled (false);
        LOGGER.warn ("phase4 Peppol receiver checks are disabled");
        break;
      }
      default:
        throw new InitializationException ("Unsupported receiver check mode: " + eReceiverCheckMode);
    }

    // Initialize the Reporting Backend only once
    if (PeppolReportingBackend.getBackendService ().initBackend (APConfigProvider.getConfig ()).isFailure ())
      throw new InitializationException ("Failed to init Peppol Reporting Backend Service");
  }

  /**
   * Initialize the entire phoss AP application including global settings, AS4, Peppol
   * configuration, all managers, startup recovery, and background schedulers.
   *
   * @param aSC
   *        The servlet context. May not be <code>null</code>.
   */
  public static void init (@NonNull final ServletContext aSC)
  {
    LOGGER.info ("Initializing phoss AP");

    WebScopeManager.onGlobalBegin (aSC);
    _initGlobalSettings (aSC);

    // JDBC managers must be initialized before _initAS4() because the AS4
    // duplicate manager (provided by APAS4ManagerFactory) is JDBC-backed and
    // requires APJdbcMetaManager to be ready.
    APBasicMetaManager.getInstance ();
    APJdbcMetaManager.getInstance ();

    _initAS4 ();
    _initPeppolAS4 ();

    // Initialize remaining managers
    APCoreMetaManager.init ();

    // This is e.g. the Identifier Factory that is used in the Peppol Receiving
    // processing
    Phase4PeppolDefaultReceiverConfiguration.setSBDHIdentifierFactory (APBasicMetaManager.getIdentifierFactory ());

    // Recover transactions that were in-flight during unclean shutdown
    StartupRecovery.run ();

    // Start background schedulers
    RetryScheduler.start ();
    ArchivalScheduler.start ();

    s_aStartupDT = OffsetDateTime.now ();
    LOGGER.info ("phoss AP initialized successfully");
  }

  /**
   * Shut down the phoss AP application by stopping schedulers, closing managers, shutting down the
   * Peppol Reporting backend, and releasing all resources.
   */
  public static void shutdown ()
  {
    LOGGER.info ("Shutting down phoss AP");

    RetryScheduler.stop ();
    ArchivalScheduler.stop ();
    APCoreMetaManager.shutdown ();

    if (WebScopeManager.isGlobalScopePresent ())
    {
      // Shutdown the Peppol Reporting Backend service, if it was initialized
      final IPeppolReportingBackendSPI aPRBS = PeppolReportingBackend.getBackendService ();
      if (aPRBS != null && aPRBS.isInitialized ())
        aPRBS.shutdownBackend ();

      AS4ServerInitializer.shutdownAS4Server ();
      WebFileIO.resetPaths ();
      WebScopeManager.onGlobalEnd ();
    }

    LOGGER.info ("phoss AP shutdown complete");
  }
}
