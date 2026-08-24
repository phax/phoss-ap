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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.json.IJson;
import com.helger.json.IJsonObject;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.EVerificationResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;

/**
 * Test class for class {@link InboundTransactionResponse}.
 *
 * @author Philip Helger
 */
public final class InboundTransactionResponseTest
{
  @Test
  public void testDefaultConstructor ()
  {
    final InboundTransactionResponse a = new InboundTransactionResponse ();
    assertNull (a.getID ());
    assertNull (a.getSenderID ());
    assertNull (a.getReceiverID ());
    assertNull (a.getDocTypeID ());
    assertNull (a.getProcessID ());
    assertNull (a.getAS4MessageID ());
    assertNull (a.getSbdhInstanceID ());
    assertNull (a.getC2SeatID ());
    assertNull (a.getC3SeatID ());
    assertNull (a.getStatus ());
    assertEquals (0, a.getAttemptCount ());
    assertNull (a.getReceivedDT ());
    assertNull (a.getCompletedDT ());
    assertNull (a.getReportingStatus ());
    assertNull (a.getNextRetryDT ());
    assertNull (a.getErrorDetails ());
    assertNull (a.getC4CountryCode ());
    assertFalse (a.isDuplicateAS4 ());
    assertFalse (a.isDuplicateSBDH ());
    assertNull (a.getMlsResponseCode ());
  }

  @Test
  public void testSettersAndGetters ()
  {
    final InboundTransactionResponse a = new InboundTransactionResponse ();
    a.setID ("id1");
    a.setSenderID ("sender1");
    a.setReceiverID ("receiver1");
    a.setDocTypeID ("docType1");
    a.setProcessID ("process1");
    a.setAS4MessageID ("as4-1");
    a.setSbdhInstanceID ("sbdh-1");
    a.setC2SeatID ("c2seat1");
    a.setC3SeatID ("c3seat1");
    a.setStatus ("forwarded");
    a.setAttemptCount (3);
    a.setReceivedDT ("2026-03-29T10:00:00Z");
    a.setCompletedDT ("2026-03-29T10:01:00Z");
    a.setReportingStatus ("reported");
    a.setNextRetryDT ("2026-03-29T11:00:00Z");
    a.setErrorDetails ("some error");
    a.setC4CountryCode ("DE");
    a.setDuplicateAS4 (true);
    a.setDuplicateSBDH (true);
    a.setMlsResponseCode ("AP");

    assertEquals ("id1", a.getID ());
    assertEquals ("sender1", a.getSenderID ());
    assertEquals ("receiver1", a.getReceiverID ());
    assertEquals ("docType1", a.getDocTypeID ());
    assertEquals ("process1", a.getProcessID ());
    assertEquals ("as4-1", a.getAS4MessageID ());
    assertEquals ("sbdh-1", a.getSbdhInstanceID ());
    assertEquals ("c2seat1", a.getC2SeatID ());
    assertEquals ("c3seat1", a.getC3SeatID ());
    assertEquals ("forwarded", a.getStatus ());
    assertEquals (3, a.getAttemptCount ());
    assertEquals ("2026-03-29T10:00:00Z", a.getReceivedDT ());
    assertEquals ("2026-03-29T10:01:00Z", a.getCompletedDT ());
    assertEquals ("reported", a.getReportingStatus ());
    assertEquals ("2026-03-29T11:00:00Z", a.getNextRetryDT ());
    assertEquals ("some error", a.getErrorDetails ());
    assertEquals ("DE", a.getC4CountryCode ());
    assertTrue (a.isDuplicateAS4 ());
    assertTrue (a.isDuplicateSBDH ());
    assertEquals ("AP", a.getMlsResponseCode ());
  }

  @Test
  public void testToJson ()
  {
    final InboundTransactionResponse a = new InboundTransactionResponse ();
    a.setID ("id1");
    a.setSenderID ("sender1");
    a.setAS4MessageID ("as4-1");
    a.setAttemptCount (3);
    a.setDuplicateAS4 (true);
    a.setDuplicateSBDH (false);

    final IJsonObject aJson = a.toJson ();
    assertNotNull (aJson);
    assertEquals ("id1", aJson.getValue ("id"));
    assertEquals ("sender1", aJson.getValue ("senderID"));
    assertEquals ("as4-1", aJson.getValue ("as4MessageID"));
    assertEquals (Integer.valueOf (3), aJson.getValue ("attemptCount"));
    assertEquals (Boolean.TRUE, aJson.getValue ("isDuplicateAS4"));
    assertEquals (Boolean.FALSE, aJson.getValue ("isDuplicateSBDH"));

    // Fields that were never set must not be contained
    assertFalse (aJson.containsKey ("receiverID"));
    assertFalse (aJson.containsKey ("errorDetails"));
  }

