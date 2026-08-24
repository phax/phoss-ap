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
package com.helger.phoss.ap.api.spi;

import java.time.OffsetDateTime;
import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.IsSPIInterface;
import com.helger.phoss.ap.api.model.VerifierResult;
import com.helger.peppol.mls.EPeppolMLSResponseCode;

/**
 * SPI interface for receiving notifications about permanent processing failures. Implementations
 * are loaded via {@link java.util.ServiceLoader}. Multiple handlers may be registered. Concrete
 * implementations are deployment-specific (e.g., email, Slack, monitoring system webhook).
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface IAPNotificationHandlerSPI
{
  /**
   * Called when an inbound receiver is not serviced.
   *
   * @param sSenderID
   *        Peppol sender ID (C1). Never <code>null</code>.
   * @param sReceiverID
   *        Peppol receiver ID (C4). Never <code>null</code>.
   * @param sDocTypeID
   *        Peppol document type ID. Never <code>null</code>.
   * @param sProcessID
   *        Peppol process ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        SBDH Instance Identifier. Never <code>null</code>.
   */
  void onInboundReceiverNotServiced (@NonNull String sSenderID,
                                     @NonNull String sReceiverID,
                                     @NonNull String sDocTypeID,
                                     @NonNull String sProcessID,
                                     @NonNull String sSbdhInstanceID);

  /**
   * Called when an outbound or inbound document fails optional verification and is rejected.
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param sErrorDetails
   *        Optional error details. May be <code>null</code>.
   */
  void onInboundVerificationRejection (@NonNull String sTransactionID,
                                       @NonNull String sSbdhInstanceID,
                                       @Nullable String sErrorDetails);

  /**
   * Called when the verification of an inbound document was deferred, because a document verifier
   * made no verdict about the document - e.g. because its backend service was unavailable - and
   * the configured fail mode is <code>deferred</code>. The document is neither forwarded nor
   * rejected yet and <b>no MLS was sent to C2</b>; it is re-verified at the provided date and time.
   * <p>
   * This callback is fired for every deferral, i.e. also for every unsuccessful re-verification. It
   * is the signal that a verifier needs operator attention: if the situation is not resolved within
   * <code>verification.deferred.max-duration</code>, the document is rejected and
   * {@link #onInboundVerificationRejection(String, String, String)} is fired.
   * </p>
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param sVerifierName
   *        The name of the document verifier that made no verdict. Never <code>null</code>.
   * @param aNextRetryDT
   *        The date and time of the next scheduled re-verification. Never <code>null</code>.
   * @param sErrorDetails
   *        Optional error details. May be <code>null</code>.
   * @since 0.12.0
   */
  void onInboundVerificationDeferred (@NonNull String sTransactionID,
                                      @NonNull String sSbdhInstanceID,
                                      @NonNull String sVerifierName,
                                      @NonNull OffsetDateTime aNextRetryDT,
                                      @Nullable String sErrorDetails);

  /**
   * Called when an outbound document fails optional verification before sending and is rejected. No
   * outbound transaction has been created yet, so this callback only carries the SBDH Instance
   * Identifier.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param aVerifierResult
   *        The result that led to the rejection, carrying the name of the decisive verifier, the
   *        human-readable reason and the individual
   *        {@link com.helger.phoss.ap.api.model.VerificationIssue}s. Never <code>null</code>. The
   *        outcome category also states whether the document was actually found to be invalid or
   *        whether the verifier could not make a verdict at all.
   * @since 0.9.0 - carries the {@link VerifierResult} instead of a flat error string since 0.12.0
   */
  void onOutboundVerificationRejection (@NonNull String sSbdhInstanceID, @NonNull VerifierResult aVerifierResult);

  /**
   * Called when an inbound AS4 message is rejected because it was detected as a duplicate before an
   * inbound transaction could be persisted.
   *
   * @param sSenderID
   *        Peppol sender ID (C1). Never <code>null</code>.
   * @param sReceiverID
   *        Peppol receiver ID (C4). Never <code>null</code>.
   * @param sDocTypeID
   *        Peppol document type ID. Never <code>null</code>.
   * @param sProcessID
   *        Peppol process ID. Never <code>null</code>.
   * @param sSenderProviderID
   *        Peppol sender provider ID (C2), usually derived from the signing certificate common
   *        name. May be <code>null</code>.
   * @param sAS4MessageID
   *        AS4 Message ID. May be <code>null</code>.
   * @param sSbdhInstanceID
   *        SBDH Instance Identifier. Never <code>null</code>.
   * @param bIsDuplicateAS4
   *        <code>true</code> if an AS4 message with the same ID was previously received.
   * @param bIsDuplicateSBDH
   *        <code>true</code> if an SBDH with the same Instance ID was previously received.
   * @param sErrorDetails
   *        Error details sent back as AS4 error. Never <code>null</code>.
   * @since 0.10.0
   */
  void onInboundDuplicateRejected (@NonNull String sSenderID,
                                   @NonNull String sReceiverID,
                                   @NonNull String sDocTypeID,
                                   @NonNull String sProcessID,
                                   @Nullable String sSenderProviderID,
                                   @Nullable String sAS4MessageID,
                                   @NonNull String sSbdhInstanceID,
                                   boolean bIsDuplicateAS4,
                                   boolean bIsDuplicateSBDH,
                                   @NonNull String sErrorDetails);

  /**
   * Called when the inbound message is an MLS but could not be correlated with an outbound
   * transaction.
   *
   * @param sTransactionID
   *        The incoming transaction ID. May not be <code>null</code>.
   * @param sReferencedSbdhInstanceID
   *        The referenced SBDH ID from the MLS. May not be <code>null</code>.
   * @param eMlsResponseCode
   *        The response code contained in the MLS. May not be <code>null</code>.
   */
  void onInboundMLSCorrelationError (@NonNull String sTransactionID,
                                     @NonNull String sReferencedSbdhInstanceID,
                                     @NonNull EPeppolMLSResponseCode eMlsResponseCode);

  /**
   * Called when an outbound MLS cannot be delivered to the requested custom <code>MLS_TO</code>
   * receiver because that receiver could not be resolved via SMP lookup, and sending falls back to
   * the default SPID receiver derived from the sending C2's Peppol AP certificate (see MLS SPOG
   * section 5.4).
   *
   * @param sOutboundTransactionID
   *        The outbound MLS transaction ID. Never <code>null</code>.
   * @param sReferencedSbdhInstanceID
   *        The SBDH Instance Identifier of the original business document the MLS refers to. Never
   *        <code>null</code>.
   * @param sAttemptedMlsToParticipantID
   *        The custom <code>MLS_TO</code> receiver participant ID that could not be reached. Never
   *        <code>null</code>.
   * @param sFallbackDefaultSpidParticipantID
   *        The default SPID receiver participant ID used as the fallback target. Never
   *        <code>null</code>.
   * @since 0.11.0
   */
  void onSpecialMlsToNotReachable (@NonNull String sOutboundTransactionID,
                                   @NonNull String sReferencedSbdhInstanceID,
                                   @NonNull String sAttemptedMlsToParticipantID,
                                   @NonNull String sFallbackDefaultSpidParticipantID);

  /**
   * Called if an inbound messages could not be forwarded properly. The database state has already
   * been updated when this is called.
   *
   * @param sTransactionID
   *        The inbound transaction ID. May not be <code>null</code>.
   * @param bIsRetry
   *        <code>true</code> if it is a retry, <code>false</code> if it is the original request.
   */
  void onInboundForwardingError (@NonNull String sTransactionID, boolean bIsRetry);

  /**
   * Called when an inbound transaction permanently fails after exhausting all forwarding retries.
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param sErrorDetails
   *        Optional error details. May be <code>null</code>.
   */
  void onInboundPermanentForwardingFailure (@NonNull String sTransactionID,
                                            @NonNull String sSbdhInstanceID,
                                            @Nullable String sErrorDetails);

  /**
   * Called when an outbound transaction permanently fails after exhausting all sending retries.
   *
   * @param sTransactionID
   *        The transaction ID. Never <code>null</code>.
   * @param sSbdhInstanceID
   *        The SBDH Instance Identifier. Never <code>null</code>.
   * @param sErrorDetails
   *        Optional error details. May be <code>null</code>.
   */
  void onOutboundPermanentSendingFailure (@NonNull String sTransactionID,
                                          @NonNull String sSbdhInstanceID,
                                          @Nullable String sErrorDetails);

  /**
   * Called when creating, validating or sending a Peppol Reporting TSR report failed.
   *
   * @param aYearMonth
   *        The year and month for which the reporting should be performed never <code>null</code>.
   */
  void onPeppolReportingTSRFailure (@NonNull YearMonth aYearMonth);

  /**
   * Called when creating, validating or sending a Peppol Reporting EUSR report failed.
   *
   * @param aYearMonth
   *        The year and month for which the reporting should be performed never <code>null</code>.
   */
  void onPeppolReportingEUSRFailure (@NonNull YearMonth aYearMonth);

  /**
   * Called when an unexpected exception occurs during processing that is not covered by more
   * specific notification methods.
   *
   * @param sContext
   *        A short description of where the exception occurred (e.g. class and method name). Never
   *        <code>null</code>.
   * @param sMessage
   *        A human-readable description of what went wrong. Never <code>null</code>.
   * @param aException
   *        The caught exception. Never <code>null</code>.
   */
  void onUnexpectedException (@NonNull String sContext, @NonNull String sMessage, @NonNull Exception aException);
}
