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
package com.helger.phoss.ap.webapp.controller;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerSslBundle;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.peppol.servlet.Phase4PeppolAS4Servlet;
import com.helger.phoss.ap.core.servlet.APServletInit;
import com.helger.phoss.ap.core.startup.APStartupCheckManager;
import com.helger.phoss.ap.core.startup.StartupCheckTlsCertificateSeparation;
import com.helger.phoss.ap.dirsender.DirectorySenderScheduler;
import com.helger.phoss.ap.webapp.config.SpringProfileConfigIntegration;
import com.helger.web.scope.mgr.WebScopeManager;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.ServletContext;

@Configuration
public class AS4ServletRegistration
{
  private final ObjectProvider <ServerProperties> m_aServerProperties;
  private final ObjectProvider <SslBundles> m_aSslBundles;

  /**
   * Constructor. The injected {@link SpringProfileConfigIntegration} parameter ensures that Spring
   * profile-specific properties are loaded into ph-config before
   * {@link APServletInit#init(ServletContext)} runs.
   *
   * @param aProfileConfigIntegration
   *        Injected solely for bean ordering. Not used directly.
   * @param aServerProperties
   *        The Spring Boot server properties, to determine the TLS server certificate. May be
   *        unavailable.
   * @param aSslBundles
   *        The Spring Boot SSL bundles, to resolve <code>server.ssl.bundle</code>. May be
   *        unavailable.
   */
  public AS4ServletRegistration (final SpringProfileConfigIntegration aProfileConfigIntegration,
                                 final ObjectProvider <ServerProperties> aServerProperties,
                                 final ObjectProvider <SslBundles> aSslBundles)
  {
    // Dependency injection ensures profile config is loaded first
    m_aServerProperties = aServerProperties;
    m_aSslBundles = aSslBundles;
  }

  /**
   * @return The certificates of the TLS server key store of the embedded web server, or an empty
   *         list if the web server does not terminate TLS. Never <code>null</code>.
   */
  @NonNull
  private ICommonsList <X509Certificate> _getTlsServerCertificates ()
  {
    final ICommonsList <X509Certificate> ret = new CommonsArrayList <> ();
    final ServerProperties aServerProperties = m_aServerProperties.getIfAvailable ();
    final Ssl aSsl = aServerProperties == null ? null : aServerProperties.getSsl ();
    if (!Ssl.isEnabled (aSsl))
      return ret;

    final SslBundle aBundle = WebServerSslBundle.get (aSsl, m_aSslBundles.getIfAvailable ());
    final KeyStore aKeyStore = aBundle.getStores ().getKeyStore ();
    if (aKeyStore == null)
      return ret;

    final String sKeyAlias = aBundle.getKey ().getAlias ();
    try
    {
      for (final String sAlias : Collections.list (aKeyStore.aliases ()))
        if ((StringHelper.isEmpty (sKeyAlias) || sKeyAlias.equals (sAlias)) && aKeyStore.isKeyEntry (sAlias))
        {
          final Certificate aCert = aKeyStore.getCertificate (sAlias);
          if (aCert instanceof final X509Certificate aX509Cert)
            ret.add (aX509Cert);
        }
    }
    catch (final KeyStoreException ex)
    {
      throw new IllegalStateException ("Failed to read the TLS server key store", ex);
    }
    return ret;
  }

  private void _init (@NonNull final ServletContext aSC)
  {
    // Do it only once
    if (!WebScopeManager.isGlobalScopePresent ())
    {
      // Only Spring knows the TLS configuration - so the check is registered from here
      APStartupCheckManager.registerCheck (new StartupCheckTlsCertificateSeparation (this::_getTlsServerCertificates));
      APServletInit.init (aSC);
      DirectorySenderScheduler.start ();
    }
  }

  /**
   * Create the {@link Phase4PeppolAS4Servlet} registration bean, mapped to {@code /as4}.
   *
   * @param ctx
   *        The servlet context. May not be <code>null</code>.
   * @return The servlet registration bean. Never <code>null</code>.
   */
  @Bean
  public ServletRegistrationBean <Phase4PeppolAS4Servlet> as4Servlet (final ServletContext ctx)
  {
    // Must be called BEFORE the servlet is instantiated
    _init (ctx);

    final Phase4PeppolAS4Servlet aServlet = new Phase4PeppolAS4Servlet ();
    final ServletRegistrationBean <Phase4PeppolAS4Servlet> aReg = new ServletRegistrationBean <> (aServlet, "/as4");
    aReg.setName ("Phase4PeppolAS4Servlet");
    aReg.setLoadOnStartup (1);
    return aReg;
  }

  /**
   * Special class that is only present to have a graceful shutdown. The the bean method below.
   *
   * @author Philip Helger
   */
  private static final class Destroyer
  {
    @PreDestroy
    public void destroy ()
    {
      DirectorySenderScheduler.stop ();
      APServletInit.shutdown ();
    }
  }

  /**
   * Create the Destroyer bean that triggers graceful shutdown of {@link APServletInit} when the
   * Spring context is closed.
   *
   * @return A new Destroyer instance. Never <code>null</code>.
   */
  @Bean
  public Destroyer destroyer ()
  {
    return new Destroyer ();
  }
}
