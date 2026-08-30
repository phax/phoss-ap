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
package com.helger.phoss.ap.sentry;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.model.VerifierResult;
import com.helger.phoss.ap.api.spi.IAPNotificationHandlerSPI;

import io.sentry.Sentry;
import io.sentry.SentryAttributes;
import io.sentry.SentryLogLevel;
import io.sentry.logger.SentryLogParameters;

/**
 * Special implementation of {@link IAPNotificationHandlerSPI} for Sentry log events. It is not
 * registered as an SPI provider, because it is included dependent on the existence of the Sentry
 * dependencies.
 *
 * @author Philip Helger
 */
public class APNotificationHandlerSentry implements IAPNotificationHandlerSPI
{
  /**
   * Create the Sentry attribute map from alternating key-value pairs. Contrary to
   * {@link Map#of(Object, Object)} <code>null</code> values are allowed - the respective attribute
   * is simply not part of the resulting map, because Sentry has no representation for "no value".
   *
   * @param aKeyValuePairs
   *        Alternating keys (of type String) and values. May not be <code>null</code> and the
   *        length must be even.
   * @return The map with all non-<code>null</code> values, in the order of the parameters. Never
   *         <code>null</code>.
   */
  @NonNull
  private static Map <String, Object> _params (@NonNull final Object... aKeyValuePairs)
  {
    ValueEnforcer.isTrue (aKeyValuePairs.length % 2 == 0, "The number of key-value parameters must be even");

    final ICommonsOrderedMap <String, Object> ret = new CommonsLinkedHashMap <> ();
    for (int i = 0; i < aKeyValuePairs.length; i += 2)
      ret.putIfNotNull ((String) aKeyValuePairs[i], aKeyValuePairs[i + 1]);
    return ret;
  }

  private static void _logError (@NonNull final String sMsg, @NonNull final Map <String, Object> aParams)
  {
    Sentry.logger ().log (SentryLogLevel.ERROR, SentryLogParameters.create (SentryAttributes.fromMap (aParams)), sMsg);
  }

  private static void _logWarn (@NonNull final String sMsg, @NonNull final Map <String, Object> aParams)
  {
    Sentry.logger ().log (SentryLogLevel.WARN, SentryLogParameters.create (SentryAttributes.fromMap (aParams)), sMsg);
  }

  /** {@inheritDoc} */
  public void onInboundVerificationRejection (@NonNull final String sTransactionID,
                                              @NonNull final String sSbdhInstanceID,
                                              @Nullable final String sErrorDetails,
                                              @NonNull final MlsOutcome aMlsOutcome)
  {
    _logError ("onInboundVerificationRejection",
               _params ("transactionID",
                       sTransactionID,
                       "sbdhInstanceID",
                       sSbdhInstanceID,
                       "errorDetails",
                       sErrorDetails,
                       "mlsResponseCode",
                       aMlsOutcome.getResponseCodeID (),
                       "issueCount",
                       Integer.toString (aMlsOutcome.getIssues ().size ())));
  }

  /** {@inheritDoc} */
  public void onInboundVerificationDeferred (@NonNull final String sTransactionID,
                                             @NonNull final String sSbdhInstanceID,
                                             @NonNull final String sVerifierName,
                                             @NonNull final OffsetDateTime aNextRetryDT,
                                             @Nullable final String sErrorDetails)
  {
    // Deliberately a warning - the document is not lost, but the verifier needs attention
    _logWarn ("onInboundVerificationDeferred",
              _params ("transactionID",
                      sTransactionID,
                      "sbdhInstanceID",
                      sSbdhInstanceID,
                      "verifierName",
                      sVerifierName,
                      "nextRetryDT",
                      aNextRetryDT,
                      "errorDetails",
                      sErrorDetails));
  }

  /** {@inheritDoc} */
  public void onOutboundVerificationRejection (@NonNull final String sSbdhInstanceID,
                                               @NonNull final VerifierResult aVerifierResult)
  {
    _logError ("onOutboundVerificationRejection",
               _params ("sbdhInstanceID",
                       sSbdhInstanceID,
                       "verifierName",
                       aVerifierResult.verifierName (),
                       "errorDetails",
                       aVerifierResult.outcome ().getMessage (),
                       "issueCount",
                       Integer.toString (aVerifierResult.outcome ().getAllIssues ().size ())));
  }

  /** {@inheritDoc} */
  public void onOutboundPermanentSendingFailure (@NonNull final String sTransactionID,
                                                 @NonNull final String sSbdhInstanceID,
                                                 @Nullable final String sErrorDetails)
  {
    _logError ("onPermanentSendingFailure",
               _params ("transactionID",
                       sTransactionID,
                       "sbdhInstanceID",
                       sSbdhInstanceID,
                       "errorDetails",
                       sErrorDetails));
  }

