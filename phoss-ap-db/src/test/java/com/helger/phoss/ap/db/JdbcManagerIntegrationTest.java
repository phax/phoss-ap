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
package com.helger.phoss.ap.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.junit.ClassRule;
import org.junit.Test;

import com.helger.base.state.EChange;
import com.helger.base.state.EContinue;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.duplicate.IAS4DuplicateItem;
import com.helger.phase4.duplicate.IAS4DuplicateManager;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.IArchivalManager;
import com.helger.phoss.ap.api.IInboundForwardingAttemptManager;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.IOutboundSendingAttemptManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.model.IInboundForwardingAttempt;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.IOutboundSendingAttempt;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Integration tests for all JDBC managers. Uses in-memory H2 with {@code db/migrate-h2} (kept in
 * sync with {@code db/migrate-postgresql}). All tests share a single scope/connection to avoid
 * issues with the static DataSourceProvider in APDBExecutor.
 *
 * @author Philip Helger
 */
public final class JdbcManagerIntegrationTest
{
  @ClassRule
  public static final ScopeTestRule RULE = new ScopeTestRule ();

  private static String _uniqueID ()
  {
    return "test-" + UUID.randomUUID ().toString ();
  }

  @NonNull
  private static OffsetDateTime _now ()
  {
    return APBasicMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ();
  }

  // --- OutboundTransactionManager ---

  private static String _createOutboundTx ()
  {
    return APJdbcMetaManager.getOutboundTransactionMgr ()
                            .create (ETransactionType.BUSINESS_DOCUMENT,
                                     "iso6523-actorid-upis::9915:sender",
                                     "iso6523-actorid-upis::9915:receiver",
                                     "busdox-docid-qns::urn:test:invoice",
                                     "cenbii-procid-ubl::urn:test:process",
                                     _uniqueID (),
                                     ESourceType.PAYLOAD_ONLY,
                                     "/tmp/test-outbound.sbd",
                                     1024L,
                                     "abc123hash012345678901234567890123456789012345678901234567890123",
                                     "DE",
                                     _now (),
                                     null,
                                     null,
                                     null,
                                     null,
                                     null,
                                     null);
  }

  @Test
  public void testOutboundCreateAndGetByID ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = _createOutboundTx ();
    assertNotNull (sID);

