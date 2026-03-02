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
import org.slf4j.Logger;
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
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.AS4Error;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.logging.Phase4LogCustomizer;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EDuplicateDetectionMode;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IPeppolReceiverCheckSPI;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.basic.storage.DocumentStorageHelper;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.helper.HashHelper;
import com.helger.phoss.ap.core.mls.MlsHandler;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.security.certificate.CertificateHelper;

import oasis.names.specification.ubl.schema.xsd.applicationresponse_21.ApplicationResponseType;

@IsSPIImplementation
public class Phase4InboundMessageProcessorSPI implements IPhase4PeppolIncomingSBDHandlerSPI
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4InboundMessageProcessorSPI.class);

  public void handleIncomingSBD (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @NonNull final HttpHeaderMap aHeaders,
                                 @NonNull final Ebms3UserMessage aUserMessage,
                                 final byte @NonNull [] aSBDBytes,
                                 @NonNull final StandardBusinessDocument aSBD,
                                 @NonNull final PeppolSBDHData aPeppolSBD,
                                 @NonNull final IAS4IncomingMessageState aIncomingState,
                                 @NonNull final AS4ErrorList aProcessingErrorMessages) throws Exception
  {
    final String sLogPrefix = "[" + aMessageMetadata.getIncomingUniqueID () + "] ";
    Phase4LogCustomizer.setThreadLocalLogPrefix (sLogPrefix);
    try
    {
      final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
      final Locale aDisplayLocale = CPhossAP.DEFAULT_LOCALE;

      final String sIncomingID = aMessageMetadata.getIncomingUniqueID ();
      final String sAS4MessageID = aIncomingState.getMessageID ();
      final String sSenderID = aPeppolSBD.getSenderURIEncoded ();
      final String sReceiverID = aPeppolSBD.getReceiverURIEncoded ();
      final String sDocTypeID = aPeppolSBD.getDocumentTypeURIEncoded ();
      final String sProcessID = aPeppolSBD.getProcessURIEncoded ();
      final String sSbdhInstanceID = aPeppolSBD.getInstanceIdentifier ();
      String sC1CountryCode = aPeppolSBD.getCountryC1 ();
      if (StringHelper.isEmpty (sC1CountryCode))
      {
        // Fallback to ZZ to make sure the reporting item can be created
        sC1CountryCode = CPeppolReporting.REPLACEMENT_COUNTRY_CODE;
      }
      final String sC2ID = CertificateHelper.getSubjectCN (aIncomingState.getSigningCertificate ());
      final String sC3ID = APCoreConfig.getPeppolSeatID ();

      LOGGER.info (sLogPrefix + "Received inbound SBD: SBDH=" + sSbdhInstanceID + " AS4=" + sAS4MessageID);

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
          LOGGER.error (sLogPrefix + sMsg);
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
          LOGGER.error (sLogPrefix + sMsg);
          aProcessingErrorMessages.add (AS4Error.builder ()
                                                .ebmsError (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                                                 .refToMessageInError (aIncomingState.getMessageID ())
                                                                                 .errorDetail (sMsg))
                                                .build ());
          return;
        }
      }

      // Receiver check
      for (final IPeppolReceiverCheckSPI aReceiverCheck : APCoreMetaManager.getAllPeppolReceiverChecks ())
      {
        if (!aReceiverCheck.isReceiverServiced (sReceiverID, sDocTypeID, sProcessID))
        {
          LOGGER.error (sLogPrefix + "Receiver not serviced '" + sReceiverID + "'");
          aProcessingErrorMessages.add (AS4Error.builder ()
                                                .ebmsError (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                                                 .refToMessageInError (aIncomingState.getMessageID ())
                                                                                 .errorDetail ("PEPPOL:NOT_SERVICED"))
                                                .build ());

          for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
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
        LOGGER.warn (sLogPrefix +
                     "The incoming AS4 message has not AS4 message timestamp - using the current date time instead");
      }

      // Store document to disk
      final String sDocumentPath = DocumentStorageHelper.storeDocument (new File (APBasicConfig.getStorageInboundPath ()),
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
      if (sTxID == null)
        throw new IllegalStateException ("Failed to store incoming transaction");

      // Optional verification
      if (APCoreConfig.isVerificationInboundEnabled ())
      {
        for (final IInboundDocumentVerifierSPI aVerifier : APCoreMetaManager.getAllInboundVerifiers ())
        {
          if (aVerifier.verifyDocument (aSBDBytes, sDocTypeID, sProcessID).isFailure ())
          {
            LOGGER.warn (sLogPrefix + "Inbound document verification failed for '" + sSbdhInstanceID + "'");
            aTxMgr.updateStatus (sTxID, EInboundStatus.REJECTED);

            for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
              aHandler.onInboundVerificationRejection (sTxID, sSbdhInstanceID, "Inbound verification failed");
            return;
          }
        }
      }

      if (sDocTypeID.equals (EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getURIEncoded ()) &&
        sProcessID.equals (EPredefinedProcessIdentifier.urn_peppol_edec_mls.getURIEncoded ()))
      {
        LOGGER.info (sLogPrefix + "Handling incoming MLS message");
        final ErrorList aXSDErrors = new ErrorList ();
        final ApplicationResponseType aMLS = new PeppolMLSMarshaller ().setCollectErrors (aXSDErrors)
                                                                       .read (aPeppolSBD.getBusinessMessageNoClone ());
        if (aMLS == null)
        {
          LOGGER.error (sLogPrefix + "Failed to parse incoming MLS");
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

        // The reference ID in the MLS is the SBDH Instance ID of the original outbound business
        // document
        final String sReferencedSbdhInstanceID = aBuilder.referenceId ();
        if (StringHelper.isEmpty (sReferencedSbdhInstanceID))
        {
          LOGGER.error (sLogPrefix + "MLS message '" + sSbdhInstanceID + "' has no reference ID - cannot correlate");
          aTxMgr.updateStatus (sTxID, EInboundStatus.PERMANENTLY_FAILED);
          return;
        }

        // Correlate with the original outbound transaction and update its MLS status
        if (MlsHandler.handleIncomingMls (sLogPrefix,
                                          sReferencedSbdhInstanceID,
                                          aBuilder.responseCode (),
                                          aAS4Timestamp,
                                          aBuilder.id ()).isFailure ())
        {
          for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            aHandler.onInboundMLSCorrelationError (sTxID, sReferencedSbdhInstanceID, aBuilder.responseCode ());
        }
      }

      // Forward - Business Document and MLS
      final var aTx = aTxMgr.getByID (sTxID);
      if (aTx == null)
        throw new IllegalStateException ("Failed to resolve previously stored transaction with ID '" + sTxID + "'");

      if (InboundOrchestrator.forwardDocument (sLogPrefix, aTx).isFailure ())
      {
        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onInboundForwardingError (sTxID, false);
      }
    }
    finally
    {
      Phase4LogCustomizer.clearThreadLocals ();
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
