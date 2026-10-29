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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import com.helger.base.state.EContinue;
import com.helger.base.state.ESuccess;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EForwardingMode;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.EMlsSendingTrigger;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.codelist.EVerificationRejectionForwarding;
import com.helger.phoss.ap.api.codelist.EVerificationResult;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.api.model.MlsOutcomeIssue;
import com.helger.phoss.ap.api.model.VerificationIssue;
import com.helger.phoss.ap.api.model.VerificationOutcome;
import com.helger.phoss.ap.api.model.VerifierResult;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.mls.MlsHandler;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Integration tests for the persisted verification verdict of {@link InboundOrchestrator}. Unlike
 * {@link InboundOrchestratorVerifierTest}, which only exercises the pure verifier evaluation, these
 * tests need the database, because the verdict is written to the inbound transaction.
 * <p>
 * It also covers <code>forwarding.mls.skip-uncorrelated</code> and the repeated correlation of an
 * inbound MLS. That cannot go into a separate test class, because the static DataSourceProvider of
 * the database layer can only be initialised once per JVM.
 * </p>
 *
 * @author Philip Helger
 */
public final class InboundVerificationResultIntegrationTest
{
  @ClassRule
  public static final ScopeTestRule RULE = new ScopeTestRule ();

  private static final String LOG_PREFIX = "[Test] ";
  private static final String MLS_DOCTYPE_ID = EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getURIEncoded ();
  private static final String MLS_PROCESS_ID = EPredefinedProcessIdentifier.urn_peppol_edec_mls.getURIEncoded ();

  private IConfigWithFallback m_aOldConfig;

  /**
   * Replace the global configuration with one that adds the provided overrides on top of the
   * regular test configuration.
   *
   * @param aValues
   *        The configuration values to set. May not be <code>null</code>.
   */
  private void _overrideConfig (@NonNull final Map <String, String> aValues)
  {
    final MultiConfigurationValueProvider aVP = ConfigFactory.createDefaultValueProvider ();
    // Highest priority wins over the values from application.properties
    aVP.addConfigurationSource (new ConfigurationSourceFunction (aValues::get), Integer.MAX_VALUE);
    m_aOldConfig = APConfigProvider.setConfig (new ConfigWithFallback (aVP));
  }

  /**
   * Replace the global configuration with one that adds the provided override on top of the
   * regular test configuration.
   *
   * @param sKey
   *        The configuration key to set. May not be <code>null</code>.
   * @param sValue
   *        The configuration value to set. May not be <code>null</code>.
   */
  private void _overrideConfig (@NonNull final String sKey, @NonNull final String sValue)
  {
    _overrideConfig (Map.of (sKey, sValue));
  }

  @After
  public void restoreConfig ()
  {
    if (m_aOldConfig != null)
    {
      APConfigProvider.setConfig (m_aOldConfig);
      m_aOldConfig = null;
    }
  }

