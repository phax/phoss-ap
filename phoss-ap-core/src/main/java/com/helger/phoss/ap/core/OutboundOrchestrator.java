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

import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phoss.ap.api.IOutboundSendingAttemptManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.api.spi.IOutboundDocumentVerifierSPI;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.helper.BackoffCalculator;
import com.helger.phoss.ap.core.helper.HashHelper;
import com.helger.phoss.ap.db.APJdbcMetaManager;

public final class OutboundOrchestrator
{
  private static final Logger LOGGER = LoggerFactory.getLogger (OutboundOrchestrator.class);

  private OutboundOrchestrator ()
  {}

  @Nullable
  public static IOutboundTransaction submitRawDocument (@NonNull final String sSenderID,
                                                        @NonNull final String sReceiverID,
                                                        @NonNull final String sDocTypeID,
                                                        @NonNull final String sProcessID,
                                                        @NonNull final String sSbdhInstanceID,
                                                        @NonNull final String sC1CountryCode,
                                                        final byte @NonNull [] aDocumentBytes,
                                                        @Nullable final String sMlsTo)
  {
    LOGGER.info ("Submitting raw document with SBDH Instance ID '" + sSbdhInstanceID + "'");

    final String sDocumentHash = HashHelper.sha256Hex (aDocumentBytes);

    // Optional verification
    if (APCoreConfig.isVerificationOutboundEnabled ())
    {
      for (final IOutboundDocumentVerifierSPI aVerifier : APMetaManager.getAllOutboundVerifiers ())
      {
        if (aVerifier.verifyDocument (aDocumentBytes, sDocTypeID, sProcessID).isFailure ())
        {
          LOGGER.warn ("Outbound document verification failed for SBDH: " + sSbdhInstanceID);
          return null;
        }
      }
    }

    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    // Create in pending state
    final String sTransactionID = aMgr.create (ETransactionType.BUSINESS_DOCUMENT,
                                               sSenderID,
                                               sReceiverID,
                                               sDocTypeID,
                                               sProcessID,
                                               sSbdhInstanceID,
                                               ESourceType.RAW_XML,
                                               aDocumentBytes,
                                               aDocumentBytes.length,
                                               sDocumentHash,
                                               sC1CountryCode,
                                               sMlsTo,
                                               null);
    return aMgr.getByID (sTransactionID);
  }

  @Nullable
  public static IOutboundTransaction submitPrebuiltSBD (@NonNull final String sSenderID,
                                                        @NonNull final String sReceiverID,
                                                        @NonNull final String sDocTypeID,
                                                        @NonNull final String sProcessID,
                                                        @NonNull final String sSbdhInstanceID,
                                                        @NonNull final String sC1CountryCode,
                                                        final byte @NonNull [] aSbdBytes,
                                                        @Nullable final String sMlsTo)
  {
    LOGGER.info ("Submitting pre-built SBD with SBDH Instance ID '" + sSbdhInstanceID + "'");

    final String sDocumentHash = HashHelper.sha256Hex (aSbdBytes);

    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    // Create in pending state
    final String sTransactionID = aMgr.create (ETransactionType.BUSINESS_DOCUMENT,
                                               sSenderID,
                                               sReceiverID,
                                               sDocTypeID,
                                               sProcessID,
                                               sSbdhInstanceID,
                                               ESourceType.PREBUILT_SBD,
                                               aSbdBytes,
                                               aSbdBytes.length,
                                               sDocumentHash,
                                               sC1CountryCode,
                                               sMlsTo,
                                               null);
    return aMgr.getByID (sTransactionID);
  }

  public static void processPendingOutbound (@NonNull final IOutboundTransaction aTx)
  {
    final String sID = aTx.getID ();
    final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundSendingAttemptManager aAttemptMgr = APJdbcMetaManager.getOutboundSendingAttemptMgr ();

    LOGGER.info ("Processing outbound transaction '" + sID + "'");

    aTxMgr.updateStatus (sID, EOutboundStatus.SENDING);

    // TODO: SMP lookup to find endpoint URL
    // TODO: Circuit breaker check
    // TODO: phase4 sending via Phase4PeppolSender.builder()

    // For now, record a placeholder — actual AS4 sending will be integrated when
    // the full phase4 configuration is in place
    final int nNewAttemptCount = aTx.getAttemptCount () + 1;
    final String sAS4MessageID = MessageHelperMethods.createRandomMessageID ();
    final OffsetDateTime aAS4Timestamp = aTimestampMgr.getCurrentDateTime ();

    try
    {
      // Actual sending would happen here using Phase4PeppolSender
      // On success:
      aAttemptMgr.create (sID, sAS4MessageID, aAS4Timestamp, null, null, EAttemptStatus.SUCCESS, null);
      aTxMgr.updateStatusCompleted (sID, EOutboundStatus.SENT);
      LOGGER.info ("Outbound transaction sent successfully '" + sID + "'");
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Outbound sending failed for transaction '" + sID + "'", ex);

      aAttemptMgr.create (sID, sAS4MessageID, aAS4Timestamp, null, null, EAttemptStatus.FAILED, ex.getMessage ());

      if (nNewAttemptCount >= APCoreConfig.getRetrySendingMaxAttempts ())
      {
        aTxMgr.updateStatusAndRetry (sID, EOutboundStatus.PERMANENTLY_FAILED, nNewAttemptCount, null, ex.getMessage ());
        // Notify
        for (final var aHandler : APMetaManager.getAllNotificationHandlers ())
          aHandler.onPermanentSendingFailure (sID, aTx.getSbdhInstanceID (), ex.getMessage ());
      }
      else
      {
        final OffsetDateTime aNextRetry = BackoffCalculator.calculateNextRetry (nNewAttemptCount,
                                                                                APCoreConfig.getRetrySendingInitialBackoffMs (),
                                                                                APCoreConfig.getRetrySendingBackoffMultiplier (),
                                                                                APCoreConfig.getRetrySendingMaxBackoffMs ());
        aTxMgr.updateStatusAndRetry (sID, EOutboundStatus.FAILED, nNewAttemptCount, aNextRetry, ex.getMessage ());
      }
    }
  }
}
