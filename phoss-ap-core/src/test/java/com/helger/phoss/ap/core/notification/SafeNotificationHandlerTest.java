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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.YearMonth;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.spi.IAPNotificationHandlerSPI;

/**
 * Test class for {@link SafeNotificationHandler}.
 *
 * @author Philip Helger
 */
public final class SafeNotificationHandlerTest
{
  /**
   * A counting notification handler that records how many times each method was called.
   */
  private static final class CountingHandler implements IAPNotificationHandlerSPI
  {
    final AtomicInteger m_aInboundVerificationRejectionCount = new AtomicInteger (0);
    final AtomicInteger m_aInboundReceiverNotServicedCount = new AtomicInteger (0);
    final AtomicInteger m_aInboundMLSCorrelationErrorCount = new AtomicInteger (0);
    final AtomicInteger m_aInboundForwardingErrorCount = new AtomicInteger (0);
    final AtomicInteger m_aInboundPermanentForwardingFailureCount = new AtomicInteger (0);
    final AtomicInteger m_aOutboundPermanentSendingFailureCount = new AtomicInteger (0);
    final AtomicInteger m_aPeppolReportingTSRFailureCount = new AtomicInteger (0);
    final AtomicInteger m_aPeppolReportingEUSRFailureCount = new AtomicInteger (0);
    final AtomicInteger m_aUnexpectedExceptionCount = new AtomicInteger (0);

    public void onInboundVerificationRejection (@NonNull final String sTransactionID,
                                                @NonNull final String sSbdhInstanceID,
                                                @Nullable final String sErrorDetails)
    {
      m_aInboundVerificationRejectionCount.incrementAndGet ();
    }

    public void onInboundReceiverNotServiced (@NonNull final String sSenderID,
                                              @NonNull final String sReceiverID,
                                              @NonNull final String sDocTypeID,
                                              @NonNull final String sProcessID,
                                              @NonNull final String sSbdhInstanceID)
    {
      m_aInboundReceiverNotServicedCount.incrementAndGet ();
    }

    public void onInboundMLSCorrelationError (@NonNull final String sTransactionID,
                                              @NonNull final String sReferencedSbdhInstanceID,
                                              @NonNull final EPeppolMLSResponseCode eMlsResponseCode)
    {
      m_aInboundMLSCorrelationErrorCount.incrementAndGet ();
    }

    public void onInboundForwardingError (@NonNull final String sTransactionID, final boolean bIsRetry)
    {
      m_aInboundForwardingErrorCount.incrementAndGet ();
    }

    public void onInboundPermanentForwardingFailure (@NonNull final String sTransactionID,
                                                     @NonNull final String sSbdhInstanceID,
                                                     @Nullable final String sErrorDetails)
    {
      m_aInboundPermanentForwardingFailureCount.incrementAndGet ();
    }

    public void onOutboundPermanentSendingFailure (@NonNull final String sTransactionID,
                                                   @NonNull final String sSbdhInstanceID,
                                                   @Nullable final String sErrorDetails)
    {
      m_aOutboundPermanentSendingFailureCount.incrementAndGet ();
    }

    public void onPeppolReportingTSRFailure (@NonNull final YearMonth aYearMonth)
    {
      m_aPeppolReportingTSRFailureCount.incrementAndGet ();
    }

    public void onPeppolReportingEUSRFailure (@NonNull final YearMonth aYearMonth)
    {
      m_aPeppolReportingEUSRFailureCount.incrementAndGet ();
    }

    public void onUnexpectedException (@NonNull final String sContext,
                                       @NonNull final String sMessage,
                                       @NonNull final Exception aException)
    {
      m_aUnexpectedExceptionCount.incrementAndGet ();
    }
  }

  /**
   * A handler that throws RuntimeException from every method.
   */
  private static final class ThrowingHandler implements IAPNotificationHandlerSPI
  {
    public void onInboundVerificationRejection (@NonNull final String sTransactionID,
                                                @NonNull final String sSbdhInstanceID,
                                                @Nullable final String sErrorDetails)
    {
      throw new RuntimeException ("test-onInboundVerificationRejection");
    }

    public void onInboundReceiverNotServiced (@NonNull final String sSenderID,
                                              @NonNull final String sReceiverID,
                                              @NonNull final String sDocTypeID,
                                              @NonNull final String sProcessID,
                                              @NonNull final String sSbdhInstanceID)
    {
      throw new RuntimeException ("test-onInboundReceiverNotServiced");
    }

    public void onInboundMLSCorrelationError (@NonNull final String sTransactionID,
                                              @NonNull final String sReferencedSbdhInstanceID,
                                              @NonNull final EPeppolMLSResponseCode eMlsResponseCode)
    {
      throw new RuntimeException ("test-onInboundMLSCorrelationError");
    }

    public void onInboundForwardingError (@NonNull final String sTransactionID, final boolean bIsRetry)
    {
      throw new RuntimeException ("test-onInboundForwardingError");
    }

    public void onInboundPermanentForwardingFailure (@NonNull final String sTransactionID,
                                                     @NonNull final String sSbdhInstanceID,
                                                     @Nullable final String sErrorDetails)
    {
      throw new RuntimeException ("test-onInboundPermanentForwardingFailure");
    }

