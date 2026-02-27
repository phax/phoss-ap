/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phoss.ap.webapp;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.helger.phoss.ap.core.APCoreConfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiTokenFilter extends OncePerRequestFilter
{
  private static final String HEADER_TOKEN = "X-Token";

  @Override
  protected boolean shouldNotFilter (@NonNull final HttpServletRequest request)
  {
    // Only filter /api/** requests
    return !request.getRequestURI ().startsWith ("/api/");
  }

  @Override
  protected void doFilterInternal (@NonNull final HttpServletRequest request,
                                   @NonNull final HttpServletResponse response,
                                   @NonNull final FilterChain filterChain) throws ServletException, IOException
  {
    final String sRequiredToken = APCoreConfig.getPhase4ApiRequiredToken ();
    if (sRequiredToken != null && !sRequiredToken.isEmpty ())
    {
      final String sProvidedToken = request.getHeader (HEADER_TOKEN);
      if (!sRequiredToken.equals (sProvidedToken))
      {
        response.setStatus (HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType ("application/json");
        response.getWriter ().write ("{\"error\":\"Invalid or missing API token\"}");
        return;
      }
    }

    filterChain.doFilter (request, response);
  }
}
