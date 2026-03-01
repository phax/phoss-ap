package com.helger.phoss.ap.core.inbound;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.state.ESuccess;
import com.helger.phoss.ap.api.IInboundForwardingAttemptManager;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.spi.IDocumentForwarder;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.CircuitBreakerManager;
import com.helger.phoss.ap.core.helper.BackoffCalculator;
import com.helger.phoss.ap.core.reporting.ReportingManager;
import com.helger.phoss.ap.db.APJdbcMetaManager;

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

  @NonNull
  public static ESuccess forwardDocument (@Nullable final IInboundTransaction aTx)
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundForwardingAttemptManager aAttemptMgr = APJdbcMetaManager.getInboundForwardingAttemptMgr ();

    final String sCircuitBreakerID = "phoss-ap-forwarder";
    if (CircuitBreakerManager.tryAcquirePermit (sCircuitBreakerID))
    {
      final IDocumentForwarder aForwarder = APCoreMetaManager.getForwarder ();
      if (aForwarder == null)
      {
        LOGGER.error ("Internal error - No document forwarder configured");
        aTxMgr.updateStatus (aTx.getID (), EInboundStatus.PERMANENTLY_FAILED);
        return ESuccess.FAILURE;
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
        CircuitBreakerManager.recordSuccess (sCircuitBreakerID);
        aAttemptMgr.createSuccess (aTx.getID ());

        aTxMgr.updateStatusCompleted (aTx.getID (), EInboundStatus.FORWARDED);
        LOGGER.info ("Forwarding successful for transaction '" + aTx.getID () + "'");

        if (aResult.hasCountryCodeC4 ())
        {
          // We can store the reporting item immediately
          aTxMgr.updateC4CountryCode (aTx.getID (), aResult.getCountryCodeC4 ());
          if (ReportingManager.storeInboundForReporting (aTx.getID ()).isFailure ())
            LOGGER.error ("Forwarding successful, but failed to store Peppol Reporting entry for '" +
                          aTx.getID () +
                          "'");
        }

        return ESuccess.SUCCESS;
      }

      // Forwarding failed
      CircuitBreakerManager.recordFailure (sCircuitBreakerID);
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

        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
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

    return ESuccess.FAILURE;
  }
}
