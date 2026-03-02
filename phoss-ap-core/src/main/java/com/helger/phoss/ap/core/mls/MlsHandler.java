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
package com.helger.phoss.ap.core.mls;

import java.io.File;
import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ESuccess;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.basic.storage.DocumentStorageHelper;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.helper.HashHelper;
import com.helger.phoss.ap.db.APJdbcMetaManager;

public final class MlsHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MlsHandler.class);

  private MlsHandler ()
  {}

  public static void handleInboundOutcome (@NonNull final IInboundTransaction aTx,
                                           @NonNull final EPeppolMLSResponseCode eResponseCode)
  {
    final EPeppolMLSType eMlsType = aTx.getMlsType ();
    final IInboundTransactionManager aInboundMgr = APJdbcMetaManager.getInboundTransactionMgr ();

    // Determine if we should send MLS
    if (eMlsType == EPeppolMLSType.FAILURE_ONLY && eResponseCode.isSuccess ())
    {
      LOGGER.info ("MLS not required for transaction " +
                   aTx.getID () +
                   " (FAILURE_ONLY, outcome=" +
                   eResponseCode.getID () +
                   ")");
      aInboundMgr.updateMlsFields (aTx.getID (), eResponseCode, null);
      return;
    }

    LOGGER.info ("Creating MLS response (" +
                 eResponseCode.getID () +
                 ") for inbound transaction '" +
                 aTx.getID () +
                 "'");

    // Create an outbound transaction for the MLS response
    final IOutboundTransactionManager aOutboundMgr = APJdbcMetaManager.getOutboundTransactionMgr ();

    // TODO MLS response bytes would be created from peppol-mls library
    // For now, placeholder
    final byte [] aMlsBytes = {};
    final String sMlsSbdhInstanceID = PeppolSBDHData.createRandomSBDHInstanceIdentifier ();
    final OffsetDateTime aAS4SendingDT = APBasicMetaManager.getTimestampMgr ().getCurrentDateTime ();

    // Store MLS document to disk
    final String sDocumentPath = DocumentStorageHelper.storeDocument (new File (APBasicConfig.getStorageOutboundPath ()),
                                                                      aAS4SendingDT,
                                                                      sMlsSbdhInstanceID + ".mls",
                                                                      aMlsBytes);

    final String sMlsTxID = aOutboundMgr.create (ETransactionType.MLS_RESPONSE,
                                                 aTx.getReceiverID (),
                                                 aTx.getSenderID (),
                                                 EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getURIEncoded (),
                                                 EPredefinedProcessIdentifier.urn_peppol_edec_mls.getURIEncoded (),
                                                 sMlsSbdhInstanceID,
                                                 ESourceType.RAW_XML,
                                                 sDocumentPath,
                                                 aMlsBytes.length,
                                                 HashHelper.sha256Hex (aMlsBytes),
                                                 APCoreConfig.getPeppolOwnerCountryCode (),
                                                 aAS4SendingDT,
                                                 null,
                                                 aTx.getID ());

    // Update inbound with MLS fields
    aInboundMgr.updateMlsFields (aTx.getID (), eResponseCode, sMlsTxID);
  }

  /**
   * Correlate an incoming MLS to a previous outbound transaction.
   *
   * @param sSbdhInstanceID
   *        SBDH Instance ID. May not be <code>null</code>.
   * @param eResponseCode
   *        The MLS response code received. May not be <code>null</code>.
   * @param aMlsAS4ReceivedDT
   *        The MLS AS4 receiving date time for the SLR. May not be <code>null</code>.
   * @param sMlsID
   *        The MLS document ID received. May not be <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  public static ESuccess handleIncomingMls (@NonNull final String sLogPrefix,
                                            @NonNull final String sSbdhInstanceID,
                                            @NonNull final EPeppolMLSResponseCode eResponseCode,
                                            @NonNull final OffsetDateTime aMlsAS4ReceivedDT,
                                            @Nullable final String sMlsID)
  {
    LOGGER.info (sLogPrefix +
                 "Received MLS response (" +
                 eResponseCode.getID () +
                 ") for SBDH '" +
                 sSbdhInstanceID +
                 "'");

    final IOutboundTransactionManager aOutboundMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundTransaction aTx = aOutboundMgr.getBySbdhInstanceID (sSbdhInstanceID);
    if (aTx == null)
    {
      LOGGER.warn (sLogPrefix + "No outbound transaction found for SBDH '" + sSbdhInstanceID + "'");
      return ESuccess.FAILURE;
    }

    final EMlsReceptionStatus eMlsStatus = switch (eResponseCode)
    {
      case ACCEPTANCE -> EMlsReceptionStatus.RECEIVED_AP;
      case ACKNOWLEDGING -> EMlsReceptionStatus.RECEIVED_AB;
      case REJECTION -> EMlsReceptionStatus.RECEIVED_RE;
    };

    // Store in DB
    if (aOutboundMgr.updateMlsStatus (aTx.getID (), eMlsStatus, aMlsAS4ReceivedDT, sMlsID).isFailure ())
      return ESuccess.FAILURE;

    LOGGER.info (sLogPrefix +
                 "Updated MLS status for transaction '" +
                 aTx.getID () +
                 "' to '" +
                 eMlsStatus.getID () +
                 "'");
    return ESuccess.SUCCESS;
  }
}
