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

import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.spi.INotificationHandlerSPI;

/**
 * This is a wrapper class around another {@link INotificationHandlerSPI} implementation that wraps
 * all exceptions and logs them accordingly. <br>
 * Note: this class is manually instantiated to wrap SPI loaded instances.
 *
 * @author Philip Helger
 */
public final class SafeNotificationHandler implements INotificationHandlerSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SafeNotificationHandler.class);

  private final INotificationHandlerSPI m_aHdl;

  public SafeNotificationHandler (@NonNull final INotificationHandlerSPI aHdl)
  {
    ValueEnforcer.notNull (aHdl, "Handler");
    m_aHdl = aHdl;
  }

  public void onInboundVerificationRejection (@NonNull final String sTransactionID,
                                              @NonNull final String sSbdhInstanceID,
                                              @Nullable final String sErrorDetails)
  {
    try
    {
      m_aHdl.onInboundVerificationRejection (sTransactionID, sSbdhInstanceID, sErrorDetails);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onInboundVerificationRejection on " + m_aHdl, ex);
    }
  }

  public void onInboundReceiverNotServiced (@NonNull final String sSenderID,
                                            @NonNull final String sReceiverID,
                                            @NonNull final String sDocTypeID,
                                            @NonNull final String sProcessID,
                                            @NonNull final String sSbdhInstanceID)
  {
    try
    {
      m_aHdl.onInboundReceiverNotServiced (sSenderID, sReceiverID, sDocTypeID, sProcessID, sSbdhInstanceID);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onInboundReceiverNotServiced on " + m_aHdl, ex);
    }
  }

  public void onInboundMLSCorrelationError (@NonNull final String sTransactionID,
                                            @NonNull final String sReferencedSbdhInstanceID,
                                            @NonNull final EPeppolMLSResponseCode eMlsResponseCode)
  {
    try
    {
      m_aHdl.onInboundMLSCorrelationError (sTransactionID, sReferencedSbdhInstanceID, eMlsResponseCode);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onInboundMLSCorrelationError on " + m_aHdl, ex);
    }
  }

  public void onInboundForwardingError (@NonNull final String sTransactionID, final boolean bIsRetry)
  {
    try
    {
      m_aHdl.onInboundForwardingError (sTransactionID, bIsRetry);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onInboundForwardingError on " + m_aHdl, ex);
    }
  }

  public void onInboundPermanentForwardingFailure (@NonNull final String sTransactionID,
                                                   @NonNull final String sSbdhInstanceID,
                                                   @Nullable final String sErrorDetails)
  {
    try
    {
      m_aHdl.onInboundPermanentForwardingFailure (sTransactionID, sSbdhInstanceID, sErrorDetails);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onPermanentForwardingFailure on " + m_aHdl, ex);
    }
  }

  public void onOutboundPermanentSendingFailure (@NonNull final String sTransactionID,
                                                 @NonNull final String sSbdhInstanceID,
                                                 @Nullable final String sErrorDetails)
  {
    try
    {
      m_aHdl.onOutboundPermanentSendingFailure (sTransactionID, sSbdhInstanceID, sErrorDetails);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onPermanentSendingFailure on " + m_aHdl, ex);
    }
  }

  public void onPeppolReportingTSRFailure (@NonNull final YearMonth aYearMonth)
  {
    try
    {
      m_aHdl.onPeppolReportingTSRFailure (aYearMonth);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onPeppolReportingTSRFailure on " + m_aHdl, ex);
    }
  }

  public void onPeppolReportingEUSRFailure (@NonNull final YearMonth aYearMonth)
  {
    try
    {
      m_aHdl.onPeppolReportingEUSRFailure (aYearMonth);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error invoking onPeppolReportingEUSRFailure on " + m_aHdl, ex);
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Handler", m_aHdl).getToString ();
  }
}
