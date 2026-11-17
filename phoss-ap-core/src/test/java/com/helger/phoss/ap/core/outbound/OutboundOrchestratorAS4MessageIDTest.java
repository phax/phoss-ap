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
package com.helger.phoss.ap.core.outbound;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.helger.phase4.model.message.MessageHelperMethods;

/**
 * Test class for {@link OutboundOrchestrator#getAS4MessageIDSyntaxError(String)}.
 *
 * @author Philip Helger
 */
public final class OutboundOrchestratorAS4MessageIDTest
{
  @Test
  public void testValid ()
  {
    assertNull (OutboundOrchestrator.getAS4MessageIDSyntaxError ("326f1827-c7fe@backend.example.org"));
    assertNull (OutboundOrchestrator.getAS4MessageIDSyntaxError (MessageHelperMethods.createRandomMessageID ()));
  }

  @Test
  public void testInvalidSyntax ()
  {
    assertNotNull (OutboundOrchestrator.getAS4MessageIDSyntaxError (""));
    assertNotNull (OutboundOrchestrator.getAS4MessageIDSyntaxError ("no-at-sign"));
    assertNotNull (OutboundOrchestrator.getAS4MessageIDSyntaxError ("@backend.example.org"));
    assertNotNull (OutboundOrchestrator.getAS4MessageIDSyntaxError ("id@"));
    assertNotNull (OutboundOrchestrator.getAS4MessageIDSyntaxError ("a@b@c"));
    assertNotNull (OutboundOrchestrator.getAS4MessageIDSyntaxError ("id with space@backend.example.org"));
    // Not trimmed
    assertNotNull (OutboundOrchestrator.getAS4MessageIDSyntaxError (" id@backend.example.org"));
  }

  @Test
  public void testMaxLength ()
  {
    final String sDomain = "@backend.example.org";
    final String sMaxLength = "x".repeat (OutboundOrchestrator.REQUESTED_AS4_MESSAGE_ID_MAX_LENGTH - sDomain.length ()) +
                              sDomain;
    assertNull (OutboundOrchestrator.getAS4MessageIDSyntaxError (sMaxLength));
    assertNotNull (OutboundOrchestrator.getAS4MessageIDSyntaxError ("x" + sMaxLength));
  }
}