    final IOutboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (sID, aTx.getID ());
    assertEquals (ETransactionType.BUSINESS_DOCUMENT, aTx.getTransactionType ());
    assertEquals ("iso6523-actorid-upis::9915:sender", aTx.getSenderID ());
    assertEquals ("iso6523-actorid-upis::9915:receiver", aTx.getReceiverID ());
    assertEquals ("busdox-docid-qns::urn:test:invoice", aTx.getDocTypeID ());
    assertEquals ("cenbii-procid-ubl::urn:test:process", aTx.getProcessID ());
    assertEquals (ESourceType.PAYLOAD_ONLY, aTx.getSourceType ());
    assertEquals ("/tmp/test-outbound.sbd", aTx.getDocumentPath ());
    assertEquals (1024L, aTx.getDocumentSize ());
    assertEquals ("abc123hash012345678901234567890123456789012345678901234567890123", aTx.getDocumentHash ());
    assertEquals ("DE", aTx.getC1CountryCode ());
    assertEquals (EOutboundStatus.PENDING, aTx.getStatus ());
    assertEquals (0, aTx.getAttemptCount ());
    assertNotNull (aTx.getCreatedDT ());
    assertNull (aTx.getCompletedDT ());
    assertEquals (EReportingStatus.PENDING, aTx.getReportingStatus ());
    assertNull (aTx.getNextRetryDT ());
    assertNull (aTx.getErrorDetails ());
    assertNull (aTx.getMlsTo ());
    assertEquals (EMlsReceptionStatus.PENDING, aTx.getMlsStatus ());
    assertNull (aTx.getMlsReceivedDT ());
    assertNull (aTx.getMlsID ());
    assertNull (aTx.getMlsInboundTransactionID ());
  }

  @Test
  public void testOutboundGetByIDNotFound ()
  {
    assertNull (APJdbcMetaManager.getOutboundTransactionMgr ().getByID ("non-existent-id"));
  }

  @Test
  public void testOutboundGetBySbdhInstanceID ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sSbdhID = _uniqueID ();
    final String sID = aMgr.create (ETransactionType.BUSINESS_DOCUMENT,
                                    "iso6523-actorid-upis::9915:sender",
                                    "iso6523-actorid-upis::9915:receiver",
                                    "busdox-docid-qns::urn:test:invoice",
                                    "cenbii-procid-ubl::urn:test:process",
                                    sSbdhID,
                                    ESourceType.PAYLOAD_ONLY,
                                    "/tmp/test.sbd",
                                    512L,
                                    "hash456",
                                    "AT",
                                    _now (),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null);
    assertNotNull (sID);

    final IOutboundTransaction aTx = aMgr.getBySbdhInstanceID (sSbdhID);
    assertNotNull (aTx);
    assertEquals (sID, aTx.getID ());
    assertEquals (sSbdhID, aTx.getSbdhInstanceID ());
  }

  @Test
  public void testOutboundGetBySbdhInstanceIDNotFound ()
  {
    assertNull (APJdbcMetaManager.getOutboundTransactionMgr ().getBySbdhInstanceID ("non-existent-sbdh"));
  }

  @Test
  public void testOutboundCreateWithMlsFields ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = aMgr.create (ETransactionType.BUSINESS_DOCUMENT,
                                    "iso6523-actorid-upis::9915:sender",
                                    "iso6523-actorid-upis::9915:receiver",
                                    "busdox-docid-qns::urn:test:invoice",
                                    "cenbii-procid-ubl::urn:test:process",
                                    _uniqueID (),
                                    ESourceType.PAYLOAD_ONLY,
                                    "/tmp/test.sbd",
                                    256L,
                                    "hashMls",
                                    "DE",
                                    _now (),
                                    "iso6523-actorid-upis::9915:mlsto",
                                    null,
                                    null,
                                    null,
                                    null,
                                    null);
    assertNotNull (sID);

    final IOutboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals ("iso6523-actorid-upis::9915:mlsto", aTx.getMlsTo ());
    assertEquals (EMlsReceptionStatus.PENDING, aTx.getMlsStatus ());
  }

  @Test
  public void testOutboundCreateMlsResponse ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = aMgr.create (ETransactionType.MLS_RESPONSE,
                                    "iso6523-actorid-upis::9915:sender",
                                    "iso6523-actorid-upis::9915:receiver",
                                    "mls-doc-type",
                                    "mls-process",
                                    _uniqueID (),
                                    ESourceType.PAYLOAD_ONLY,
                                    "/tmp/test-mls.sbd",
                                    128L,
                                    "hashMlsResp",
                                    "DE",
                                    _now (),
                                    null,
                                    "inbound-tx-ref-123",
                                    null,
                                    null,
                                    null,
                                    null);
    assertNotNull (sID);

    final IOutboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (ETransactionType.MLS_RESPONSE, aTx.getTransactionType ());
    assertNull (aTx.getMlsStatus ());
    assertEquals ("inbound-tx-ref-123", aTx.getMlsInboundTransactionID ());
  }

  @Test
  public void testOutboundCreateWithSbdhOverrides ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = aMgr.create (ETransactionType.BUSINESS_DOCUMENT,
                                    "iso6523-actorid-upis::9915:sender",
                                    "iso6523-actorid-upis::9915:receiver",
                                    "busdox-docid-qns::urn:test:facturx",
                                    "cenbii-procid-ubl::urn:test:process",
                                    _uniqueID (),
                                    ESourceType.PAYLOAD_ONLY,
                                    "/tmp/test-pdf.pdf",
                                    4096L,
                                    "hashPdf01234567890123456789012345678901234567890123456789012345",
                                    "FR",
                                    _now (),
                                    null,
                                    null,
                                    "urn:peppol:doctype:pdf+xml",
                                    "0",
                                    "factur-x",
                                    "application/pdf");
    assertNotNull (sID);

    final IOutboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals ("urn:peppol:doctype:pdf+xml", aTx.getSbdhStandard ());
    assertEquals ("0", aTx.getSbdhTypeVersion ());
    assertEquals ("factur-x", aTx.getSbdhType ());
    assertEquals ("application/pdf", aTx.getPayloadMimeType ());
    assertEquals (4096L, aTx.getDocumentSize ());
    assertEquals ("FR", aTx.getC1CountryCode ());
  }

  @Test
  public void testOutboundCreateWithoutSbdhOverrides ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = _createOutboundTx ();
    assertNotNull (sID);

    final IOutboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertNull (aTx.getSbdhStandard ());
    assertNull (aTx.getSbdhTypeVersion ());
    assertNull (aTx.getSbdhType ());
    assertNull (aTx.getPayloadMimeType ());
  }

  @Test
  public void testOutboundCreatePrebuiltSBD ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = aMgr.create (ETransactionType.BUSINESS_DOCUMENT,
                                    "iso6523-actorid-upis::9915:sender",
                                    "iso6523-actorid-upis::9915:receiver",
                                    "busdox-docid-qns::urn:test:invoice",
                                    "cenbii-procid-ubl::urn:test:process",
                                    _uniqueID (),
                                    ESourceType.PREBUILT_SBD,
                                    "/tmp/test-prebuilt.sbd",
                                    2048L,
                                    "hashPrebuilt",
                                    "AT",
                                    _now (),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null);
    assertNotNull (sID);

    final IOutboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (ESourceType.PREBUILT_SBD, aTx.getSourceType ());
    assertEquals (2048L, aTx.getDocumentSize ());
  }

  @Test
  public void testOutboundUpdateStatus ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = _createOutboundTx ();
    assertNotNull (sID);

    assertTrue (aMgr.updateStatus (sID, EOutboundStatus.SENDING).isSuccess ());

    final IOutboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (EOutboundStatus.SENDING, aTx.getStatus ());
  }

  @Test
  public void testOutboundUpdateStatusAndRetry ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = _createOutboundTx ();
    assertNotNull (sID);

    final OffsetDateTime aNextRetry = _now ().plusHours (1);
    assertTrue (aMgr.updateStatusAndRetry (sID, EOutboundStatus.FAILED, 1, aNextRetry, "Connection refused")
                    .isSuccess ());

    final IOutboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (EOutboundStatus.FAILED, aTx.getStatus ());
    assertEquals (1, aTx.getAttemptCount ());
    assertNotNull (aTx.getNextRetryDT ());
    assertEquals ("Connection refused", aTx.getErrorDetails ());
  }

  @Test
  public void testOutboundUpdateStatusCompleted ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = _createOutboundTx ();
    assertNotNull (sID);

    assertTrue (aMgr.updateStatusCompleted (sID, EOutboundStatus.SENT).isSuccess ());

    final IOutboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (EOutboundStatus.SENT, aTx.getStatus ());
    assertNotNull (aTx.getCompletedDT ());
  }

  @Test
  public void testOutboundUpdateMlsStatus ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = _createOutboundTx ();
    assertNotNull (sID);

    final OffsetDateTime aMlsReceivedDT = _now ();
    assertTrue (aMgr.updateMlsStatus (sID,
                                      EMlsReceptionStatus.RECEIVED_AP,
                                      aMlsReceivedDT,
                                      "mls-msg-001",
                                      "inbound-mls-tx-001").isSuccess ());

    final IOutboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (EMlsReceptionStatus.RECEIVED_AP, aTx.getMlsStatus ());
    assertNotNull (aTx.getMlsReceivedDT ());
    assertEquals ("mls-msg-001", aTx.getMlsID ());
    assertEquals ("inbound-mls-tx-001", aTx.getMlsInboundTransactionID ());
  }

  @Test
  public void testOutboundUpdateReportingStatus ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = _createOutboundTx ();
    assertNotNull (sID);

    assertTrue (aMgr.updateReportingStatus (sID, EReportingStatus.REPORTED).isSuccess ());

    final IOutboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (EReportingStatus.REPORTED, aTx.getReportingStatus ());
  }

  @Test
  public void testOutboundGetAllInTransmission ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = _createOutboundTx ();
    assertNotNull (sID);

    final ICommonsList <IOutboundTransaction> aList = aMgr.getAllInTransmission ();
    assertNotNull (aList);
    assertTrue (aList.containsAny (x -> x.getID ().equals (sID)));
  }

  @Test
  public void testOutboundGetAllForRetry ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = _createOutboundTx ();
    assertNotNull (sID);

    final OffsetDateTime aPast = _now ().minusHours (1);
    aMgr.updateStatusAndRetry (sID, EOutboundStatus.FAILED, 1, aPast, "test error");

    final ICommonsList <IOutboundTransaction> aList = aMgr.getAllForRetry (100);
    assertNotNull (aList);
    assertTrue (aList.containsAny (x -> x.getID ().equals (sID)));
  }

  @Test
  public void testOutboundGetAllForArchival ()
  {
    final IOutboundTransactionManager aMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final String sID = _createOutboundTx ();
    assertNotNull (sID);

    aMgr.updateStatusCompleted (sID, EOutboundStatus.SENT);
    aMgr.updateReportingStatus (sID, EReportingStatus.REPORTED);

    final ICommonsList <IOutboundTransaction> aList = aMgr.getAllForArchival (100);
    assertNotNull (aList);
    assertTrue (aList.containsAny (x -> x.getID ().equals (sID)));
  }

  // --- InboundTransactionManager ---

  private static String _createInboundTx ()
  {
    return APJdbcMetaManager.getInboundTransactionMgr ()
                            .create (_uniqueID (),
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
                                     _uniqueID (),
                                     _now (),
                                     _uniqueID (),
                                     "DE",
                                     false,
                                     false,
                                     null,
                                     EPeppolMLSType.ALWAYS_SEND);
  }

  @Test
  public void testInboundCreateAndGetByID ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = _createInboundTx ();
    assertNotNull (sID);

    final IInboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (sID, aTx.getID ());
    assertEquals ("POP000001", aTx.getC2SeatID ());
    assertEquals ("POP000002", aTx.getC3SeatID ());
    assertEquals ("CN=TestCert", aTx.getSigningCertCN ());
    assertEquals ("iso6523-actorid-upis::9915:sender", aTx.getSenderID ());
    assertEquals ("iso6523-actorid-upis::9915:receiver", aTx.getReceiverID ());
    assertEquals ("/tmp/test-inbound.sbd", aTx.getDocumentPath ());
    assertEquals (2048L, aTx.getDocumentSize ());
    assertEquals ("sha256hash012345678901234567890123456789012345678901234567890123", aTx.getDocumentHash ());
    assertEquals ("DE", aTx.getC1CountryCode ());
    assertFalse (aTx.isDuplicateAS4 ());
    assertFalse (aTx.isDuplicateSBDH ());
    assertEquals (EInboundStatus.RECEIVED, aTx.getStatus ());
    assertEquals (0, aTx.getAttemptCount ());
    assertNotNull (aTx.getReceivedDT ());
    assertNull (aTx.getCompletedDT ());
    assertEquals (EReportingStatus.PENDING, aTx.getReportingStatus ());
    assertNull (aTx.getMlsTo ());
    assertEquals (EPeppolMLSType.ALWAYS_SEND, aTx.getMlsType ());
    assertNull (aTx.getMlsResponseCode ());
    assertNull (aTx.getMlsOutboundTransactionID ());
  }

  @Test
  public void testInboundGetByIDNotFound ()
  {
    assertNull (APJdbcMetaManager.getInboundTransactionMgr ().getByID ("non-existent-id"));
  }

  @Test
  public void testInboundGetByAS4MessageID ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sAS4MsgID = _uniqueID ();
    final String sID = aMgr.create (_uniqueID (),
                                    "POP000001",
                                    "POP000002",
                                    "CN=TestCert",
                                    "iso6523-actorid-upis::9915:sender",
                                    "iso6523-actorid-upis::9915:receiver",
                                    "busdox-docid-qns::urn:test:invoice",
                                    "cenbii-procid-ubl::urn:test:process",
                                    "/tmp/test.sbd",
                                    100L,
                                    "hash1",
                                    sAS4MsgID,
                                    _now (),
                                    _uniqueID (),
                                    "AT",
                                    false,
                                    false,
                                    null,
                                    EPeppolMLSType.FAILURE_ONLY);
    assertNotNull (sID);

    final IInboundTransaction aTx = aMgr.getByAS4MessageID (sAS4MsgID);
    assertNotNull (aTx);
    assertEquals (sID, aTx.getID ());
  }

  @Test
  public void testInboundContainsByAS4MessageID ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sAS4MsgID = _uniqueID ();
    assertFalse (aMgr.containsByAS4MessageID (sAS4MsgID));

    aMgr.create (_uniqueID (),
                 "POP000001",
                 "POP000002",
                 "CN=TestCert",
                 "iso6523-actorid-upis::9915:sender",
                 "iso6523-actorid-upis::9915:receiver",
                 "busdox-docid-qns::urn:test:invoice",
                 "cenbii-procid-ubl::urn:test:process",
                 "/tmp/test.sbd",
                 100L,
                 "hash2",
                 sAS4MsgID,
                 _now (),
                 _uniqueID (),
                 "DE",
                 false,
                 false,
                 null,
                 EPeppolMLSType.ALWAYS_SEND);

    assertTrue (aMgr.containsByAS4MessageID (sAS4MsgID));
  }

  @Test
  public void testInboundGetBySbdhInstanceID ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sSbdhID = _uniqueID ();
    final String sID = aMgr.create (_uniqueID (),
                                    "POP000001",
                                    "POP000002",
                                    "CN=TestCert",
                                    "iso6523-actorid-upis::9915:sender",
                                    "iso6523-actorid-upis::9915:receiver",
                                    "busdox-docid-qns::urn:test:invoice",
                                    "cenbii-procid-ubl::urn:test:process",
                                    "/tmp/test.sbd",
                                    100L,
                                    "hash3",
                                    _uniqueID (),
                                    _now (),
                                    sSbdhID,
                                    "DE",
                                    false,
                                    false,
                                    null,
                                    EPeppolMLSType.ALWAYS_SEND);
    assertNotNull (sID);

    final IInboundTransaction aTx = aMgr.getBySbdhInstanceID (sSbdhID);
    assertNotNull (aTx);
    assertEquals (sID, aTx.getID ());
    assertEquals (sSbdhID, aTx.getSbdhInstanceID ());
  }

  @Test
  public void testInboundContainsBySbdhInstanceID ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sSbdhID = _uniqueID ();
    assertFalse (aMgr.containsBySbdhInstanceID (sSbdhID));

    aMgr.create (_uniqueID (),
                 "POP000001",
                 "POP000002",
                 "CN=TestCert",
                 "iso6523-actorid-upis::9915:sender",
                 "iso6523-actorid-upis::9915:receiver",
                 "busdox-docid-qns::urn:test:invoice",
                 "cenbii-procid-ubl::urn:test:process",
                 "/tmp/test.sbd",
                 100L,
                 "hash4",
                 _uniqueID (),
                 _now (),
                 sSbdhID,
                 "DE",
                 false,
                 false,
                 null,
                 EPeppolMLSType.ALWAYS_SEND);

    assertTrue (aMgr.containsBySbdhInstanceID (sSbdhID));
  }

  @Test
  public void testInboundCreateWithDuplicateFlags ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = aMgr.create (_uniqueID (),
                                    "POP000001",
                                    "POP000002",
                                    "CN=TestCert",
                                    "iso6523-actorid-upis::9915:sender",
                                    "iso6523-actorid-upis::9915:receiver",
                                    "busdox-docid-qns::urn:test:invoice",
                                    "cenbii-procid-ubl::urn:test:process",
                                    "/tmp/test.sbd",
                                    100L,
                                    "hash5",
                                    _uniqueID (),
                                    _now (),
                                    _uniqueID (),
                                    "DE",
                                    true,
                                    true,
                                    null,
                                    EPeppolMLSType.ALWAYS_SEND);
    assertNotNull (sID);

    final IInboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertTrue (aTx.isDuplicateAS4 ());
    assertTrue (aTx.isDuplicateSBDH ());
  }

  @Test
  public void testInboundCreateWithMlsTo ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = aMgr.create (_uniqueID (),
                                    "POP000001",
                                    "POP000002",
                                    "CN=TestCert",
                                    "iso6523-actorid-upis::9915:sender",
                                    "iso6523-actorid-upis::9915:receiver",
                                    "busdox-docid-qns::urn:test:invoice",
                                    "cenbii-procid-ubl::urn:test:process",
                                    "/tmp/test.sbd",
                                    100L,
                                    "hash6",
                                    _uniqueID (),
                                    _now (),
                                    _uniqueID (),
                                    "DE",
                                    false,
                                    false,
                                    "iso6523-actorid-upis::9915:mlsto",
                                    EPeppolMLSType.FAILURE_ONLY);
    assertNotNull (sID);

    final IInboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals ("iso6523-actorid-upis::9915:mlsto", aTx.getMlsTo ());
    assertEquals (EPeppolMLSType.FAILURE_ONLY, aTx.getMlsType ());
  }

  @Test
  public void testInboundUpdateStatus ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = _createInboundTx ();
    assertNotNull (sID);

    assertTrue (aMgr.updateStatus (sID, EInboundStatus.FORWARDING).isSuccess ());

    assertEquals (EInboundStatus.FORWARDING, aMgr.getByID (sID).getStatus ());
  }

  @Test
  public void testInboundUpdateStatusAndRetry ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = _createInboundTx ();
    assertNotNull (sID);

    final OffsetDateTime aNextRetry = _now ().plusHours (1);
    assertTrue (aMgr.updateStatusAndRetry (sID, EInboundStatus.FORWARD_FAILED, 1, aNextRetry, "Forwarding timeout")
                    .isSuccess ());

    final IInboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (EInboundStatus.FORWARD_FAILED, aTx.getStatus ());
    assertEquals (1, aTx.getAttemptCount ());
    assertNotNull (aTx.getNextRetryDT ());
    assertEquals ("Forwarding timeout", aTx.getErrorDetails ());
  }

  @Test
  public void testInboundUpdateStatusCompleted ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = _createInboundTx ();
    assertNotNull (sID);

    assertTrue (aMgr.updateStatusCompleted (sID, EInboundStatus.FORWARDED).isSuccess ());

    final IInboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (EInboundStatus.FORWARDED, aTx.getStatus ());
    assertNotNull (aTx.getCompletedDT ());
  }

  @Test
  public void testInboundUpdateC4CountryCode ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = _createInboundTx ();
    assertNotNull (sID);

    assertTrue (aMgr.updateC4CountryCode (sID, "AT").isSuccess ());

    assertEquals ("AT", aMgr.getByID (sID).getC4CountryCode ());
  }

  @Test
  public void testInboundUpdateMlsFields ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = _createInboundTx ();
    assertNotNull (sID);

    assertTrue (aMgr.updateMlsFields (sID, EPeppolMLSResponseCode.ACCEPTANCE, "mls-outbound-tx-001").isSuccess ());

    final IInboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertEquals (EPeppolMLSResponseCode.ACCEPTANCE, aTx.getMlsResponseCode ());
    assertEquals ("mls-outbound-tx-001", aTx.getMlsOutboundTransactionID ());
  }

  @Test
  public void testInboundUpdateMlsFieldsNull ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = _createInboundTx ();
    assertNotNull (sID);

    assertTrue (aMgr.updateMlsFields (sID, null, null).isSuccess ());

    final IInboundTransaction aTx = aMgr.getByID (sID);
    assertNotNull (aTx);
    assertNull (aTx.getMlsResponseCode ());
    assertNull (aTx.getMlsOutboundTransactionID ());
  }

  @Test
  public void testInboundUpdateReportingStatus ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = _createInboundTx ();
    assertNotNull (sID);

    assertTrue (aMgr.updateReportingStatus (sID, EReportingStatus.REPORTED).isSuccess ());

    assertEquals (EReportingStatus.REPORTED, aMgr.getByID (sID).getReportingStatus ());
  }

  @Test
  public void testInboundGetAllInProcessing ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = _createInboundTx ();
    assertNotNull (sID);

    final ICommonsList <IInboundTransaction> aList = aMgr.getAllInProcessing ();
    assertNotNull (aList);
    assertTrue (aList.containsAny (x -> x.getID ().equals (sID)));
  }

  @Test
  public void testInboundGetAllForRetry ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = _createInboundTx ();
    assertNotNull (sID);

    final OffsetDateTime aPast = _now ().minusHours (1);
    aMgr.updateStatusAndRetry (sID, EInboundStatus.FORWARD_FAILED, 1, aPast, "test error");

    final ICommonsList <IInboundTransaction> aList = aMgr.getAllForRetry (100);
    assertNotNull (aList);
    assertTrue (aList.containsAny (x -> x.getID ().equals (sID)));
  }

  @Test
  public void testInboundGetAllForArchival ()
  {
    final IInboundTransactionManager aMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final String sID = _createInboundTx ();
    assertNotNull (sID);

    aMgr.updateStatusCompleted (sID, EInboundStatus.FORWARDED);
    aMgr.updateReportingStatus (sID, EReportingStatus.REPORTED);

    final ICommonsList <IInboundTransaction> aList = aMgr.getAllForArchival (100);
    assertNotNull (aList);
    assertTrue (aList.containsAny (x -> x.getID ().equals (sID)));
  }

  // --- OutboundSendingAttemptManager ---

  @Test
  public void testSendingAttemptCreateSuccess ()
  {
    final IOutboundSendingAttemptManager aMgr = APJdbcMetaManager.getOutboundSendingAttemptMgr ();
    final String sTxID = _createOutboundTx ();
    assertNotNull (sTxID);

    final String sAttemptID = aMgr.create (sTxID,
                                           "as4-msg-" + UUID.randomUUID ().toString (),
                                           _now (),
                                           "receipt-msg-001",
                                           Integer.valueOf (200),
                                           EAttemptStatus.SUCCESS,
                                           null,
                                           null);
    assertNotNull (sAttemptID);

    final ICommonsList <IOutboundSendingAttempt> aAttempts = aMgr.getByTransactionID (sTxID);
    assertEquals (1, aAttempts.size ());

    final IOutboundSendingAttempt aAttempt = aAttempts.getFirstOrNull ();
    assertNotNull (aAttempt);
    assertEquals (sAttemptID, aAttempt.getID ());
    assertEquals (sTxID, aAttempt.getOutboundTransactionID ());
    assertEquals (EAttemptStatus.SUCCESS, aAttempt.getAttemptStatus ());
    assertNotNull (aAttempt.getAttemptDT ());
    assertNull (aAttempt.getErrorDetails ());
  }

  @Test
  public void testSendingAttemptCreateFailure ()
  {
    final IOutboundSendingAttemptManager aMgr = APJdbcMetaManager.getOutboundSendingAttemptMgr ();
    final String sTxID = _createOutboundTx ();
    assertNotNull (sTxID);

    final String sAttemptID = aMgr.create (sTxID,
                                           "as4-msg-" + UUID.randomUUID ().toString (),
                                           _now (),
                                           null,
                                           null,
                                           EAttemptStatus.FAILED,
                                           "Connection refused",
                                           null);
    assertNotNull (sAttemptID);

    final IOutboundSendingAttempt aAttempt = aMgr.getByTransactionID (sTxID).getFirstOrNull ();
    assertNotNull (aAttempt);
    assertEquals (EAttemptStatus.FAILED, aAttempt.getAttemptStatus ());
    assertEquals ("Connection refused", aAttempt.getErrorDetails ());
  }

  @Test
  public void testSendingAttemptMultiple ()
  {
    final IOutboundSendingAttemptManager aMgr = APJdbcMetaManager.getOutboundSendingAttemptMgr ();
    final String sTxID = _createOutboundTx ();
    assertNotNull (sTxID);

    final OffsetDateTime aNow = _now ();
    aMgr.create (sTxID, _uniqueID (), aNow, null, null, EAttemptStatus.FAILED, "Timeout", null);
    aMgr.create (sTxID,
                 _uniqueID (),
                 aNow.plusMinutes (5),
                 null,
                 Integer.valueOf (503),
                 EAttemptStatus.FAILED,
                 "Service unavailable",
                 null);
    aMgr.create (sTxID,
                 _uniqueID (),
                 aNow.plusMinutes (10),
                 "receipt-002",
                 Integer.valueOf (200),
                 EAttemptStatus.SUCCESS,
                 null,
                 null);

    final ICommonsList <IOutboundSendingAttempt> aAttempts = aMgr.getByTransactionID (sTxID);
    assertEquals (3, aAttempts.size ());
    assertEquals (EAttemptStatus.FAILED, aAttempts.get (0).getAttemptStatus ());
    assertEquals (EAttemptStatus.FAILED, aAttempts.get (1).getAttemptStatus ());
    assertEquals (EAttemptStatus.SUCCESS, aAttempts.get (2).getAttemptStatus ());
  }

  @Test
  public void testSendingAttemptGetByTransactionIDEmpty ()
  {
    final ICommonsList <IOutboundSendingAttempt> aAttempts = APJdbcMetaManager.getOutboundSendingAttemptMgr ()
                                                                              .getByTransactionID ("non-existent-tx");
    assertNotNull (aAttempts);
    assertTrue (aAttempts.isEmpty ());
  }

  // --- InboundForwardingAttemptManager ---

  @Test
  public void testForwardingAttemptCreateSuccess ()
  {
    final IInboundForwardingAttemptManager aMgr = APJdbcMetaManager.getInboundForwardingAttemptMgr ();
    final String sTxID = _createInboundTx ();
    assertNotNull (sTxID);

    final String sAttemptID = aMgr.createSuccess (sTxID);
    assertNotNull (sAttemptID);

    final ICommonsList <IInboundForwardingAttempt> aAttempts = aMgr.getByTransactionID (sTxID);
    assertEquals (1, aAttempts.size ());

    final IInboundForwardingAttempt aAttempt = aAttempts.getFirstOrNull ();
    assertNotNull (aAttempt);
    assertEquals (sAttemptID, aAttempt.getID ());
    assertEquals (sTxID, aAttempt.getInboundTransactionID ());
    assertEquals (EAttemptStatus.SUCCESS, aAttempt.getAttemptStatus ());
    assertNotNull (aAttempt.getAttemptDT ());
    assertNull (aAttempt.getErrorCode ());
    assertNull (aAttempt.getErrorDetails ());
  }

  @Test
  public void testForwardingAttemptCreateFailure ()
  {
    final IInboundForwardingAttemptManager aMgr = APJdbcMetaManager.getInboundForwardingAttemptMgr ();
    final String sTxID = _createInboundTx ();
    assertNotNull (sTxID);

    final String sAttemptID = aMgr.createFailure (sTxID, "HTTP_500", "Internal server error");
    assertNotNull (sAttemptID);

    final IInboundForwardingAttempt aAttempt = aMgr.getByTransactionID (sTxID).getFirstOrNull ();
    assertNotNull (aAttempt);
    assertEquals (EAttemptStatus.FAILED, aAttempt.getAttemptStatus ());
    assertEquals ("HTTP_500", aAttempt.getErrorCode ());
    assertEquals ("Internal server error", aAttempt.getErrorDetails ());
  }

  @Test
  public void testForwardingAttemptMultiple ()
  {
    final IInboundForwardingAttemptManager aMgr = APJdbcMetaManager.getInboundForwardingAttemptMgr ();
    final String sTxID = _createInboundTx ();
    assertNotNull (sTxID);

    aMgr.createFailure (sTxID, "HTTP_503", "Service unavailable");
    aMgr.createFailure (sTxID, "HTTP_502", "Bad gateway");
    aMgr.createSuccess (sTxID);

    final ICommonsList <IInboundForwardingAttempt> aAttempts = aMgr.getByTransactionID (sTxID);
    assertEquals (3, aAttempts.size ());
    assertEquals (EAttemptStatus.FAILED, aAttempts.get (0).getAttemptStatus ());
    assertEquals (EAttemptStatus.FAILED, aAttempts.get (1).getAttemptStatus ());
    assertEquals (EAttemptStatus.SUCCESS, aAttempts.get (2).getAttemptStatus ());
  }

  @Test
  public void testForwardingAttemptGetByTransactionIDEmpty ()
  {
    final ICommonsList <IInboundForwardingAttempt> aAttempts = APJdbcMetaManager.getInboundForwardingAttemptMgr ()
                                                                                .getByTransactionID ("non-existent-tx");
    assertNotNull (aAttempts);
    assertTrue (aAttempts.isEmpty ());
  }

  // --- ArchivalManager ---

  @Test
  public void testArchiveOutboundTransaction ()
  {
    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundSendingAttemptManager aAttemptMgr = APJdbcMetaManager.getOutboundSendingAttemptMgr ();
    final IArchivalManager aArchivalMgr = APJdbcMetaManager.getArchivalMgr ();

    final String sTxID = _createOutboundTx ();
    assertNotNull (sTxID);

    aAttemptMgr.create (sTxID,
                        _uniqueID (),
                        _now (),
                        "receipt-archive",
                        Integer.valueOf (200),
                        EAttemptStatus.SUCCESS,
                        null,
                        null);
    aTxMgr.updateStatusCompleted (sTxID, EOutboundStatus.SENT);

    assertTrue (aArchivalMgr.archiveOutboundTransaction (sTxID).isSuccess ());

    assertNull (aTxMgr.getByID (sTxID));
    assertTrue (aAttemptMgr.getByTransactionID (sTxID).isEmpty ());
  }

  @Test
  public void testArchiveInboundTransaction ()
  {
    final IInboundTransactionManager aTxMgr = APJdbcMetaManager.getInboundTransactionMgr ();
    final IInboundForwardingAttemptManager aAttemptMgr = APJdbcMetaManager.getInboundForwardingAttemptMgr ();
    final IArchivalManager aArchivalMgr = APJdbcMetaManager.getArchivalMgr ();

    final String sTxID = _createInboundTx ();
    assertNotNull (sTxID);

    aAttemptMgr.createSuccess (sTxID);
    aTxMgr.updateStatusCompleted (sTxID, EInboundStatus.FORWARDED);

    assertTrue (aArchivalMgr.archiveInboundTransaction (sTxID).isSuccess ());

    assertNull (aTxMgr.getByID (sTxID));
    assertTrue (aAttemptMgr.getByTransactionID (sTxID).isEmpty ());
  }

  // --- AS4DuplicateManagerJdbc ---

  @Test
  public void testAS4DuplicateRegisterAndCheck ()
  {
    final IAS4DuplicateManager aMgr = APJdbcMetaManager.getAS4DuplicateMgr ();

    final String sMessageID = _uniqueID ();
    assertEquals (EContinue.CONTINUE, aMgr.registerAndCheck (sMessageID, "profile-1", "pmode-1"));
    // Second call with same ID must return BREAK
    assertEquals (EContinue.BREAK, aMgr.registerAndCheck (sMessageID, "profile-1", "pmode-1"));

    final IAS4DuplicateItem aItem = aMgr.getItemOfMessageID (sMessageID);
    assertNotNull (aItem);
    assertEquals (sMessageID, aItem.getMessageID ());
    assertEquals ("profile-1", aItem.getProfileID ());
    assertEquals ("pmode-1", aItem.getPModeID ());
    assertNotNull (aItem.getDateTime ());
  }

  @Test
  public void testAS4DuplicateNullOrEmptyMessageID ()
  {
    final IAS4DuplicateManager aMgr = APJdbcMetaManager.getAS4DuplicateMgr ();

    // Null/empty must NOT be persisted and must always return CONTINUE
    assertEquals (EContinue.CONTINUE, aMgr.registerAndCheck (null, "p", "m"));
    assertEquals (EContinue.CONTINUE, aMgr.registerAndCheck ("", "p", "m"));
    assertNull (aMgr.getItemOfMessageID (null));
    assertNull (aMgr.getItemOfMessageID (""));
  }

  @Test
  public void testAS4DuplicateNullProfileAndPMode ()
  {
    final IAS4DuplicateManager aMgr = APJdbcMetaManager.getAS4DuplicateMgr ();

    final String sMessageID = _uniqueID ();
    assertEquals (EContinue.CONTINUE, aMgr.registerAndCheck (sMessageID, null, null));

    final IAS4DuplicateItem aItem = aMgr.getItemOfMessageID (sMessageID);
    assertNotNull (aItem);
    assertNull (aItem.getProfileID ());
    assertNull (aItem.getPModeID ());
  }

  @Test
  public void testAS4DuplicateEvictAllItemsBefore ()
  {
    final IAS4DuplicateManager aMgr = APJdbcMetaManager.getAS4DuplicateMgr ();

    final String sMessageID = _uniqueID ();
    assertEquals (EContinue.CONTINUE, aMgr.registerAndCheck (sMessageID, null, null));
    assertNotNull (aMgr.getItemOfMessageID (sMessageID));

    // Evict everything strictly older than "far future" - must include our row
    final OffsetDateTime aFarFuture = _now ().plusDays (1);
    final ICommonsList <String> aEvicted = aMgr.evictAllItemsBefore (aFarFuture);
    assertTrue (aEvicted.contains (sMessageID));
    assertNull (aMgr.getItemOfMessageID (sMessageID));
  }

  @Test
  public void testAS4DuplicateEvictKeepsRecent ()
  {
    final IAS4DuplicateManager aMgr = APJdbcMetaManager.getAS4DuplicateMgr ();

    final String sMessageID = _uniqueID ();
    assertEquals (EContinue.CONTINUE, aMgr.registerAndCheck (sMessageID, null, null));

    // Evict items older than 1 day ago - the row we just inserted must survive
    final OffsetDateTime aFarPast = _now ().minusDays (1);
    final ICommonsList <String> aEvicted = aMgr.evictAllItemsBefore (aFarPast);
    assertFalse (aEvicted.contains (sMessageID));
    assertNotNull (aMgr.getItemOfMessageID (sMessageID));
  }

  @Test
  public void testAS4DuplicateClearCache ()
  {
    final IAS4DuplicateManager aMgr = APJdbcMetaManager.getAS4DuplicateMgr ();

    final String sMessageID = _uniqueID ();
    assertEquals (EContinue.CONTINUE, aMgr.registerAndCheck (sMessageID, null, null));
    assertFalse (aMgr.isEmpty ());

    assertEquals (EChange.CHANGED, aMgr.clearCache ());
    assertTrue (aMgr.isEmpty ());
    // A second clear on an empty table must be UNCHANGED
    assertEquals (EChange.UNCHANGED, aMgr.clearCache ());
  }
}
