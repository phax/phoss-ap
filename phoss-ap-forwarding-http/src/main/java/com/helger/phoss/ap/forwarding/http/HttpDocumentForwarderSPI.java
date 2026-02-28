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
package com.helger.phoss.ap.forwarding.http;

import java.io.IOException;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.HttpClientSettingsConfig;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.spi.IDocumentForwarderSPI;

@IsSPIImplementation
public class HttpDocumentForwarderSPI implements IDocumentForwarderSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HttpDocumentForwarderSPI.class);

  private final HttpClientSettings m_aHCS = new HttpClientSettings ();
  private String m_sEndpoint;

  public void initFromConfiguration (@NonNull final IConfigWithFallback aConfig)
  {
    HttpClientSettingsConfig.assignConfigValues (m_aHCS, aConfig, "forwarding.");
    m_sEndpoint = aConfig.getAsString (APConfigurationProperties.FORWARDING_HTTP_ENDPOINT);
  }

  @NonNull
  public ForwardingResult forwardDocument (@NonNull final IInboundTransaction aTransaction)
  {
    if (StringHelper.isEmpty (m_sEndpoint))
    {
      LOGGER.error ("HTTP forwarding endpoint not configured");
      return ForwardingResult.failure ("http_configuration_error", "HTTP forwarding endpoint not configured");
    }

    try (final HttpClientManager aHttpClientMgr = new HttpClientManager ())
    {
      final HttpPost aPost = new HttpPost (m_sEndpoint);
      aPost.setEntity (new ByteArrayEntity (aTransaction.getDocumentBytes (), ContentType.APPLICATION_XML));

      LOGGER.info ("Forwarding inbound transaction '" +
                   aTransaction.getID () +
                   "' (SBDH ID '" +
                   aTransaction.getSbdhInstanceID () +
                   "') to '" +
                   m_sEndpoint +
                   "'");

      aHttpClientMgr.execute (aPost, new ResponseHandlerByteArray ());

      LOGGER.info ("HTTP forwarding successful for transaction " + aTransaction.getID ());
      return ForwardingResult.success ();
    }
    catch (final ExtendedHttpResponseException ex)
    {
      LOGGER.error ("HTTP forwarding failed for transaction '" + aTransaction.getID () + "'", ex);
      // Status code already in the message
      return ForwardingResult.failure ("http_status", ex.getMessage ());
    }
    catch (final IOException ex)
    {
      LOGGER.error ("HTTP forwarding failed for transaction '" + aTransaction.getID () + "'", ex);
      return ForwardingResult.failure ("http_io_error", ex.getMessage () + " (" + ex.getClass ().getName () + ")");
    }
    catch (final Exception ex)
    {
      LOGGER.error ("HTTP forwarding failed for transaction '" + aTransaction.getID () + "'", ex);
      return ForwardingResult.failure ("http_error", ex.getMessage () + " (" + ex.getClass ().getName () + ")");
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
