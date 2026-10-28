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
package com.helger.phoss.ap.core.job;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.base.timing.StopWatch;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EMlsSendingTrigger;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.telemetry.Telemetry;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.ITelemetrySpan;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.inbound.InboundOrchestrator;
import com.helger.phoss.ap.core.mls.MlsHandler;
import com.helger.phoss.ap.core.outbound.OutboundOrchestrator;
import com.helger.phoss.ap.db.APJdbcMetaManager;

/**
 * This class makes sure the inbound and outbound transactions are automatically retried.
 *
 * @author Philip Helger
 */
public final class RetryScheduler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (RetryScheduler.class);

  private static Timer s_aTimer;

  private RetryScheduler ()
  {}

  // Runs before the retries, so that the rows of a stopped instance get due after the grace period
  private static void _markInFlightAsRecovering ()
  {
    try
    {
      final OffsetDateTime aNow = APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ();
      final OffsetDateTime aRecoveryDueDT = aNow.plus (APCoreConfig.getRecoveryGracePeriodInFlight ());
      final long nOutbound = APJdbcMetaManager.getOutboundTransactionMgr ()
                                              .markAllInFlightAsRecovering (aRecoveryDueDT);
      final long nInbound = APJdbcMetaManager.getInboundTransactionMgr ().markAllInFlightAsRecovering (aRecoveryDueDT);
      if (nOutbound > 0 || nInbound > 0)
        LOGGER.info ("Marked " +
                     nOutbound +
                     " outbound and " +
                     nInbound +
                     " inbound in-flight transactions as recovering - they are retried at " +
                     aRecoveryDueDT +
                     " unless they are finished before");

      final OffsetDateTime aReceivedDueDT = aNow.plus (APCoreConfig.getRecoveryGracePeriodReceived ());
      final long nReceived = APJdbcMetaManager.getInboundTransactionMgr ().markAllReceivedAsRecovering (aReceivedDueDT);
      if (nReceived > 0)
        LOGGER.info ("Marked " +
                     nReceived +
                     " received inbound transactions as receive recovering - their processing is resumed at " +
                     aReceivedDueDT +
                     " unless they are finished before");
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error marking in-flight transactions as recovering", ex);

      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onUnexpectedException ("RetryScheduler._markInFlightAsRecovering",
                                        "Internal error marking in-flight transactions as recovering",
                                        ex);
    }
  }

  private static void _retryOutbound (@Nonnegative final int nBatchSize)
  {
    final var aOutboundMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final StopWatch aSW = StopWatch.createdStarted ();
    int nProcessed = 0;

    try (final ITelemetrySpan aSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_SCHEDULER_CYCLE,
                                                           ETelemetrySpanKind.INTERNAL)
                                               .setAttribute (CPhossAPOtel.ATTR_SCHEDULER_NAME, "retry")
                                               .setAttribute (CPhossAPOtel.ATTR_IS_OUTBOUND, true))
    {
      try
      {
        final ICommonsList <IOutboundTransaction> aTransactions = aOutboundMgr.getAllForRetry (nBatchSize);

        if (aTransactions.isNotEmpty ())
        {
          LOGGER.info ("Retrying " + aTransactions.size () + " outbound transactions");
          final String sLogPrefix = "[RetryOutbound] ";

          for (final IOutboundTransaction aTx : aTransactions)
          {
            // The row locks of the SELECT end with its commit, so another instance may hold the
            // same transaction - only the one that claims it may send it
            if (aOutboundMgr.claimForRetry (aTx.getID ()).isFailure ())
            {
              if (LOGGER.isDebugEnabled ())
                LOGGER.debug (sLogPrefix +
                              "Outbound transaction '" +
                              aTx.getID () +
                              "' is no longer eligible for retry (claimed by another instance) - skipping");
              continue;
            }

            nProcessed++;
            try
            {
              OutboundOrchestrator.processPendingOutbound (sLogPrefix, aTx);
            }
            catch (final Exception ex)
            {
              LOGGER.error ("Error retrying outbound transaction '" + aTx.getID () + "'", ex);

              for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
                aHandler.onUnexpectedException ("RetryScheduler._retryOutbound",
                                                "Error retrying outbound transaction '" + aTx.getID () + "'",
                                                ex);
            }
          }
        }
        else
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Found no outbound transactions for retry");
        }
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Internal error in outbound retry cycle", ex);

        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onUnexpectedException ("RetryScheduler._retryOutbound",
                                          "Internal error in outbound retry cycle",
                                          ex);
      }
      finally
      {
        aSpan.setAttribute (CPhossAPOtel.ATTR_SCHEDULER_ITEMS, nProcessed);
      }
    }

    final Duration aCycleDuration = aSW.stopAndGetDuration ();
    for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
      aHandler.onRetrySchedulerCycle (true, nProcessed, aCycleDuration);
  }

  // Resumes the processing of a document whose receiving instance stopped before it was forwarded
  private static void _resumeReceiveRecovering (@Nonnegative final int nBatchSize)
  {
    final var aInboundMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    int nProcessed = 0;

    // Note: deliberately no "onRetrySchedulerCycle" callback - that SPI method only distinguishes
    // inbound from outbound, and the receive recovery is neither of the two existing cycles
    try (final ITelemetrySpan aSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_SCHEDULER_CYCLE,
                                                           ETelemetrySpanKind.INTERNAL)
                                               .setAttribute (CPhossAPOtel.ATTR_SCHEDULER_NAME, "receive-recovery")
                                               .setAttribute (CPhossAPOtel.ATTR_IS_OUTBOUND, false))
    {
      try
      {
        final ICommonsList <IInboundTransaction> aTransactions = aInboundMgr.getAllForReceiveRecovery (nBatchSize);

        if (aTransactions.isNotEmpty ())
        {
          LOGGER.info ("Resuming " + aTransactions.size () + " received inbound transactions");
          final String sLogPrefix = "[ReceiveRecovery] ";

          for (final IInboundTransaction aInboundTx : aTransactions)
          {
            if (aInboundMgr.claimForReceiveRecovery (aInboundTx.getID ()).isFailure ())
            {
              if (LOGGER.isDebugEnabled ())
                LOGGER.debug (sLogPrefix +
                              "Inbound transaction '" +
                              aInboundTx.getID () +
                              "' is no longer eligible for receive recovery (finished or claimed by another instance) - skipping");
              continue;
            }

            // Re-read, so that the status is "received" again
            final IInboundTransaction aClaimedTx = aInboundMgr.getByID (aInboundTx.getID ());
            if (aClaimedTx == null)
              continue;

            nProcessed++;
            LOGGER.warn (sLogPrefix +
                         "Resuming the processing of inbound transaction '" +
                         aClaimedTx.getID () +
                         "' with SBDH '" +
                         aClaimedTx.getSbdhInstanceID () +
                         "', that was left behind by an instance that stopped unexpectedly");
            if (InboundOrchestrator.resumeDeferredInboundDocument (sLogPrefix, aClaimedTx).isFailure ())
            {
              // A still unavailable verifier is not a forwarding error
              final IInboundTransaction aUpdatedTx = aInboundMgr.getByID (aClaimedTx.getID ());
              if (aUpdatedTx != null && aUpdatedTx.getStatus () != EInboundStatus.VERIFICATION_DEFERRED)
                for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
                  aHandler.onInboundForwardingError (aClaimedTx.getID (), true);
            }
          }
        }
        else
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Found no inbound transactions for receive recovery");
        }
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Internal error in inbound receive recovery cycle", ex);
        aSpan.recordException (ex).setStatusError (ex.getMessage ());

        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onUnexpectedException ("RetryScheduler._resumeReceiveRecovering",
                                          "Internal error in inbound receive recovery cycle",
                                          ex);
      }
      finally
      {
        aSpan.setAttribute (CPhossAPOtel.ATTR_SCHEDULER_ITEMS, nProcessed);
      }
    }
  }

  private static void _retryDeferredVerification (@Nonnegative final int nBatchSize)
  {
    final var aInboundMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    int nProcessed = 0;

    // Note: deliberately no "onRetrySchedulerCycle" callback - that SPI method only distinguishes
    // inbound from outbound, and the re-verification is neither of the two existing cycles
    try (final ITelemetrySpan aSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_SCHEDULER_CYCLE,
                                                           ETelemetrySpanKind.INTERNAL)
                                               .setAttribute (CPhossAPOtel.ATTR_SCHEDULER_NAME, "reverify")
                                               .setAttribute (CPhossAPOtel.ATTR_IS_OUTBOUND, false))
    {
      try
      {
        final ICommonsList <IInboundTransaction> aTransactions = aInboundMgr.getAllForVerificationRetry (nBatchSize);

        if (aTransactions.isNotEmpty ())
        {
          LOGGER.info ("Re-verifying " + aTransactions.size () + " inbound transactions");
          final String sLogPrefix = "[RetryVerification] ";
          final Duration aLease = APCoreConfig.getVerificationDeferredRetryInterval ();

          for (final IInboundTransaction aInboundTx : aTransactions)
          {
            final OffsetDateTime aLeaseEndDT = APBasicMetaManager.getTimestampMgr ()
                                                                 .getCurrentDateTimeUTC ()
                                                                 .plus (aLease);
            if (aInboundMgr.claimForVerificationRetry (aInboundTx.getID (), aLeaseEndDT).isFailure ())
            {
              if (LOGGER.isDebugEnabled ())
                LOGGER.debug (sLogPrefix +
                              "Inbound transaction '" +
                              aInboundTx.getID () +
                              "' is no longer eligible for re-verification (claimed by another instance) - skipping");
              continue;
            }

            nProcessed++;
            if (InboundOrchestrator.resumeDeferredInboundDocument (sLogPrefix, aInboundTx).isFailure ())
            {
              // A still unavailable verifier is not a forwarding error, so the notification
              // handlers are only called, if the document left the deferred state
              final IInboundTransaction aUpdatedTx = aInboundMgr.getByID (aInboundTx.getID ());
              if (aUpdatedTx != null && aUpdatedTx.getStatus () != EInboundStatus.VERIFICATION_DEFERRED)
                for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
                  aHandler.onInboundForwardingError (aInboundTx.getID (), true);
            }
          }
        }
        else
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Found no inbound transactions for re-verification");
        }
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Internal error in inbound re-verification cycle", ex);
        aSpan.recordException (ex).setStatusError (ex.getMessage ());

        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onUnexpectedException ("RetryScheduler._retryDeferredVerification",
                                          "Internal error in inbound re-verification cycle",
                                          ex);
      }
      finally
      {
        aSpan.setAttribute (CPhossAPOtel.ATTR_SCHEDULER_ITEMS, nProcessed);
      }
    }
  }

  private static void _retryInbound (@Nonnegative final int nBatchSize)
  {
    final var aInboundMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final StopWatch aSW = StopWatch.createdStarted ();
    int nProcessed = 0;

    try (final ITelemetrySpan aSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_SCHEDULER_CYCLE,
                                                           ETelemetrySpanKind.INTERNAL)
                                               .setAttribute (CPhossAPOtel.ATTR_SCHEDULER_NAME, "retry")
                                               .setAttribute (CPhossAPOtel.ATTR_IS_OUTBOUND, false))
    {
      try
      {
        final ICommonsList <IInboundTransaction> aTransactions = aInboundMgr.getAllForRetry (nBatchSize);

        if (aTransactions.isNotEmpty ())
        {
          LOGGER.info ("Retrying " + aTransactions.size () + " inbound forwarding transactions");
          final String sLogPrefix = "[RetryInbound] ";

          for (final IInboundTransaction aInboundTx : aTransactions)
          {
            if (aInboundMgr.claimForRetry (aInboundTx.getID ()).isFailure ())
            {
              if (LOGGER.isDebugEnabled ())
                LOGGER.debug (sLogPrefix +
                              "Inbound transaction '" +
                              aInboundTx.getID () +
                              "' is no longer eligible for retry (claimed by another instance) - skipping");
              continue;
            }

            nProcessed++;
            // Re-forward using the original InboundOrchestrator logic
            if (InboundOrchestrator.forwardDocumentAndSendMls (sLogPrefix, aInboundTx).isFailure ())
            {
              for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
                aHandler.onInboundForwardingError (aInboundTx.getID (), true);
            }
          }
        }
        else
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Found no inbound transactions for retry");
        }
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Internal error in inbound retry cycle", ex);

        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onUnexpectedException ("RetryScheduler._retryInbound", "Internal error in inbound retry cycle", ex);
      }
      finally
      {
        aSpan.setAttribute (CPhossAPOtel.ATTR_SCHEDULER_ITEMS, nProcessed);
      }
    }

    final Duration aCycleDuration = aSW.stopAndGetDuration ();
    for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
      aHandler.onRetrySchedulerCycle (false, nProcessed, aCycleDuration);
  }

  /**
   * The MLS watchdog of the trigger mode {@link EMlsSendingTrigger#API}: answer C2 on behalf of a
   * Receiver Backend that did not report the outcome of a successfully forwarded document within
   * {@code mls.sending.api.timeout}. Without it a silent backend would push this AP out of the
   * MLS-1 compliance of the Peppol Network Policy - 99.5 % of all MLS responses within 20 minutes.
   * <p>
   * The timeout is measured from the reception of the document, because that is where the MLS-1
   * clock starts. A forwarding that itself took a long time therefore shortens the window of the
   * backend instead of extending the SLA budget - in the extreme case the fallback goes out with
   * the first cycle after the forwarding, which is the correct answer when there is no budget left.
   * </p>
   *
   * @param nBatchSize
   *        The maximum number of transactions to answer per cycle. Must be &gt; 0.
   * @since 0.13.0
   */
  private static void _sendMlsAfterApiTimeout (@Nonnegative final int nBatchSize)
  {
    // Only the trigger mode "api" defers an MLS at all
    if (APCoreConfig.getMlsSendingTrigger () != EMlsSendingTrigger.API)
      return;

    // Nothing to answer if MLS sending is globally disabled
    if (!APCoreConfig.isMlsSendingEnabled ())
      return;

    final var aInboundMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    int nProcessed = 0;

    // Note: deliberately no "onRetrySchedulerCycle" callback - that SPI method only distinguishes
    // inbound from outbound, and the MLS fallback is neither of the two existing cycles
    try (final ITelemetrySpan aSpan = Telemetry.startSpan (CPhossAPOtel.SPAN_SCHEDULER_CYCLE,
                                                           ETelemetrySpanKind.INTERNAL)
                                               .setAttribute (CPhossAPOtel.ATTR_SCHEDULER_NAME, "mls-fallback")
                                               .setAttribute (CPhossAPOtel.ATTR_IS_OUTBOUND, false))
    {
      try
      {
        final Duration aTimeout = APCoreConfig.getMlsSendingApiTimeout ();
        final OffsetDateTime aMaxAS4Timestamp = APBasicMetaManager.getTimestampMgr ()
                                                                  .getCurrentDateTimeUTC ()
                                                                  .minus (aTimeout);
        final ICommonsList <IInboundTransaction> aTransactions = aInboundMgr.getAllForMlsApiTimeout (nBatchSize,
                                                                                                     aMaxAS4Timestamp);

        if (aTransactions.isNotEmpty ())
        {
          final EPeppolMLSResponseCode eResponseCode = APCoreConfig.getMlsSendingApiTimeoutResponseCode ();
          LOGGER.info ("Sending the fallback MLS (" +
                       eResponseCode.getID () +
                       ") for " +
                       aTransactions.size () +
                       " inbound transactions, because no MLS status was reported within " +
                       aTimeout);

          final String sResponseText = "No MLS status was reported by the Receiver Backend within " + aTimeout;
          final MlsOutcome aOutcome = eResponseCode == EPeppolMLSResponseCode.ACCEPTANCE ? MlsOutcome.acceptance ()
                                                                                         : MlsOutcome.acknowledging (sResponseText);

          for (final IInboundTransaction aInboundTx : aTransactions)
          {
            // The DB query already excludes them, but a custom manager implementation might not
            if (InboundOrchestrator.isMlsSuppressedAfterRejection (aInboundTx))
              continue;

            // A FAILURE_ONLY transaction never gets a positive MLS, so recording a fallback
            // response code for it would only block the rejection the backend may still report
            if (aInboundTx.getMlsType () == EPeppolMLSType.FAILURE_ONLY)
              continue;

            nProcessed++;
            try
            {
              MlsHandler.triggerSendingInboundResultMls (aInboundTx, aOutcome);
            }
            catch (final Exception ex)
            {
              LOGGER.error ("Error sending the fallback MLS for inbound transaction '" + aInboundTx.getID () + "'", ex);

              for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
                aHandler.onUnexpectedException ("RetryScheduler._sendMlsAfterApiTimeout",
                                                "Error sending the fallback MLS for inbound transaction '" +
                                                                                          aInboundTx.getID () +
                                                                                          "'",
                                                ex);
            }
          }
        }
        else
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Found no inbound transactions waiting for an API triggered MLS");
        }
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Internal error in MLS fallback cycle", ex);
        aSpan.recordException (ex).setStatusError (ex.getMessage ());

        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onUnexpectedException ("RetryScheduler._sendMlsAfterApiTimeout",
                                          "Internal error in MLS fallback cycle",
                                          ex);
      }
      finally
      {
        aSpan.setAttribute (CPhossAPOtel.ATTR_SCHEDULER_ITEMS, nProcessed);
      }
    }
  }

  /**
   * Start the retry scheduler. It periodically checks for outbound and inbound transactions that
   * are eligible for retry and processes them. If the retry scheduler is disabled via
   * configuration, this method does nothing.
   */
  public static void start ()
  {
    if (!APCoreConfig.isRetrySchedulerEnabled ())
    {
      LOGGER.info ("phoss AP retry scheduler is disabled");
      return;
    }

    // The configuration was verified by StartupCheckRetryScheduler
    final int nBatchSize = APCoreConfig.getRetrySchedulerBatchSize ();

    final Duration aInterval = APCoreConfig.getRetrySchedulerInterval ();
    final long nIntervalMs = aInterval.toMillis ();
    LOGGER.info ("Starting phoss AP retry scheduler with interval " + aInterval + " and batch size " + nBatchSize);

    s_aTimer = new Timer ("phoss-ap-retry-scheduler", true);
    s_aTimer.scheduleAtFixedRate (new TimerTask ()
    {
      @Override
      public void run ()
      {
        _markInFlightAsRecovering ();
        _retryOutbound (nBatchSize);
        _retryInbound (nBatchSize);
        _resumeReceiveRecovering (nBatchSize);
        _retryDeferredVerification (nBatchSize);
        _sendMlsAfterApiTimeout (nBatchSize);
      }
    }, nIntervalMs, nIntervalMs);
  }

  /**
   * Stop the retry scheduler. If it was not started, this method does nothing.
   */
  public static void stop ()
  {
    if (s_aTimer != null)
    {
      s_aTimer.cancel ();
      s_aTimer = null;
      LOGGER.info ("phoss AP retry scheduler stopped");
    }
  }
}
