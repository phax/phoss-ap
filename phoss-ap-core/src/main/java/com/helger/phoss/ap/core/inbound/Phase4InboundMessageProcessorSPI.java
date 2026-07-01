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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.http.header.HttpHeaderMap;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.AS4Error;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.logging.Phase4LogCustomizer;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phase4.util.Phase4Exception;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.core.APCoreConfig;

/**
 * SPI implementation for processing inbound Peppol AS4 messages. This is a thin boundary adapter
 * that translates the phase4-specific message representation into the domain inputs of
 * {@link InboundOrchestrator#processIncomingDocument} and translates the resulting processing
 * errors back into AS4/EBMS errors. All business logic lives in {@link InboundOrchestrator}.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class Phase4InboundMessageProcessorSPI implements IPhase4PeppolIncomingSBDHandlerSPI
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (Phase4InboundMessageProcessorSPI.class);

  /**
   * Adapt the AS4 transport message timestamp to a domain offset date time.
   *
   * @param aMessageTimestamp
   *        The AS4 message timestamp. May be <code>null</code>.
   * @return <code>null</code> if no timestamp was provided, otherwise the resolved offset date
   *         time.
   */
  @Nullable
  private static OffsetDateTime _adaptAS4Timestamp (@Nullable final XMLOffsetDateTime aMessageTimestamp)
  {
    if (aMessageTimestamp == null)
      return null;

    // Was a timezone offset provided?
    if (aMessageTimestamp.getOffset () != null)
    {
      // Use provided timezone offset
      return aMessageTimestamp.toOffsetDateTime ();
    }

    // Default to UTC as per AS4 specification
    return OffsetDateTime.of (aMessageTimestamp.toLocalDateTime (), ZoneOffset.UTC);
  }

  /** {@inheritDoc} */
  public void handleIncomingSBD (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @NonNull final HttpHeaderMap aHeaders,
                                 @NonNull final Ebms3UserMessage aUserMessage,
                                 final byte @NonNull [] aSBDBytes,
                                 @NonNull final StandardBusinessDocument aSBD,
                                 @NonNull final PeppolSBDHData aPeppolSBD,
                                 @NonNull final IAS4IncomingMessageState aIncomingState,
                                 @NonNull final AS4ErrorList aProcessingErrorMessages) throws Exception
  {
    if (!APCoreConfig.isReceivingEnabled ())
    {
      LOGGER.info ("Peppol AP receiving is disabled");
      throw new Phase4Exception ("Peppol AP receiving is disabled");
    }

    final String sLogPrefix = "[" + aMessageMetadata.getIncomingUniqueID () + "] ";
    Phase4LogCustomizer.setThreadLocalLogPrefix (sLogPrefix);
    try
    {
      // Adapt the AS4 transport message timestamp to a domain timestamp
      final OffsetDateTime aProvidedAS4Timestamp = _adaptAS4Timestamp (aIncomingState.getMessageTimestamp ());

      // Delegate the entire inbound processing pipeline to the control layer
      final ICommonsList <String> aProcessingErrors = InboundOrchestrator.processIncomingDocument (sLogPrefix,
                                                                                                   aMessageMetadata.getIncomingUniqueID (),
                                                                                                   aIncomingState.getMessageID (),
                                                                                                   aIncomingState.getSigningCertificate (),
                                                                                                   aProvidedAS4Timestamp,
                                                                                                   aPeppolSBD,
                                                                                                   aSBDBytes);

      // Translate the domain-level processing errors back into AS4/EBMS errors
      final Locale aDisplayLocale = CPhossAP.DEFAULT_LOCALE;
      for (final String sErrorDetail : aProcessingErrors)
        aProcessingErrorMessages.add (AS4Error.builder ()
                                              .ebmsError (EEbmsError.EBMS_OTHER.errorBuilder (aDisplayLocale)
                                                                               .refToMessageInError (aIncomingState.getMessageID ())
                                                                               .errorDetail (sErrorDetail))
                                              .build ());
    }
    finally
    {
      Phase4LogCustomizer.clearThreadLocals ();
    }
  }

  /** {@inheritDoc} */
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
