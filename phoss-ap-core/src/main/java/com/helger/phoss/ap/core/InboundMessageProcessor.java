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

import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.base.state.ESuccess;
import com.helger.http.header.HttpHeaderMap;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phoss.ap.api.IInboundTransaction;
import com.helger.phoss.ap.api.codelist.EDuplicateDetectionMode;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.spi.IDocumentForwarderSPI;
import com.helger.phoss.ap.api.spi.IDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IReceiverCheckSPI;
import com.helger.phoss.ap.core.helper.BackoffCalculator;
import com.helger.phoss.ap.core.helper.HashHelper;
import com.helger.phoss.ap.db.APMetaJDBCManager;
import com.helger.phoss.ap.db.InboundForwardingAttemptManagerJDBC;
import com.helger.phoss.ap.db.InboundTransactionManagerJDBC;

public class InboundMessageProcessor implements IPhase4PeppolIncomingSBDHandlerSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (InboundMessageProcessor.class);

  public void handleIncomingSBD (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @NonNull final HttpHeaderMap aHeaders,
                                 @NonNull final Ebms3UserMessage aUserMessage,
                                 final byte @NonNull [] aSBDBytes,
                                 @NonNull final StandardBusinessDocument aSBD,
                                 @NonNull final PeppolSBDHData aPeppolSBD,
                                 @NonNull final IAS4IncomingMessageState aState,
                                 @NonNull final AS4ErrorList aProcessingErrorMessages) throws Exception
  {
    final InboundTransactionManagerJDBC aTxMgr = APMetaJDBCManager.getInboundTransactionMgr ();

    final String sIncomingID = aMessageMetadata.getIncomingUniqueID ();
    final String sAS4MessageID = aState.getMessageID ();
    final String sSenderID = aPeppolSBD.getSenderAsIdentifier ().getURIEncoded ();
    final String sReceiverID = aPeppolSBD.getReceiverAsIdentifier ().getURIEncoded ();
    final String sDocTypeID = aPeppolSBD.getDocumentTypeAsIdentifier ().getURIEncoded ();
    final String sProcessID = aPeppolSBD.getProcessAsIdentifier ().getURIEncoded ();
    final String sSbdhInstanceID = aPeppolSBD.getInstanceIdentifier ();

    LOGGER.info ("Received inbound SBD: SBDH=" + sSbdhInstanceID + " AS4=" + sAS4MessageID);

    // Signing certificate CN
    String sSigningCertCN = "";
    final X509Certificate aSigningCert = aState.getSigningCertificate ();
    if (aSigningCert != null)
      sSigningCertCN = aSigningCert.getSubjectX500Principal ().getName ();

    // Duplicate detection
    boolean bIsDuplicateAS4 = false;
    boolean bIsDuplicateSBDH = false;

    if (aTxMgr.getByAS4MessageID (sAS4MessageID) != null)
    {
      bIsDuplicateAS4 = true;
      if (APCoreConfig.getDuplicateDetectionAS4Mode () == EDuplicateDetectionMode.REJECT)
      {
        LOGGER.warn ("Rejecting duplicate AS4 message '" + sAS4MessageID + "'");
        return;
      }
    }
    if (aTxMgr.getBySbdhInstanceID (sSbdhInstanceID) != null)
    {
      bIsDuplicateSBDH = true;
      if (APCoreConfig.getDuplicateDetectionSBDHMode () == EDuplicateDetectionMode.REJECT)
      {
        LOGGER.warn ("Rejecting duplicate SBDH instance '" + sSbdhInstanceID + "'");
        return;
      }
    }

    // Receiver check
    for (final IReceiverCheckSPI aCheck : APMetaManager.getAllReceiverChecks ())
    {
      if (!aCheck.isReceiverServiced (sReceiverID, sDocTypeID, sProcessID))
      {
        LOGGER.warn ("Receiver not serviced '" + sReceiverID + "'");
        return;
      }
    }

    // Store in DB
    final String sDocumentHash = HashHelper.sha256Hex (aSBDBytes);
    final OffsetDateTime aAS4Timestamp = aState.getMessageTimestamp () != null ? aState.getMessageTimestamp ()
                                                                                       .toOffsetDateTime ()
                                                                               : APMetaJDBCManager.getTimestampMgr ()
                                                                                                  .getCurrentDateTime ();

    final String sTxID = aTxMgr.create (sIncomingID,
                                        APCoreConfig.getPeppolSeatID () != null ? APCoreConfig.getPeppolSeatID () : "",
                                        // TODO C3 seat ID
                                        APCoreConfig.getPeppolSeatID () != null ? APCoreConfig.getPeppolSeatID () : "",
                                        sSigningCertCN,
                                        sSenderID,
                                        sReceiverID,
                                        sDocTypeID,
                                        sProcessID,
                                        aSBDBytes,
                                        aSBDBytes.length,
                                        sDocumentHash,
                                        sAS4MessageID,
                                        aAS4Timestamp,
                                        sSbdhInstanceID,
                                        bIsDuplicateAS4,
                                        bIsDuplicateSBDH,
                                        null,
                                        APCoreConfig.getMlsType ());

    // Optional verification
    if (APCoreConfig.isVerificationInboundEnabled ())
    {
      for (final IDocumentVerifierSPI aVerifier : APMetaManager.getAllVerifiers ())
      {
        if (aVerifier.verifyDocument (aSBDBytes, sDocTypeID, sProcessID).isFailure ())
        {
          LOGGER.warn ("Inbound document verification failed for '" + sSbdhInstanceID + "'");
          aTxMgr.updateStatus (sTxID, EInboundStatus.REJECTED);
          for (final var aHandler : APMetaManager.getAllNotificationHandlers ())
            aHandler.onVerificationRejection (sTxID, sSbdhInstanceID, "Verification failed");
          return;
        }
      }
    }

    // Forward
    final var aTx = aTxMgr.getByID (sTxID);
    _forwardDocument (aTx);
  }

  private void _forwardDocument (@Nullable final IInboundTransaction aTx)
  {
    final InboundTransactionManagerJDBC aTxMgr = APMetaJDBCManager.getInboundTransactionMgr ();
    final InboundForwardingAttemptManagerJDBC aAttemptMgr = APMetaJDBCManager.getInboundForwardingAttemptMgr ();

    final IDocumentForwarderSPI aForwarder = APMetaManager.getForwarder ();
    if (aForwarder == null)
    {
      LOGGER.error ("No document forwarder configured");
      aTxMgr.updateStatus (aTx.getID (), EInboundStatus.PERMANENTLY_FAILED);
      return;
    }

    // Set status
    aTxMgr.updateStatus (aTx.getID (), EInboundStatus.FORWARDING);

    // Actual forwarding
    final ESuccess eResult = aForwarder.forwardDocument (aTx);

    if (eResult.isSuccess ())
    {
      // Forwarding worked
      aAttemptMgr.createSuccess (aTx.getID ());
      aTxMgr.updateStatusCompleted (aTx.getID (), EInboundStatus.FORWARDED);
      LOGGER.info ("Forwarding successful for transaction: " + aTx);
    }
    else
    {
      // Forwarding failed
      aAttemptMgr.createFailure (aTx.getID (), "forwarding_error", "Forwarding failed");

      final int nNewAttemptCount = aTx.getAttemptCount () + 1;
      final int nMaxRetryAttempts = APCoreConfig.getRetryForwardingMaxAttempts ();
      if (nNewAttemptCount >= nMaxRetryAttempts)
      {
        aTxMgr.updateStatusAndRetry (aTx.getID (),
                                     EInboundStatus.PERMANENTLY_FAILED,
                                     nNewAttemptCount,
                                     null,
                                     "Max retries (" + nMaxRetryAttempts + ") exhausted");

        for (final var aHandler : APMetaManager.getAllNotificationHandlers ())
          aHandler.onPermanentForwardingFailure (aTx.getID (), aTx.getSbdhInstanceID (), "Max retries exhausted");
      }
      else
      {
        final var aNextRetry = BackoffCalculator.calculateNextRetry (nNewAttemptCount,
                                                                     APCoreConfig.getRetryForwardingInitialBackoffMs (),
                                                                     APCoreConfig.getRetryForwardingBackoffMultiplier (),
                                                                     APCoreConfig.getRetryForwardingMaxBackoffMs ());
        aTxMgr.updateStatusAndRetry (aTx.getID (),
                                     EInboundStatus.FORWARD_FAILED,
                                     nNewAttemptCount,
                                     aNextRetry,
                                     "Forwarding failed");
      }
    }
  }

  public void processAS4ResponseMessage (@NonNull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                         @NonNull final IAS4IncomingMessageState aIncomingState,
                                         @NonNull final String sResponseMessageID,
                                         final byte [] aResponseBytes,
                                         final boolean bResponsePayloadIsAvailable,
                                         @NonNull final AS4ErrorList aEbmsErrorMessages)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("AS4 response message received: " + sResponseMessageID);
  }
}
