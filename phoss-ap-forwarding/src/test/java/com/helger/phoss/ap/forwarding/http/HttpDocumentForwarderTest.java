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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.base.codec.base64.Base64;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.json.IJsonArray;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.codelist.EForwardingMode;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.EVerificationResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;

/**
 * Test class for class {@link HttpDocumentForwarder}, focusing on the verification headers.
 *
 * @author Philip Helger
 */
public final class HttpDocumentForwarderTest
{
  private static final String HEADER_RESULT = "X-Verification-Result";
  private static final String HEADER_DETAILS = "X-Verification-Details";
  private static final String HEADER_TRUNCATED = "X-Verification-Details-Truncated";
  private static final String ENDPOINT_URL = "http://localhost:8888/forwarding";
  private static final int MAX_DETAILS_LENGTH = 4096;

  /**
   * A minimal inbound transaction that only carries what the verification headers need.
   */
  private static final class MockInboundTransaction implements IInboundTransaction
  {
    private final EVerificationResult m_eVerificationResult;
    private final String m_sVerificationDetails;

    MockInboundTransaction (@Nullable final EVerificationResult eVerificationResult,
                            @Nullable final String sVerificationDetails)
    {
      m_eVerificationResult = eVerificationResult;
      m_sVerificationDetails = sVerificationDetails;
    }

    @NonNull
    public String getID ()
    {
      return "tx-1";
    }

    @Nullable
    public String getIncomingID ()
    {
      return null;
    }

    @Nullable
    public String getC2SeatID ()
    {
      return null;
    }

    @Nullable
    public String getC3SeatID ()
    {
      return null;
    }

    @Nullable
    public String getSigningCertCN ()
    {
      return null;
    }

    @NonNull
    public String getSenderID ()
    {
      return "iso6523-actorid-upis::9915:sender";
    }

    @NonNull
    public String getReceiverID ()
    {
      return "iso6523-actorid-upis::9915:receiver";
    }

    @NonNull
    public String getDocTypeID ()
    {
      return "busdox-docid-qns::doctype";
    }

    @NonNull
    public String getProcessID ()
    {
      return "cenbii-procid-ubl::process";
    }

    @NonNull
    public String getDocumentPath ()
    {
      return "doc/tx-1.xml";
    }

    public long getDocumentSize ()
    {
      return 0;
    }

    @Nullable
    public String getDocumentHash ()
    {
      return null;
    }

    @Nullable
    public String getAS4MessageID ()
    {
      return null;
    }

    @Nullable
    public OffsetDateTime getAS4Timestamp ()
    {
      return null;
    }

    @NonNull
    public String getSbdhInstanceID ()
    {
      return "sbdh-1";
    }

    @Nullable
    public String getC1CountryCode ()
    {
      return null;
    }

    @Nullable
    public String getC4CountryCode ()
    {
      return null;
    }

    public boolean isDuplicateAS4 ()
    {
      return false;
    }

    public boolean isDuplicateSBDH ()
    {
      return false;
    }

    @NonNull
    public EInboundStatus getStatus ()
    {
      return EInboundStatus.RECEIVED;
    }

    public int getAttemptCount ()
    {
      return 0;
    }

    @Nullable
    public OffsetDateTime getReceivedDT ()
    {
      return null;
    }

    @Nullable
    public OffsetDateTime getCompletedDT ()
    {
      return null;
    }

    @Nullable
    public EReportingStatus getReportingStatus ()
    {
      return null;
    }

    @Nullable
    public OffsetDateTime getNextRetryDT ()
    {
      return null;
    }

    @Nullable
    public String getErrorDetails ()
    {
      return null;
    }

    @Nullable
    public String getMlsTo ()
    {
      return null;
    }

    @Nullable
    public EPeppolMLSType getMlsType ()
    {
      return null;
    }

    @Nullable
    public EPeppolMLSResponseCode getMlsResponseCode ()
    {
      return null;
    }

    @Nullable
    public String getMlsOutboundTransactionID ()
    {
      return null;
    }

    @Nullable
    public EVerificationResult getVerificationResult ()
    {
      return m_eVerificationResult;
    }

    @Nullable
    public String getVerificationDetails ()
    {
      return m_sVerificationDetails;
    }
  }

  @NonNull
  private static HttpDocumentForwarder _createForwarder (final boolean bSendDetails)
  {
    final ICommonsMap <String, String> aValues = new CommonsHashMap <> ();
    aValues.put ("forwarding.http.endpoint", ENDPOINT_URL);
    if (bSendDetails)
      aValues.put ("forwarding.http.verification-details", "true");

    final MultiConfigurationValueProvider aVP = ConfigFactory.createDefaultValueProvider ();
    // Highest priority wins over the values from application.properties
    aVP.addConfigurationSource (new ConfigurationSourceFunction (aValues::get), Integer.MAX_VALUE);
    final IConfigWithFallback aConfig = new ConfigWithFallback (aVP);

    final HttpDocumentForwarder aForwarder = new HttpDocumentForwarder (EForwardingMode.HTTP_POST_ASYNC);
    assertEquals (ESuccess.SUCCESS, aForwarder.initFromConfiguration (aConfig, "forwarding."));
    return aForwarder;
  }

