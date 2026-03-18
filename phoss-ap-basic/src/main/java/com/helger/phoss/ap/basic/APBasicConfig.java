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
package com.helger.phoss.ap.basic;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.HttpClientSettingsConfig;
import com.helger.phoss.ap.api.codelist.EPeppolIdentifierMode;
import com.helger.phoss.ap.api.codelist.EStorageMode;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Configuration accessor for basic AP settings such as document storage paths, Peppol identifier
 * mode, and outbound HTTP proxy settings. All values are read from the central
 * {@link APConfigProvider} configuration.
 *
 * @author Philip Helger
 */
@Immutable
public final class APBasicConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APBasicConfig.class);

  private APBasicConfig ()
  {}

  @NonNull
  private static IConfigWithFallback _getConfig ()
  {
    return APConfigProvider.getConfig ();
  }

  /**
   * @return The configured storage mode. Never <code>null</code>.
   * @since v0.1.1
   */
  @NonNull
  public static EStorageMode getStorageMode ()
  {
    final String sMode = _getConfig ().getAsString (APConfigurationProperties.STORAGE_MODE);
    return EStorageMode.getFromIDOrDefault (sMode);
  }

  /**
   * @return The configured path for storing inbound documents. This applies to all storage modes in
   *         the same way. Never <code>null</code>.
   */
  @NonNull
  public static String getStorageInboundPath ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.STORAGE_INBOUND_PATH,
                                      APConfigurationProperties.STORAGE_INBOUND_PATH_DEFAULT);
  }

  /**
   * @return The configured path for storing outbound documents. This applies to all storage modes
   *         in the same way.Never <code>null</code>.
   */
  @NonNull
  public static String getStorageOutboundPath ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.STORAGE_OUTBOUND_PATH,
                                      APConfigurationProperties.STORAGE_OUTBOUND_PATH_DEFAULT);
  }

  /**
   * @return The configured S3 bucket for document storage. May be <code>null</code>.
   * @since v0.1.1
   */
  @Nullable
  public static String getStorageS3Bucket ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.STORAGE_S3_BUCKET);
  }

  /**
   * @return The configured S3 region for document storage. May be <code>null</code>.
   * @since v0.1.1
   */
  @Nullable
  public static String getStorageS3Region ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.STORAGE_S3_REGION);
  }

  /**
   * @return The configured S3 access key ID for document storage. May be <code>null</code> for IAM
   *         role-based authentication.
   * @since v0.1.1
   */
  @Nullable
  public static String getStorageS3AccessKeyID ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.STORAGE_S3_ACCESS_KEY_ID);
  }

  /**
   * @return The configured S3 secret access key for document storage. May be <code>null</code> for
   *         IAM role-based authentication.
   * @since v0.1.1
   */
  @Nullable
  public static String getStorageS3SecretAccessKey ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.STORAGE_S3_SECRET_ACCESS_KEY);
  }

  /**
   * @return The configured Peppol identifier mode (strict or lax). Never <code>null</code>.
   */
  @NonNull
  public static EPeppolIdentifierMode getPeppolIdentifierMode ()
  {
    final String sVal = _getConfig ().getAsString (APConfigurationProperties.PEPPOL_IDENTIFIER_MODE);
    return EPeppolIdentifierMode.getFromIDOrDefault (sVal);
  }

  private static final AtomicBoolean PROXY_INITED = new AtomicBoolean (false);
  private static HttpClientSettingsConfig.HttpClientConfig s_aHCC = null;

  /**
   * Apply the configured outbound HTTP proxy settings to the provided {@link HttpClientSettings}.
   * This reads the <code>http.proxy.*</code> configuration properties and applies them to the
   * general proxy of the provided settings object.
   *
   * @param aHCS
   *        The HTTP client settings to configure. May not be <code>null</code>.
   */
  public static void applyHttpProxySettings (@NonNull final HttpClientSettings aHCS)
  {
    HttpClientSettingsConfig.HttpClientConfig aHCC = s_aHCC;
    if (PROXY_INITED.compareAndSet (false, true))
    {
      // No special configuration prefix needed
      s_aHCC = aHCC = HttpClientSettingsConfig.HttpClientConfig.create (_getConfig (), "");
      if (aHCC != null && aHCC.getHttpProxyEnabled (false).isTrue ())
        LOGGER.info ("Using HTTP outbound proxy " + aHCC.getHttpProxyObject ());
    }
    if (aHCC != null)
      HttpClientSettingsConfig.assignConfigValuesForProxy (aHCS.getGeneralProxy (), aHCC);
  }
}
