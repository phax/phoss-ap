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
package com.helger.phoss.ap.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Test class for class {@link OutboundS3SubmitRequest}.
 *
 * @author Philip Helger
 */
public final class OutboundS3SubmitRequestTest
{
  @Test
  public void testDefaultConstructor ()
  {
    final OutboundS3SubmitRequest a = new OutboundS3SubmitRequest ();
    assertNull (a.getSenderID ());
    assertNull (a.getReceiverID ());
    assertNull (a.getDocTypeID ());
    assertNull (a.getProcessID ());
    assertNull (a.getC1CountryCode ());
    assertNull (a.getS3Bucket ());
    assertNull (a.getS3Key ());
    assertNull (a.getSbdhInstanceID ());
    assertNull (a.getMlsTo ());
    assertNull (a.getSbdhStandard ());
    assertNull (a.getSbdhTypeVersion ());
    assertNull (a.getSbdhType ());
    assertNull (a.getPayloadMimeType ());
    assertNull (a.getCustom1 ());
    assertNull (a.getCustom2 ());
    assertNull (a.getCustom3 ());
  }

  @Test
  public void testSetters ()
  {
    final OutboundS3SubmitRequest a = new OutboundS3SubmitRequest ();
    a.setSenderID ("sender1");
    a.setReceiverID ("receiver1");
    a.setDocTypeID ("docType1");
    a.setProcessID ("process1");
    a.setC1CountryCode ("AT");
    a.setS3Bucket ("my-bucket");
    a.setS3Key ("my-key");
    a.setSbdhInstanceID ("sbdh-1");
    a.setMlsTo ("mls-to-1");
    a.setSbdhStandard ("std1");
    a.setSbdhTypeVersion ("1.0");
    a.setSbdhType ("type1");
    a.setPayloadMimeType ("application/pdf");
    a.setCustom1 ("c1");
    a.setCustom2 ("c2");
    a.setCustom3 ("c3");

    assertEquals ("sender1", a.getSenderID ());
    assertEquals ("receiver1", a.getReceiverID ());
    assertEquals ("docType1", a.getDocTypeID ());
    assertEquals ("process1", a.getProcessID ());
    assertEquals ("AT", a.getC1CountryCode ());
    assertEquals ("my-bucket", a.getS3Bucket ());
    assertEquals ("my-key", a.getS3Key ());
    assertEquals ("sbdh-1", a.getSbdhInstanceID ());
    assertEquals ("mls-to-1", a.getMlsTo ());
    assertEquals ("std1", a.getSbdhStandard ());
    assertEquals ("1.0", a.getSbdhTypeVersion ());
    assertEquals ("type1", a.getSbdhType ());
    assertEquals ("application/pdf", a.getPayloadMimeType ());
    assertEquals ("c1", a.getCustom1 ());
    assertEquals ("c2", a.getCustom2 ());
    assertEquals ("c3", a.getCustom3 ());
  }
}
