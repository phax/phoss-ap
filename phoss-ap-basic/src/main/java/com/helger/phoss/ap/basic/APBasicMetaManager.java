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

import java.util.ServiceLoader;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.exception.InitializationException;
import com.helger.base.lang.clazz.ClassHelper;
import com.helger.base.string.StringHelper;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.factory.PeppolLaxIdentifierFactory;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.spi.IDocumentPayloadManagerProviderSPI;
import com.helger.phoss.ap.basic.mgr.APTimestampManager;
import com.helger.phoss.ap.basic.mgr.DocumentPayloadManagerFileSystem;
import com.helger.phoss.ap.basic.mgr.DocumentPayloadManagerS3;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * Central class to access all basic managers
 *
 * @author Philip Helger
 */
public final class APBasicMetaManager extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APBasicMetaManager.class);

  private IIdentifierFactory m_aIdentifierFactory;
  private IAPTimestampManager m_aTimestampMgr;
  private IDocumentPayloadManager m_aDocPayloadMgr;

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public APBasicMetaManager ()
  {}

  /**
   * @return The global singleton instance of this manager. Never <code>null</code>.
   */
  @NonNull
  public static APBasicMetaManager getInstance ()
  {
    return getGlobalSingleton (APBasicMetaManager.class);
  }

  @Override
  protected void onAfterInstantiation (@NonNull final IScope aScope)
  {
    LOGGER.info ("Initializing " + ClassHelper.getClassLocalName (this));
    try
    {
      // Determine identifier factory from configuration
      m_aIdentifierFactory = switch (APBasicConfig.getPeppolIdentifierMode ())
      {
        case STRICT ->
        {
          LOGGER.info ("Using strict Peppol Identifier Factory");
          yield PeppolIdentifierFactory.INSTANCE;
        }
        case LAX ->
        {
          LOGGER.info ("Using lax Peppol Identifier Factory");
          yield PeppolLaxIdentifierFactory.INSTANCE;
        }
      };

      m_aTimestampMgr = new APTimestampManager ();

      // Initialize document payload manager last
      m_aDocPayloadMgr = switch (APBasicConfig.getStorageMode ())
      {
        case FILE_SYSTEM ->
        {
          LOGGER.info ("Using filesystem document storage backend");
          yield new DocumentPayloadManagerFileSystem ();
        }
        case S3 ->
        {
          LOGGER.info ("Using S3 document storage backend");
          yield new DocumentPayloadManagerS3 ();
        }
        case SPI ->
        {
          // Storage backend is provided by a deployment-supplied SPI implementation, selected by
          // ID. The AP makes no assumption about the underlying storage technology.
          yield _createSPIPayloadManager ();
        }
      };
      m_aDocPayloadMgr.verifyConfiguration ();

      LOGGER.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), ex);
    }
  }

  @NonNull
  private static IDocumentPayloadManager _createSPIPayloadManager ()
  {
    final String sProviderID = APBasicConfig.getStorageSpiID ();
    if (StringHelper.isEmpty (sProviderID))
      throw new InitializationException ("Storage mode 'spi' requires configuration property '" +
                                         APConfigurationProperties.STORAGE_SPI_ID +
                                         "' to be set");

    for (final IDocumentPayloadManagerProviderSPI aProvider : ServiceLoader.load (IDocumentPayloadManagerProviderSPI.class))
      if (sProviderID.equals (aProvider.getID ()))
      {
        LOGGER.info ("Using custom document storage backend via SPI provider '" + sProviderID + "'");
        return aProvider.createDocumentPayloadManager ();
      }

    throw new InitializationException ("No document payload manager provider SPI found for ID '" +
                                       sProviderID +
                                       "' from configuration property '" +
                                       APConfigurationProperties.STORAGE_SPI_ID +
                                       "'");
  }

  /**
   * @return The timestamp manager used for generating UTC timestamps. Never <code>null</code>.
   */
  @NonNull
  public static IAPTimestampManager getTimestampMgr ()
  {
    return getInstance ().m_aTimestampMgr;
  }

  /**
   * @return The Peppol identifier factory (strict or lax) as configured. Never <code>null</code>.
   */
  @NonNull
  public static IIdentifierFactory getIdentifierFactory ()
  {
    return getInstance ().m_aIdentifierFactory;
  }

  /**
   * @return The document payload manager for storing and retrieving documents on the filesystem.
   *         Never <code>null</code>.
   */
  @NonNull
  public static IDocumentPayloadManager getDocPayloadMgr ()
  {
    return getInstance ().m_aDocPayloadMgr;
  }
}
