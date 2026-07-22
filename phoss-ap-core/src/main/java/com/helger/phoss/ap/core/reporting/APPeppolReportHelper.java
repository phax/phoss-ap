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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.IntConsumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.state.ESuccess;
import com.helger.base.timing.StopWatch;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.reporting.api.CPeppolReporting;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.eusr.EndUserStatisticsReport;
import com.helger.peppol.reporting.jaxb.eusr.EndUserStatisticsReport110Marshaller;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppol.reporting.jaxb.tsr.TransactionStatisticsReport101Marshaller;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;
import com.helger.peppol.reporting.tsr.TransactionStatisticsReport;
import com.helger.peppol.reportingsupport.EPeppolReportType;
import com.helger.peppol.reportingsupport.IPeppolReportSenderCallback;
import com.helger.peppol.reportingsupport.PeppolReportingSupport;
import com.helger.peppol.reportingsupport.sql.PeppolReportSQLHandler;
import com.helger.peppol.reportingsupport.sql.PeppolReportStorageSQL;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.spis.SPIDHelper;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.telemetry.Telemetry;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.outbound.OutboundOrchestrator;

/**
 * Helper class for Peppol Reporting report generation
 *
 * @author Philip Helger
 */
public final class APPeppolReportHelper
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (APPeppolReportHelper.class);

  /**
   * Validate and normalize the given year and month values for use in the Peppol Reporting API. The
   * year must be at least 2024, the month must be between 1 and 12, and the resulting date must not
   * be in the future.
   *
   * @param nYear
   *        The year value (clamped to a minimum of 2024).
   * @param nMonth
   *        The month value (clamped to 1..12).
   * @return A valid {@link YearMonth} instance. Never <code>null</code>.
   * @throws IllegalArgumentException
   *         if the resulting year/month is in the future.
   */
  @NonNull
  public static YearMonth getValidYearMonthInAPI (final int nYear, final int nMonth)
  {
    final int nRealYear = Math.max (nYear, 2024);
    final int nRealMonth = Math.min (Math.max (nMonth, 1), 12);

    final LocalDate aNow = APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ().toLocalDate ();
    if (nRealYear > aNow.getYear ())
      throw new IllegalArgumentException ("The year value " + nRealYear + " is in the future");
    if (nRealYear == aNow.getYear () && nRealMonth > aNow.getMonthValue ())
      throw new IllegalArgumentException ("The month value " + nRealMonth + " is in the future");

    return YearMonth.of (nRealYear, nRealMonth);
  }

  /**
   * Create a Peppol Transaction Statistics Report (TSR) for the given reporting period.
   *
   * @param aYearMonth
   *        The reporting period. May not be <code>null</code>.
   * @param aItemCountSink
   *        Callback invoked with the number of reporting items read from the backend before the
   *        report is built. Only invoked when the backend read succeeded. Never <code>null</code>.
   * @return The created TSR or <code>null</code> if the reporting items could not be retrieved.
   * @throws PeppolReportingBackendException
   *         if an error occurs accessing the reporting backend.
   */
  @Nullable
  public static TransactionStatisticsReportType createTSR (@NonNull final YearMonth aYearMonth,
                                                           @NonNull final IntConsumer aItemCountSink) throws PeppolReportingBackendException
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
      aItemCountSink.accept (aReportingItems.size ());
      // Create report with the read transactions
      return TransactionStatisticsReport.builder ()
                                        .monthOf (aYearMonth)
                                        .reportingServiceProviderID (APCoreConfig.getPeppolOwnerSeatID ())
                                        .reportingItemList (aReportingItems)
                                        .build ();
    }
    return null;
  }

  /**
   * Create a Peppol End User Statistics Report (EUSR) for the given reporting period.
   *
   * @param aYearMonth
   *        The reporting period. May not be <code>null</code>.
   * @param aItemCountSink
   *        Callback invoked with the number of reporting items read from the backend before the
   *        report is built. Only invoked when the backend read succeeded. Never <code>null</code>.
   * @return The created EUSR or <code>null</code> if the reporting items could not be retrieved.
   * @throws PeppolReportingBackendException
   *         if an error occurs accessing the reporting backend.
   */
  @Nullable
  public static EndUserStatisticsReportType createEUSR (@NonNull final YearMonth aYearMonth,
                                                        @NonNull final IntConsumer aItemCountSink) throws PeppolReportingBackendException
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
      aItemCountSink.accept (aReportingItems.size ());
      // Create report with the read transactions
      return EndUserStatisticsReport.builder ()
                                    .monthOf (aYearMonth)
                                    .reportingServiceProviderID (APCoreConfig.getPeppolOwnerSeatID ())
                                    .reportingItemList (aReportingItems)
                                    .build ();
    }
    return null;
  }

  /**
   * Create a Peppol Transaction Statistics Report (TSR) for the given reporting period and return
   * its XML serialization. Exceptions and backend read failures are handled internally: registered
   * notification handlers are invoked and <code>null</code> is returned. The operation is wrapped
   * in a {@link CPhossAPOtel#SPAN_REPORTING_TSR} trace span.
   *
   * @param aYearMonth
   *        The reporting period. May not be <code>null</code>.
   * @return The marshalled TSR XML string, or <code>null</code> if the report could not be created.
   */
  @Nullable
  public static String createTSRAsString (@NonNull final YearMonth aYearMonth)
  {
    ValueEnforcer.notNull (aYearMonth, "YearMonth");

    return Telemetry.withSpan (CPhossAPOtel.SPAN_REPORTING_TSR, ETelemetrySpanKind.PRODUCER, aSpan -> {
      aSpan.setAttribute (CPhossAPOtel.ATTR_REPORT_TYPE, "TSR")
           .setAttribute (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH, aYearMonth.toString ());
      try
      {
        final TransactionStatisticsReportType aReport = createTSR (aYearMonth,
                                                                   nCount -> aSpan.setAttribute (CPhossAPOtel.ATTR_REPORT_ITEM_COUNT,
                                                                                                 nCount));
        if (aReport == null)
        {
          LOGGER.error ("Failed to read Peppol Reporting backend data for TSR for " + aYearMonth);
          for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            aHandler.onPeppolReportingTSRFailure (aYearMonth);
          return null;
        }
        return new TransactionStatisticsReport101Marshaller ().getAsString (aReport);
      }
      catch (final PeppolReportingBackendException ex)
      {
        LOGGER.error ("Failed to read Peppol Reporting Items for TSR for " + aYearMonth, ex);
        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        {
          aHandler.onUnexpectedException ("APPeppolReportHelper.createTSRAsString",
                                          "Failed to read Peppol Reporting backend data for TSR for " + aYearMonth,
                                          ex);
          aHandler.onPeppolReportingTSRFailure (aYearMonth);
        }
        return null;
      }
    });
  }

  /**
   * Create a Peppol End User Statistics Report (EUSR) for the given reporting period and return its
   * XML serialization. Exceptions and backend read failures are handled internally: registered
   * notification handlers are invoked and <code>null</code> is returned. The operation is wrapped
   * in a {@link CPhossAPOtel#SPAN_REPORTING_EUSR} trace span.
   *
   * @param aYearMonth
   *        The reporting period. May not be <code>null</code>.
   * @return The marshalled EUSR XML string, or <code>null</code> if the report could not be
   *         created.
   */
  @Nullable
  public static String createEUSRAsString (@NonNull final YearMonth aYearMonth)
  {
    ValueEnforcer.notNull (aYearMonth, "YearMonth");

    return Telemetry.withSpan (CPhossAPOtel.SPAN_REPORTING_EUSR, ETelemetrySpanKind.PRODUCER, aSpan -> {
      aSpan.setAttribute (CPhossAPOtel.ATTR_REPORT_TYPE, "EUSR")
           .setAttribute (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH, aYearMonth.toString ());
      try
      {
        final EndUserStatisticsReportType aReport = createEUSR (aYearMonth,
                                                                nCount -> aSpan.setAttribute (CPhossAPOtel.ATTR_REPORT_ITEM_COUNT,
                                                                                              nCount));
        if (aReport == null)
        {
          LOGGER.error ("Failed to read Peppol Reporting backend data for EUSR for " + aYearMonth);
          for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            aHandler.onPeppolReportingEUSRFailure (aYearMonth);
          return null;
        }
        return new EndUserStatisticsReport110Marshaller ().getAsString (aReport);
      }
      catch (final PeppolReportingBackendException ex)
      {
        LOGGER.error ("Failed to read Peppol Reporting Items for EUSR for " + aYearMonth, ex);
        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        {
          aHandler.onUnexpectedException ("APPeppolReportHelper.createEUSRAsString",
                                          "Failed to read Peppol Reporting backend data for EUSR for " + aYearMonth,
                                          ex);
          aHandler.onPeppolReportingEUSRFailure (aYearMonth);
        }
        return null;
      }
    });
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

    // How to do AS4 sending
    final IPeppolReportSenderCallback aPeppolSender = (aDocTypeID, aProcessID, sMessagePayload) -> {
      final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();
      final EPeppolNetwork ePeppolStage = APCoreConfig.getPeppolStage ();

      // Sender: your company participant ID
      final IParticipantIdentifier aSenderID = aIF.createParticipantIdentifierWithDefaultScheme (SPIDHelper.SPIS_PARTICIPANT_ID_SCHEME +
                                                                                                 ":" +
                                                                                                 APCoreConfig.getPeppolOwnerSPID ());

      // Receiver: production OpenPeppol; test Helger
      // OpenPeppol doesn't offer this participant ID on test :-/
      final IParticipantIdentifier aReceiverID = aIF.createParticipantIdentifierWithDefaultScheme (ePeppolStage.isProduction () ? CPeppolReporting.OPENPEPPOL_PARTICIPANT_ID
                                                                                                                                : "9915:helger");

      final String sC1CountryCode = APCoreConfig.getPeppolOwnerCountryCode ();
      // Validity of country code was checked on startup

      // Store in DB
      final String sMlsTo = null;
      final String sSbdhStandard = null;
      final String sSbdhTypeVersion = null;
      final String sSbdhType = null;
      final String sPayloadMimeType = null;
      // Custom fields do not apply to system-generated Peppol Reporting documents
      final String sCustom1 = null;
      final String sCustom2 = null;
      final String sCustom3 = null;
      final IOutboundTransaction aTx = OutboundOrchestrator.submitRawDocument ("[PeppolReporting] ",
                                                                               aSenderID,
                                                                               aReceiverID,
                                                                               aDocTypeID,
                                                                               aProcessID,
                                                                               PeppolSBDHData.createRandomSBDHInstanceIdentifier (),
                                                                               sC1CountryCode,
                                                                               new NonBlockingByteArrayInputStream (sMessagePayload.getBytes (StandardCharsets.UTF_8)),
                                                                               sMlsTo,
                                                                               sSbdhStandard,
                                                                               sSbdhTypeVersion,
                                                                               sSbdhType,
                                                                               sPayloadMimeType,
                                                                               sCustom1,
                                                                               sCustom2,
                                                                               sCustom3);
      if (aTx == null)
        throw new IllegalStateException ("Failed to submit Peppol Reporting document for transmission");

      // Perform actual sending
      final Phase4PeppolSendingReport aSendingReport = OutboundOrchestrator.processPendingOutbound ("[PeppolReporting] ",
                                                                                                    aTx);
      return aSendingReport.getAsJsonString ();
    };

    boolean bTSRSuccess = false;
    boolean bEUSRSuccess = false;
    try (final PeppolReportSQLHandler aHdl = new PeppolReportSQLHandler (APConfigProvider.getConfig ()))
    {
      final PeppolReportStorageSQL aReportingStorage = new PeppolReportStorageSQL (aHdl, aHdl.getTableNamePrefix ());
      final PeppolReportingSupport aPRS = new PeppolReportingSupport (aReportingStorage);

      // Handle TSR
      bTSRSuccess = Telemetry.withSpan (CPhossAPOtel.SPAN_REPORTING_TSR, ETelemetrySpanKind.PRODUCER, aSpan -> {
        aSpan.setAttribute (CPhossAPOtel.ATTR_REPORT_TYPE, "TSR")
             .setAttribute (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH, aYearMonth.toString ());
        try
        {
          final TransactionStatisticsReportType aTSR = createTSR (aYearMonth,
                                                                  nCount -> aSpan.setAttribute (CPhossAPOtel.ATTR_REPORT_ITEM_COUNT,
                                                                                                nCount));
          if (aTSR == null)
          {
            LOGGER.error ("Failed to create TSR for " + aYearMonth);
            return Boolean.FALSE;
          }
          final Wrapper <String> aTSRString = new Wrapper <> ();
          if (Telemetry.withSpan (CPhossAPOtel.SPAN_REPORTING_TSR_VALIDATE_STORE,
                                  ETelemetrySpanKind.INTERNAL,
                                  aChildSpan -> {
                                    aChildSpan.setAttribute (CPhossAPOtel.ATTR_REPORT_TYPE, "TSR")
                                              .setAttribute (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH,
                                                             aYearMonth.toString ());
                                    return aPRS.validateAndStorePeppolTSR10 (aTSR, aTSRString::set);
                                  }).isFailure ())
          {
            LOGGER.error ("Failed to validate and store TSR for " + aYearMonth);
            return Boolean.FALSE;
          }
          if (aPRS.sendPeppolReport (aYearMonth, EPeppolReportType.TSR_V10, aTSRString.get (), aPeppolSender)
                  .isFailure ())
          {
            LOGGER.error ("Failed to send TSR for " + aYearMonth + " to OpenPeppol");
            return Boolean.FALSE;
          }
          LOGGER.info ("Successfully sent TSR for " + aYearMonth + " to OpenPeppol");
          return Boolean.TRUE;
        }
        catch (final Exception ex)
        {
          LOGGER.error ("Failed to create TSR for " + aYearMonth, ex);
          for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            aHandler.onUnexpectedException ("APPeppolReportHelper.createAndSendPeppolReports",
                                            "Failed to create TSR for " + aYearMonth,
                                            ex);
          return Boolean.FALSE;
        }
      }).booleanValue ();

      if (bTSRSuccess)
      {
        for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
          aHandler.onPeppolReportingTSRSuccess (aYearMonth);
      }
      else
      {
        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onPeppolReportingTSRFailure (aYearMonth);
      }

      // Handle EUSR
      bEUSRSuccess = Telemetry.withSpan (CPhossAPOtel.SPAN_REPORTING_EUSR, ETelemetrySpanKind.PRODUCER, aSpan -> {
        aSpan.setAttribute (CPhossAPOtel.ATTR_REPORT_TYPE, "EUSR")
             .setAttribute (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH, aYearMonth.toString ());
        try
        {
          final EndUserStatisticsReportType aEUSR = createEUSR (aYearMonth,
                                                                nCount -> aSpan.setAttribute (CPhossAPOtel.ATTR_REPORT_ITEM_COUNT,
                                                                                              nCount));
          if (aEUSR == null)
          {
            LOGGER.error ("Failed to create EUSR for " + aYearMonth);
            return Boolean.FALSE;
          }
          final Wrapper <String> aEUSRString = new Wrapper <> ();
          if (Telemetry.withSpan (CPhossAPOtel.SPAN_REPORTING_EUSR_VALIDATE_STORE,
                                  ETelemetrySpanKind.INTERNAL,
                                  aChildSpan -> {
                                    aChildSpan.setAttribute (CPhossAPOtel.ATTR_REPORT_TYPE, "EUSR")
                                              .setAttribute (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH,
                                                             aYearMonth.toString ());
                                    return aPRS.validateAndStorePeppolEUSR11 (aEUSR, aEUSRString::set);
                                  }).isFailure ())
          {
            LOGGER.error ("Failed to validate and store EUSR for " + aYearMonth);
            return Boolean.FALSE;
          }
          if (aPRS.sendPeppolReport (aYearMonth, EPeppolReportType.EUSR_V11, aEUSRString.get (), aPeppolSender)
                  .isFailure ())
          {
            LOGGER.error ("Failed to send EUSR for " + aYearMonth + " to OpenPeppol");
            return Boolean.FALSE;
          }
          LOGGER.info ("Successfully sent EUSR for " + aYearMonth + " to OpenPeppol");
          return Boolean.TRUE;
        }
        catch (final Exception ex)
        {
          LOGGER.error ("Failed to create EUSR for " + aYearMonth, ex);
          for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            aHandler.onUnexpectedException ("APPeppolReportHelper.createAndSendPeppolReports",
                                            "Failed to create EUSR for " + aYearMonth,
                                            ex);
          return Boolean.FALSE;
        }
      }).booleanValue ();

      if (bEUSRSuccess)
      {
        for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
          aHandler.onPeppolReportingEUSRSuccess (aYearMonth);
      }
      else
      {
        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onPeppolReportingEUSRFailure (aYearMonth);
      }
    }

    aSW.stop ();
    LOGGER.info ("Finished processing Peppol Reports after " + aSW.getDuration ());

    return ESuccess.valueOf (bTSRSuccess && bEUSRSuccess);
  }
}