    public void onOutboundPermanentSendingFailure (@NonNull final String sTransactionID,
                                                   @NonNull final String sSbdhInstanceID,
                                                   @Nullable final String sErrorDetails)
    {
      throw new RuntimeException ("test-onOutboundPermanentSendingFailure");
    }

    public void onPeppolReportingTSRFailure (@NonNull final YearMonth aYearMonth)
    {
      throw new RuntimeException ("test-onPeppolReportingTSRFailure");
    }

    public void onPeppolReportingEUSRFailure (@NonNull final YearMonth aYearMonth)
    {
      throw new RuntimeException ("test-onPeppolReportingEUSRFailure");
    }

    public void onUnexpectedException (@NonNull final String sContext,
                                       @NonNull final String sMessage,
                                       @NonNull final Exception aException)
    {
      throw new RuntimeException ("test-onUnexpectedException");
    }
  }

  @Test
  public void testDelegationToInnerHandler ()
  {
    final CountingHandler aInner = new CountingHandler ();
    final SafeNotificationHandler aSafe = new SafeNotificationHandler (aInner);

    aSafe.onInboundVerificationRejection ("tx-1", "sbdh-1", "error");
    assertEquals (1, aInner.m_aInboundVerificationRejectionCount.get ());

    aSafe.onInboundReceiverNotServiced ("sender", "receiver", "doctype", "process", "sbdh-2");
    assertEquals (1, aInner.m_aInboundReceiverNotServicedCount.get ());

    aSafe.onInboundMLSCorrelationError ("tx-2", "ref-sbdh", EPeppolMLSResponseCode.REJECTION);
    assertEquals (1, aInner.m_aInboundMLSCorrelationErrorCount.get ());

    aSafe.onInboundForwardingError ("tx-3", false);
    assertEquals (1, aInner.m_aInboundForwardingErrorCount.get ());

    aSafe.onInboundPermanentForwardingFailure ("tx-4", "sbdh-4", "permanent error");
    assertEquals (1, aInner.m_aInboundPermanentForwardingFailureCount.get ());

    aSafe.onOutboundPermanentSendingFailure ("tx-5", "sbdh-5", null);
    assertEquals (1, aInner.m_aOutboundPermanentSendingFailureCount.get ());

    aSafe.onPeppolReportingTSRFailure (YearMonth.of (2026, 1));
    assertEquals (1, aInner.m_aPeppolReportingTSRFailureCount.get ());

    aSafe.onPeppolReportingEUSRFailure (YearMonth.of (2026, 2));
    assertEquals (1, aInner.m_aPeppolReportingEUSRFailureCount.get ());

    aSafe.onUnexpectedException ("context", "message", new Exception ("test"));
    assertEquals (1, aInner.m_aUnexpectedExceptionCount.get ());
  }

  @Test
  public void testExceptionsSuppressed ()
  {
    final SafeNotificationHandler aSafe = new SafeNotificationHandler (new ThrowingHandler ());

    // None of these should throw
    aSafe.onInboundVerificationRejection ("tx-1", "sbdh-1", "error");
    aSafe.onInboundReceiverNotServiced ("sender", "receiver", "doctype", "process", "sbdh-2");
    aSafe.onInboundMLSCorrelationError ("tx-2", "ref-sbdh", EPeppolMLSResponseCode.REJECTION);
    aSafe.onInboundForwardingError ("tx-3", true);
    aSafe.onInboundPermanentForwardingFailure ("tx-4", "sbdh-4", null);
    aSafe.onOutboundPermanentSendingFailure ("tx-5", "sbdh-5", "details");
    aSafe.onPeppolReportingTSRFailure (YearMonth.of (2026, 3));
    aSafe.onPeppolReportingEUSRFailure (YearMonth.of (2026, 3));
    aSafe.onUnexpectedException ("ctx", "msg", new Exception ("inner"));
  }

  @Test
  public void testToStringContainsInnerHandler ()
  {
    final CountingHandler aInner = new CountingHandler ();
    final SafeNotificationHandler aSafe = new SafeNotificationHandler (aInner);

    final String sStr = aSafe.toString ();
    assertNotNull (sStr);
    assertTrue (sStr.contains ("Handler"));
  }

  @Test
  public void testMultipleCallsAreCounted ()
  {
    final CountingHandler aInner = new CountingHandler ();
    final SafeNotificationHandler aSafe = new SafeNotificationHandler (aInner);

    for (int i = 0; i < 5; i++)
      aSafe.onInboundForwardingError ("tx-" + i, i > 0);
    assertEquals (5, aInner.m_aInboundForwardingErrorCount.get ());
  }

  @Test
  public void testNullableParametersAccepted ()
  {
    final CountingHandler aInner = new CountingHandler ();
    final SafeNotificationHandler aSafe = new SafeNotificationHandler (aInner);

    // These methods accept nullable error details
    aSafe.onInboundVerificationRejection ("tx-1", "sbdh-1", null);
    assertEquals (1, aInner.m_aInboundVerificationRejectionCount.get ());

    aSafe.onInboundPermanentForwardingFailure ("tx-2", "sbdh-2", null);
    assertEquals (1, aInner.m_aInboundPermanentForwardingFailureCount.get ());

    aSafe.onOutboundPermanentSendingFailure ("tx-3", "sbdh-3", null);
    assertEquals (1, aInner.m_aOutboundPermanentSendingFailureCount.get ());
  }
}
