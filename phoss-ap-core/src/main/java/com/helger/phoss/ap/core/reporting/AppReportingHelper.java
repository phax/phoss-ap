/*
 * Copyright (C) 2023-2026 Philip Helger (www.helger.com)
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

import java.time.LocalDate;
import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.peppol.reporting.api.CPeppolReporting;
import com.helger.peppol.reporting.api.PeppolReportingHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.eusr.EndUserStatisticsReport;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;
import com.helger.peppol.reporting.tsr.TransactionStatisticsReport;
import com.helger.peppol.reportingsupport.EPeppolReportType;
import com.helger.peppol.reportingsupport.IPeppolReportSenderCallback;
import com.helger.peppol.reportingsupport.PeppolReportingSupport;
import com.helger.peppol.reportingsupport.sql.PeppolReportSQLHandler;
import com.helger.peppol.reportingsupport.sql.PeppolReportStorageSQL;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.security.certificate.TrustedCAChecker;

/**
 * Helper class for report generation
 *
 * @author Philip Helger
 */
public final class AppReportingHelper
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AppReportingHelper.class);

  @NonNull
  public static YearMonth getValidYearMonthInAPI (final int nYear, final int nMonth)
  {
    final int nRealYear = Math.max (nYear, 2024);
    final int nRealMonth = Math.min (Math.max (nMonth, 1), 12);

    final LocalDate aNow = PDTFactory.getCurrentLocalDate ();
    if (nRealYear > aNow.getYear ())
      throw new IllegalArgumentException ("The year value " + nRealYear + " is in the future");
    if (nRealYear == aNow.getYear () && nRealMonth > aNow.getMonthValue ())
      throw new IllegalArgumentException ("The month value " + nRealMonth + " is in the future");

    return YearMonth.of (nRealYear, nRealMonth);
  }

  @Nullable
  public static TransactionStatisticsReportType createTSR (@NonNull final YearMonth aYearMonth) throws PeppolReportingBackendException
  {
    LOGGER.info ("Trying to create Peppol Reporting TSR for " + aYearMonth);

    // Now get all items from data storage and store them in a list (we start
    // with an initial size of 1K to avoid too many copy operations)
    final ICommonsList <PeppolReportingItem> aReportingItems = new CommonsArrayList <> (1024);
    if (PeppolReportingBackend.withBackendDo (APConfigProvider.getConfig (),
                                              aBackend -> aBackend.forEachReportingItem (aYearMonth,
                                                                                         aReportingItems::add))
                              .isSuccess ())
    {
      // Create report with the read transactions
      return TransactionStatisticsReport.builder ()
                                        .monthOf (aYearMonth)
                                        .reportingServiceProviderID (APCoreConfig.getPeppolSeatID ())
                                        .reportingItemList (aReportingItems)
                                        .build ();
    }
    return null;
  }

  @Nullable
  public static EndUserStatisticsReportType createEUSR (@NonNull final YearMonth aYearMonth) throws PeppolReportingBackendException
  {
    LOGGER.info ("Trying to create Peppol Reporting EUSR for " + aYearMonth);

    // Now get all items from data storage and store them in a list (we start
    // with an initial size of 1K to avoid too many copy operations)
    final ICommonsList <PeppolReportingItem> aReportingItems = new CommonsArrayList <> (1024);
    if (PeppolReportingBackend.withBackendDo (APConfigProvider.getConfig (),
                                              aBackend -> aBackend.forEachReportingItem (aYearMonth,
                                                                                         aReportingItems::add))
                              .isSuccess ())
    {
      // Create report with the read transactions
      return EndUserStatisticsReport.builder ()
                                    .monthOf (aYearMonth)
                                    .reportingServiceProviderID (APCoreConfig.getPeppolSeatID ())
                                    .reportingItemList (aReportingItems)
                                    .build ();
    }
    return null;
  }

  /**
   * Create, validate, store, send and store sending reports for Peppol TSR and EUSR for one period.
   *
   * @param aYearMonth
   *        The reporting period to use. May not be <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  public static ESuccess createAndSendPeppolReports (@NonNull final YearMonth aYearMonth)
  {
    ValueEnforcer.notNull (aYearMonth, "YearMonth");

    final StopWatch aSW = StopWatch.createdStarted ();
    LOGGER.info ("Trying to create and send Peppol Reports for " + aYearMonth);

    int nSuccessCount = 0;

    // How to do AS4 sending
    final IPeppolReportSenderCallback aPeppolSender = (aDocTypeID, aProcessID, sMessagePayload) -> {
      // Make Network decisions
      final EPeppolNetwork eStage = APCoreConfig.getPeppolStage ();
      final ISMLInfo aSMLInfo = eStage.getSMLInfo ();
      final TrustedCAChecker aAPCA = eStage.isProduction () ? PeppolTrustedCA.peppolProductionAP ()
                                                            : PeppolTrustedCA.peppolTestAP ();
      // Sender: your company participant ID
      final String sSenderID = "0242:" + APCoreConfig.getPeppolSeatID ().substring (3);
      if (StringHelper.isEmpty (sSenderID))
        throw new IllegalStateException ("No Peppol Reporting Sender ID is configured");

      // Receiver: production OpenPeppol; test Helger
      // OpenPeppol doesn't offer this participant ID on test :-/
      final String sReceiverID = eStage.isProduction () ? CPeppolReporting.OPENPEPPOL_PARTICIPANT_ID : "9915:helger";

      final String sCountryC1 = APCoreConfig.getPeppolOwnerCountryCode ();
      if (!PeppolReportingHelper.isValidCountryCode (sCountryC1))
        throw new IllegalStateException ("Invalid country code of Peppol owner is defined: '" + sCountryC1 + "'");

      // Returns the sending report
      // final Phase4PeppolSendingReport aSendingReport = PeppolSender.sendPeppolMessageCreatingSbdh
      // (aSMLInfo,
      // aAPCA,
      // sMessagePayload.getBytes (StandardCharsets.UTF_8),
      // sSenderID,
      // sReceiverID,
      // aDocTypeID.getURIEncoded (),
      // aProcessID.getURIEncoded (),
      // sCountryC1);
      // String ret = aSendingReport.getAsXMLString ();
      return "";
    };

    try (final PeppolReportSQLHandler aHdl = new PeppolReportSQLHandler (APConfigProvider.getConfig ()))
    {
      final PeppolReportStorageSQL aReportingStorage = new PeppolReportStorageSQL (aHdl, aHdl.getTableNamePrefix ());
      final PeppolReportingSupport aPRS = new PeppolReportingSupport (aReportingStorage);

      // Handle TSR
      try
      {
        // Create
        final TransactionStatisticsReportType aTSR = createTSR (aYearMonth);
        if (aTSR != null)
        {
          // Validate and store
          final Wrapper <String> aTSRString = new Wrapper <> ();
          if (aPRS.validateAndStorePeppolTSR10 (aTSR, aTSRString::set).isSuccess ())
          {
            // Send to OpenPeppol
            if (aPRS.sendPeppolReport (aYearMonth, EPeppolReportType.TSR_V10, aTSRString.get (), aPeppolSender)
                    .isSuccess ())
            {
              LOGGER.info ("Successfully sent TSR for " + aYearMonth + " to OpenPeppol");
              nSuccessCount++;
            }
            else
              LOGGER.error ("Failed to send TSR for " + aYearMonth + " to OpenPeppol");
          }
          else
            LOGGER.error ("Failed to validate and store TSR for " + aYearMonth);
        }
        else
          LOGGER.error ("Failed to create TSR for " + aYearMonth);
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Failed to create TSR for " + aYearMonth, ex);
      }

      // Handle EUSR
      try
      {
        // Create
        final EndUserStatisticsReportType aEUSR = createEUSR (aYearMonth);
        if (aEUSR != null)
        {
          // Validate and store
          final Wrapper <String> aEUSRString = new Wrapper <> ();
          if (aPRS.validateAndStorePeppolEUSR11 (aEUSR, aEUSRString::set).isSuccess ())
          {
            // Send to OpenPeppol
            if (aPRS.sendPeppolReport (aYearMonth, EPeppolReportType.EUSR_V11, aEUSRString.get (), aPeppolSender)
                    .isSuccess ())
            {
              LOGGER.info ("Successfully sent EUSR for " + aYearMonth + " to OpenPeppol");
              nSuccessCount++;
            }
            else
              LOGGER.error ("Failed to send EUSR for " + aYearMonth + " to OpenPeppol");
          }
          else
            LOGGER.error ("Failed to validate and store EUSR for " + aYearMonth);
        }
        else
          LOGGER.error ("Failed to create EUSR for " + aYearMonth);
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Failed to create EUSR for " + aYearMonth, ex);
      }
    }

    aSW.stop ();
    LOGGER.info ("Finished processing Peppol Reports after " + aSW.getDuration ());

    return ESuccess.valueOf (nSuccessCount == 2);
  }
}
