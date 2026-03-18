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
package com.helger.phoss.ap.db;

import java.util.EnumSet;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.exception.InitializationException;
import com.helger.base.lang.clazz.ClassHelper;
import com.helger.base.string.StringImplode;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.api.flyway.FlywayConfiguration;
import com.helger.db.api.helper.DBSystemHelper;
import com.helger.phoss.ap.api.IArchivalManager;
import com.helger.phoss.ap.api.IInboundForwardingAttemptManager;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.IOutboundSendingAttemptManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.db.config.APFlywayConfigurationBuilder;
import com.helger.phoss.ap.db.config.APJdbcConfiguration;
import com.helger.phoss.ap.db.flyway.APFlywayMigrator;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * Central class to access all JDBC managers
 *
 * @author Philip Helger
 */
public final class APJdbcMetaManager extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APJdbcMetaManager.class);
  private static final EnumSet <EDatabaseSystemType> ALLOWED_DB_TYPES = EnumSet.of (EDatabaseSystemType.POSTGRESQL);

  private APJdbcConfiguration m_aJdbcConfig;
  private APDataSourceProvider m_aDSP;
  private OutboundTransactionManagerJdbc m_aOutboundTxMgr;
  private OutboundSendingAttemptManagerJdbc m_aOutboundAttemptMgr;
  private InboundTransactionManagerJdbc m_aInboundTxMgr;
  private IInboundForwardingAttemptManager m_aInboundAttemptMgr;
  private IArchivalManager m_aArchivalMgr;
  private MlsMetricsManagerJdbc m_aMlsMetricsMgr;

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public APJdbcMetaManager ()
  {}

  /**
   * @return The global singleton instance of this manager. Never <code>null</code>.
   */
  @NonNull
  public static APJdbcMetaManager getInstance ()
  {
    return getGlobalSingleton (APJdbcMetaManager.class);
  }

  @Override
  protected void onAfterInstantiation (@NonNull final IScope aScope)
  {
    LOGGER.info ("Initializing " + ClassHelper.getClassLocalName (this));
    try
    {
      // Init JDBC configuration
      final IConfigWithFallback aConfig = APConfigProvider.getConfig ();
      m_aJdbcConfig = new APJdbcConfiguration (aConfig);

      // Resolve database type
      final EDatabaseSystemType eDBType = m_aJdbcConfig.getJdbcDatabaseSystemType ();
      if (eDBType == null || !ALLOWED_DB_TYPES.contains (eDBType))
        throw new IllegalStateException ("The database type MUST be provided and MUST be one of " +
                                         StringImplode.imploder ()
                                                      .source (ALLOWED_DB_TYPES, EDatabaseSystemType::getID)
                                                      .separator (", ")
                                                      .build () +
                                         " - provided value is '" +
                                         m_aJdbcConfig.getJdbcDatabaseType () +
                                         "'");

      // Run Flyway
      final FlywayConfiguration aFlywayConfig = new APFlywayConfigurationBuilder (aConfig, m_aJdbcConfig).build ();
      APFlywayMigrator.runFlyway (m_aJdbcConfig, aFlywayConfig);

      // Create DataSource and DBExecutor
      m_aDSP = new APDataSourceProvider (m_aJdbcConfig);
      APDBExecutor.setDataSourceProvider (m_aDSP);

      // Create managers
      final String sTableNamePrefix = DBSystemHelper.getTableNamePrefix (m_aJdbcConfig.getJdbcDatabaseSystemType (),
                                                                         m_aJdbcConfig.getJdbcSchema ());
      final var aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
      m_aOutboundTxMgr = new OutboundTransactionManagerJdbc (aTimestampMgr, sTableNamePrefix);
      m_aOutboundAttemptMgr = new OutboundSendingAttemptManagerJdbc (aTimestampMgr, sTableNamePrefix);
      m_aInboundTxMgr = new InboundTransactionManagerJdbc (aTimestampMgr, sTableNamePrefix);
      m_aInboundAttemptMgr = new InboundForwardingAttemptManagerJdbc (aTimestampMgr, sTableNamePrefix);
      m_aArchivalMgr = new ArchivalManagerJdbc (aTimestampMgr, sTableNamePrefix);
      m_aMlsMetricsMgr = new MlsMetricsManagerJdbc (aTimestampMgr, sTableNamePrefix);

      LOGGER.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), ex);
    }
  }

  @Override
  protected void onBeforeDestroy (@NonNull final IScope aScopeToBeDestroyed) throws Exception
  {
    LOGGER.info ("Shutting down " + ClassHelper.getClassLocalName (this));
    if (m_aDSP != null)
    {
      try
      {
        m_aDSP.close ();
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Error closing DataSource", ex);
      }
    }
  }

  /**
   * @return The JDBC configuration used by this manager. Never <code>null</code>.
   */
  @NonNull
  public static APJdbcConfiguration getJdbcConfig ()
  {
    return getInstance ().m_aJdbcConfig;
  }

  /**
   * @return The outbound transaction manager. Never <code>null</code>.
   */
  @NonNull
  public static IOutboundTransactionManager getOutboundTransactionMgr ()
  {
    return getInstance ().m_aOutboundTxMgr;
  }

  /**
   * @return The outbound sending attempt manager. Never <code>null</code>.
   */
  @NonNull
  public static IOutboundSendingAttemptManager getOutboundSendingAttemptMgr ()
  {
    return getInstance ().m_aOutboundAttemptMgr;
  }

  /**
   * @return The inbound transaction manager. Never <code>null</code>.
   */
  @NonNull
  public static IInboundTransactionManager getInboundTransactionMgr ()
  {
    return getInstance ().m_aInboundTxMgr;
  }

  /**
   * @return The inbound forwarding attempt manager. Never <code>null</code>.
   */
  @NonNull
  public static IInboundForwardingAttemptManager getInboundForwardingAttemptMgr ()
  {
    return getInstance ().m_aInboundAttemptMgr;
  }

  /**
   * @return The archival manager. Never <code>null</code>.
   */
  @NonNull
  public static IArchivalManager getArchivalMgr ()
  {
    return getInstance ().m_aArchivalMgr;
  }

  /**
   * @return The MLS metrics manager for SLA calculations. Never <code>null</code>.
   */
  @NonNull
  public static MlsMetricsManagerJdbc getMlsMetricsMgr ()
  {
    return getInstance ().m_aMlsMetricsMgr;
  }
}
