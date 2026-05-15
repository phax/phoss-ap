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
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.base.exception.InitializationException;
import com.helger.base.timing.StopWatch;
import com.helger.phoss.ap.api.IArchivalManager;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.api.trace.APTrace;
import com.helger.phoss.ap.api.trace.EAPSpanKind;
import com.helger.phoss.ap.api.trace.IAPSpan;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.db.APJdbcMetaManager;

/**
 * Background scheduler that periodically deletes archived inbound and outbound transactions whose
 * {@code completed_dt} is older than the configured retention. Both the database row (and its
 * attempt rows) and the document payload file are removed. Cleanup operates exclusively on the
 * archive tables — primary tables are never touched. Cleanup requires the archival scheduler to be
 * enabled; this is enforced at startup. Document file deletion is attempted before the row is
 * deleted: a transient storage error leaves the row for the next cycle so that orphan files cannot
 * accumulate.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
public final class CleanupScheduler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CleanupScheduler.class);

  private static Timer s_aTimer;

  private CleanupScheduler ()
  {}

  private static Predicate <String> _docDeleter (final IDocumentPayloadManager aDocPayloadMgr, final String sLogPrefix)
  {
    return sDocumentPath -> {
      try
      {
        // deleteDocument returns false if the file is already missing, which is treated as success
        // (the row should still be cleaned up).
        aDocPayloadMgr.deleteDocument (sDocumentPath);
        return true;
      }
      catch (final Exception ex)
      {
        LOGGER.warn (sLogPrefix +
                     "Failed to delete document file '" +
                     sDocumentPath +
                     "', leaving archive row for next cycle: " +
                     ex.getMessage ());
        return false;
      }
    };
  }

  private static void _cleanupOutbound (@Nonnegative final int nBatchSize, final OffsetDateTime aCutoff)
  {
    final IArchivalManager aArchivalMgr = APJdbcMetaManager.getArchivalMgr ();
    final IDocumentPayloadManager aDocPayloadMgr = APBasicMetaManager.getDocPayloadMgr ();
    final StopWatch aSW = StopWatch.createdStarted ();
    int nDeleted = 0;

    try (final IAPSpan aSpan = APTrace.startSpan (CPhossAPOtel.SPAN_SCHEDULER_CYCLE, EAPSpanKind.INTERNAL)
                                      .setAttribute (CPhossAPOtel.ATTR_SCHEDULER_NAME, "cleanup")
                                      .setAttribute (CPhossAPOtel.ATTR_IS_OUTBOUND, true))
    {
      try
      {
        nDeleted = aArchivalMgr.cleanupOutbound (aCutoff,
                                                 nBatchSize,
                                                 _docDeleter (aDocPayloadMgr, "[CleanupOutbound] "));
        if (nDeleted > 0)
          LOGGER.info ("Cleaned up " + nDeleted + " archived outbound transactions");
        else
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Found no archived outbound transactions for cleanup");
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Error in outbound cleanup cycle", ex);

        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onUnexpectedException ("CleanupScheduler._cleanupOutbound", "Error in outbound cleanup cycle", ex);
      }
      finally
      {
        aSpan.setAttribute (CPhossAPOtel.ATTR_SCHEDULER_ITEMS, nDeleted);
      }
    }

    final Duration aCycleDuration = aSW.stopAndGetDuration ();
    for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
      aHandler.onCleanupSchedulerCycle (true, nDeleted, aCycleDuration);
  }

  private static void _cleanupInbound (@Nonnegative final int nBatchSize, final OffsetDateTime aCutoff)
  {
    final IArchivalManager aArchivalMgr = APJdbcMetaManager.getArchivalMgr ();
    final IDocumentPayloadManager aDocPayloadMgr = APBasicMetaManager.getDocPayloadMgr ();
    final StopWatch aSW = StopWatch.createdStarted ();
    int nDeleted = 0;

    try (final IAPSpan aSpan = APTrace.startSpan (CPhossAPOtel.SPAN_SCHEDULER_CYCLE, EAPSpanKind.INTERNAL)
                                      .setAttribute (CPhossAPOtel.ATTR_SCHEDULER_NAME, "cleanup")
                                      .setAttribute (CPhossAPOtel.ATTR_IS_OUTBOUND, false))
    {
      try
      {
        nDeleted = aArchivalMgr.cleanupInbound (aCutoff, nBatchSize, _docDeleter (aDocPayloadMgr, "[CleanupInbound] "));
        if (nDeleted > 0)
          LOGGER.info ("Cleaned up " + nDeleted + " archived inbound transactions");
        else
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Found no archived inbound transactions for cleanup");
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Error in inbound cleanup cycle", ex);

        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onUnexpectedException ("CleanupScheduler._cleanupInbound", "Error in inbound cleanup cycle", ex);
      }
      finally
      {
        aSpan.setAttribute (CPhossAPOtel.ATTR_SCHEDULER_ITEMS, nDeleted);
      }
    }

    final Duration aCycleDuration = aSW.stopAndGetDuration ();
    for (final var aHandler : APCoreMetaManager.getAllLifecycleHandlers ())
      aHandler.onCleanupSchedulerCycle (false, nDeleted, aCycleDuration);
  }

  /**
   * Start the cleanup scheduler. If the cleanup scheduler is disabled via configuration, this
   * method does nothing. Validates configuration preconditions and throws
   * {@link InitializationException} on misconfiguration.
   */
  public static void start ()
  {
    if (!APCoreConfig.isCleanupSchedulerEnabled ())
    {
      LOGGER.info ("phoss AP cleanup scheduler is disabled");
      return;
    }

    if (!APCoreConfig.isArchivalSchedulerEnabled ())
      throw new InitializationException ("Cleanup scheduler is enabled but the archival scheduler is disabled. " +
                                         "Cleanup only operates on already-archived transactions, so archival must be enabled.");

    final Duration aRetention = APCoreConfig.getCleanupSchedulerRetention ();
    if (aRetention.compareTo (APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION_MIN) < 0)
      throw new InitializationException ("Cleanup retention '" +
                                         aRetention +
                                         "' is below the minimum of " +
                                         APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION_MIN +
                                         " - reconfigure '" +
                                         APConfigurationProperties.CLEANUP_SCHEDULER_RETENTION +
                                         "' to a value of at least 2 days.");

    final Duration aArchivalSafetyWindow = APCoreConfig.getArchivalSchedulerInterval ().multipliedBy (2);
    if (aRetention.compareTo (aArchivalSafetyWindow) < 0)
      throw new InitializationException ("Cleanup retention '" +
                                         aRetention +
                                         "' must be greater than 2x the archival interval (" +
                                         aArchivalSafetyWindow +
                                         ") so the archiver has time to land completed transactions before cleanup considers them.");

    final int nBatchSize = APCoreConfig.getCleanupSchedulerBatchSize ();
    if (nBatchSize < 1)
      throw new InitializationException ("The cleanup scheduler batch size must be >= 1, but is " + nBatchSize);

    final Duration aInterval = APCoreConfig.getCleanupSchedulerInterval ();
    final long nIntervalMs = aInterval.toMillis ();
    if (nIntervalMs < 1)
      throw new InitializationException ("The cleanup scheduler interval must be >= 1 ms, but is " + aInterval);

    LOGGER.info ("Starting phoss AP cleanup scheduler with interval " +
                 aInterval +
                 ", retention " +
                 aRetention +
                 " and batch size " +
                 nBatchSize);

    s_aTimer = new Timer ("phoss-ap-cleanup-scheduler", true);
    s_aTimer.scheduleAtFixedRate (new TimerTask ()
    {
      @Override
      public void run ()
      {
        final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
        final OffsetDateTime aCutoff = aTimestampMgr.getCurrentDateTimeUTC ().minus (aRetention);
        _cleanupOutbound (nBatchSize, aCutoff);
        _cleanupInbound (nBatchSize, aCutoff);
      }
    }, nIntervalMs, nIntervalMs);
  }

  /**
   * Stop the cleanup scheduler. If it was not started, this method does nothing.
   */
  public static void stop ()
  {
    if (s_aTimer != null)
    {
      s_aTimer.cancel ();
      s_aTimer = null;
      LOGGER.info ("phoss AP cleanup scheduler stopped");
    }
  }
}
