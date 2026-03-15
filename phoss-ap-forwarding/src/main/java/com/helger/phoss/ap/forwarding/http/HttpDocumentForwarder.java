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
package com.helger.phoss.ap.forwarding.http;

import java.io.IOException;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.url.URLHelper;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.HttpClientSettingsConfig;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.json.IJsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.phoss.ap.api.codelist.EForwardingMode;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.basic.APBasicMetaManager;

/**
 * Implementation of {@link IDocumentForwarder} for using HTTP.
 *
 * @author Philip Helger
 */
public class HttpDocumentForwarder implements IDocumentForwarder
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HttpDocumentForwarder.class);

  private final EForwardingMode m_eMode;
  private String m_sEndpointURL;
  private final HttpClientSettings m_aHCS = new HttpClientSettings ();

  public HttpDocumentForwarder (@NonNull final EForwardingMode eMode)
  {
    ValueEnforcer.notNull (eMode, "Mode");
    if (eMode != EForwardingMode.HTTP_POST_SYNC && eMode != EForwardingMode.HTTP_POST_ASYNC)
      throw new IllegalArgumentException ("Unsupported forwaring mode " + eMode + " provided");
    m_eMode = eMode;
  }

  @NonNull
  public ESuccess initFromConfiguration (@NonNull final IConfigWithFallback aConfig)
  {
    m_sEndpointURL = aConfig.getAsString (APConfigurationProperties.FORWARDING_HTTP_ENDPOINT);
    if (StringHelper.isEmpty (m_sEndpointURL))
    {
      LOGGER.error ("The forwarding HTTP endpoint is missing");
      return ESuccess.FAILURE;
    }
    if (URLHelper.getAsURL (m_sEndpointURL) == null)
    {
      LOGGER.error ("The provided forwarding HTTP endpoint '" + m_sEndpointURL + "' is not a valid URL");
      return ESuccess.FAILURE;
    }

    HttpClientSettingsConfig.assignConfigValues (m_aHCS, aConfig, "forwarding.");
    return ESuccess.SUCCESS;
  }

  @NonNull
  public ForwardingResult forwardDocument (@NonNull final IInboundTransaction aTransaction)
  {
    final IDocumentPayloadManager aDocPayloadMgr = APBasicMetaManager.getDocPayloadMgr ();

    try (final HttpClientManager aHttpClientMgr = new HttpClientManager ())
    {
      final HttpPost aPost = new HttpPost (m_sEndpointURL);
      aPost.setEntity (new InputStreamEntity (aDocPayloadMgr.openDocumentStreamForRead (aTransaction.getDocumentPath ()),
                                              ContentType.APPLICATION_XML));

      LOGGER.info ("Forwarding inbound transaction '" +
                   aTransaction.getID () +
                   "' (SBDH ID '" +
                   aTransaction.getSbdhInstanceID () +
                   "') to '" +
                   m_sEndpointURL +
                   "'");

      final byte [] aResponse = aHttpClientMgr.execute (aPost, new ResponseHandlerByteArray ());
      return switch (m_eMode)
      {
        case HTTP_POST_SYNC -> {
          final IJsonObject aJsonObject = JsonReader.builder ().source (aResponse).readAsObject ();
          if (aJsonObject == null)
            yield ForwardingResult.failure ("http_response_error", "Failed to parse response as JSON object");

          final String sCountryCodeC4 = aJsonObject.getAsString ("countryCodeC4");
          LOGGER.info ("Received C4 Country Code is '" + sCountryCodeC4 + "'");
          yield ForwardingResult.success (sCountryCodeC4);
        }
        case HTTP_POST_ASYNC -> {
          LOGGER.info ("HTTP forwarding successful for transaction " + aTransaction.getID ());
          yield ForwardingResult.success ();
        }
        default -> {
          LOGGER.error ("Unexpected forwarding mode " + m_eMode + " for HTTP forwarder");
          yield ForwardingResult.failure ("http_configuration_error", "Unexpected forwarding mode " + m_eMode);
        }
      };
    }
    catch (final ExtendedHttpResponseException ex)
    {
      LOGGER.error ("HTTP forwarding failed for transaction '" + aTransaction.getID () + "'", ex);
      // Status code already in the message
      return ForwardingResult.failure ("http_status", ex.getMessage ());
    }
    catch (final IOException ex)
    {
      LOGGER.error ("HTTP forwarding failed for transaction '" +
                    aTransaction.getID () +
                    "': " +
                    ex.getMessage () +
                    " (" +
                    ex.getClass ().getName () +
                    ")");
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
    return new ToStringGenerator (this).append ("Mode", m_eMode)
                                       .append ("EnpointURL", m_sEndpointURL)
                                       .append ("HCS", m_aHCS)
                                       .getToString ();
  }
}
