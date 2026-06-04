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
package com.helger.phoss.ap.core.forwarding;

import java.util.ServiceLoader;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.exception.InitializationException;
import com.helger.base.string.StringHelper;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phoss.ap.api.codelist.EForwardingMode;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.spi.IDocumentForwarderProviderSPI;
import com.helger.phoss.ap.forwarding.filesystem.FilesystemDocumentForwarder;
import com.helger.phoss.ap.forwarding.http.HttpDocumentForwarder;
import com.helger.phoss.ap.forwarding.s3.S3DocumentForwarder;
import com.helger.phoss.ap.forwarding.sftp.SftpDocumentForwarder;

/**
 * Factory for built-in and SPI-provided document forwarders.
 *
 * @author Philip Helger
 * @since 0.9.1
 */
public final class DocumentForwarderFactory
{
  private DocumentForwarderFactory ()
  {}

  @NonNull
  private static IDocumentForwarder _createSPIForwarder (@NonNull final IConfigWithFallback aConfig,
                                                         @NonNull @Nonempty final String sKeyPrefix)
  {
    final String sIDKey = sKeyPrefix + APConfigurationProperties.FORWARDING_SPI_ID_SUFFIX;
    final String sProviderID = aConfig.getAsString (sIDKey);
    if (StringHelper.isEmpty (sProviderID))
      throw new InitializationException ("Forwarding mode 'spi' requires configuration property '" +
                                         sIDKey +
                                         "' to be set");

    for (final IDocumentForwarderProviderSPI aProvider : ServiceLoader.load (IDocumentForwarderProviderSPI.class))
      if (sProviderID.equals (aProvider.getID ()))
        return aProvider.createDocumentForwarder ();

    throw new InitializationException ("No document forwarder provider SPI found for ID '" +
                                       sProviderID +
                                       "' from configuration property '" +
                                       sIDKey +
                                       "'");
  }

  /**
   * Create a new forwarder instance for the given mode. Does not initialize it from configuration.
   *
   * @param eMode
   *        The forwarding mode. May not be <code>null</code>.
   * @param aConfig
   *        The configuration to read from. May not be <code>null</code>.
   * @param sKeyPrefix
   *        The configuration prefix to use. May not be <code>null</code>.
   * @return A new forwarder instance. Never <code>null</code>.
   */
  @NonNull
  public static IDocumentForwarder create (@NonNull final EForwardingMode eMode,
                                           @NonNull final IConfigWithFallback aConfig,
                                           @NonNull @Nonempty final String sKeyPrefix)
  {
    return switch (eMode)
    {
      case HTTP_POST_SYNC, HTTP_POST_ASYNC -> new HttpDocumentForwarder (eMode);
      case S3_LINK -> new S3DocumentForwarder ();
      case SFTP -> new SftpDocumentForwarder ();
      case FILESYSTEM -> new FilesystemDocumentForwarder ();
      case SPI -> _createSPIForwarder (aConfig, sKeyPrefix);
    };
  }
}