  @NonNull
  private static String _createIssuesJson (final int nIssues, final int nDescriptionLength)
  {
    final IJsonArray aIssues = new JsonArray ();
    for (int i = 0; i < nIssues; ++i)
      aIssues.add (new JsonObject ().add ("level", "error")
                                    .add ("type", "business-rule")
                                    .add ("code", "BR-" + i)
                                    .add ("location", "/Invoice")
                                    .add ("description", "x".repeat (nDescriptionLength)));
    return aIssues.getAsJsonString ();
  }

  @Test
  public void testNoVerdictMeansNoHeader ()
  {
    final HttpDocumentForwarder aForwarder = _createForwarder (true);
    final HttpPost aPost = new HttpPost (ENDPOINT_URL);

    // Verification is disabled, or the verdict is still deferred
    aForwarder.applyVerificationHeaders (aPost, new MockInboundTransaction (null, null));
    assertFalse (aPost.containsHeader (HEADER_RESULT));
    assertFalse (aPost.containsHeader (HEADER_DETAILS));
  }

  @Test
  public void testVerdictIsAlwaysSent ()
  {
    final HttpDocumentForwarder aForwarder = _createForwarder (false);
    for (final EVerificationResult eResult : EVerificationResult.values ())
    {
      final HttpPost aPost = new HttpPost (ENDPOINT_URL);
      aForwarder.applyVerificationHeaders (aPost, new MockInboundTransaction (eResult, null));
      assertNotNull (aPost.getFirstHeader (HEADER_RESULT));
      assertEquals (eResult.getID (), aPost.getFirstHeader (HEADER_RESULT).getValue ());
    }
  }

  @Test
  public void testRejectedDocumentCarriesTheResultHeader ()
  {
    final HttpDocumentForwarder aForwarder = _createForwarder (false);
    final HttpPost aPost = new HttpPost (ENDPOINT_URL);

    aForwarder.applyVerificationHeaders (aPost,
                                         new MockInboundTransaction (EVerificationResult.REJECTED,
                                                                     _createIssuesJson (1, 10)));
    assertEquals ("rejected", aPost.getFirstHeader (HEADER_RESULT).getValue ());
    // The details are opt-in
    assertFalse (aPost.containsHeader (HEADER_DETAILS));
    assertFalse (aPost.containsHeader (HEADER_TRUNCATED));
  }

  @Test
  public void testDetailsAreSentWhenEnabled ()
  {
    final HttpDocumentForwarder aForwarder = _createForwarder (true);
    final HttpPost aPost = new HttpPost (ENDPOINT_URL);
    final String sDetails = _createIssuesJson (2, 10);

    aForwarder.applyVerificationHeaders (aPost, new MockInboundTransaction (EVerificationResult.REJECTED, sDetails));
    assertEquals ("rejected", aPost.getFirstHeader (HEADER_RESULT).getValue ());

    final String sEncoded = aPost.getFirstHeader (HEADER_DETAILS).getValue ();
    assertEquals (sDetails, Base64.safeDecodeAsString (sEncoded, StandardCharsets.UTF_8));

    // Everything fit, so nothing was dropped
    assertFalse (aPost.containsHeader (HEADER_TRUNCATED));
  }

  @Test
  public void testDetailsAreTruncatedToFit ()
  {
    final HttpDocumentForwarder aForwarder = _createForwarder (true);
    final HttpPost aPost = new HttpPost (ENDPOINT_URL);
    // Way beyond the characters a header value is limited to
    final String sDetails = _createIssuesJson (50, 500);

    aForwarder.applyVerificationHeaders (aPost, new MockInboundTransaction (EVerificationResult.REJECTED, sDetails));

    final String sEncoded = aPost.getFirstHeader (HEADER_DETAILS).getValue ();
    assertTrue (sEncoded.length () <= MAX_DETAILS_LENGTH);
    assertEquals ("true", aPost.getFirstHeader (HEADER_TRUNCATED).getValue ());

    // What is left must still be a valid JSON array - just a shorter one
    final IJsonArray aDecoded = JsonReader.builder ()
                                          .source (Base64.safeDecodeAsString (sEncoded, StandardCharsets.UTF_8))
                                          .readAsArray ();
    assertNotNull (aDecoded);
    assertTrue (aDecoded.size () < 50);
  }

  @Test
  public void testBrokenDetailsAreSkipped ()
  {
    final HttpDocumentForwarder aForwarder = _createForwarder (true);
    final HttpPost aPost = new HttpPost (ENDPOINT_URL);

    aForwarder.applyVerificationHeaders (aPost,
                                         new MockInboundTransaction (EVerificationResult.REJECTED, "this is no JSON"));
    // The verdict is unaffected by unparsable details
    assertEquals ("rejected", aPost.getFirstHeader (HEADER_RESULT).getValue ());
    assertFalse (aPost.containsHeader (HEADER_DETAILS));
  }

  @Test
  public void testEmptyDetailsSendNoDetailsHeader ()
  {
    final HttpDocumentForwarder aForwarder = _createForwarder (true);
    final HttpPost aPost = new HttpPost (ENDPOINT_URL);

    aForwarder.applyVerificationHeaders (aPost, new MockInboundTransaction (EVerificationResult.PASSED, null));
    assertEquals ("passed", aPost.getFirstHeader (HEADER_RESULT).getValue ());
    assertFalse (aPost.containsHeader (HEADER_DETAILS));
  }
}
