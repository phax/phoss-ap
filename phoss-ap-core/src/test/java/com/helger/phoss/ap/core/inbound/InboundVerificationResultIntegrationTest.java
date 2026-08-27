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

import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import com.helger.base.state.EContinue;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EVerificationRejectionForwarding;
import com.helger.phoss.ap.api.codelist.EVerificationResult;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.VerificationIssue;
import com.helger.phoss.ap.api.model.VerificationOutcome;
import com.helger.phoss.ap.api.model.VerifierResult;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Integration tests for the persisted verification verdict of {@link InboundOrchestrator}. Unlike
 * {@link InboundOrchestratorVerifierTest}, which only exercises the pure verifier evaluation, these
 * tests need the database, because the verdict is written to the inbound transaction.
 *
 * @author Philip Helger
 */
public final class InboundVerificationResultIntegrationTest
{
  @ClassRule
  public static final ScopeTestRule RULE = new ScopeTestRule ();

  private static final String LOG_PREFIX = "[Test] ";

  private IConfigWithFallback m_aOldConfig;

  /**
   * Replace the global configuration with one that adds the provided overrides on top of the
   * regular test configuration.
   *
   * @param sKey
   *        The configuration key to set. May not be <code>null</code>.
   * @param sValue
   *        The configuration value to set. May not be <code>null</code>.
   */
  private void _overrideConfig (@NonNull final String sKey, @NonNull final String sValue)
  {
    final MultiConfigurationValueProvider aVP = com.helger.config.ConfigFactory.createDefaultValueProvider ();
    // Highest priority wins over the values from application.properties
    aVP.addConfigurationSource (new ConfigurationSourceFunction (k -> sKey.equals (k) ? sValue : null),
                                Integer.MAX_VALUE);
    m_aOldConfig = APConfigProvider.setConfig (new ConfigWithFallback (aVP));
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
  private static IInboundTransaction _createInboundTx ()
  {
    final String sID = "test-" + UUID.randomUUID ();
    final String sTxID = APJdbcMetaManager.getInboundTransactionMgr ()
                                          .create (sID,
                                                   "POP000001",
                                                   "POP000002",
                                                   "CN=TestCert",
                                                   "iso6523-actorid-upis::9915:sender",
                                                   "iso6523-actorid-upis::9915:receiver",
                                                   "busdox-docid-qns::urn:test:invoice",
                                                   "cenbii-procid-ubl::urn:test:process",
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
    final IInboundTransaction aTx = APJdbcMetaManager.getInboundTransactionMgr ().getByID (sTxID);
    assertNotNull (aTx);
    return aTx;
  }

  @Test
  public void testUnavailableWithFailModeOpenIsRecordedAsUnverified ()
  {
    _overrideConfig ("verification.verifier-fail-mode", "open");

    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundTransaction aTx = _createInboundTx ();
    assertNull (aTx.getVerificationResult ());

    final VerifierResult aVR = new VerifierResult (VerificationOutcome.serviceUnavailable ("Connection refused"),
                                                   "Scanner");
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

    final VerifierResult aVR = new VerifierResult (VerificationOutcome.serviceUnavailable ("Connection refused"),
                                                   "Scanner");
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

  @NonNull
  private static VerifierResult _createRejection ()
  {
    final VerificationIssue aError = VerificationIssue.businessRuleViolation ("BR-1",
                                                                             "/Invoice",
                                                                             "The document is invalid");
    return new VerifierResult (VerificationOutcome.rejected ("Document verification failed",
                                                             new CommonsArrayList <> (aError)),
                               "Scanner");
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
    final IInboundTransaction aRejected = _rejectWithMode (EVerificationRejectionForwarding.RETRY,
                                                           EContinue.CONTINUE);
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
}