  @NonNull
  private static IInboundTransaction _createInboundTx (@NonNull final String sDocTypeID,
                                                       @NonNull final String sProcessID,
                                                       @Nullable final String sCorrelatedOutboundTxID)
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = "test-" + UUID.randomUUID ();
    final String sTxID = aMgr.create (sID,
                                      "POP000001",
                                      "POP000002",
                                      "CN=TestCert",
                                      "iso6523-actorid-upis::9915:sender",
                                      "iso6523-actorid-upis::9915:receiver",
                                      sDocTypeID,
                                      sProcessID,
                                      "/tmp/test-inbound.sbd",
                                      2048L,
                                      "sha256hash012345678901234567890123456789012345678901234567890123",
                                      "test-" + UUID.randomUUID (),
                                      APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC (),
                                      "test-" + UUID.randomUUID (),
                                      "DE",
                                      false,
                                      false,
                                      null,
                                      EPeppolMLSType.ALWAYS_SEND);
    assertNotNull (sTxID);
    if (sCorrelatedOutboundTxID != null)
      assertTrue (aMgr.updateMlsCorrelatedOutboundTransactionID (sTxID, sCorrelatedOutboundTxID).isSuccess ());
    final IInboundTransaction aTx = aMgr.getByID (sTxID);
    assertNotNull (aTx);
    return aTx;
  }

  @NonNull
  private static IOutboundTransaction _createOutboundTx ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sTxID = aMgr.create (ETransactionType.BUSINESS_DOCUMENT,
                                      "iso6523-actorid-upis::9915:sender",
                                      "iso6523-actorid-upis::9915:receiver",
                                      "busdox-docid-qns::urn:test:invoice",
                                      "cenbii-procid-ubl::urn:test:process",
                                      "test-" + UUID.randomUUID (),
                                      ESourceType.PAYLOAD_ONLY,
                                      "/tmp/test-outbound.sbd",
                                      1024L,
                                      "abc123hash012345678901234567890123456789012345678901234567890123",
                                      "DE",
                                      APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC (),
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null);
    assertNotNull (sTxID);
    final IOutboundTransaction aTx = aMgr.getByID (sTxID);
    assertNotNull (aTx);
    return aTx;
  }

  @NonNull
  private static IInboundTransaction _createInboundTx ()
  {
    return _createInboundTx ("busdox-docid-qns::urn:test:invoice", "cenbii-procid-ubl::urn:test:process", null);
  }

  @NonNull
  private static IInboundTransaction _createUncorrelatedMls ()
  {
    return _createInboundTx (MLS_DOCTYPE_ID, MLS_PROCESS_ID, null);
  }

  @Test
  public void testUnavailableWithFailModeOpenIsRecordedAsUnverified ()
  {
    _overrideConfig ("verification.verifier-fail-mode", "open");

    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();
    assertNull (aTx.getVerificationResult ());

    final VerifierResult aVR = new VerifierResult ("id1",
                                                   "Scanner",
                                                   VerificationOutcome.serviceUnavailable ("Connection refused"));
    // Fail mode "open" forwards the document, so processing continues ...
    assertSame (EContinue.CONTINUE, InboundOrchestrator.handleVerifierResult (LOG_PREFIX, aTx, aVR));

    // ... but the document was never actually inspected, and that must stay visible afterwards
    final IInboundTransaction aUpdated = aMgr.getByID (aTx.getID ());
    assertNotNull (aUpdated);
    assertSame (EVerificationResult.UNVERIFIED, aUpdated.getVerificationResult ());
    // The status is untouched - the verdict is independent of the lifecycle
    assertSame (EInboundStatus.RECEIVED, aUpdated.getStatus ());
  }

  @Test
  public void testUnavailableWithFailModeDeferredRecordsNoVerdict ()
  {
    _overrideConfig ("verification.verifier-fail-mode", "deferred");

    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();

    final VerifierResult aVR = new VerifierResult ("id1",
                                                   "Scanner",
                                                   VerificationOutcome.serviceUnavailable ("Connection refused"));
    assertSame (EContinue.BREAK, InboundOrchestrator.handleVerifierResult (LOG_PREFIX, aTx, aVR));

    final IInboundTransaction aUpdated = aMgr.getByID (aTx.getID ());
    assertNotNull (aUpdated);
    // The verdict is still open while the verification is deferred
    assertNull (aUpdated.getVerificationResult ());
    assertSame (EInboundStatus.VERIFICATION_DEFERRED, aUpdated.getStatus ());
  }

  @Test
  public void testPassedIsRecorded ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();

    final VerifierResult aVR = VerifierResult.passed (VerificationOutcome.passed ());
    assertSame (EContinue.CONTINUE, InboundOrchestrator.handleVerifierResult (LOG_PREFIX, aTx, aVR));

    final IInboundTransaction aUpdated = aMgr.getByID (aTx.getID ());
    assertNotNull (aUpdated);
    assertSame (EVerificationResult.PASSED, aUpdated.getVerificationResult ());
    assertNull (aUpdated.getVerificationDetails ());
  }

  @Test
  public void testPassedWithWarningsStoresTheFindings ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();

    final VerificationIssue aWarning = VerificationIssue.businessRuleWarning ("W-1", "/Invoice", "Just a warning");
    final VerifierResult aVR = VerifierResult.passed (VerificationOutcome.passed (new CommonsArrayList <> (aWarning)));
    assertSame (EContinue.CONTINUE, InboundOrchestrator.handleVerifierResult (LOG_PREFIX, aTx, aVR));

    final IInboundTransaction aUpdated = aMgr.getByID (aTx.getID ());
    assertNotNull (aUpdated);
    assertSame (EVerificationResult.PASSED, aUpdated.getVerificationResult ());

    // The findings of an accepted document are warnings - they are stored in the neutral form
    final String sDetails = aUpdated.getVerificationDetails ();
    assertNotNull (sDetails);
    assertTrue (sDetails.contains ("\"level\":\"warning\""));
    assertTrue (sDetails.contains ("\"code\":\"W-1\""));
    assertEquals (-1, sDetails.indexOf ("responseCode"));
  }

  @Test
  public void testStoredMlsWarningsOfAPassedDocument ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();
    // Not verified at all
    assertNull (InboundOrchestrator.getStoredMlsWarnings (aTx));

    final VerificationIssue aWarning = VerificationIssue.businessRuleWarning ("W-1", "/Invoice", "Just a warning");
    final VerifierResult aVR = VerifierResult.passed (VerificationOutcome.passed (new CommonsArrayList <> (aWarning)));
    assertSame (EContinue.CONTINUE, InboundOrchestrator.handleVerifierResult (LOG_PREFIX, aTx, aVR));

    // A forwarding retry only has the stored transaction - it must yield the same line responses
    // as the in-memory findings of the initial receive path
    final IInboundTransaction aUpdated = aMgr.getByID (aTx.getID ());
    assertNotNull (aUpdated);
    final ICommonsList <MlsOutcomeIssue> aStored = InboundOrchestrator.getStoredMlsWarnings (aUpdated);
    assertNotNull (aStored);
    final ICommonsList <MlsOutcomeIssue> aInMemory = InboundOrchestrator.getAllMlsIssues (aVR);
    assertEquals (aInMemory.size (), aStored.size ());
    assertEquals (1, aStored.size ());
    assertSame (aInMemory.getFirstOrNull ().getStatusReasonCode (), aStored.getFirstOrNull ().getStatusReasonCode ());
    assertEquals (aInMemory.getFirstOrNull ().getErrorField (), aStored.getFirstOrNull ().getErrorField ());
    assertEquals (aInMemory.getFirstOrNull ().getDescription (), aStored.getFirstOrNull ().getDescription ());
  }

  @Test
  public void testStoredMlsWarningsOfARejectedDocument ()
  {
    // The findings of a rejection were already sent as the negative MLS
    final IInboundTransaction aRejected = _rejectWithMode (EVerificationRejectionForwarding.RETRY, EContinue.CONTINUE);
    assertSame (EVerificationResult.REJECTED, aRejected.getVerificationResult ());
    assertNotNull (aRejected.getVerificationDetails ());
    assertNull (InboundOrchestrator.getStoredMlsWarnings (aRejected));
  }

  @NonNull
  private static VerifierResult _createRejection ()
  {
    final VerificationIssue aError = VerificationIssue.businessRuleViolation ("BR-1",
                                                                              "/Invoice",
                                                                              "The document is invalid");
    return new VerifierResult ("id1",
                               "Scanner",
                               VerificationOutcome.rejected ("Document verification failed",
                                                             new CommonsArrayList <> (aError)));
  }

  /**
   * Set the rejection forwarding mode and reject a freshly created transaction.
   *
   * @param eMode
   *        The mode to configure. May not be <code>null</code>.
   * @return The transaction as it is stored after the rejection. Never <code>null</code>.
   */
  @NonNull
  private IInboundTransaction _rejectWithMode (@NonNull final EVerificationRejectionForwarding eMode,
                                               @NonNull final EContinue eExpected)
  {
    _overrideConfig ("verification.inbound.rejection-forwarding", eMode.getID ());

    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();
    assertNull (aTx.getVerificationResult ());

    assertSame (eExpected, InboundOrchestrator.handleVerifierResult (LOG_PREFIX, aTx, _createRejection ()));

    final IInboundTransaction aUpdated = aMgr.getByID (aTx.getID ());
    assertNotNull (aUpdated);
    return aUpdated;
  }

  @Test
  public void testRejectionForwardingNoneIsTerminal ()
  {
    // The default mode - the document is never forwarded
    final IInboundTransaction aUpdated = _rejectWithMode (EVerificationRejectionForwarding.NONE, EContinue.BREAK);

    assertSame (EVerificationResult.REJECTED, aUpdated.getVerificationResult ());
    assertSame (EInboundStatus.REJECTED, aUpdated.getStatus ());
    assertEquals (0, aUpdated.getAttemptCount ());
    // The rejection reason is in the error details, as before 0.12.0
    final String sErrorDetails = aUpdated.getErrorDetails ();
    assertNotNull (sErrorDetails);
    assertTrue (sErrorDetails.startsWith (InboundOrchestrator.ERROR_DETAILS_VERIFICATION_REJECTED));
  }

  @Test
  public void testRejectionForwardingBestEffortStaysRejected ()
  {
    // The copy to C4 is fire-and-forget, so the transaction is terminal as in mode "none"
    final IInboundTransaction aUpdated = _rejectWithMode (EVerificationRejectionForwarding.BEST_EFFORT,
                                                          EContinue.BREAK);

    assertSame (EVerificationResult.REJECTED, aUpdated.getVerificationResult ());
    assertSame (EInboundStatus.REJECTED, aUpdated.getStatus ());
    // No regular forwarding was performed, so the attempt count is untouched
    assertEquals (0, aUpdated.getAttemptCount ());
  }

  @Test
  public void testRejectionForwardingRetryContinues ()
  {
    // The document must reach the regular forwarding, so the status is left to it
    final IInboundTransaction aUpdated = _rejectWithMode (EVerificationRejectionForwarding.RETRY, EContinue.CONTINUE);

    assertSame (EVerificationResult.REJECTED, aUpdated.getVerificationResult ());
    assertSame (EInboundStatus.RECEIVED, aUpdated.getStatus ());
    assertNull (aUpdated.getErrorDetails ());

    // The findings survive the forwarding, because they are not in the error details
    final String sDetails = aUpdated.getVerificationDetails ();
    assertNotNull (sDetails);
    assertTrue (sDetails.contains ("\"code\":\"BR-1\""));
  }

  @Test
  public void testRejectionForwardingInvalidValueFallsBackToNone ()
  {
    _overrideConfig ("verification.inbound.rejection-forwarding", "bogus");

    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();

    assertSame (EContinue.BREAK, InboundOrchestrator.handleVerifierResult (LOG_PREFIX, aTx, _createRejection ()));

    final IInboundTransaction aUpdated = aMgr.getByID (aTx.getID ());
    assertNotNull (aUpdated);
    assertSame (EInboundStatus.REJECTED, aUpdated.getStatus ());
  }

  @Test
  public void testMlsSuppressionFollowsTheVerdict ()
  {
    // A rejected but forwarded document was already answered with the negative MLS (RE) - no
    // further MLS may be sent for it, no matter which code path asks
    final IInboundTransaction aRejected = _rejectWithMode (EVerificationRejectionForwarding.RETRY, EContinue.CONTINUE);
    assertTrue (InboundOrchestrator.isMlsSuppressedAfterRejection (aRejected));

    // A stale instance - loaded before the verdict was written - must not be used for the decision
    assertFalse (InboundOrchestrator.isMlsSuppressedAfterRejection (_createInboundTx ()));
  }

  @Test
  public void testMlsIsNotSuppressedForAPassedDocument ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();

    assertSame (EContinue.CONTINUE,
                InboundOrchestrator.handleVerifierResult (LOG_PREFIX,
                                                          aTx,
                                                          VerifierResult.passed (VerificationOutcome.passed ())));

    final IInboundTransaction aUpdated = aMgr.getByID (aTx.getID ());
    assertNotNull (aUpdated);
    assertFalse (InboundOrchestrator.isMlsSuppressedAfterRejection (aUpdated));
  }

  @Test
  public void testSkipUncorrelatedMlsDisabledByDefault ()
  {
    assertFalse (InboundOrchestrator.isSkipPrimaryForwarding (_createUncorrelatedMls ()));
  }

  @Test
  public void testSkipUncorrelatedMlsExplicitlyDisabled ()
  {
    _overrideConfig (APConfigurationProperties.FORWARDING_MLS_SKIP_UNCORRELATED, "false");
    assertFalse (InboundOrchestrator.isSkipPrimaryForwarding (_createUncorrelatedMls ()));
  }

  @Test
  public void testSkipOnlyUncorrelatedMls ()
  {
    _overrideConfig (APConfigurationProperties.FORWARDING_MLS_SKIP_UNCORRELATED, "true");

    assertTrue (InboundOrchestrator.isSkipPrimaryForwarding (_createUncorrelatedMls ()));
    // A correlated MLS is forwarded
    assertFalse (InboundOrchestrator.isSkipPrimaryForwarding (_createInboundTx (MLS_DOCTYPE_ID,
                                                                                MLS_PROCESS_ID,
                                                                                "outbound-" + UUID.randomUUID ())));
    // A business document is never correlated, but is always forwarded
    assertFalse (InboundOrchestrator.isSkipPrimaryForwarding (_createInboundTx ()));
  }

  @Test
  public void testSkippedUncorrelatedMlsEndsInNotForwarded ()
  {
    _overrideConfig (APConfigurationProperties.FORWARDING_MLS_SKIP_UNCORRELATED, "true");

    final IInboundTransaction aTx = _createUncorrelatedMls ();
    // A deliberate skip is not a forwarding failure
    assertTrue (InboundOrchestrator.forwardDocument (LOG_PREFIX, aTx).isSuccess ());

    final IInboundTransaction aUpdated = APJdbcMetaManager.getInboundTransactionMgr ().getByID (aTx.getID ());
    assertNotNull (aUpdated);
    assertSame (EInboundStatus.NOT_FORWARDED, aUpdated.getStatus ());
    assertTrue (aUpdated.getStatus ().isFinalState ());
    assertNotNull (aUpdated.getCompletedDT ());
    // Nothing was attempted
    assertEquals (0, aUpdated.getAttemptCount ());
    assertTrue (APJdbcMetaManager.getInboundForwardingAttemptMgr ().getByTransactionID (aTx.getID ()).isEmpty ());
  }

  @Test
  public void testRepeatedMlsCorrelationIsCompleted ()
  {
    final IOutboundTransactionManager aOutboundMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IInboundTransactionManager aInboundMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final OffsetDateTime aNow = APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ();
    final IOutboundTransaction aOutboundTx = _createOutboundTx ();
    final IInboundTransaction aMlsTx = _createUncorrelatedMls ();

    // The receiving instance stored the MLS status, but stopped before it linked the inbound MLS
    assertTrue (aOutboundMgr.updateMlsStatus (aOutboundTx.getID (),
                                              EMlsReceptionStatus.RECEIVED_AP,
                                              aNow,
                                              "mls-1",
                                              aMlsTx.getID ())
                            .isSuccess ());
    assertNull (aInboundMgr.getByID (aMlsTx.getID ()).getMlsCorrelatedOutboundTransactionID ());

    // Resumed: the correlation is completed again
    assertTrue (MlsHandler.handleIncomingMls (LOG_PREFIX,
                                              aOutboundTx.getSbdhInstanceID (),
                                              EPeppolMLSResponseCode.ACCEPTANCE,
                                              aNow,
                                              "mls-1",
                                              aMlsTx.getID ())
                          .isSuccess ());
    assertEquals (aOutboundTx.getID (), aInboundMgr.getByID (aMlsTx.getID ()).getMlsCorrelatedOutboundTransactionID ());

    // Another MLS for the same outbound transaction is still ignored
    final IInboundTransaction aLaterMlsTx = _createUncorrelatedMls ();
    assertTrue (MlsHandler.handleIncomingMls (LOG_PREFIX,
                                              aOutboundTx.getSbdhInstanceID (),
                                              EPeppolMLSResponseCode.REJECTION,
                                              aNow,
                                              "mls-2",
                                              aLaterMlsTx.getID ())
                          .isSuccess ());
    assertNull (aInboundMgr.getByID (aLaterMlsTx.getID ()).getMlsCorrelatedOutboundTransactionID ());
    final IOutboundTransaction aFinalTx = aOutboundMgr.getByID (aOutboundTx.getID ());
    assertEquals (EMlsReceptionStatus.RECEIVED_AP, aFinalTx.getMlsStatus ());
    assertEquals (aMlsTx.getID (), aFinalTx.getMlsInboundTransactionID ());
  }

  /**
   * Enable the MLS sending with the {@link MockControlledForwarder} as the primary forwarder. The
   * forwarder is created once per JVM, because {@link APCoreMetaManager#init()} reads the
   * forwarding configuration only then.
   * <p>
   * There is deliberately no Peppol stage, so the AS4 sending of a created MLS fails on the worker
   * thread. That is expected: the tests are only interested in the MLS claim and in the creation of
   * the MLS outbound transaction, which both happen before.
   * </p>
   *
   * @param eTrigger
   *        The MLS sending trigger to use. May not be <code>null</code>.
   */
  private void _enableMlsWithControlledForwarder (@NonNull final EMlsSendingTrigger eTrigger)
  {
    _overrideConfig (Map.of (APConfigurationProperties.FORWARDING_MODE,
                             EForwardingMode.SPI.getID (),
                             IDocumentForwarder.DEFAULT_CONFIG_KEY_PREFIX + APConfigurationProperties.FORWARDING_SPI_ID_SUFFIX,
                             MockControlledForwarder.ID,
                             APConfigurationProperties.MLS_SENDING_ENABLED,
                             "true",
                             APConfigurationProperties.MLS_SENDING_TRIGGER,
                             eTrigger.getID (),
                             APConfigurationProperties.PEPPOL_OWNER_SEATID,
                             "POP000003"));
    synchronized (InboundVerificationResultIntegrationTest.class)
    {
      if (!(APCoreMetaManager.getForwarder () instanceof MockControlledForwarder))
        APCoreMetaManager.init ();
    }
    assertTrue (APCoreMetaManager.getForwarder () instanceof MockControlledForwarder);
  }

  /**
   * Forward like the retry scheduler and the replay do, and wait until the positive MLS was handled.
   *
   * @param aTx
   *        The transaction to forward, as read from the database. May not be <code>null</code>.
   * @param eExpected
   *        The expected forwarding result. May not be <code>null</code>.
   * @return <code>true</code> if a positive MLS was dispatched at all.
   */
  private static boolean _forwardAndWaitForMls (@NonNull final IInboundTransaction aTx, @NonNull final ESuccess eExpected)
  {
    final Wrapper <CompletableFuture <Void>> aMlsCompletion = new Wrapper <> ();
    assertSame (eExpected, InboundOrchestrator.forwardDocumentAndSendMls (LOG_PREFIX, aTx, aMlsCompletion));
    final CompletableFuture <Void> aFuture = aMlsCompletion.get ();
    if (aFuture == null)
      return false;
    aFuture.join ();
    return true;
  }

  @NonNull
  private static IInboundTransaction _reRead (@NonNull final IInboundTransaction aTx)
  {
    final IInboundTransaction ret = APJdbcMetaManager.getInboundTransactionMgr ().getByID (aTx.getID ());
    assertNotNull (ret);
    return ret;
  }

  @NonNull
  private static OffsetDateTime _now ()
  {
    return APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ();
  }

  private static long _getOutboundCount ()
  {
    return APJdbcMetaManager.getOutboundTransactionMgr ().getTransactionCount ();
  }

  /**
   * Assert that exactly one positive MLS (AP) was created for the provided transaction.
   *
   * @param aTx
   *        The transaction. May not be <code>null</code>.
   * @param nOutboundCountBefore
   *        The number of outbound transactions before the MLS was created.
   * @return The ID of the MLS outbound transaction. Never <code>null</code>.
   */
  @NonNull
  private static String _assertOnePositiveMls (@NonNull final IInboundTransaction aTx, final long nOutboundCountBefore)
  {
    final IInboundTransaction aUpdated = _reRead (aTx);
    assertSame (EPeppolMLSResponseCode.ACCEPTANCE, aUpdated.getMlsResponseCode ());
    assertNotNull (aUpdated.getMlsOutboundTransactionID ());
    assertEquals (nOutboundCountBefore + 1, _getOutboundCount ());

    final IOutboundTransaction aMlsTx = APJdbcMetaManager.getOutboundTransactionMgr ()
                                                         .getByID (aUpdated.getMlsOutboundTransactionID ());
    assertNotNull (aMlsTx);
    assertSame (ETransactionType.MLS_RESPONSE, aMlsTx.getTransactionType ());
    return aUpdated.getMlsOutboundTransactionID ();
  }

  @Test
  public void testForwardingRetryAfterFailureSendsOnePositiveMls ()
  {
    _enableMlsWithControlledForwarder (EMlsSendingTrigger.AUTO);
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();

    // Accepted with a warning, so that the MLS carries a line response read back from the DB
    final VerificationIssue aWarning = VerificationIssue.businessRuleWarning ("W-1", "/Invoice", "Just a warning");
    assertSame (EContinue.CONTINUE,
                InboundOrchestrator.handleVerifierResult (LOG_PREFIX,
                                                          aTx,
                                                          VerifierResult.passed (VerificationOutcome.passed (new CommonsArrayList <> (aWarning)))));

    final long nOutboundCountBefore = _getOutboundCount ();

    // The first forwarding fails - no MLS yet
    MockControlledForwarder.queueResult (aTx.getID (), ForwardingResult.failure ("test", "C4 is down"));
    assertFalse (_forwardAndWaitForMls (_reRead (aTx), ESuccess.FAILURE));
    assertSame (EInboundStatus.FORWARD_FAILED, _reRead (aTx).getStatus ());
    assertNull (_reRead (aTx).getMlsResponseCode ());
    assertEquals (nOutboundCountBefore, _getOutboundCount ());

    // The retry scheduler picks it up once it is due
    aMgr.updateStatusAndRetry (aTx.getID (), EInboundStatus.FORWARD_FAILED, 1, _now ().minusMinutes (1), "C4 is down");
    assertTrue (aMgr.claimForRetry (aTx.getID ()).isSuccess ());
    assertTrue (_forwardAndWaitForMls (_reRead (aTx), ESuccess.SUCCESS));

    assertSame (EInboundStatus.FORWARDED, _reRead (aTx).getStatus ());
    assertEquals (2, MockControlledForwarder.getCallCount (aTx.getID ()));
    _assertOnePositiveMls (aTx, nOutboundCountBefore);
  }

  @Test
  public void testRecoveringForwardedByRetrySchedulerSendsOnePositiveMls ()
  {
    _enableMlsWithControlledForwarder (EMlsSendingTrigger.AUTO);
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();

    // The instance stopped while forwarding it
    aMgr.updateStatus (aTx.getID (), EInboundStatus.FORWARDING);
    assertTrue (aMgr.markAllInFlightAsRecovering (_now ().minusMinutes (1)) >= 1);
    assertSame (EInboundStatus.RECOVERING, _reRead (aTx).getStatus ());

    final long nOutboundCountBefore = _getOutboundCount ();
    assertTrue (aMgr.claimForRetry (aTx.getID ()).isSuccess ());
    assertTrue (_forwardAndWaitForMls (_reRead (aTx), ESuccess.SUCCESS));

    assertSame (EInboundStatus.FORWARDED, _reRead (aTx).getStatus ());
    _assertOnePositiveMls (aTx, nOutboundCountBefore);
  }

  @Test
  public void testReplayOfForwardFailedSendsOnePositiveMls ()
  {
    _enableMlsWithControlledForwarder (EMlsSendingTrigger.AUTO);
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();

    MockControlledForwarder.queueResult (aTx.getID (), ForwardingResult.failure ("test", "C4 is down"));
    assertFalse (_forwardAndWaitForMls (_reRead (aTx), ESuccess.FAILURE));

    // Not due yet - a replay does not wait for the backoff
    final long nOutboundCountBefore = _getOutboundCount ();
    assertTrue (aMgr.claimForReplay (aTx.getID (), EInboundStatus.FORWARD_FAILED).isSuccess ());
    assertTrue (_forwardAndWaitForMls (_reRead (aTx), ESuccess.SUCCESS));

    _assertOnePositiveMls (aTx, nOutboundCountBefore);
  }

  @Test
  public void testReplayOfForwardedSendsNoSecondMls ()
  {
    _enableMlsWithControlledForwarder (EMlsSendingTrigger.AUTO);
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();

    final long nOutboundCountBefore = _getOutboundCount ();
    assertTrue (_forwardAndWaitForMls (_reRead (aTx), ESuccess.SUCCESS));
    final String sMlsTxID = _assertOnePositiveMls (aTx, nOutboundCountBefore);

    // The replay forwards it again, but C2 was already answered
    assertTrue (aMgr.claimForReplay (aTx.getID (), EInboundStatus.FORWARDED).isSuccess ());
    assertTrue (_forwardAndWaitForMls (_reRead (aTx), ESuccess.SUCCESS));

    assertEquals (2, MockControlledForwarder.getCallCount (aTx.getID ()));
    assertEquals (sMlsTxID, _reRead (aTx).getMlsOutboundTransactionID ());
    assertEquals (nOutboundCountBefore + 1, _getOutboundCount ());
  }

  @Test
  public void testReplayOfPermanentlyFailedSendsNoSecondMls ()
  {
    _enableMlsWithControlledForwarder (EMlsSendingTrigger.AUTO);
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();

    // C2 was answered with AB when the forwarding failed permanently
    aMgr.updateStatusCompleted (aTx.getID (), EInboundStatus.PERMANENTLY_FAILED);
    assertTrue (aMgr.claimMlsResponseCode (aTx.getID (), EPeppolMLSResponseCode.ACKNOWLEDGING).isSuccess ());

    final long nOutboundCountBefore = _getOutboundCount ();
    assertTrue (aMgr.claimForReplay (aTx.getID (), EInboundStatus.PERMANENTLY_FAILED).isSuccess ());
    assertTrue (_forwardAndWaitForMls (_reRead (aTx), ESuccess.SUCCESS));

    assertSame (EInboundStatus.FORWARDED, _reRead (aTx).getStatus ());
    assertSame (EPeppolMLSResponseCode.ACKNOWLEDGING, _reRead (aTx).getMlsResponseCode ());
    assertEquals (nOutboundCountBefore, _getOutboundCount ());
  }

  @Test
  public void testRetryWithMlsTriggerApiSendsNoMls ()
  {
    _enableMlsWithControlledForwarder (EMlsSendingTrigger.API);
    final IInboundTransaction aTx = _createInboundTx ();

    final long nOutboundCountBefore = _getOutboundCount ();
    // Deferred to the Receiver Backend - and to the MLS watchdog, if it stays silent
    assertFalse (_forwardAndWaitForMls (_reRead (aTx), ESuccess.SUCCESS));

    assertSame (EInboundStatus.FORWARDED, _reRead (aTx).getStatus ());
    assertNull (_reRead (aTx).getMlsResponseCode ());
    assertEquals (nOutboundCountBefore, _getOutboundCount ());
  }

  @Test
  public void testRetryOfMlsDocumentSendsNoMls ()
  {
    _enableMlsWithControlledForwarder (EMlsSendingTrigger.AUTO);
    final IInboundTransaction aTx = _createUncorrelatedMls ();

    final long nOutboundCountBefore = _getOutboundCount ();
    // Never an MLS as the answer to an MLS
    assertFalse (_forwardAndWaitForMls (_reRead (aTx), ESuccess.SUCCESS));

    assertSame (EInboundStatus.FORWARDED, _reRead (aTx).getStatus ());
    assertNull (_reRead (aTx).getMlsResponseCode ());
    assertEquals (nOutboundCountBefore, _getOutboundCount ());
  }

  @Test
  public void testRetryOfRejectedButForwardedSendsNoPositiveMls ()
  {
    _enableMlsWithControlledForwarder (EMlsSendingTrigger.AUTO);
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();

    // Rejected, answered with RE and forwarded anyway because of rejection forwarding "retry"
    aMgr.updateVerificationResult (aTx.getID (), EVerificationResult.REJECTED, null);
    assertTrue (aMgr.claimMlsResponseCode (aTx.getID (), EPeppolMLSResponseCode.REJECTION).isSuccess ());
    MockControlledForwarder.queueResult (aTx.getID (), ForwardingResult.failure ("test", "C4 is down"));
    assertFalse (_forwardAndWaitForMls (_reRead (aTx), ESuccess.FAILURE));

    final long nOutboundCountBefore = _getOutboundCount ();
    aMgr.updateStatusAndRetry (aTx.getID (), EInboundStatus.FORWARD_FAILED, 1, _now ().minusMinutes (1), "C4 is down");
    assertTrue (aMgr.claimForRetry (aTx.getID ()).isSuccess ());
    assertFalse (_forwardAndWaitForMls (_reRead (aTx), ESuccess.SUCCESS));

    assertSame (EInboundStatus.FORWARDED, _reRead (aTx).getStatus ());
    assertSame (EPeppolMLSResponseCode.REJECTION, _reRead (aTx).getMlsResponseCode ());
    assertEquals (nOutboundCountBefore, _getOutboundCount ());
  }
}
