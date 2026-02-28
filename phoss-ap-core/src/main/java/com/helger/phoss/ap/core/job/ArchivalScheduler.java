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
package com.helger.phoss.ap.core.job;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.IArchivalManager;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.db.APJdbcMetaManager;

public final class ArchivalScheduler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ArchivalScheduler.class);
  private static final int BATCH_SIZE = 100;

  private static Timer s_aTimer;

  private ArchivalScheduler ()
  {}

  private static void _archiveOutbound ()
  {
    try
    {
      final ICommonsList <IOutboundTransaction> aTransactions = APJdbcMetaManager.getOutboundTransactionMgr ()
                                                                                 .getAllForArchival (BATCH_SIZE);
      if (aTransactions.isNotEmpty ())
      {
        LOGGER.info ("Archiving " + aTransactions.size () + " outbound transactions");
        final IArchivalManager aArchivalMgr = APJdbcMetaManager.getArchivalMgr ();
        for (final IOutboundTransaction aTx : aTransactions)
        {
          aArchivalMgr.archiveOutboundTransaction (aTx.getID ());
        }
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error in outbound archival cycle", ex);
    }
  }

  private static void _archiveInbound ()
  {
    try
    {
      final ICommonsList <IInboundTransaction> aTransactions = APJdbcMetaManager.getInboundTransactionMgr ()
                                                                                .getAllForArchival (BATCH_SIZE);
      if (aTransactions.isNotEmpty ())
      {
        LOGGER.info ("Archiving " + aTransactions.size () + " inbound transactions");
        final IArchivalManager aArchivalMgr = APJdbcMetaManager.getArchivalMgr ();
        for (final IInboundTransaction aTx : aTransactions)
        {
          aArchivalMgr.archiveInboundTransaction (aTx.getID ());
        }
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error in inbound archival cycle", ex);
    }
  }

  public static void start ()
  {
    if (!APCoreConfig.isArchivalSchedulerEnabled ())
    {
      LOGGER.info ("Archival scheduler is disabled");
      return;
    }

    final long nIntervalMs = APCoreConfig.getArchivalSchedulerIntervalMs ();
    LOGGER.info ("Starting archival scheduler with interval " + nIntervalMs + " ms");

    s_aTimer = new Timer ("ap-archival-scheduler", true);
    s_aTimer.scheduleAtFixedRate (new TimerTask ()
    {
      @Override
      public void run ()
      {
        _archiveOutbound ();
        _archiveInbound ();
      }
    }, nIntervalMs, nIntervalMs);
  }

  public static void stop ()
  {
    if (s_aTimer != null)
    {
      s_aTimer.cancel ();
      s_aTimer = null;
      LOGGER.info ("Archival scheduler stopped");
    }
  }
}
