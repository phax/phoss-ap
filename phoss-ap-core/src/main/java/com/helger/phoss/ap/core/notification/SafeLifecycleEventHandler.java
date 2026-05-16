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
package com.helger.phoss.ap.core.notification;

import java.time.Duration;
import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.spi.IAPLifecycleEventSPI;

/**
 * Wrapper around an {@link IAPLifecycleEventSPI} implementation that swallows and logs any
 * exception thrown by the wrapped handler. This prevents a misbehaving lifecycle handler from
 * interrupting the AP's core processing path.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
public final class SafeLifecycleEventHandler implements IAPLifecycleEventSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SafeLifecycleEventHandler.class);

  private final IAPLifecycleEventSPI m_aHdl;

  /**
   * Constructor wrapping an existing lifecycle handler with exception safety.
   *
   * @param aHdl
   *        The lifecycle handler to wrap. May not be <code>null</code>.
   */
  public SafeLifecycleEventHandler (@NonNull final IAPLifecycleEventSPI aHdl)
  {
    ValueEnforcer.notNull (aHdl, "Handler");
    m_aHdl = aHdl;
  }

  /** {@inheritDoc} */
  public void onInboundDocumentReceived (@NonNull final String sTransactionID,
                                         @NonNull final String sSenderID,
                                         @NonNull final String sReceiverID,
                                         @NonNull final String sDocTypeID,
                                         @NonNull final String sProcessID,
                                         @NonNull final String sSbdhInstanceID,
                                         final boolean bIsDuplicateAS4,
                                         final boolean bIsDuplicateSBDH)
  {
    try
    {
      m_aHdl.onInboundDocumentReceived (sTransactionID,
                                        sSenderID,
                                        sReceiverID,
                                        sDocTypeID,
                                        sProcessID,
                                        sSbdhInstanceID,
                                        bIsDuplicateAS4,
                                        bIsDuplicateSBDH);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onInboundDocumentReceived on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  public void onInboundVerificationAccepted (@NonNull final String sTransactionID,
                                             @NonNull final String sSbdhInstanceID)
  {
    try
    {
      m_aHdl.onInboundVerificationAccepted (sTransactionID, sSbdhInstanceID);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onInboundVerificationAccepted on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  public void onOutboundVerificationAccepted (@NonNull final String sSbdhInstanceID)
  {
    try
    {
      m_aHdl.onOutboundVerificationAccepted (sSbdhInstanceID);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onOutboundVerificationAccepted on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  public void onInboundMLSCorrelated (@NonNull final String sMlsTransactionID,
                                      @NonNull final String sReferencedSbdhInstanceID,
                                      @NonNull final EPeppolMLSResponseCode eMlsResponseCode,
                                      @Nullable final Duration aRoundTrip)
  {
    try
    {
      m_aHdl.onInboundMLSCorrelated (sMlsTransactionID, sReferencedSbdhInstanceID, eMlsResponseCode, aRoundTrip);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onInboundMLSCorrelated on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  public void onInboundDocumentForwarded (@NonNull final String sTransactionID,
                                          @NonNull final String sSbdhInstanceID,
                                          @Nullable final Duration aForwardingDuration,
                                          final boolean bIsRetry)
  {
    try
    {
      m_aHdl.onInboundDocumentForwarded (sTransactionID, sSbdhInstanceID, aForwardingDuration, bIsRetry);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onInboundDocumentForwarded on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  public void onOutboundDocumentAccepted (@NonNull final String sTransactionID,
                                          @NonNull final String sSenderID,
                                          @NonNull final String sReceiverID,
                                          @NonNull final String sDocTypeID,
                                          @NonNull final String sProcessID,
                                          @NonNull final String sSbdhInstanceID)
  {
    try
    {
      m_aHdl.onOutboundDocumentAccepted (sTransactionID,
                                         sSenderID,
                                         sReceiverID,
                                         sDocTypeID,
                                         sProcessID,
                                         sSbdhInstanceID);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onOutboundDocumentAccepted on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  public void onOutboundDocumentSent (@NonNull final String sTransactionID,
                                      @NonNull final String sSbdhInstanceID,
                                      @Nullable final Duration aSendingDuration,
                                      @Nonnegative final int nAttempts)
  {
    try
    {
      m_aHdl.onOutboundDocumentSent (sTransactionID, sSbdhInstanceID, aSendingDuration, nAttempts);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onOutboundDocumentSent on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  public void onPeppolReportingTSRSuccess (@NonNull final YearMonth aYearMonth)
  {
    try
    {
      m_aHdl.onPeppolReportingTSRSuccess (aYearMonth);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onPeppolReportingTSRSuccess on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  public void onPeppolReportingEUSRSuccess (@NonNull final YearMonth aYearMonth)
  {
    try
    {
      m_aHdl.onPeppolReportingEUSRSuccess (aYearMonth);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onPeppolReportingEUSRSuccess on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  public void onRetrySchedulerCycle (final boolean bIsOutbound,
                                     @Nonnegative final int nProcessed,
                                     @NonNull final Duration aCycleDuration)
  {
    try
    {
      m_aHdl.onRetrySchedulerCycle (bIsOutbound, nProcessed, aCycleDuration);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onRetrySchedulerCycle on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  public void onArchivalSchedulerCycle (final boolean bIsOutbound,
                                        @Nonnegative final int nArchived,
                                        @NonNull final Duration aCycleDuration)
  {
    try
    {
      m_aHdl.onArchivalSchedulerCycle (bIsOutbound, nArchived, aCycleDuration);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onArchivalSchedulerCycle on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  public void onCleanupSchedulerCycle (final boolean bIsOutbound,
                                       @Nonnegative final int nDeleted,
                                       @NonNull final Duration aCycleDuration)
  {
    try
    {
      m_aHdl.onCleanupSchedulerCycle (bIsOutbound, nDeleted, aCycleDuration);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onCleanupSchedulerCycle on " + m_aHdl, ex);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Handler", m_aHdl).getToString ();
  }
}
