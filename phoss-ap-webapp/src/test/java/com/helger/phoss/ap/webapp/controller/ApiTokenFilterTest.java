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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.helger.phoss.ap.core.APCoreConfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test class for {@link ApiTokenFilter}.
 *
 * @author Karel Krýda
 */
final class ApiTokenFilterTest
{
  private final ApiTokenFilter m_aFilter = new ApiTokenFilter ();

  @Test
  void testShouldFilterApiPathUnderDefaultContext ()
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    when (aRequest.getContextPath ()).thenReturn ("");
    when (aRequest.getRequestURI ()).thenReturn ("/api/inbound/in-processing");
    when (aRequest.getServletPath ()).thenReturn ("/api/inbound/in-processing");

    assertFalse (m_aFilter.shouldNotFilter (aRequest));
  }

  @Test
  void testShouldFilterApiPathUnderCustomContext ()
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    when (aRequest.getContextPath ()).thenReturn ("/ap");
    when (aRequest.getRequestURI ()).thenReturn ("/ap/api/inbound/in-processing");
    when (aRequest.getServletPath ()).thenReturn ("/api/inbound/in-processing");

    assertFalse (m_aFilter.shouldNotFilter (aRequest));
  }

  @Test
  void testShouldSkipActuatorUnderDefaultContext ()
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    when (aRequest.getContextPath ()).thenReturn ("");
    when (aRequest.getRequestURI ()).thenReturn ("/actuator/health");
    when (aRequest.getServletPath ()).thenReturn ("/actuator/health");

    assertTrue (m_aFilter.shouldNotFilter (aRequest));
  }

  @Test
  void testShouldSkipActuatorUnderCustomContext ()
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    when (aRequest.getContextPath ()).thenReturn ("/ap");
    when (aRequest.getRequestURI ()).thenReturn ("/ap/actuator/health");
    when (aRequest.getServletPath ()).thenReturn ("/actuator/health");

    assertTrue (m_aFilter.shouldNotFilter (aRequest));
  }

  @Test
  void testShouldSkipManagement ()
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    when (aRequest.getServletPath ()).thenReturn ("/management/status");

    assertTrue (m_aFilter.shouldNotFilter (aRequest));
  }

  @Test
  void testShouldSkipRoot ()
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    when (aRequest.getServletPath ()).thenReturn ("/");

    assertTrue (m_aFilter.shouldNotFilter (aRequest));
  }

  @Test
  void testShouldSkipOpenApi ()
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    when (aRequest.getServletPath ()).thenReturn ("/openapi/v3/api-docs");

    assertTrue (m_aFilter.shouldNotFilter (aRequest));
  }

  @Test
  void testDoFilterProceedsWhenNoTokenConfigured () throws Exception
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    final HttpServletResponse aResponse = mock (HttpServletResponse.class);
    final FilterChain aChain = mock (FilterChain.class);

    try (final MockedStatic <APCoreConfig> aMocked = mockStatic (APCoreConfig.class))
    {
      aMocked.when (APCoreConfig::getPhase4ApiRequiredToken).thenReturn (null);
      m_aFilter.doFilterInternal (aRequest, aResponse, aChain);
    }

    verify (aChain).doFilter (aRequest, aResponse);
    verify (aResponse, never ()).setStatus (HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  void testDoFilterProceedsWhenTokenConfiguredEmpty () throws Exception
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    final HttpServletResponse aResponse = mock (HttpServletResponse.class);
    final FilterChain aChain = mock (FilterChain.class);

    try (final MockedStatic <APCoreConfig> aMocked = mockStatic (APCoreConfig.class))
    {
      aMocked.when (APCoreConfig::getPhase4ApiRequiredToken).thenReturn ("");
      m_aFilter.doFilterInternal (aRequest, aResponse, aChain);
    }

    verify (aChain).doFilter (aRequest, aResponse);
    verify (aResponse, never ()).setStatus (HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  void testDoFilterProceedsOnCorrectToken () throws Exception
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    final HttpServletResponse aResponse = mock (HttpServletResponse.class);
    final FilterChain aChain = mock (FilterChain.class);
    when (aRequest.getHeader ("X-Token")).thenReturn ("secret-token");

    try (final MockedStatic <APCoreConfig> aMocked = mockStatic (APCoreConfig.class))
    {
      aMocked.when (APCoreConfig::getPhase4ApiRequiredToken).thenReturn ("secret-token");
      m_aFilter.doFilterInternal (aRequest, aResponse, aChain);
    }

    verify (aChain).doFilter (aRequest, aResponse);
    verify (aResponse, never ()).setStatus (HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  void testDoFilterRejectsWrongToken () throws Exception
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    final HttpServletResponse aResponse = mock (HttpServletResponse.class);
    final FilterChain aChain = mock (FilterChain.class);
    final StringWriter aBody = new StringWriter ();
    when (aRequest.getHeader ("X-Token")).thenReturn ("wrong");
    when (aResponse.getWriter ()).thenReturn (new PrintWriter (aBody));

    try (final MockedStatic <APCoreConfig> aMocked = mockStatic (APCoreConfig.class))
    {
      aMocked.when (APCoreConfig::getPhase4ApiRequiredToken).thenReturn ("secret-token");
      m_aFilter.doFilterInternal (aRequest, aResponse, aChain);
    }

    verify (aResponse).setStatus (HttpServletResponse.SC_UNAUTHORIZED);
    verify (aResponse).setContentType ("application/json");
    verify (aChain, never ()).doFilter (aRequest, aResponse);
    assertTrue (aBody.toString ().contains ("Invalid or missing API token"));
  }

  @Test
  void testDoFilterRejectsMissingToken () throws Exception
  {
    final HttpServletRequest aRequest = mock (HttpServletRequest.class);
    final HttpServletResponse aResponse = mock (HttpServletResponse.class);
    final FilterChain aChain = mock (FilterChain.class);
    final StringWriter aBody = new StringWriter ();
    when (aRequest.getHeader (anyString ())).thenReturn (null);
    when (aResponse.getWriter ()).thenReturn (new PrintWriter (aBody));

    try (final MockedStatic <APCoreConfig> aMocked = mockStatic (APCoreConfig.class))
    {
      aMocked.when (APCoreConfig::getPhase4ApiRequiredToken).thenReturn ("secret-token");
      m_aFilter.doFilterInternal (aRequest, aResponse, aChain);
    }

    verify (aResponse).setStatus (HttpServletResponse.SC_UNAUTHORIZED);
    verify (aChain, never ()).doFilter (aRequest, aResponse);
    assertTrue (aBody.toString ().contains ("Invalid or missing API token"));
  }
}