  @Test
  public void testNoJavaBeanGetterReturnsJson ()
  {
    // A JavaBean getter returning a ph-json type is picked up as a property by
    // Jackson and breaks the serialization of all REST responses - see
    // https://github.com/phax/phoss-ap/issues/77
    for (final Method aMethod : InboundTransactionResponse.class.getMethods ())
    {
      final String sName = aMethod.getName ();
      if (aMethod.getParameterCount () == 0 && (sName.startsWith ("get") || sName.startsWith ("is")))
        assertFalse ("The JavaBean getter '" + sName + "' must not return a ph-json type",
                     IJson.class.isAssignableFrom (aMethod.getReturnType ()));
    }
  }

  @Test
  public void testFromDomainAllFieldsPopulated ()
  {
    final OffsetDateTime aNow = OffsetDateTime.of (2026, 3, 29, 10, 0, 0, 0, ZoneOffset.UTC);
    final OffsetDateTime aCompleted = aNow.plusMinutes (5);
    final OffsetDateTime aNextRetry = aNow.plusHours (1);

    final IInboundTransaction aTx = _createMock ("tx-1",
                                                  "inc-1",
                                                  "C2-SEAT",
                                                  "C3-SEAT",
                                                  "CN=Test",
                                                  "iso6523-actorid-upis::0088:sender",
                                                  "iso6523-actorid-upis::0088:receiver",
                                                  "docType-1",
                                                  "process-1",
                                                  "/path/doc.xml",
                                                  1234L,
                                                  "abc123hash",
                                                  "as4-msg-1",
                                                  aNow,
                                                  "sbdh-inst-1",
                                                  "AT",
                                                  "DE",
                                                  true,
                                                  false,
                                                  EInboundStatus.FORWARDED,
                                                  2,
                                                  aNow,
                                                  aCompleted,
                                                  EReportingStatus.REPORTED,
                                                  aNextRetry,
                                                  "some error",
                                                  "mls-to-1",
                                                  EPeppolMLSType.ALWAYS_SEND,
                                                  EPeppolMLSResponseCode.ACCEPTANCE,
                                                  "mls-out-1",
                                                  EVerificationResult.REJECTED,
                                                  "[{\"level\":\"error\",\"type\":\"business_rule\",\"description\":\"Nope\"}]");

    final InboundTransactionResponse aResp = InboundTransactionResponse.fromDomain (aTx);
    assertNotNull (aResp);
    assertEquals ("tx-1", aResp.getID ());
    assertEquals ("iso6523-actorid-upis::0088:sender", aResp.getSenderID ());
    assertEquals ("iso6523-actorid-upis::0088:receiver", aResp.getReceiverID ());
    assertEquals ("docType-1", aResp.getDocTypeID ());
    assertEquals ("process-1", aResp.getProcessID ());
    assertEquals ("as4-msg-1", aResp.getAS4MessageID ());
    assertEquals ("sbdh-inst-1", aResp.getSbdhInstanceID ());
    assertEquals ("C2-SEAT", aResp.getC2SeatID ());
    assertEquals ("C3-SEAT", aResp.getC3SeatID ());
    assertEquals ("forwarded", aResp.getStatus ());
    assertEquals (2, aResp.getAttemptCount ());
    assertNotNull (aResp.getReceivedDT ());
    assertNotNull (aResp.getCompletedDT ());
    assertEquals ("reported", aResp.getReportingStatus ());
    assertNotNull (aResp.getNextRetryDT ());
    assertEquals ("some error", aResp.getErrorDetails ());
    assertEquals ("DE", aResp.getC4CountryCode ());
    assertTrue (aResp.isDuplicateAS4 ());
    assertFalse (aResp.isDuplicateSBDH ());
    assertEquals ("AP", aResp.getMlsResponseCode ());
    assertEquals ("rejected", aResp.getVerificationResult ());
    assertEquals ("[{\"level\":\"error\",\"type\":\"business_rule\",\"description\":\"Nope\"}]", aResp.getVerificationDetails ());
  }

  @Test
  public void testFromDomainNullableFieldsNull ()
  {
    final OffsetDateTime aNow = OffsetDateTime.of (2026, 3, 29, 10, 0, 0, 0, ZoneOffset.UTC);

    final IInboundTransaction aTx = _createMock ("tx-2",
                                                  "inc-2",
                                                  "C2-SEAT",
                                                  "C3-SEAT",
                                                  "CN=Test",
                                                  "sender",
                                                  "receiver",
                                                  "docType",
                                                  "process",
                                                  "/path/doc.xml",
                                                  0L,
                                                  "hash",
                                                  "as4-msg-2",
                                                  aNow,
                                                  "sbdh-2",
                                                  "AT",
                                                  null,
                                                  false,
                                                  false,
                                                  EInboundStatus.RECEIVED,
                                                  0,
                                                  aNow,
                                                  null,
                                                  EReportingStatus.PENDING,
                                                  null,
                                                  null,
                                                  null,
                                                  EPeppolMLSType.ALWAYS_SEND,
                                                  null,
                                                  null,
                                                  null,
                                                  null);

    final InboundTransactionResponse aResp = InboundTransactionResponse.fromDomain (aTx);
    assertNotNull (aResp);
    assertNull (aResp.getCompletedDT ());
    assertNull (aResp.getNextRetryDT ());
    assertNull (aResp.getErrorDetails ());
    assertNull (aResp.getC4CountryCode ());
    assertNull (aResp.getMlsResponseCode ());
    assertNull (aResp.getVerificationResult ());
    assertNull (aResp.getVerificationDetails ());
    assertFalse (aResp.isDuplicateAS4 ());
    assertFalse (aResp.isDuplicateSBDH ());
    assertEquals ("received", aResp.getStatus ());
    assertEquals ("pending", aResp.getReportingStatus ());
  }