  /** {@inheritDoc} */
  public void onInboundReceiverNotServiced (@NonNull final String sSenderID,
                                            @NonNull final String sReceiverID,
                                            @NonNull final String sDocTypeID,
                                            @NonNull final String sProcessID,
                                            @NonNull final String sSbdhInstanceID)
  {
    _logError ("onInboundReceiverNotServiced",
               _params ("senderID",
                       sSenderID,
                       "receiverID",
                       sReceiverID,
                       "docTypeID",
                       sDocTypeID,
                       "processID",
                       sProcessID,
                       "sbdhInstanceID",
                       sSbdhInstanceID));
  }

  /** {@inheritDoc} */
  public void onInboundPermanentForwardingFailure (@NonNull final String sTransactionID,
                                                   @NonNull final String sSbdhInstanceID,
                                                   @Nullable final String sErrorDetails)
  {
    _logError ("onPermanentForwardingFailure",
               _params ("transactionID",
                       sTransactionID,
                       "sbdhInstanceID",
                       sSbdhInstanceID,
                       "errorDetails",
                       sErrorDetails));
  }

  /** {@inheritDoc} */
  public void onInboundDuplicateRejected (@NonNull final String sSenderID,
                                          @NonNull final String sReceiverID,
                                          @NonNull final String sDocTypeID,
                                          @NonNull final String sProcessID,
                                          @Nullable final String sSenderProviderID,
                                          @Nullable final String sAS4MessageID,
                                          @NonNull final String sSbdhInstanceID,
                                          final boolean bIsDuplicateAS4,
                                          final boolean bIsDuplicateSBDH,
                                          @NonNull final String sErrorDetails)
  {
    _logError ("onInboundDuplicateRejected",
               _params ("senderID",
                       sSenderID,
                       "receiverID",
                       sReceiverID,
                       "docTypeID",
                       sDocTypeID,
                       "processID",
                       sProcessID,
                       "senderProviderID",
                       sSenderProviderID,
                       "AS4MessageID",
                       sAS4MessageID,
                       "sbdhInstanceID",
                       sSbdhInstanceID,
                       "isDuplicateAS4",
                       Boolean.valueOf (bIsDuplicateAS4),
                       "isDuplicateSBDH",
                       Boolean.valueOf (bIsDuplicateSBDH),
                       "errorDetails",
                       sErrorDetails));
  }

  /** {@inheritDoc} */
  public void onInboundMLSCorrelationError (@NonNull final String sTransactionID,
                                            @NonNull final String sReferencedSbdhInstanceID,
                                            @NonNull final EPeppolMLSResponseCode eMlsResponseCode)
  {
    _logError ("onInboundMLSCorrelationError",
               _params ("transactionID",
                       sTransactionID,
                       "referencedSbdhInstanceID",
                       sReferencedSbdhInstanceID,
                       "mlsResponseCode",
                       eMlsResponseCode.getID ()));
  }

  /** {@inheritDoc} */
  public void onSpecialMlsToNotReachable (@NonNull final String sOutboundTransactionID,
                                          @NonNull final String sReferencedSbdhInstanceID,
                                          @NonNull final String sAttemptedMlsToParticipantID,
                                          @NonNull final String sFallbackDefaultSpidParticipantID)
  {
    _logError ("onSpecialMlsToNotReachable",
               _params ("outboundTransactionID",
                       sOutboundTransactionID,
                       "referencedSbdhInstanceID",
                       sReferencedSbdhInstanceID,
                       "attemptedMlsToParticipantID",
                       sAttemptedMlsToParticipantID,
                       "fallbackDefaultSpidParticipantID",
                       sFallbackDefaultSpidParticipantID));
  }

  /** {@inheritDoc} */
  public void onInboundForwardingError (@NonNull final String sTransactionID, final boolean bIsRetry)
  {
    _logError ("onInboundForwardingError",
               _params ("transactionID", sTransactionID, "isRetry", Boolean.valueOf (bIsRetry)));
  }

  /** {@inheritDoc} */
  public void onPeppolReportingTSRFailure (@NonNull final YearMonth aYearMonth)
  {
    _logError ("onPeppolReportingTSRFailure",
               _params ("year",
                       Integer.valueOf (aYearMonth.getYear ()),
                       "month",
                       Integer.valueOf (aYearMonth.getMonthValue ())));
  }

  /** {@inheritDoc} */
  public void onPeppolReportingEUSRFailure (@NonNull final YearMonth aYearMonth)
  {
    _logError ("onPeppolReportingEUSRFailure",
               _params ("year",
                       Integer.valueOf (aYearMonth.getYear ()),
                       "month",
                       Integer.valueOf (aYearMonth.getMonthValue ())));
  }

  /** {@inheritDoc} */
  public void onUnexpectedException (@NonNull final String sContext,
                                     @NonNull final String sMessage,
                                     @NonNull final Exception aException)
  {
    Sentry.withScope (scope -> {
      scope.setExtra ("context", sContext);
      scope.setExtra ("message", sMessage);
      scope.setExtra ("exceptionClass", aException.getClass ().getName ());
      Sentry.captureException (aException);
    });
  }
}
