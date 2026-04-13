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
package com.helger.phoss.ap.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.db.APJdbcMetaManager;

/**
 * Recovers in-flight transactions that were interrupted by an unclean shutdown. Outbound
 * transactions in {@code SENDING} state are reset to {@code FAILED} and inbound transactions in
 * {@code FORWARDING} state are reset to {@code FORWARD_FAILED}, both with immediate retry
 * eligibility.
 *
 * @author Philip Helger
 */
public final class StartupRecovery
{
  private static final Logger LOGGER = LoggerFactory.getLogger (StartupRecovery.class);

  private StartupRecovery ()
  {}

  /**
   * Run the startup recovery process. If startup recovery is disabled via configuration, this
   * method returns immediately. Otherwise it resets all outbound transactions stuck in
   * {@code SENDING} status and all inbound transactions stuck in {@code FORWARDING} status so they
   * become eligible for retry.
   */
  public static void run ()
  {
    if (!APCoreConfig.isStartupRecoveryEnabled ())
    {
      LOGGER.info ("Startup recovery is disabled");
      return;
    }

    LOGGER.info ("Running startup recovery");

    final var aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
    final var aOutboundMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final var aInboundMgr = APJdbcMetaManager.getInboundTransactionMgr ();

    // Reset outbound 'sending' -> 'failed' with immediate retry
    final var aSendingTxs = aOutboundMgr.getAllInTransmission ();
    int nOutboundRecovered = 0;
    for (final var aTx : aSendingTxs)
    {
      if (aTx.getStatus () == EOutboundStatus.SENDING)
      {
        aOutboundMgr.updateStatusAndRetry (aTx.getID (),
                                           EOutboundStatus.FAILED,
                                           aTx.getAttemptCount (),
                                           aTimestampMgr.getCurrentDateTimeUTC (),
                                           "Recovered from unclean shutdown");
        nOutboundRecovered++;
      }
    }

    // Reset inbound 'forwarding' -> 'forward_failed' with immediate retry
    final var aForwardingTxs = aInboundMgr.getAllInProcessing ();
    int nInboundRecovered = 0;
    for (final var aTx : aForwardingTxs)
    {
      if (aTx.getStatus () == EInboundStatus.FORWARDING)
      {
        aInboundMgr.updateStatusAndRetry (aTx.getID (),
                                          EInboundStatus.FORWARD_FAILED,
                                          aTx.getAttemptCount (),
                                          aTimestampMgr.getCurrentDateTimeUTC (),
                                          "Recovered from unclean shutdown");
        nInboundRecovered++;
      }
    }

    LOGGER.info ("Startup recovery complete: " +
                 nOutboundRecovered +
                 " outbound, " +
                 nInboundRecovered +
                 " inbound transactions recovered");
  }
}
