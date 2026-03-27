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

/**
 * This class makes sure the inbound and outbound transactions are automatically retried.
 *
 * @author Philip Helger
 */
public final class RetryScheduler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (RetryScheduler.class);
  private static final int BATCH_SIZE = 50;

  private static Timer s_aTimer;

  private RetryScheduler ()
  {}

  private static void _retryOutbound ()
  {
    final var aOutboundMgr = APJdbcMetaManager.getOutboundTransactionMgr ();

    try
    {
      final ICommonsList <IOutboundTransaction> aTransactions = aOutboundMgr.getAllForRetry (BATCH_SIZE);

      if (aTransactions.isNotEmpty ())
      {
        LOGGER.info ("Retrying " + aTransactions.size () + " outbound transactions");
        final String sLogPrefix = "[RetryOutbound] ";

        for (final IOutboundTransaction aTx : aTransactions)
        {
          try
          {
            OutboundOrchestrator.processPendingOutbound (sLogPrefix, aTx);
          }
          catch (final Exception ex)
          {
            LOGGER.error ("Error retrying outbound transaction '" + aTx.getID () + "'", ex);

            for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
              aHandler.onUnexpectedException ("RetryScheduler._retryOutbound",
                                              "Error retrying outbound transaction '" + aTx.getID () + "'",
                                              ex);
          }
        }
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Found no outbound transactions for retry");
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error in outbound retry cycle", ex);

      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onUnexpectedException ("RetryScheduler._retryOutbound", "Internal error in outbound retry cycle", ex);
    }
  }

  private static void _retryInbound ()
  {
    final var aInboundMgr = APJdbcMetaManager.getInboundTransactionMgr ();

    try
    {
      final ICommonsList <IInboundTransaction> aTransactions = aInboundMgr.getAllForRetry (BATCH_SIZE);

      if (aTransactions.isNotEmpty ())
      {
        LOGGER.info ("Retrying " + aTransactions.size () + " inbound forwarding transactions");
        final String sLogPrefix = "[RetryInbound] ";

        for (final IInboundTransaction aTx : aTransactions)
        {
          // Re-forward using the original InboundOrchestrator logic
          if (InboundOrchestrator.forwardDocument (sLogPrefix, aTx).isFailure ())
          {
            for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
              aHandler.onInboundForwardingError (aTx.getID (), true);
          }
        }
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Found no inbound transactions for retry");
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error in inbound retry cycle", ex);

      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onUnexpectedException ("RetryScheduler._retryInbound", "Internal error in inbound retry cycle", ex);
    }
  }

  /**
   * Start the retry scheduler. It periodically checks for outbound and inbound transactions that
   * are eligible for retry and processes them.
   */
  public static void start ()
  {
    final long nIntervalMs = APCoreConfig.getRetrySchedulerIntervalMs ();
    LOGGER.info ("Starting phoss AP retry scheduler with interval " + nIntervalMs + " ms");

    s_aTimer = new Timer ("phoss-ap-retry-scheduler", true);
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

  /**
   * Stop the retry scheduler. If it was not started, this method does nothing.
   */
  public static void stop ()
  {
    if (s_aTimer != null)
    {
      s_aTimer.cancel ();
      s_aTimer = null;
      LOGGER.info ("phoss AP retry scheduler stopped");
    }
  }
}
