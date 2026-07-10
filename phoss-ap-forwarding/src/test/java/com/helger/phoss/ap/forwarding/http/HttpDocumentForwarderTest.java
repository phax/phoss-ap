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
package com.helger.phoss.ap.forwarding.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.json.IJsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.codelist.EForwardingMode;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.scope.mock.ScopeTestRule;
import com.sun.net.httpserver.HttpServer;

/**
 * Test class for class {@link HttpDocumentForwarder}.
 *
 * @author Philip Helger
 */
public final class HttpDocumentForwarderTest
{
  @Rule
  public final TemporaryFolder m_aTempFolder = new TemporaryFolder ();

  @Rule
  public final ScopeTestRule m_aScopeRule = new ScopeTestRule ();

  @NonNull
  private static IConfigWithFallback _createConfig ()
  {
    return new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ());
  }

  @NonNull
  private static IInboundTransaction _createTransaction (@NonNull final String sDocumentPath, final long nDocumentSize)
  {
    return new IInboundTransaction ()
    {
      public String getID () { return "tx-1"; }
      public String getIncomingID () { return "incoming-1"; }
      public String getC2SeatID () { return "C2-SEAT"; }
      public String getC3SeatID () { return "C3-SEAT"; }
      public String getSigningCertCN () { return "signing-cn"; }
      public String getSenderID () { return "iso6523-actorid-upis::sender"; }
      public String getReceiverID () { return "iso6523-actorid-upis::receiver"; }
      public String getDocTypeID () { return "doctype"; }
      public String getProcessID () { return "process"; }
      public String getDocumentPath () { return sDocumentPath; }
      public long getDocumentSize () { return nDocumentSize; }
      public String getDocumentHash () { return "hash"; }
      public String getAS4MessageID () { return "as4-message"; }
      public OffsetDateTime getAS4Timestamp () { return OffsetDateTime.parse ("2026-01-02T03:04:05Z"); }
      public String getSbdhInstanceID () { return "sbdh-1"; }
      public String getC1CountryCode () { return "DE"; }
      public String getC4CountryCode () { return "AT"; }
      public boolean isDuplicateAS4 () { return false; }
      public boolean isDuplicateSBDH () { return false; }
      public EInboundStatus getStatus () { return EInboundStatus.RECEIVED; }
      public int getAttemptCount () { return 1; }
      public OffsetDateTime getReceivedDT () { return OffsetDateTime.parse ("2026-01-02T03:05:05Z"); }
      public OffsetDateTime getCompletedDT () { return null; }
      public EReportingStatus getReportingStatus () { return EReportingStatus.PENDING; }
      public OffsetDateTime getNextRetryDT () { return null; }
      public String getErrorDetails () { return null; }
      public String getMlsTo () { return null; }
      public EPeppolMLSType getMlsType () { return EPeppolMLSType.ALWAYS_SEND; }
      public EPeppolMLSResponseCode getMlsResponseCode () { return null; }
      public String getMlsOutboundTransactionID () { return null; }
    };
  }

  private static HttpServer _startServer (@NonNull final AtomicReference <String> aContentType,
                                          @NonNull final AtomicReference <String> aBody) throws IOException
  {
    final HttpServer aServer = HttpServer.create (new InetSocketAddress ("127.0.0.1", 0), 0);
    aServer.createContext ("/forward", aExchange -> {
      aContentType.set (aExchange.getRequestHeaders ().getFirst ("Content-Type"));
      aBody.set (new String (aExchange.getRequestBody ().readAllBytes (), StandardCharsets.UTF_8));
      aExchange.sendResponseHeaders (204, -1);
      aExchange.close ();
    });
    aServer.start ();
    return aServer;
  }

  private void _runForwardingTest (final boolean bJsonEnabled,
                                   @NonNull final AtomicReference <String> aContentType,
                                   @NonNull final AtomicReference <String> aBody) throws Exception
  {
    final byte [] aXML = "<Invoice><ID>123</ID></Invoice>".getBytes (StandardCharsets.UTF_8);
    final var aXMLFile = m_aTempFolder.newFile ("document.xml").toPath ();
    Files.write (aXMLFile, aXML);

    System.setProperty (APConfigurationProperties.STORAGE_INBOUND_PATH, m_aTempFolder.newFolder ("inbound").getAbsolutePath ());
    System.setProperty (APConfigurationProperties.STORAGE_OUTBOUND_PATH, m_aTempFolder.newFolder ("outbound").getAbsolutePath ());

    final HttpServer aServer = _startServer (aContentType, aBody);
    try
    {
      final int nPort = aServer.getAddress ().getPort ();
      System.setProperty ("forwarding.http.endpoint", "http://127.0.0.1:" + nPort + "/forward");
      System.setProperty ("forwarding.http.json.enabled", Boolean.toString (bJsonEnabled));

      final HttpDocumentForwarder aForwarder = new HttpDocumentForwarder (EForwardingMode.HTTP_POST_ASYNC);
      assertTrue (aForwarder.initFromConfiguration (_createConfig (), "forwarding.").isSuccess ());
      assertTrue (aForwarder.forwardDocument (_createTransaction (aXMLFile.toString (), aXML.length)).isSuccess ());
    }
    finally
    {
      aServer.stop (0);
      System.clearProperty ("forwarding.http.endpoint");
      System.clearProperty ("forwarding.http.json.enabled");
      System.clearProperty (APConfigurationProperties.STORAGE_INBOUND_PATH);
      System.clearProperty (APConfigurationProperties.STORAGE_OUTBOUND_PATH);
    }
  }

  @Test
  public void testXmlForwardingIsDefault () throws Exception
  {
    final AtomicReference <String> aContentType = new AtomicReference <> ();
    final AtomicReference <String> aBody = new AtomicReference <> ();

    _runForwardingTest (false, aContentType, aBody);

    assertNotNull (aContentType.get ());
    assertTrue (aContentType.get ().startsWith ("application/xml"));
    assertEquals ("<Invoice><ID>123</ID></Invoice>", aBody.get ());
  }

  @Test
  public void testJsonForwardingIncludesXmlContent () throws Exception
  {
    final AtomicReference <String> aContentType = new AtomicReference <> ();
    final AtomicReference <String> aBody = new AtomicReference <> ();

    _runForwardingTest (true, aContentType, aBody);

    assertNotNull (aContentType.get ());
    assertTrue (aContentType.get ().startsWith ("application/json"));

    final IJsonObject aJson = JsonReader.builder ().source (aBody.get ()).readAsObject ();
    assertNotNull (aJson);
    assertEquals ("<Invoice><ID>123</ID></Invoice>", aJson.getAsString ("xmlContent"));
    assertTrue (aBody.get ().contains ("\"transaction\""));
    assertTrue (aBody.get ().contains ("\"tx-1\""));
  }
}
