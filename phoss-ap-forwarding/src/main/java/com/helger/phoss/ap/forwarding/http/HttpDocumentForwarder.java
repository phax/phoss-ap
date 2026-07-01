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
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.HttpClientSettingsConfig;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.json.IJsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.phoss.ap.api.codelist.EForwardingMode;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.telemetry.Telemetry;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.phoss.ap.basic.APBasicMetaManager;

/**
 * Implementation of {@link IDocumentForwarder} for using HTTP.
 *
 * @author Philip Helger
 */
public class HttpDocumentForwarder implements IDocumentForwarder
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HttpDocumentForwarder.class);
  private static final int MAX_CUSTOM_HEADERS = 100;
  private static final String HEADER_SBDH_INSTANCE_ID = "X-SBDH-Instance-ID";
  // Configuration key suffixes (relative to the configured base prefix)
  private static final String SUFFIX_HTTP_ENDPOINT = "http.endpoint";
  private static final String SUFFIX_HTTP_HEADERS_PREFIX = "http.headers.";
  private static final String SUFFIX_HTTP_HEADER_NAME = ".name";
  private static final String SUFFIX_HTTP_HEADER_VALUE = ".value";

  private final EForwardingMode m_eMode;
  private String m_sEndpointURL;
  private final HttpClientSettings m_aHCS = new HttpClientSettings ();
  private final ICommonsOrderedMap <String, String> m_aCustomHeaders = new CommonsLinkedHashMap <> ();

  /**
   * Constructor for creating an HTTP document forwarder with the specified forwarding mode.
   *
   * @param eMode
   *        The forwarding mode to use. Must be either {@link EForwardingMode#HTTP_POST_SYNC} or
   *        {@link EForwardingMode#HTTP_POST_ASYNC}. May not be <code>null</code>.
   */
  public HttpDocumentForwarder (@NonNull final EForwardingMode eMode)
  {
    ValueEnforcer.notNull (eMode, "Mode");
    if (eMode != EForwardingMode.HTTP_POST_SYNC && eMode != EForwardingMode.HTTP_POST_ASYNC)
      throw new IllegalArgumentException ("Unsupported forwaring mode " + eMode + " provided");
    m_eMode = eMode;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isWithDeliveryConfirmation ()
  {
    return true;
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess initFromConfiguration (@NonNull final IConfigWithFallback aConfig, @NonNull final String sKeyPrefix)
  {
    ValueEnforcer.notNull (sKeyPrefix, "KeyPrefix");

    final String sEndpointKey = sKeyPrefix + SUFFIX_HTTP_ENDPOINT;
    m_sEndpointURL = aConfig.getAsString (sEndpointKey);
    if (StringHelper.isEmpty (m_sEndpointURL))
    {
      LOGGER.error ("The forwarding HTTP endpoint at '" + sEndpointKey + "' is missing");
      return ESuccess.FAILURE;
    }
    if (URLHelper.getAsURL (m_sEndpointURL) == null)
    {
      LOGGER.error ("The provided forwarding HTTP endpoint '" + m_sEndpointURL + "' is not a valid URL");
      return ESuccess.FAILURE;
    }

    HttpClientSettingsConfig.assignConfigValues (m_aHCS, aConfig, sKeyPrefix);

    // Load custom HTTP headers (indexed: <prefix>http.headers.1.name / .value)
    final String sHeadersPrefix = sKeyPrefix + SUFFIX_HTTP_HEADERS_PREFIX;
    for (int nIndex = 1;; nIndex++)
    {
      final String sName = aConfig.getAsString (sHeadersPrefix + nIndex + SUFFIX_HTTP_HEADER_NAME);
      if (StringHelper.isEmpty (sName))
        break;

      final String sValue = aConfig.getAsString (sHeadersPrefix + nIndex + SUFFIX_HTTP_HEADER_VALUE);
      m_aCustomHeaders.put (sName, sValue != null ? sValue : "");
      LOGGER.info ("Configured custom forwarding HTTP header '" + sName + "'");

      if (nIndex >= MAX_CUSTOM_HEADERS)
      {
        LOGGER.warn ("A maximum of " +
                     MAX_CUSTOM_HEADERS +
                     " custom headers is allowed. Skipping all additional ones.");
        break;
      }
    }

    return ESuccess.SUCCESS;
  }

  /** {@inheritDoc} */
  @NonNull
  public ForwardingResult forwardDocument (@NonNull final IInboundTransaction aTransaction)
  {
    return Telemetry.withSpan (CPhossAPOtel.SPAN_FORWARDER_DISPATCH, ETelemetrySpanKind.CLIENT, aSpan -> {
      aSpan.setAttribute (CPhossAPOtel.ATTR_FORWARDER_TYPE, "http")
           .setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID, aTransaction.getID ());
      return _doForwardDocument (aTransaction);
    });
  }

  @NonNull
  private ForwardingResult _doForwardDocument (@NonNull final IInboundTransaction aTransaction)
  {
    final IDocumentPayloadManager aDocPayloadMgr = APBasicMetaManager.getDocPayloadMgr ();

    try (final HttpClientManager aHttpClientMgr = HttpClientManager.create (m_aHCS))
    {
      final HttpPost aPost = new HttpPost (m_sEndpointURL);
      aPost.setEntity (new InputStreamEntity (aDocPayloadMgr.openDocumentStreamForRead (aTransaction.getDocumentPath ()),
                                              ContentType.APPLICATION_XML));

      // Apply custom headers (case-insensitive by using setHeader which overwrites existing)
      for (final var aEntry : m_aCustomHeaders.entrySet ())
        aPost.setHeader (aEntry.getKey (), aEntry.getValue ());

      aPost.setHeader (HEADER_SBDH_INSTANCE_ID, aTransaction.getSbdhInstanceID ());

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
        case HTTP_POST_SYNC ->
        {
          final IJsonObject aJsonObject = JsonReader.builder ().source (aResponse).readAsObject ();
          if (aJsonObject == null)
            yield ForwardingResult.failure ("http_response_error", "Failed to parse response as JSON object");

          // Check if the receiver explicitly disallows retries
          final String sRetry = aJsonObject.getAsString ("retry");
          if ("none".equals (sRetry))
          {
            final String sErrorMessage = aJsonObject.getAsString ("errorMessage");
            LOGGER.warn ("Receiver indicated no retry for transaction '" +
                         aTransaction.getID () +
                         "'" +
                         (sErrorMessage != null ? ": " + sErrorMessage : ""));
            yield ForwardingResult.failureNoRetry ("http_sync_no_retry",
                                                   sErrorMessage != null ? sErrorMessage
                                                                         : "Receiver indicated no retry");
          }

          final String sCountryCodeC4 = aJsonObject.getAsString ("countryCodeC4");
          LOGGER.info ("Received C4 Country Code is '" + sCountryCodeC4 + "'");
          yield ForwardingResult.success (sCountryCodeC4);
        }
        case HTTP_POST_ASYNC ->
        {
          LOGGER.info ("HTTP forwarding successful for transaction " + aTransaction.getID ());
          yield ForwardingResult.success ();
        }
        default ->
        {
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

  /** {@inheritDoc} */
  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Mode", m_eMode)
                                       .append ("EnpointURL", m_sEndpointURL)
                                       .append ("HCS", m_aHCS)
                                       .append ("CustomHeaders", m_aCustomHeaders.size ())
                                       .getToString ();
  }
}