  @NonNull
  private static IInboundTransaction _createMock (@NonNull final String sID,
                                                   @NonNull final String sIncomingID,
                                                   @NonNull final String sC2SeatID,
                                                   @NonNull final String sC3SeatID,
                                                   @NonNull final String sSigningCertCN,
                                                   @NonNull final String sSenderID,
                                                   @NonNull final String sReceiverID,
                                                   @NonNull final String sDocTypeID,
                                                   @NonNull final String sProcessID,
                                                   @NonNull final String sDocumentPath,
                                                   final long nDocumentSize,
                                                   @NonNull final String sDocumentHash,
                                                   @NonNull final String sAS4MessageID,
                                                   @NonNull final OffsetDateTime aAS4Timestamp,
                                                   @NonNull final String sSbdhInstanceID,
                                                   @NonNull final String sC1CountryCode,
                                                   @Nullable final String sC4CountryCode,
                                                   final boolean bIsDuplicateAS4,
                                                   final boolean bIsDuplicateSBDH,
                                                   @NonNull final EInboundStatus eStatus,
                                                   final int nAttemptCount,
                                                   @NonNull final OffsetDateTime aReceivedDT,
                                                   @Nullable final OffsetDateTime aCompletedDT,
                                                   @NonNull final EReportingStatus eReportingStatus,
                                                   @Nullable final OffsetDateTime aNextRetryDT,
                                                   @Nullable final String sErrorDetails,
                                                   @Nullable final String sMlsTo,
                                                   @NonNull final EPeppolMLSType eMlsType,
                                                   @Nullable final EPeppolMLSResponseCode eMlsResponseCode,
                                                   @Nullable final String sMlsOutboundTransactionID,
                                                   @Nullable final EVerificationResult eVerificationResult,
                                                   @Nullable final String sVerificationDetails)
  {
    return new IInboundTransaction ()
    {
      public String getID () { return sID; }
      public String getIncomingID () { return sIncomingID; }
      public String getC2SeatID () { return sC2SeatID; }
      public String getC3SeatID () { return sC3SeatID; }
      public String getSigningCertCN () { return sSigningCertCN; }
      public String getSenderID () { return sSenderID; }
      public String getReceiverID () { return sReceiverID; }
      public String getDocTypeID () { return sDocTypeID; }
      public String getProcessID () { return sProcessID; }
      public String getDocumentPath () { return sDocumentPath; }
      public long getDocumentSize () { return nDocumentSize; }
      public String getDocumentHash () { return sDocumentHash; }
      public String getAS4MessageID () { return sAS4MessageID; }
      public OffsetDateTime getAS4Timestamp () { return aAS4Timestamp; }
      public String getSbdhInstanceID () { return sSbdhInstanceID; }
      public String getC1CountryCode () { return sC1CountryCode; }
      public String getC4CountryCode () { return sC4CountryCode; }
      public boolean isDuplicateAS4 () { return bIsDuplicateAS4; }
      public boolean isDuplicateSBDH () { return bIsDuplicateSBDH; }
      public EInboundStatus getStatus () { return eStatus; }
      public int getAttemptCount () { return nAttemptCount; }
      public OffsetDateTime getReceivedDT () { return aReceivedDT; }
      public OffsetDateTime getCompletedDT () { return aCompletedDT; }
      public EReportingStatus getReportingStatus () { return eReportingStatus; }
      public OffsetDateTime getNextRetryDT () { return aNextRetryDT; }
      public String getErrorDetails () { return sErrorDetails; }
      public String getMlsTo () { return sMlsTo; }
      public EPeppolMLSType getMlsType () { return eMlsType; }
      public EPeppolMLSResponseCode getMlsResponseCode () { return eMlsResponseCode; }
      public String getMlsOutboundTransactionID () { return sMlsOutboundTransactionID; }
      public EVerificationResult getVerificationResult () { return eVerificationResult; }
      public String getVerificationDetails () { return sVerificationDetails; }
    };
  }
}
