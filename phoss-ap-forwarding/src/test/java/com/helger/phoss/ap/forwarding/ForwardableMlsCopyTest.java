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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.helger.phoss.ap.api.codelist.EForwardableKind;
import com.helger.phoss.ap.api.model.ForwardableDocument;

/**
 * Test class for the adaptation of a self-generated MLS to a forwardable document, i.e. the copy of
 * the outgoing MLS that is handed to C4.
 *
 * @author Philip Helger
 */
public final class ForwardableMlsCopyTest
{
  @Test
  public void testEveryFieldIsMapped ()
  {
    final ForwardableDocument aDoc = ForwardableDocument.fromOutboundMlsCopy (new MockOutboundMlsTransaction ());

    assertEquals (MockOutboundMlsTransaction.ID, aDoc.id ());
    assertEquals (MockOutboundMlsTransaction.SBDH_INSTANCE_ID, aDoc.sbdhInstanceID ());
    assertEquals (MockOutboundMlsTransaction.DOCUMENT_PATH, aDoc.documentPath ());
    assertEquals (MockOutboundMlsTransaction.DOCUMENT_SIZE, aDoc.documentSize ());
    assertEquals (MockOutboundMlsTransaction.DOCTYPE_ID, aDoc.docTypeID ());
    assertEquals (MockOutboundMlsTransaction.PROCESS_ID, aDoc.processID ());
    assertEquals (MockOutboundMlsTransaction.SENDER_ID, aDoc.senderID ());
    assertEquals (MockOutboundMlsTransaction.RECEIVER_ID, aDoc.receiverID ());
    assertEquals (MockOutboundMlsTransaction.CREATED_DT, aDoc.timestamp ());
    // There is no phase4 Incoming ID for a self-generated document
    assertEquals (MockOutboundMlsTransaction.ID, aDoc.localID ());
  }

  @Test
  public void testKindIsOutboundMlsCopy ()
  {
    final ForwardableDocument aDoc = ForwardableDocument.fromOutboundMlsCopy (new MockOutboundMlsTransaction ());
    // This is the whole point: C4 can tell an MLS we sent from an MLS we received
    assertSame (EForwardableKind.OUTBOUND_MLS_COPY, aDoc.kind ());
    assertFalse (aDoc.kind ().isInbound ());
  }

  @Test
  public void testNoVerificationAndNoSidecar ()
  {
    final ForwardableDocument aDoc = ForwardableDocument.fromOutboundMlsCopy (new MockOutboundMlsTransaction ());
    // A self-generated document is never verified
    assertNull (aDoc.verificationResult ());
    assertNull (aDoc.verificationDetails ());
    // OutboundTransactionResponse has no JSON rendering, so no metadata sidecar is offered
    assertNull (aDoc.metadataJson ());
  }
}
