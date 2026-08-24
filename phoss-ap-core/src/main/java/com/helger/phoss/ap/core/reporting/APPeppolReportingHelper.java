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
package com.helger.phoss.ap.core.reporting;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.enduser.PeppolEndUserHelper;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.db.APJdbcMetaManager;

/**
 * Manager to handle Peppol Reporting items for inbound and outbound data.
 *
 * @author Philip Helger
 */
public final class APPeppolReportingHelper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APPeppolReportingHelper.class);

  private APPeppolReportingHelper ()
  {}

  /**
   * Determine the End User ID to be used for Peppol Reporting from the provided participant
   * identifier. When sending, that is the C1 (sender) participant identifier, when receiving it is
   * the C4 (receiver) participant identifier.
   * <p>
   * The participant identifier is not used as-is, because several countries have multiple
   * identifier schemes running in parallel that all identify the same End User - e.g.
   * <code>0208:0123456789</code> and <code>9925:BE0123456789</code> in Belgium. Using the
   * participant identifier directly would therefore count a single End User multiple times in the
   * End User Statistics Report (EUSR). {@link PeppolEndUserHelper} unifies the identifier and
   * applies its mapping rules - see
   * <a href="https://github.com/phax/peppol-commons/issues/80">peppol-commons issue #80</a>.
   * Deployments can customize the mapping rules via the static methods of
   * {@link PeppolEndUserHelper}.
   * </p>
   *
   * @param aParticipantID
   *        The participant identifier to determine the End User ID of. May not be
   *        <code>null</code>.
   * @return The URI encoded representation of the effective End User participant identifier - e.g.
   *         <code>iso6523-actorid-upis::0208:0123456789</code>. Neither <code>null</code> nor
   *         empty.
   */
  @NonNull
  @Nonempty
  public static String getEffectiveEndUserID (@NonNull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final String ret = PeppolEndUserHelper.getEffectiveEndUserID (aParticipantID);
    // Fallback for participant identifiers with an empty value
    return StringHelper.isNotEmpty (ret) ? ret : aParticipantID.getURIEncoded ();
  }

  /**
   * Store a Peppol Reporting item for the given outbound transaction and update its reporting
   * status.
   *
   * @param sTransactionID
   *        The outbound transaction ID. May not be <code>null</code>.
   * @param aReportingItem
   *        The pre-built Peppol Reporting item to store. May not be <code>null</code>.
   * @return {@link ESuccess#SUCCESS} if stored successfully, {@link ESuccess#FAILURE} otherwise.
   */
  @NonNull
  public static ESuccess createOutboundPeppolReportingItem (@NonNull final String sTransactionID,
                                                            @NonNull final PeppolReportingItem aReportingItem)
  {
    ValueEnforcer.notNull (sTransactionID, "TransactionID");
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");

    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();

    LOGGER.info ("Counting outbound transaction '" + sTransactionID + "' for Peppol Reporting");

    try
    {
      // Re-read the transaction to get the latest data
      if (!aTxMgr.containsTransactionWithID (sTransactionID))
        throw new IllegalArgumentException ("The provided outbound transaction ID '" +
                                            sTransactionID +
                                            "' does not exist");

      PeppolReportingBackend.withBackendDo (APConfigProvider.getConfig (),
                                            aBackend -> aBackend.storeReportingItem (aReportingItem));

      // Remember that we did it
      return aTxMgr.updateReportingStatus (sTransactionID, EReportingStatus.REPORTED);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to store Peppol Reporting data for outbound transaction '" + sTransactionID + "'", ex);

      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onUnexpectedException ("APPeppolReportingHelper.createOutboundPeppolReportingItem",
                                        "Failed to store Peppol Reporting data for outbound transaction '" +
                                                                                                     sTransactionID +
                                                                                                     "'",
                                        ex);
    }
    return ESuccess.FAILURE;
  }

  /**
   * Build and store a Peppol Reporting item for the given inbound transaction and update its
   * reporting status. The reporting item is constructed from the transaction's metadata including
   * sender, receiver, document type, process, and country codes.
   *
   * @param sTransactionID
   *        The inbound transaction ID. May not be <code>null</code>.
   * @return {@link ESuccess#SUCCESS} if stored successfully, {@link ESuccess#FAILURE} otherwise.
   */
  @NonNull
  public static ESuccess createInboundPeppolReportingItem (@NonNull final String sTransactionID)
  {
    ValueEnforcer.notNull (sTransactionID, "TransactionID");

    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();

    LOGGER.info ("Counting inbound transaction '" + sTransactionID + "' for Peppol Reporting");

    try
    {
      // Re-read the transaction to get the latest data
      final var aTx = aTxMgr.getByID (sTransactionID);
      if (aTx == null)
        throw new IllegalArgumentException ("The provided transaction ID '" + sTransactionID + "' does not exist");

      if (aTx.getReportingStatus () == EReportingStatus.REPORTED)
      {
        // A document that is forwarded a second time - e.g. via the replay API - must not be
        // counted a second time in the Peppol Reporting
        LOGGER.info ("Inbound transaction '" +
                     sTransactionID +
                     "' was already counted for Peppol Reporting - not counting it again");
        return ESuccess.SUCCESS;
      }

      if (StringHelper.isEmpty (aTx.getC4CountryCode ()))
        throw new IllegalStateException ("Inbound transaction '" + sTransactionID + "' has no C4 country code yet");

      final IDocumentTypeIdentifier aDocTypeID = aIF.parseDocumentTypeIdentifier (aTx.getDocTypeID ());
      if (aDocTypeID == null)
      {
        throw new IllegalStateException ("Inbound transaction '" +
                                         sTransactionID +
                                         "' contains the invalid document type ID '" +
                                         aTx.getDocTypeID () +
                                         "'");
      }

      final IProcessIdentifier aProcessID = aIF.parseProcessIdentifier (aTx.getProcessID ());
      if (aProcessID == null)
      {
        throw new IllegalStateException ("Inbound transaction '" +
                                         sTransactionID +
                                         "' contains the invalid process ID '" +
                                         aTx.getProcessID () +
                                         "'");
      }

      // The C4 participant identifier is the End User of an inbound transaction
      final IParticipantIdentifier aReceiverID = aIF.parseParticipantIdentifier (aTx.getReceiverID ());
      if (aReceiverID == null)
      {
        throw new IllegalStateException ("Inbound transaction '" +
                                         sTransactionID +
                                         "' contains the invalid receiver participant ID '" +
                                         aTx.getReceiverID () +
                                         "'");
      }

      final PeppolReportingItem aReportingItem = PeppolReportingItem.builder ()
                                                                    .exchangeDateTime (aTx.getAS4Timestamp ())
                                                                    .directionReceiving ()
                                                                    .c2ID (aTx.getC2SeatID ())
                                                                    .c3ID (aTx.getC3SeatID ())
                                                                    .docTypeID (aDocTypeID)
                                                                    .processID (aProcessID)
                                                                    .transportProtocolPeppolAS4v2 ()
                                                                    .c1CountryCode (aTx.getC1CountryCode ())
                                                                    .c4CountryCode (aTx.getC4CountryCode ())
                                                                    .endUserID (getEffectiveEndUserID (aReceiverID))
                                                                    .build ();

      PeppolReportingBackend.withBackendDo (APConfigProvider.getConfig (),
                                            aBackend -> aBackend.storeReportingItem (aReportingItem));

      // Remember that we did it
      return aTxMgr.updateReportingStatus (sTransactionID, EReportingStatus.REPORTED);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to store Peppol Reporting data for inbound transaction '" + sTransactionID + "'", ex);

      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onUnexpectedException ("APPeppolReportingHelper.createInboundPeppolReportingItem",
                                        "Failed to store Peppol Reporting data for inbound transaction '" +
                                                                                                    sTransactionID +
                                                                                                    "'",
                                        ex);
    }
    return ESuccess.FAILURE;
  }
}
