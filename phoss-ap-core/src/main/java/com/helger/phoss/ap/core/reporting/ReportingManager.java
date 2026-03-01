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

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.db.APJdbcMetaManager;

public final class ReportingManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ReportingManager.class);

  private ReportingManager ()
  {}

  @NonNull
  public static ESuccess createOutboundPeppolReportingItem (@NonNull final String sTransactionID)
  {
    LOGGER.info ("Marking outbound transaction as reported: " + sTransactionID);
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    // TODO outbound reporting
    return aTxMgr.updateReportingStatus (sTransactionID, EReportingStatus.REPORTED);
  }

  @NonNull
  public static ESuccess createInboundPeppolReportingItem (@NonNull final String sTransactionID)
  {
    ValueEnforcer.notNull (sTransactionID, "TransactionID");

    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();

    // Re-read the transaction to get the latest data
    final var aTx = aTxMgr.getByID (sTransactionID);
    if (aTx == null)
      throw new IllegalArgumentException ("The provided transaction ID '" + sTransactionID + "' does not exist");

    LOGGER.info ("Counting inbound transaction '" + sTransactionID + "' for Peppol Reporting");

    if (StringHelper.isEmpty (aTx.getC4CountryCode ()))
      throw new IllegalStateException ("Inbound transaction '" + sTransactionID + "' has no C4 country code yet");

    try
    {
      final IDocumentTypeIdentifier aDocTypeID = PeppolIdentifierFactory.INSTANCE.parseDocumentTypeIdentifier (aTx.getDocTypeID ());
      if (aDocTypeID == null)
        throw new IllegalStateException ("Inbound transaction '" +
                                         sTransactionID +
                                         "' contains the invalid document type ID '" +
                                         aTx.getDocTypeID () +
                                         "'");

      final IProcessIdentifier aProcessID = PeppolIdentifierFactory.INSTANCE.parseProcessIdentifier (aTx.getProcessID ());
      if (aProcessID == null)
        throw new IllegalStateException ("Inbound transaction '" +
                                         sTransactionID +
                                         "' contains the invalid process ID '" +
                                         aTx.getProcessID () +
                                         "'");

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
                                                                    .endUserID (aTx.getReceiverID ())
                                                                    .build ();

      PeppolReportingBackend.withBackendDo (APConfigProvider.getConfig (),
                                            aBackend -> aBackend.storeReportingItem (aReportingItem));

      // Remember that we did it
      return aTxMgr.updateReportingStatus (sTransactionID, EReportingStatus.REPORTED);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to store Peppol Reporting data for inbound transaction '" + sTransactionID + "'");
    }
    return ESuccess.FAILURE;
  }
}
