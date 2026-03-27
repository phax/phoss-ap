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
package com.helger.phoss.ap.core.job;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.base.exception.InitializationException;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.db.APJdbcMetaManager;

/**
 * Background scheduler that periodically archives completed inbound and outbound transactions.
 *
 * @author Philip Helger
 */
public final class ArchivalScheduler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ArchivalScheduler.class);

  private static Timer s_aTimer;

  private ArchivalScheduler ()
  {}

  private static void _archiveOutbound (@Nonnegative final int nBatchSize)
  {
    final var aOutboundMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final var aArchivalMgr = APJdbcMetaManager.getArchivalMgr ();

    try
    {
      final ICommonsList <IOutboundTransaction> aTransactions = aOutboundMgr.getAllForArchival (nBatchSize);
      if (aTransactions.isNotEmpty ())
      {
        LOGGER.info ("Archiving " + aTransactions.size () + " outbound transactions");
        for (final IOutboundTransaction aTx : aTransactions)
          aArchivalMgr.archiveOutboundTransaction (aTx.getID ());
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Found no outbound transactions for archiving");
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error in outbound archival cycle", ex);

      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onUnexpectedException ("ArchivalScheduler._archiveOutbound", "Error in outbound archival cycle", ex);
    }
  }

  private static void _archiveInbound (@Nonnegative final int nBatchSize)
  {
    final var aInboundMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final var aArchivalMgr = APJdbcMetaManager.getArchivalMgr ();

    try
    {
      final ICommonsList <IInboundTransaction> aTransactions = aInboundMgr.getAllForArchival (nBatchSize);
      if (aTransactions.isNotEmpty ())
      {
        LOGGER.info ("Archiving " + aTransactions.size () + " inbound transactions");
        for (final IInboundTransaction aTx : aTransactions)
          aArchivalMgr.archiveInboundTransaction (aTx.getID ());
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Found no inbound transactions for archiving");
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error in inbound archival cycle", ex);

      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onUnexpectedException ("ArchivalScheduler._archiveInbound", "Error in inbound archival cycle", ex);
    }
  }

  /**
   * Start the archival scheduler. If the archival scheduler is disabled via configuration, this
   * method does nothing.
   */
  public static void start ()
  {
    if (!APCoreConfig.isArchivalSchedulerEnabled ())
    {
      LOGGER.info ("phoss AP archival scheduler is disabled");
      return;
    }

    final int nBatchSize = APCoreConfig.getArchivalSchedulerBatchSize ();
    if (nBatchSize < 1)
      throw new InitializationException ("The archival scheduler batch size must be >= 1, but is " + nBatchSize);

    final long nIntervalMs = APCoreConfig.getArchivalSchedulerIntervalMs ();
    LOGGER.info ("Starting phoss AP archival scheduler with interval " +
                 nIntervalMs +
                 " ms and batch size " +
                 nBatchSize);

    s_aTimer = new Timer ("phoss-ap-archival-scheduler", true);
    s_aTimer.scheduleAtFixedRate (new TimerTask ()
    {
      @Override
      public void run ()
      {
        _archiveOutbound (nBatchSize);
        _archiveInbound (nBatchSize);
      }
    }, nIntervalMs, nIntervalMs);
  }

  /**
   * Stop the archival scheduler. If it was not started, this method does nothing.
   */
  public static void stop ()
  {
    if (s_aTimer != null)
    {
      s_aTimer.cancel ();
      s_aTimer = null;
      LOGGER.info ("phoss AP archival scheduler stopped");
    }
  }
}
