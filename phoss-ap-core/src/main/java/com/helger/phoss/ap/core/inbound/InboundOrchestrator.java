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

import java.time.Duration;
import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.state.ESuccess;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.IInboundForwardingAttemptManager;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.model.MlsOutcomeIssue;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.api.trace.APTrace;
import com.helger.phoss.ap.api.trace.EAPSpanKind;
import com.helger.phoss.ap.api.trace.IAPSpan;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.CircuitBreakerManager;
import com.helger.phoss.ap.core.helper.BackoffCalculator;
import com.helger.phoss.ap.core.mls.MlsHandler;
import com.helger.phoss.ap.core.reporting.APPeppolReportingHelper;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.photon.io.PhotonWorkerPool;

/**
 * Internal orchestrator to handle messages received via the Peppol Network
 *
 * @author Philip Helger
 */
@Immutable
public final class InboundOrchestrator
{
  private static final Logger LOGGER = LoggerFactory.getLogger (InboundOrchestrator.class);

  private InboundOrchestrator ()
  {}

  /**
   * Forward a received inbound document to the configured C4 endpoint. Handles retry scheduling
   * with exponential backoff and triggers MLS rejection responses when maximum retries are
   * exhausted.
   *
   * @param sLogPrefix
   *        Log message prefix for traceability. May not be <code>null</code>.
   * @param aInboundTx
   *        The inbound transaction to forward. May not be <code>null</code>.
   * @return {@link ESuccess#SUCCESS} if forwarding succeeded, {@link ESuccess#FAILURE} otherwise.
   */
  @NonNull
  public static ESuccess forwardDocument (@NonNull final String sLogPrefix,
                                          @NonNull final IInboundTransaction aInboundTx)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundForwardingAttemptManager aAttemptMgr = APJdbcMetaManager.getInboundForwardingAttemptMgr ();
    final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();

