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
package com.helger.phoss.ap.core.inbound;

import java.io.File;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.base.string.StringHelper;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.http.header.HttpHeaderMap;
import com.helger.peppol.mls.PeppolMLSBuilder;
import com.helger.peppol.mls.PeppolMLSMarshaller;
import com.helger.peppol.reporting.api.CPeppolReporting;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.AS4Error;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.IInboundForwardingAttemptManager;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EDuplicateDetectionMode;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.spi.IDocumentForwarderSPI;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IPeppolReceiverCheckSPI;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.basic.storage.DocumentStorageHelper;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APMetaManager;
import com.helger.phoss.ap.core.ReportingManager;
import com.helger.phoss.ap.core.helper.BackoffCalculator;
import com.helger.phoss.ap.core.helper.HashHelper;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.security.certificate.CertificateHelper;

import oasis.names.specification.ubl.schema.xsd.applicationresponse_21.ApplicationResponseType;

@IsSPIImplementation
public class Phase4InboundMessageProcessorSPI implements IPhase4PeppolIncomingSBDHandlerSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4InboundMessageProcessorSPI.class);

  private void _forwardDocument (@Nullable final IInboundTransaction aTx)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundForwardingAttemptManager aAttemptMgr = APJdbcMetaManager.getInboundForwardingAttemptMgr ();

    final IDocumentForwarderSPI aForwarder = APMetaManager.getForwarder ();
    if (aForwarder == null)
    {
      LOGGER.error ("Internal error - No document forwarder configured");
      aTxMgr.updateStatus (aTx.getID (), EInboundStatus.PERMANENTLY_FAILED);
      return;
    }

    // Set status
    aTxMgr.updateStatus (aTx.getID (), EInboundStatus.FORWARDING);

    // Actual forwarding
    ForwardingResult aResult;
    try
    {
      aResult = aForwarder.forwardDocument (aTx);
    }
    catch (final Exception ex)
    {
      // Be resilient...
      aResult = ForwardingResult.failure ("forward_exception",
                                          "Internal error forwarding the document: " +
                                                               ex.getMessage () +
                                                               " (" +
                                                               ex.getClass ().getName () +
                                                               ")");
    }

    if (aResult.isSuccess ())
    {
      // Forwarding worked
      aAttemptMgr.createSuccess (aTx.getID ());
      aTxMgr.updateStatusCompleted (aTx.getID (), EInboundStatus.FORWARDED);
      LOGGER.info ("Forwarding successful for transaction: " + aTx);

      if (aResult.hasCountryCodeC4 ())
      {
        // We can store the reporting item immediately
        aTxMgr.updateC4CountryCode (aTx.getID (), aResult.getCountryCodeC4 ());
        ReportingManager.storeInboundForReporting (aTx);
      }
    }
    else
    {
      // Forwarding failed
      aAttemptMgr.createFailure (aTx.getID (), aResult.getErrorCode (), aResult.getErrorDetails ());

      final int nNewAttemptCount = aTx.getAttemptCount () + 1;
      final int nMaxRetryAttempts = APCoreConfig.getRetryForwardingMaxAttempts ();
      if (nNewAttemptCount >= nMaxRetryAttempts)
      {
        // Maximum number of retries are exhausted - we go on "permanently failed"
        aTxMgr.updateStatusAndRetry (aTx.getID (),
                                     EInboundStatus.PERMANENTLY_FAILED,
                                     nNewAttemptCount,
                                     null,
                                     "Max retries (" +
                                           nMaxRetryAttempts +
                                           ") exhausted: " +
                                           aResult.getErrorDetails ());

        for (final var aHandler : APMetaManager.getAllNotificationHandlers ())
          aHandler.onPermanentForwardingFailure (aTx.getID (), aTx.getSbdhInstanceID (), "Max retries exhausted");
      }
      else
      {
        // Calculate the next retry and remember it
        final var aNextRetry = BackoffCalculator.calculateNextRetry (nNewAttemptCount,
                                                                     APCoreConfig.getRetryForwardingInitialBackoffMs (),
                                                                     APCoreConfig.getRetryForwardingBackoffMultiplier (),
                                                                     APCoreConfig.getRetryForwardingMaxBackoffMs ());
        aTxMgr.updateStatusAndRetry (aTx.getID (),
                                     EInboundStatus.FORWARD_FAILED,
                                     nNewAttemptCount,
                                     aNextRetry,
                                     aResult.getErrorDetails ());
      }
    }
  }

  public void handleIncomingSBD (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @NonNull final HttpHeaderMap aHeaders,
                                 @NonNull final Ebms3UserMessage aUserMessage,
                                 final byte @NonNull [] aSBDBytes,
                                 @NonNull final StandardBusinessDocument aSBD,
                                 @NonNull final PeppolSBDHData aPeppolSBD,
                                 @NonNull final IAS4IncomingMessageState aIncomingState,
                                 @NonNull final AS4ErrorList aProcessingErrorMessages) throws Exception
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final Locale aDisplayLocale = CPhossAP.DEFAULT_LOCALE;

    final String sIncomingID = aMessageMetadata.getIncomingUniqueID ();
    final String sAS4MessageID = aIncomingState.getMessageID ();
    final String sSenderID = aPeppolSBD.getSenderAsIdentifier ().getURIEncoded ();
    final String sReceiverID = aPeppolSBD.getReceiverAsIdentifier ().getURIEncoded ();
    final String sDocTypeID = aPeppolSBD.getDocumentTypeAsIdentifier ().getURIEncoded ();
    final String sProcessID = aPeppolSBD.getProcessAsIdentifier ().getURIEncoded ();
    final String sSbdhInstanceID = aPeppolSBD.getInstanceIdentifier ();
    String sC1CountryCode = aPeppolSBD.getCountryC1 ();
    if (StringHelper.isEmpty (sC1CountryCode))
    {
      // Fallback to ZZ to make sure the reporting item can be created
      sC1CountryCode = CPeppolReporting.REPLACEMENT_COUNTRY_CODE;
    }
    final String sC2ID = CertificateHelper.getSubjectCN (aIncomingState.getSigningCertificate ());
    final String sC3ID = APCoreConfig.getPeppolSeatID ();

    LOGGER.info ("Received inbound SBD: SBDH=" + sSbdhInstanceID + " AS4=" + sAS4MessageID);

    // Signing certificate CN
    String sSigningCertCN = "";
    final X509Certificate aSigningCert = aIncomingState.getSigningCertificate ();
    if (aSigningCert != null)
      sSigningCertCN = aSigningCert.getSubjectX500Principal ().getName ();

    // Duplicate detection
    boolean bIsDuplicateAS4 = false;
    boolean bIsDuplicateSBDH = false;

    if (aTxMgr.containsByAS4MessageID (sAS4MessageID))
    {
      bIsDuplicateAS4 = true;
      if (APCoreConfig.getDuplicateDetectionAS4Mode () == EDuplicateDetectionMode.REJECT)
      {
        final String sMsg = "Rejecting duplicate AS4 message '" + sAS4MessageID + "'";
        LOGGER.error (sMsg);
        aProcessingErrorMessages.add (AS4Error.builder ()
                                              .ebmsError (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                                               .refToMessageInError (aIncomingState.getMessageID ())
                                                                               .errorDetail (sMsg))
                                              .build ());
        return;
      }
    }

    if (aTxMgr.containsBySbdhInstanceID (sSbdhInstanceID))
    {
      bIsDuplicateSBDH = true;
      if (APCoreConfig.getDuplicateDetectionSBDHMode () == EDuplicateDetectionMode.REJECT)
      {
        final String sMsg = "Rejecting duplicate SBDH instance '" + sSbdhInstanceID + "'";
        LOGGER.error (sMsg);
        aProcessingErrorMessages.add (AS4Error.builder ()
                                              .ebmsError (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                                               .refToMessageInError (aIncomingState.getMessageID ())
                                                                               .errorDetail (sMsg))
                                              .build ());
        return;
      }
    }

    // Receiver check
    for (final IPeppolReceiverCheckSPI aReceiverCheck : APMetaManager.getAllPeppolReceiverChecks ())
    {
      if (!aReceiverCheck.isReceiverServiced (sReceiverID, sDocTypeID, sProcessID))
      {
        LOGGER.error ("Receiver not serviced '" + sReceiverID + "'");
        aProcessingErrorMessages.add (AS4Error.builder ()
                                              .ebmsError (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                                               .refToMessageInError (aIncomingState.getMessageID ())
                                                                               .errorDetail ("PEPPOL:NOT_SERVICED"))
                                              .build ());

        for (final var aHandler : APMetaManager.getAllNotificationHandlers ())
          aHandler.onInboundReceiverNotServiced (sSenderID, sReceiverID, sDocTypeID, sProcessID, sSbdhInstanceID);

        return;
      }
    }

    final String sSbdhHash = HashHelper.sha256Hex (aSBDBytes);
    final OffsetDateTime aAS4Timestamp;
    if (aIncomingState.getMessageTimestamp () != null)
    {
      // Was an offset provided?
      if (aIncomingState.getMessageTimestamp ().getOffset () != null)
        aAS4Timestamp = aIncomingState.getMessageTimestamp ().toOffsetDateTime ();
      else
      {
        // Default to UTC as per AS4 specification
        aAS4Timestamp = OffsetDateTime.of (aIncomingState.getMessageTimestamp ().toLocalDateTime (), ZoneOffset.UTC);
      }
    }
    else
    {
      aAS4Timestamp = APBasicMetaManager.getTimestampMgr ().getCurrentDateTime ();
      LOGGER.warn ("The incoming AS4 message has not AS4 message timestamp - using the current date time instead");
    }

    // Store document to disk
    final String sDocumentPath = DocumentStorageHelper.storeDocument (new File (APCoreConfig.getStorageInboundPath ()),
                                                                      aAS4Timestamp,
                                                                      sSbdhInstanceID + ".sbd",
                                                                      aSBDBytes);

    // Store in DB
    final String sTxID = aTxMgr.create (sIncomingID,
                                        sC2ID,
                                        sC3ID,
                                        sSigningCertCN,
                                        sSenderID,
                                        sReceiverID,
                                        sDocTypeID,
                                        sProcessID,
                                        sDocumentPath,
                                        aSBDBytes.length,
                                        sSbdhHash,
                                        sAS4MessageID,
                                        aAS4Timestamp,
                                        sSbdhInstanceID,
                                        sC1CountryCode,
                                        bIsDuplicateAS4,
                                        bIsDuplicateSBDH,
                                        null,
                                        APCoreConfig.getMlsType ());

    // Optional verification
    if (APCoreConfig.isVerificationInboundEnabled ())
    {
      for (final IInboundDocumentVerifierSPI aVerifier : APMetaManager.getAllInboundVerifiers ())
      {
        if (aVerifier.verifyDocument (aSBDBytes, sDocTypeID, sProcessID).isFailure ())
        {
          LOGGER.warn ("Inbound document verification failed for '" + sSbdhInstanceID + "'");
          aTxMgr.updateStatus (sTxID, EInboundStatus.REJECTED);

          for (final var aHandler : APMetaManager.getAllNotificationHandlers ())
            aHandler.onInboundVerificationRejection (sTxID, sSbdhInstanceID, "Inbound verification failed");
          return;
        }
      }
    }

    if (sDocTypeID.equals (EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getURIEncoded ()))
    {
      LOGGER.info ("Handling incoming MLS message");
      final ErrorList aXSDErrors = new ErrorList ();
      final ApplicationResponseType aMLS = new PeppolMLSMarshaller ().setCollectErrors (aXSDErrors)
                                                                     .read (aPeppolSBD.getBusinessMessageNoClone ());
      if (aMLS == null)
      {
        LOGGER.error ("Failed to parse incoming MLS");
        // Add all XSD errors to the output
        for (final IError aError : aXSDErrors)
        {
          final String sDetails = "Peppol MLS XSD Issue: " + aError.getAsString (aDisplayLocale);
          aProcessingErrorMessages.add (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                             .refToMessageInError (sAS4MessageID)
                                                             .errorDetail (sDetails)
                                                             .build ());
        }
        return;
      }

      final PeppolMLSBuilder aBuilder = PeppolMLSBuilder.createForApplicationResponse (aMLS);
      // TODO handle incoming MLS
    }

    // Forward
    final var aTx = aTxMgr.getByID (sTxID);
    _forwardDocument (aTx);
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
