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

import java.net.URI;
import java.time.Duration;

import org.apache.hc.core5.util.Timeout;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.httpclient.HttpClientManager;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.smpclient.httpclient.SMPHttpClientSettings;
import com.helger.smpclient.peppol.CachingSMPClientReadOnly;
import com.helger.smpclient.peppol.SMPClientCache;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;

/**
 * Central factory for all Peppol SMP clients used by this AP. Because an SMP client that resolves
 * its host via SML/NAPTR is bound to a single participant identifier, a new client instance is
 * needed for each receiver - and therefore for (almost) every outbound message. To avoid that each
 * of these short living clients starts from scratch, this class provides
 * <ul>
 * <li>a process wide {@link SMPClientCache}, so that Service Group and Service Metadata responses
 * are reused across client instances, and</li>
 * <li>a process wide {@link HttpClientManager}, so that the HTTP connection pool (and therefore all
 * established TLS connections) is reused across client instances instead of being created and
 * closed per SMP query.</li>
 * </ul>
 * {@link #init()} must be called before the first client is created and {@link #shutdown()} on
 * application shutdown, to close the shared {@link HttpClientManager}.
 *
 * @author Philip Helger
 * @since 0.11.0
 */
@ThreadSafe
public final class SMPClientManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPClientManager.class);

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static HttpClientManager s_aSharedHttpClientMgr;
  // Until init() was called, the configuration default applies
  @GuardedBy ("RW_LOCK")
  private static boolean s_bCacheEnabled = APConfigurationProperties.PEPPOL_SMP_CACHE_ENABLED_DEFAULT;

  private SMPClientManager ()
  {}

  /**
   * Initialize the shared SMP client cache and the shared HTTP connection pool from the
   * configuration. Must be called before the first SMP client is created.
   */
  public static void init ()
  {
    // Make sure a previously created HTTP client manager is not leaked
    shutdown ();

    // Create the shared cache first, so that it is in place before any client is created
    final boolean bCacheEnabled = APCoreConfig.isPeppolSmpCacheEnabled ();
    if (bCacheEnabled)
    {
      final Duration aCacheTTL = APCoreConfig.getPeppolSmpCacheTTL ();
      final int nMaxSize = APCoreConfig.getPeppolSmpCacheMaxSize ();
      SMPClientCache.setDefaultInstance (new SMPClientCache (aCacheTTL, nMaxSize));
      LOGGER.info ("Peppol SMP client caching is enabled with a TTL of " +
                   aCacheTTL +
                   " and a maximum of " +
                   (nMaxSize > 0 ? Integer.toString (nMaxSize) : "unlimited") +
                   " entries per cache");
    }
    else
      LOGGER.info ("Peppol SMP client caching is disabled");

    // Create the shared HTTP client manager, so that the connection pool is reused for all SMP
    // queries of all SMP clients. Because the shared manager takes precedence over the per-client
    // HTTP client settings (see _configure), these are the only timeouts that are ever effective
    // for an SMP query
    final SMPHttpClientSettings aHCS = new SMPHttpClientSettings ();
    APBasicConfig.applyHttpProxySettings (aHCS);
    final Duration aConnectTimeout = APCoreConfig.getPeppolSmpTimeoutConnect ();
    final Duration aResponseTimeout = APCoreConfig.getPeppolSmpTimeoutResponse ();
    aHCS.setConnectTimeout (Timeout.of (aConnectTimeout));
    aHCS.setResponseTimeout (Timeout.of (aResponseTimeout));
    final HttpClientManager aSharedHttpClientMgr = HttpClientManager.create (aHCS);

    RW_LOCK.writeLocked (() -> {
      s_bCacheEnabled = bCacheEnabled;
      s_aSharedHttpClientMgr = aSharedHttpClientMgr;
    });
    LOGGER.info ("Created the shared HTTP client manager for all Peppol SMP queries with a connect timeout of " +
                 aConnectTimeout +
                 " and a response timeout of " +
                 aResponseTimeout);
  }

  /**
   * Close the shared HTTP connection pool. Afterwards no SMP client created by this class may be
   * used anymore.
   */
  public static void shutdown ()
  {
    @SuppressWarnings ("resource")
    final HttpClientManager aSharedHttpClientMgr = RW_LOCK.writeLockedGet (() -> {
      final HttpClientManager ret = s_aSharedHttpClientMgr;
      s_aSharedHttpClientMgr = null;
      return ret;
    });

    if (aSharedHttpClientMgr != null)
      try
      {
        aSharedHttpClientMgr.close ();
        LOGGER.info ("Closed the shared HTTP client manager of all Peppol SMP queries");
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Failed to close the shared HTTP client manager of all Peppol SMP queries", ex);
      }
  }

  /**
   * @return {@code true} if SMP client caching is enabled, {@code false} if not. Only valid after
   *         {@link #init()} was called.
   */
  public static boolean isCacheEnabled ()
  {
    return RW_LOCK.readLockedBoolean (() -> s_bCacheEnabled);
  }

  /**
   * @return The shared cache used by all created SMP clients. Never <code>null</code>. Note: the
   *         cache is not used if caching is disabled (see {@link #isCacheEnabled()}).
   */
  @NonNull
  public static SMPClientCache getCache ()
  {
    return SMPClientCache.getDefaultInstance ();
  }

  /**
   * @return The shared HTTP client manager used for all SMP queries. May be <code>null</code> if
   *         {@link #init()} was not yet called or {@link #shutdown()} was already called.
   */
  @Nullable
  public static HttpClientManager getSharedHttpClientManager ()
  {
    return RW_LOCK.readLockedGet (() -> s_aSharedHttpClientMgr);
  }

  @NonNull
  private static <T extends SMPClientReadOnly> T _configure (@NonNull final T aSMPClient)
  {
    // Only needed as a fallback, in case no shared HTTP client manager is present
    APBasicConfig.applyHttpProxySettings (aSMPClient.httpClientSettings ());

    // The shared manager takes precedence over the client specific HTTP client settings
    final HttpClientManager aSharedHttpClientMgr = getSharedHttpClientManager ();
    if (aSharedHttpClientMgr != null)
      aSMPClient.setSharedHttpClientManager (aSharedHttpClientMgr);
    else
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("No shared HTTP client manager is present - each SMP query will create its own one");

    return aSMPClient;
  }

  /**
   * Create a new SMP client for the provided receiver participant identifier, resolving the SMP
   * host via SML/NAPTR.
   *
   * @param aURLProvider
   *        The URL provider to be used for the DNS based resolution. May not be <code>null</code>.
   * @param aParticipantID
   *        The receiver participant identifier to be queried. May not be <code>null</code>.
   * @param aSMLInfo
   *        The SML to be used for the DNS based resolution. May not be <code>null</code>.
   * @return The new SMP client. Never <code>null</code>.
   * @throws SMPDNSResolutionException
   *         If the participant identifier could not be resolved via DNS.
   */
  @NonNull
  public static SMPClientReadOnly createSMPClient (@NonNull final ISMPURLProvider aURLProvider,
                                                   @NonNull final IParticipantIdentifier aParticipantID,
                                                   @NonNull final ISMLInfo aSMLInfo) throws SMPDNSResolutionException
  {
    ValueEnforcer.notNull (aURLProvider, "URLProvider");
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aSMLInfo, "SMLInfo");

    return _configure (isCacheEnabled () ? new CachingSMPClientReadOnly (aURLProvider, aParticipantID, aSMLInfo)
                                         : new SMPClientReadOnly (aURLProvider, aParticipantID, aSMLInfo));
  }

  /**
   * Create a new SMP client for a fixed SMP host URI, without any DNS resolution.
   *
   * @param aSMPHost
   *        The SMP host URI to be queried. May not be <code>null</code>.
   * @return The new SMP client. Never <code>null</code>.
   */
  @NonNull
  public static SMPClientReadOnly createSMPClient (@NonNull final URI aSMPHost)
  {
    ValueEnforcer.notNull (aSMPHost, "SMPHost");

    return _configure (isCacheEnabled () ? new CachingSMPClientReadOnly (aSMPHost) : new SMPClientReadOnly (aSMPHost));
  }
}
