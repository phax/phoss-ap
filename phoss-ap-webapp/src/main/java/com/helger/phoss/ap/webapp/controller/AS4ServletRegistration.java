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

import org.jspecify.annotations.NonNull;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.helger.phase4.peppol.servlet.Phase4PeppolAS4Servlet;
import com.helger.phoss.ap.core.servlet.APServletInit;
import com.helger.phoss.ap.webapp.config.SpringProfileConfigIntegration;
import com.helger.web.scope.mgr.WebScopeManager;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.ServletContext;

@Configuration
public class AS4ServletRegistration
{
  /**
   * Constructor. The injected {@link SpringProfileConfigIntegration} parameter
   * ensures that Spring profile-specific properties are loaded into ph-config
   * before {@link APServletInit#init(ServletContext)} runs.
   *
   * @param aProfileConfigIntegration
   *        Injected solely for bean ordering. Not used directly.
   */
  @SuppressWarnings ("unused")
  public AS4ServletRegistration (final SpringProfileConfigIntegration aProfileConfigIntegration)
  {
    // Dependency injection ensures profile config is loaded first
  }

  private void _init (@NonNull final ServletContext aSC)
  {
    // Do it only once
    if (!WebScopeManager.isGlobalScopePresent ())
      APServletInit.init (aSC);
  }

  /**
   * Create the {@link Phase4PeppolAS4Servlet} registration bean, mapped to
   * {@code /as4}.
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
   * Special class that is only present to have a graceful shutdown. The the
   * bean method below.
   *
   * @author Philip Helger
   */
  private static final class Destroyer
  {
    @PreDestroy
    public void destroy ()
    {
      APServletInit.shutdown ();
    }
  }

  /**
   * Create the {@link Destroyer} bean that triggers graceful shutdown of
   * {@link APServletInit} when the Spring context is closed.
   *
   * @return A new {@link Destroyer} instance. Never <code>null</code>.
   */
  @Bean
  public Destroyer destroyer ()
  {
    return new Destroyer ();
  }
}
