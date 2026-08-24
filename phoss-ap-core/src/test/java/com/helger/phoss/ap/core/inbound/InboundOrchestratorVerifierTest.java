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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.model.MlsOutcomeIssue;
import com.helger.phoss.ap.api.model.VerificationOutcome;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.core.inbound.InboundOrchestrator.VerifierResult;

/**
 * Test class for the inbound document verifier evaluation of {@link InboundOrchestrator}.
 *
 * @author Philip Helger
 */
public final class InboundOrchestratorVerifierTest
{
  private static final String LOG_PREFIX = "[Test] ";
  private static final String DOC_PATH = "/tmp/whatever.sbd";
  private static final IDocumentTypeIdentifier DOCTYPE_ID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
  private static final IProcessIdentifier PROCESS_ID = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");

  /**
   * A verifier that always returns the provided outcome and remembers if it was called.
   */
  private static final class MockVerifier implements IInboundDocumentVerifierSPI
  {
    private final String m_sName;
    private final VerificationOutcome m_aOutcome;
    private boolean m_bCalled = false;

    MockVerifier (@NonNull @Nonempty final String sName, @NonNull final VerificationOutcome aOutcome)
    {
      m_sName = sName;
      m_aOutcome = aOutcome;
    }

    @Override
    @NonNull
    @Nonempty
    public String getVerifierName ()
    {
      return m_sName;
    }

    @NonNull
    public VerificationOutcome verifyInboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                                      @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                      @NonNull final IProcessIdentifier aProcessID)
    {
      m_bCalled = true;
      return m_aOutcome;
    }
  }

  @NonNull
  private static VerifierResult _run (@NonNull final List <MockVerifier> aVerifiers)
  {
    return InboundOrchestrator.runInboundVerifiers (LOG_PREFIX, aVerifiers, DOC_PATH, DOCTYPE_ID, PROCESS_ID);
  }

  @Test
  public void testAllAccepting ()
  {
    final VerifierResult aVR = _run (new CommonsArrayList <> (new MockVerifier ("V1", VerificationOutcome.passed ()),
                                                              new MockVerifier ("V2", VerificationOutcome.passed ())));
    assertTrue (aVR.outcome ().isPassed ());
    assertNull (aVR.outcome ().getMlsOutcome ());
    assertNull (aVR.verifierName ());
  }

  @Test
  public void testDefaultVerifierName ()
  {
    // The default name of a verifier is its local class name
    final IInboundDocumentVerifierSPI aVerifier = (sDocumentPath, aDocTypeID, aProcessID) -> VerificationOutcome
                                                                                                                .serviceUnavailable ("down");
    final VerifierResult aVR = InboundOrchestrator.runInboundVerifiers (LOG_PREFIX,
                                                                        new CommonsArrayList <> (aVerifier),
                                                                        DOC_PATH,
                                                                        DOCTYPE_ID,
                                                                        PROCESS_ID);
    assertTrue (aVR.outcome ().isServiceUnavailable ());
    assertTrue (aVR.verifierName ().contains ("InboundOrchestratorVerifierTest"));
  }

  @Test
  public void testRejectionWins ()
  {
    // The rejection of the second verifier must win over the unavailability of the first one
    final MlsOutcome aMls = MlsOutcome.rejection ("Malware found",
                                                  MlsOutcomeIssue.businessRuleViolation ("NA", "Virus found"));
    final MockVerifier aUnavailable = new MockVerifier ("Scanner",
                                                        VerificationOutcome.serviceUnavailable ("Connection refused"));
    final MockVerifier aRejecting = new MockVerifier ("Validator", VerificationOutcome.rejected (aMls));
    final VerifierResult aVR = _run (new CommonsArrayList <> (aUnavailable, aRejecting));
    assertTrue (aVR.outcome ().isRejected ());
    assertSame (aMls, aVR.outcome ().getMlsOutcome ());
    assertEquals ("Validator", aVR.verifierName ());
  }

  @Test
  public void testRejectionStopsEvaluation ()
  {
    final MockVerifier aRejecting = new MockVerifier ("Validator", VerificationOutcome.rejected ("Invalid document"));
    final MockVerifier aNeverCalled = new MockVerifier ("Scanner", VerificationOutcome.passed ());
    final VerifierResult aVR = _run (new CommonsArrayList <> (aRejecting, aNeverCalled));
    assertTrue (aVR.outcome ().isRejected ());
    // No MLS details were provided - they are created by the orchestrator on demand
    assertNull (aVR.outcome ().getMlsOutcome ());
    assertEquals ("Invalid document", aVR.outcome ().getMessage ());
    assertFalse (aNeverCalled.m_bCalled);
  }

  @Test
  public void testUnavailableKeepsEvaluating ()
  {
    final MockVerifier aVerifier1 = new MockVerifier ("Scanner",
                                                      VerificationOutcome.serviceUnavailable ("Connection refused"));
    final MockVerifier aVerifier2 = new MockVerifier ("Validator", VerificationOutcome.passed ());
    final VerifierResult aVR = _run (new CommonsArrayList <> (aVerifier1, aVerifier2));
    assertTrue (aVR.outcome ().isServiceUnavailable ());
    assertEquals ("Connection refused", aVR.outcome ().getMessage ());
    assertEquals ("Scanner", aVR.verifierName ());
    // The remaining verifiers must still be evaluated
    assertTrue (aVerifier2.m_bCalled);
  }

  @Test
  public void testNullOutcomeIsTreatedAsPassed ()
  {
    // The SPI contract demands a non-null outcome, but it is not enforced at runtime - such a
    // verifier may not abort the processing of the document
    final IInboundDocumentVerifierSPI aBroken = (sDocumentPath, aDocTypeID, aProcessID) -> null;
    final MockVerifier aVerifier2 = new MockVerifier ("Validator", VerificationOutcome.passed ());
    final ICommonsList <IInboundDocumentVerifierSPI> aVerifiers = new CommonsArrayList <> (aBroken, aVerifier2);
    final VerifierResult aVR = InboundOrchestrator.runInboundVerifiers (LOG_PREFIX,
                                                                        aVerifiers,
                                                                        DOC_PATH,
                                                                        DOCTYPE_ID,
                                                                        PROCESS_ID);
    assertTrue (aVR.outcome ().isPassed ());
    assertNull (aVR.verifierName ());
    // The remaining verifiers must still be evaluated
    assertTrue (aVerifier2.m_bCalled);
  }

  @Test
  public void testFirstUnavailableWins ()
  {
    final MockVerifier aVerifier1 = new MockVerifier ("First", VerificationOutcome.serviceUnavailable ("down"));
    final MockVerifier aVerifier2 = new MockVerifier ("Second", VerificationOutcome.serviceUnavailable ("down"));
    final VerifierResult aVR = _run (new CommonsArrayList <> (aVerifier1, aVerifier2));
    assertTrue (aVR.outcome ().isServiceUnavailable ());
    assertEquals ("First", aVR.verifierName ());
  }
}
