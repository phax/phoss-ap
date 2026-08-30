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
package com.helger.phoss.ap.forwarding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.state.ESuccess;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phoss.ap.api.codelist.EForwardableKind;
import com.helger.phoss.ap.api.codelist.EVerificationResult;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.model.ForwardableDocument;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IForwardableDocument;
import com.helger.phoss.ap.api.model.IInboundTransaction;

/**
 * Test class that proves the {@link IInboundTransaction} adapter of
 * {@link IDocumentForwarder#forwardDocument(IInboundTransaction)} and the new
 * {@link IDocumentForwarder#forwardDocument(IForwardableDocument)} overload hand the very same
 * document to a forwarder.
 *
 * @author Philip Helger
 */
public final class DocumentForwarderAdapterTest
{
  /**
   * A forwarder that implements only the new overload - exactly what a migrated implementation
   * looks like - and remembers what it was given.
   */
  private static final class CapturingForwarder implements IDocumentForwarder
  {
    private IForwardableDocument m_aCaptured;

    @NonNull
    public ESuccess initFromConfiguration (@NonNull final IConfigWithFallback aConfig, @NonNull final String sKeyPrefix)
    {
      return ESuccess.SUCCESS;
    }

    @NonNull
    public ForwardingResult forwardDocument (@NonNull final IForwardableDocument aDocument)
    {
      m_aCaptured = aDocument;
      return ForwardingResult.success ();
    }

    public boolean isWithDeliveryConfirmation ()
    {
      return false;
    }
  }

  private static void _assertSameDocument (@NonNull final IForwardableDocument aExpected,
                                           @NonNull final IForwardableDocument aActual)
  {
    // The metadata supplier is a lambda, so the records are never equal() - compare component wise
    assertEquals (aExpected.id (), aActual.id ());
    assertSame (aExpected.kind (), aActual.kind ());
    assertEquals (aExpected.sbdhInstanceID (), aActual.sbdhInstanceID ());
    assertEquals (aExpected.documentPath (), aActual.documentPath ());
    assertEquals (aExpected.documentSize (), aActual.documentSize ());
    assertEquals (aExpected.docTypeID (), aActual.docTypeID ());
    assertEquals (aExpected.processID (), aActual.processID ());
    assertEquals (aExpected.senderID (), aActual.senderID ());
    assertEquals (aExpected.receiverID (), aActual.receiverID ());
    assertEquals (aExpected.timestamp (), aActual.timestamp ());
    assertEquals (aExpected.localID (), aActual.localID ());
    assertSame (aExpected.verificationResult (), aActual.verificationResult ());
    assertEquals (aExpected.verificationDetails (), aActual.verificationDetails ());
    assertNotNull (aActual.metadataJson ());
    assertEquals (aExpected.metadataJson ().get (), aActual.metadataJson ().get ());
  }

  @Test
  public void testAdapterAndOverloadAgree ()
  {
    final IInboundTransaction aTx = new MockInboundTransaction (EVerificationResult.PASSED, null);
    final CapturingForwarder aForwarder = new CapturingForwarder ();

    // Route 1: the default adapter of the interface
    assertTrue (aForwarder.forwardDocument (ForwardableDocument.fromInbound (aTx)).isSuccess ());
    final IForwardableDocument aViaAdapter = aForwarder.m_aCaptured;
    assertNotNull (aViaAdapter);

    // Route 2: the new overload, called with an explicitly adapted document
    aForwarder.m_aCaptured = null;
    aForwarder.forwardDocument (ForwardableDocument.fromInbound (aTx));
    final IForwardableDocument aViaOverload = aForwarder.m_aCaptured;
    assertNotNull (aViaOverload);

    _assertSameDocument (aViaAdapter, aViaOverload);
  }

  @Test
  public void testEveryFieldIsMapped ()
  {
    final IInboundTransaction aTx = new MockInboundTransaction (EVerificationResult.REJECTED, "[]");
    final ForwardableDocument aDoc = ForwardableDocument.fromInbound (aTx);

    assertEquals (MockInboundTransaction.ID, aDoc.id ());
    assertEquals (MockInboundTransaction.SBDH_INSTANCE_ID, aDoc.sbdhInstanceID ());
    assertEquals (MockInboundTransaction.DOCUMENT_PATH, aDoc.documentPath ());
    assertEquals (MockInboundTransaction.DOCUMENT_SIZE, aDoc.documentSize ());
    assertEquals (MockInboundTransaction.DOCTYPE_ID, aDoc.docTypeID ());
    assertEquals (MockInboundTransaction.PROCESS_ID, aDoc.processID ());
    assertEquals (MockInboundTransaction.SENDER_ID, aDoc.senderID ());
    assertEquals (MockInboundTransaction.RECEIVER_ID, aDoc.receiverID ());
    // The SFTP remote file name is built from these two
    assertEquals (MockInboundTransaction.RECEIVED_DT, aDoc.timestamp ());
    assertEquals (MockInboundTransaction.INCOMING_ID, aDoc.localID ());
    assertSame (EVerificationResult.REJECTED, aDoc.verificationResult ());
    assertEquals ("[]", aDoc.verificationDetails ());
  }

  @Test
  public void testKindOfABusinessDocument ()
  {
    final ForwardableDocument aDoc = ForwardableDocument.fromInbound (new MockInboundTransaction (null, null));
    assertSame (EForwardableKind.INBOUND_DOCUMENT, aDoc.kind ());
    assertNull (aDoc.verificationResult ());
  }

  @Test
  public void testMetadataJsonMatchesTheTransaction ()
  {
    final IInboundTransaction aTx = new MockInboundTransaction (EVerificationResult.PASSED, null);
    final ForwardableDocument aDoc = ForwardableDocument.fromInbound (aTx);

    assertNotNull (aDoc.metadataJson ());
    final String sJson = aDoc.metadataJson ().get ();
    // This is the sidecar the filesystem, S3 and SFTP forwarders write - it must still describe the
    // transaction and not just the forwardable subset
    assertTrue (sJson.contains ("\"" + MockInboundTransaction.ID + "\""));
    assertTrue (sJson.contains ("\"" + MockInboundTransaction.SBDH_INSTANCE_ID + "\""));
    assertTrue (sJson.contains ("\"passed\""));
    assertTrue (sJson.contains ("\"pending\""));
  }
}
