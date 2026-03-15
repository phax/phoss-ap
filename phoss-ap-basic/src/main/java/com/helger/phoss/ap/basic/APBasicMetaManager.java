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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.exception.InitializationException;
import com.helger.base.lang.clazz.ClassHelper;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.factory.PeppolLaxIdentifierFactory;
import com.helger.phoss.ap.api.datetime.APTimestampManager;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.mgr.IDocumentStorageProvider;
import com.helger.phoss.ap.basic.storage.DocumentStorageProviderFileSystem;
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

  private IDocumentStorageProvider m_aDocStorageProvider;
  private IIdentifierFactory m_aIdentifierFactory;
  private IAPTimestampManager m_aTimestampMgr;

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public APBasicMetaManager ()
  {}

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
      m_aDocStorageProvider = new DocumentStorageProviderFileSystem ();
      m_aDocStorageProvider.verifyConfiguration ();

      // Determine identifier factory from configuration
      m_aIdentifierFactory = switch (APBasicConfig.getPeppolIdentifierMode ())
      {
        case STRICT -> {
          LOGGER.info ("Using strict Peppol Identifier Factory");
          yield PeppolIdentifierFactory.INSTANCE;
        }
        case LAX -> {
          LOGGER.info ("Using lax Peppol Identifier Factory");
          yield PeppolLaxIdentifierFactory.INSTANCE;
        }
      };

      m_aTimestampMgr = new APTimestampManager ();

      LOGGER.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), ex);
    }
  }

  @NonNull
  public static IDocumentStorageProvider getDocStorageProvider ()
  {
    return getInstance ().m_aDocStorageProvider;
  }

  @NonNull
  public static IAPTimestampManager getTimestampMgr ()
  {
    return getInstance ().m_aTimestampMgr;
  }

  @NonNull
  public static IIdentifierFactory getIdentifierFactory ()
  {
    return getInstance ().m_aIdentifierFactory;
  }
}
