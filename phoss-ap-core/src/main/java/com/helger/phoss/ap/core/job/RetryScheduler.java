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

import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.inbound.InboundOrchestrator;
import com.helger.phoss.ap.core.outbound.OutboundOrchestrator;
import com.helger.phoss.ap.db.APJdbcMetaManager;

public final class RetryScheduler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (RetryScheduler.class);
  private static final int BATCH_SIZE = 50;

  private static Timer s_aTimer;

  private RetryScheduler ()
  {}

  public static void start ()
  {
    final long nIntervalMs = APCoreConfig.getRetrySchedulerIntervalMs ();
    LOGGER.info ("Starting retry scheduler with interval " + nIntervalMs + " ms");

    s_aTimer = new Timer ("ap-retry-scheduler", true);
    s_aTimer.scheduleAtFixedRate (new TimerTask ()
    {
      @Override
      public void run ()
      {
        _retryOutbound ();
        _retryInbound ();
      }
    }, nIntervalMs, nIntervalMs);
  }

  public static void stop ()
  {
    if (s_aTimer != null)
    {
      s_aTimer.cancel ();
      s_aTimer = null;
      LOGGER.info ("Retry scheduler stopped");
    }
  }

  private static void _retryOutbound ()
  {
    try
    {
      final ICommonsList <IOutboundTransaction> aTransactions = APJdbcMetaManager.getOutboundTransactionMgr ()
                                                                                 .getAllForRetry (BATCH_SIZE);

      if (aTransactions.isNotEmpty ())
        LOGGER.info ("Retrying " + aTransactions.size () + " outbound transactions");

      for (final IOutboundTransaction aTx : aTransactions)
      {
        try
        {
          OutboundOrchestrator.processPendingOutbound (aTx);
        }
        catch (final Exception ex)
        {
          LOGGER.error ("Error retrying outbound transaction '" + aTx.getID () + "'", ex);
        }
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error in outbound retry cycle", ex);
    }
  }

  private static void _retryInbound ()
  {
    try
    {
      final ICommonsList <IInboundTransaction> aTransactions = APJdbcMetaManager.getInboundTransactionMgr ()
                                                                                .getAllForRetry (BATCH_SIZE);

      if (aTransactions.isNotEmpty ())
        LOGGER.info ("Retrying " + aTransactions.size () + " inbound forwarding transactions");

      final var aForwarder = APCoreMetaManager.getForwarder ();
      if (aForwarder == null)
        return;

      for (final IInboundTransaction aTx : aTransactions)
      {
        // Re-forward using the same InboundMessageProcessor logic
        // The forwarding is handled through the forwarder directly here
        if (InboundOrchestrator.forwardDocument (aTx).isFailure ())
        {
          for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            aHandler.onInboundForwardingError (aTx.getID (), true);
        }
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error in inbound retry cycle", ex);
    }
  }
}