    boolean bForwardSuccess = false;
    try (final IAPSpan aSpan = APTrace.startSpan (CPhossAPOtel.SPAN_INBOUND_FORWARD, EAPSpanKind.PRODUCER)
                                      .setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID, aInboundTx.getID ())
                                      .setAttribute (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID,
                                                     aInboundTx.getSbdhInstanceID ())
                                      .setAttribute (CPhossAPOtel.ATTR_IS_RETRY, aInboundTx.getAttemptCount () > 0))
    {
      try
      {
        final String sCircuitBreakerID = "phoss-ap-forwarder";
        if (CircuitBreakerManager.tryAcquirePermit (sCircuitBreakerID))
        {
          final IDocumentForwarder aForwarder = APCoreMetaManager.getForwarder ();
          if (aForwarder == null)
          {
            LOGGER.error (sLogPrefix + "Internal error - No document forwarder configured");
            aTxMgr.updateStatus (aInboundTx.getID (), EInboundStatus.PERMANENTLY_FAILED);
            return ESuccess.FAILURE;
          }

          // Set status
          aTxMgr.updateStatus (aInboundTx.getID (), EInboundStatus.FORWARDING);

          // Actual forwarding
          ForwardingResult aResult;
          try
          {
            aResult = aForwarder.forwardDocument (aInboundTx);
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

            for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            {
              aHandler.onUnexpectedException ("InboundOrchestrator.forwardDocument",
                                              "Internal error forwarding document for transaction '" +
                                                                                     aInboundTx.getID () +
                                                                                     "'",
                                              ex);
            }
          }

          if (aResult.isSuccess ())
          {
            // Forwarding worked
            CircuitBreakerManager.recordSuccess (sCircuitBreakerID);
            aAttemptMgr.createSuccess (aInboundTx.getID ());

            aTxMgr.updateStatusCompleted (aInboundTx.getID (), EInboundStatus.FORWARDED);
            LOGGER.info (sLogPrefix + "Forwarding successful for transaction '" + aInboundTx.getID () + "'");

            final OffsetDateTime aReceivedDT = aInboundTx.getAS4Timestamp ();
            final Duration aForwardingDuration = aReceivedDT != null ? Duration.between (aReceivedDT,
                                                                                         aTimestampMgr.getCurrentDateTimeUTC ())
                                                                     : null;
            final boolean bIsRetry = aInboundTx.getAttemptCount () > 0;
            for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
              aHandler.onInboundDocumentForwarded (aInboundTx.getID (),
                                                   aInboundTx.getSbdhInstanceID (),
                                                   aForwardingDuration,
                                                   bIsRetry);

            bForwardSuccess = true;

            // Determine C4 country code: either from sync response or via configured resolution
            // modes
            String sC4CountryCode = aResult.getCountryCodeC4 ();
            if (sC4CountryCode == null)
              sC4CountryCode = C4CountryCodeResolver.resolve (aInboundTx);

            if (sC4CountryCode != null)
            {
              // We can store the reporting item immediately
              aTxMgr.updateC4CountryCode (aInboundTx.getID (), sC4CountryCode);
              if (APPeppolReportingHelper.createInboundPeppolReportingItem (aInboundTx.getID ()).isFailure ())
              {
                LOGGER.error (sLogPrefix +
                              "Forwarding successful, but failed to store Peppol Reporting entry for '" +
                              aInboundTx.getID () +
                              "'");
              }
            }

            return ESuccess.SUCCESS;
          }

          // Forwarding failed
          CircuitBreakerManager.recordFailure (sCircuitBreakerID);
          aAttemptMgr.createFailure (aInboundTx.getID (), aResult.getErrorCode (), aResult.getErrorDetails ());

          final int nNewAttemptCount = aInboundTx.getAttemptCount () + 1;
          final int nMaxRetryAttempts = APCoreConfig.getRetryForwardingMaxAttempts ();
          if (!aResult.isRetryAllowed () || nNewAttemptCount >= nMaxRetryAttempts)
          {
            // Maximum number of retries are exhausted - we go on "permanently
            // failed"
            final String sFailureReason = aResult.isRetryAllowed () ? "Max retries (" +
                                                                      nMaxRetryAttempts +
                                                                      ") exhausted: " +
                                                                      aResult.getErrorDetails ()
                                                                    : "Retry disallowed by receiver: " +
                                                                      aResult.getErrorDetails ();
            aTxMgr.updateStatusAndRetry (aInboundTx.getID (),
                                         EInboundStatus.PERMANENTLY_FAILED,
                                         nNewAttemptCount,
                                         null,
                                         sFailureReason);

            // Don't send MLS as response to MLS
            if (!CPhossAP.isMLS (aInboundTx.getDocTypeID (), aInboundTx.getProcessID ()))
            {
              // Send asynchronously
              PhotonWorkerPool.getInstance ().run ("send-mls", () -> {
                // Send negative MLS (RE) with FD reason back to C2
                final MlsOutcome aOutcome = MlsOutcome.rejection ("Forwarding to C4 failed",
                                                                  MlsOutcomeIssue.failureOfDelivery ("Permanent inability to forward document to C4"));
                MlsHandler.triggerSendingInboundResultMls (aInboundTx, aOutcome);
              });
            }

            for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
            {
              aHandler.onInboundPermanentForwardingFailure (aInboundTx.getID (),
                                                            aInboundTx.getSbdhInstanceID (),
                                                            aResult.isRetryAllowed () ? "Max retries exhausted"
                                                                                      : "Retry disallowed by receiver");
            }
          }
          else
          {
            // Calculate the next retry and remember it
            final var aNextRetry = BackoffCalculator.calculateNextRetry (nNewAttemptCount,
                                                                         APCoreConfig.getRetryForwardingInitialBackoff (),
                                                                         APCoreConfig.getRetryForwardingBackoffMultiplier (),
                                                                         APCoreConfig.getRetryForwardingMaxBackoff ());
            aTxMgr.updateStatusAndRetry (aInboundTx.getID (),
                                         EInboundStatus.FORWARD_FAILED,
                                         nNewAttemptCount,
                                         aNextRetry,
                                         aResult.getErrorDetails ());
          }
        }
      }
      catch (final RuntimeException ex)
      {
        aSpan.recordException (ex);
        throw ex;
      }
      finally
      {
        if (bForwardSuccess)
          aSpan.setStatusOk ();
        else
          aSpan.setStatusError (null);
      }
    }

    return bForwardSuccess ? ESuccess.SUCCESS : ESuccess.FAILURE;
  }
}
