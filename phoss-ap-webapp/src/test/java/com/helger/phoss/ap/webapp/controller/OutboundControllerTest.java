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
package com.helger.phoss.ap.webapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.factory.PeppolLaxIdentifierFactory;

/**
 * Test class for {@link OutboundController}.
 *
 * @author Philip Helger
 */
final class OutboundControllerTest
{
  /**
   * France Factur-X - the syntax specific ID "urn:peppol:doctype:pdf+xml" is not XML. The SBDH
   * values used below are the ones from "Peppol - France - Solution Architecture 1.3.0" section
   * 6.1.1 - note that the TypeVersion is "0" and not the "D22B" from the document type ID.
   */
  private static final String DOCTYPE_PDF = "busdox-docid-qns::urn:peppol:doctype:pdf+xml##urn:cen.eu:en16931:2017#conformant#urn:peppol:france:billing:Factur-X:1.0::D22B";
  /** Regular Peppol BIS Billing UBL Invoice - the syntax specific ID is XML */
  private static final String DOCTYPE_XML = "busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1";

  @NonNull
  private static IDocumentTypeIdentifier _docType (@Nullable final String s)
  {
    final IDocumentTypeIdentifier ret = PeppolIdentifierFactory.INSTANCE.parseDocumentTypeIdentifier (s);
    assertNotNull (ret);
    return ret;
  }

  private static void _assertBadRequest (@NonNull final ResponseEntity <String> aResponse,
                                         @NonNull final String sExpectedParamName)
  {
    assertNotNull (aResponse);
    assertEquals (HttpStatus.BAD_REQUEST, aResponse.getStatusCode ());
    final String sBody = aResponse.getBody ();
    assertNotNull (sBody);
    assertTrue (sBody.contains ("'" + sExpectedParamName + "'"), sBody);
  }

  @Test
  void testXMLDocTypeNeedsNoOverrides ()
  {
    assertNull (OutboundController.validateNonXMLPayloadParams (_docType (DOCTYPE_XML), null, null, null, null));
  }

  @Test
  void testXMLDocTypeWithPayloadMimeTypeIsAccepted ()
  {
    // Only logs a warning, but is not rejected
    assertNull (OutboundController.validateNonXMLPayloadParams (_docType (DOCTYPE_XML),
                                                                null,
                                                                null,
                                                                null,
                                                                "application/pdf"));
  }

  @Test
  void testNonXMLDocTypeWithoutAnyOverride ()
  {
    _assertBadRequest (OutboundController.validateNonXMLPayloadParams (_docType (DOCTYPE_PDF), null, null, null, null),
                       "payloadMimeType");
  }

  @Test
  void testNonXMLDocTypeWithoutSbdhStandard ()
  {
    _assertBadRequest (OutboundController.validateNonXMLPayloadParams (_docType (DOCTYPE_PDF),
                                                                       null,
                                                                       "0",
                                                                       "Invoice",
                                                                       "application/pdf"), "sbdhStandard");
  }

  @Test
  void testNonXMLDocTypeWithoutSbdhTypeVersion ()
  {
    _assertBadRequest (OutboundController.validateNonXMLPayloadParams (_docType (DOCTYPE_PDF),
                                                                       "urn:peppol:doctype:pdf+xml",
                                                                       null,
                                                                       "Invoice",
                                                                       "application/pdf"), "sbdhTypeVersion");
  }

  @Test
  void testNonXMLDocTypeWithoutSbdhType ()
  {
    _assertBadRequest (OutboundController.validateNonXMLPayloadParams (_docType (DOCTYPE_PDF),
                                                                       "urn:peppol:doctype:pdf+xml",
                                                                       "0",
                                                                       null,
                                                                       "application/pdf"), "sbdhType");
  }

  @Test
  void testNonXMLDocTypeComplete ()
  {
    assertNull (OutboundController.validateNonXMLPayloadParams (_docType (DOCTYPE_PDF),
                                                                "urn:peppol:doctype:pdf+xml",
                                                                "0",
                                                                "Invoice",
                                                                "application/pdf"));
  }

  @Test
  void testNonPeppolDocTypeLayoutIsIgnored ()
  {
    // Only reachable in "lax" identifier mode. No "##" present, so the parts cannot be
    // extracted - the check must silently pass instead of failing the submission
    final IDocumentTypeIdentifier aDocTypeID = PeppolLaxIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("SomethingCompletelyDifferent");
    assertNotNull (aDocTypeID);
    assertNull (OutboundController.validateNonXMLPayloadParams (aDocTypeID, null, null, null, null));
  }

  @Test
  void testMlsType ()
  {
    assertNull (OutboundController.validateMlsType (null));
    assertNull (OutboundController.validateMlsType (""));
    assertNull (OutboundController.validateMlsType ("FAILURE_ONLY"));
    assertNull (OutboundController.validateMlsType ("ALWAYS_SEND"));
    _assertBadRequest (OutboundController.validateMlsType ("NEVER"), "mlsType");
  }

  @Test
  void testAS4MessageIDInvalidSyntax ()
  {
    // Absent means a random one is used
    assertNull (OutboundController.validateAS4MessageID (null));
    assertNull (OutboundController.validateAS4MessageID (""));

    // The syntax is checked before any database lookup
    final ResponseEntity <String> aResponse = OutboundController.validateAS4MessageID ("no-at-sign");
    assertNotNull (aResponse);
    assertEquals (HttpStatus.BAD_REQUEST, aResponse.getStatusCode ());
  }
}
