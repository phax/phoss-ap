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
package com.helger.phoss.ap.core;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.IInboundTransaction;
import com.helger.phoss.ap.api.IOutboundTransaction;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.db.APMetaJDBCManager;
import com.helger.phoss.ap.db.InboundTransactionManagerJDBC;
import com.helger.phoss.ap.db.OutboundTransactionManagerJDBC;

public final class MlsHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MlsHandler.class);

  private MlsHandler ()
  {}

  public static void handleInboundOutcome (@NonNull final IInboundTransaction aTx,
                                           @NonNull final EPeppolMLSResponseCode eResponseCode)
  {
    final EPeppolMLSType eMlsType = aTx.getMlsType ();
    final InboundTransactionManagerJDBC aInboundMgr = APMetaJDBCManager.getInboundTransactionMgr ();

    // Determine if we should send MLS
    if (eMlsType == EPeppolMLSType.FAILURE_ONLY && eResponseCode != EPeppolMLSResponseCode.REJECTION)
    {
      LOGGER.info ("MLS not required for transaction " +
                   aTx.getID () +
                   " (FAILURE_ONLY, outcome=" +
                   eResponseCode.getID () +
                   ")");
      aInboundMgr.updateMlsFields (aTx.getID (), eResponseCode, null);
      return;
    }

    LOGGER.info ("Creating MLS response (" + eResponseCode.getID () + ") for inbound transaction: " + aTx.getID ());

    // Create an outbound transaction for the MLS response
    final OutboundTransactionManagerJDBC aOutboundMgr = APMetaJDBCManager.getOutboundTransactionMgr ();

    // MLS response bytes would be created from peppol-mls library
    // For now, placeholder
    final byte [] aMlsBytes = {};
    final String sMlsSbdhInstanceID = "mls-" + java.util.UUID.randomUUID ().toString ();

    final String sMlsTxID = aOutboundMgr.create (ETransactionType.MLS_RESPONSE,
                                                 aTx.getReceiverID (),
                                                 aTx.getSenderID (),
                                                 "mls-doc-type",
                                                 "mls-process",
                                                 sMlsSbdhInstanceID,
                                                 ESourceType.RAW_XML,
                                                 aMlsBytes,
                                                 aMlsBytes.length,
                                                 "",
                                                 APCoreConfig.getPeppolOwnerCountryCode (),
                                                 null,
                                                 aTx.getID ());

    // Update inbound with MLS fields
    aInboundMgr.updateMlsFields (aTx.getID (), eResponseCode, sMlsTxID);
  }

  public static void handleIncomingMls (@NonNull final String sSbdhInstanceID,
                                        @NonNull final EPeppolMLSResponseCode eResponseCode,
                                        @NonNull final OffsetDateTime aMlsAS4ReceivedDT,
                                        @Nullable final String sMlsID)
  {
    LOGGER.info ("Received MLS response (" + eResponseCode.getID () + ") for SBDH '" + sSbdhInstanceID + "'");

    final OutboundTransactionManagerJDBC aOutboundMgr = APMetaJDBCManager.getOutboundTransactionMgr ();
    final IOutboundTransaction aTx = aOutboundMgr.getBySbdhInstanceID (sSbdhInstanceID);
    if (aTx == null)
    {
      LOGGER.warn ("No outbound transaction found for SBDH Instance ID '" + sSbdhInstanceID + "'");
      return;
    }

    final EMlsReceptionStatus eMlsStatus = switch (eResponseCode)
    {
      case ACCEPTANCE -> EMlsReceptionStatus.RECEIVED_AP;
      case ACKNOWLEDGING -> EMlsReceptionStatus.RECEIVED_AB;
      case REJECTION -> EMlsReceptionStatus.RECEIVED_RE;
    };
    aOutboundMgr.updateMlsStatus (aTx.getID (), eMlsStatus, aMlsAS4ReceivedDT, sMlsID);
    LOGGER.info ("Updated MLS status for transaction '" + aTx.getID () + "' to '" + eMlsStatus.getID () + "'");
  }
}
