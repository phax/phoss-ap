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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.ITransactionAuditManager;
import com.helger.phoss.ap.api.dto.InboundTransactionResponse;
import com.helger.phoss.ap.api.dto.OutboundTransactionResponse;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.api.model.ITransactionAuditItem;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.inbound.InboundOrchestrator;
import com.helger.phoss.ap.db.APJdbcMetaManager;

/**
 * Test class for {@link OperationsController}.
 *
 * @author Philip Helger
 */
final class OperationsControllerTest
{
  private final OperationsController m_aController = new OperationsController ();

  @Test
  void testGetInboundHistory ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMock = mockStatic (APJdbcMetaManager.class))
    {
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      when (aTxMgr.getAllTransactions (0, 50)).thenReturn (new CommonsArrayList <> ());
      aMock.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final ResponseEntity <List <InboundTransactionResponse>> aResp = m_aController.getInboundHistory (0, 50);
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals (0, aResp.getBody ().size ());
    }
  }

  @Test
  void testGetInboundSize ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMock = mockStatic (APJdbcMetaManager.class))
    {
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      when (Long.valueOf (aTxMgr.getTransactionCount ())).thenReturn (Long.valueOf (42));
      aMock.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final ResponseEntity <Long> aResp = m_aController.getInboundSize ();
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());
      assertEquals (Long.valueOf (42L), aResp.getBody ());
    }
  }

  @Test
  void testGetInboundPayloadNotFound ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMock = mockStatic (APJdbcMetaManager.class))
    {
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      when (aTxMgr.getBySbdhInstanceIDIncludingArchive ("missing-id")).thenReturn (null);
      aMock.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final ResponseEntity <byte []> aResp = m_aController.getInboundPayload ("missing-id");
      assertEquals (HttpStatus.NOT_FOUND, aResp.getStatusCode ());
    }
  }

  @Test
  void testGetInboundPayloadSuccess () throws Exception
  {
    try (final MockedStatic <APJdbcMetaManager> aMockJdbc = mockStatic (APJdbcMetaManager.class);
         final MockedStatic <APBasicMetaManager> aMockBasic = mockStatic (APBasicMetaManager.class))
    {
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      final IInboundTransaction aTx = mock (IInboundTransaction.class);
      when (aTx.getDocumentPath ()).thenReturn ("doc/path/test.xml");
      when (aTxMgr.getBySbdhInstanceIDIncludingArchive ("tx-123")).thenReturn (aTx);
      aMockJdbc.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final IDocumentPayloadManager aDocPayloadMgr = mock (IDocumentPayloadManager.class);
      final byte [] aBytes = { 1, 2, 3, 4 };
      when (aDocPayloadMgr.readDocument ("doc/path/test.xml")).thenReturn (aBytes);
      aMockBasic.when (APBasicMetaManager::getDocPayloadMgr).thenReturn (aDocPayloadMgr);

      final ResponseEntity <byte []> aResp = m_aController.getInboundPayload ("tx-123");
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());
      assertArrayEquals (aBytes, aResp.getBody ());
    }
  }

  @Test
  void testReplayInboundNotFound ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMock = mockStatic (APJdbcMetaManager.class))
    {
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      when (aTxMgr.getBySbdhInstanceIDIncludingArchive ("missing-id")).thenReturn (null);
      aMock.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final ResponseEntity <InboundTransactionResponse> aResp = m_aController.replayInbound ("missing-id");
      assertEquals (HttpStatus.NOT_FOUND, aResp.getStatusCode ());
    }
  }

  @Test
  void testReplayInboundSuccess ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMockJdbc = mockStatic (APJdbcMetaManager.class);
         final MockedStatic <InboundOrchestrator> aMockOrchestrator = mockStatic (InboundOrchestrator.class))
    {
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      final IInboundTransaction aTx = mock (IInboundTransaction.class);
      when (aTx.getStatus ()).thenReturn (com.helger.phoss.ap.api.codelist.EInboundStatus.FORWARDED);
      when (aTx.getReportingStatus ()).thenReturn (com.helger.phoss.ap.api.codelist.EReportingStatus.PENDING);
      when (aTx.getMlsType ()).thenReturn (com.helger.peppol.sbdh.EPeppolMLSType.ALWAYS_SEND);
      when (aTxMgr.getBySbdhInstanceIDIncludingArchive ("tx-123")).thenReturn (aTx);
      aMockJdbc.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      aMockOrchestrator.when (() -> InboundOrchestrator.forwardDocument ("API Replay: ", aTx))
                       .thenReturn (ESuccess.SUCCESS);

      final ResponseEntity <InboundTransactionResponse> aResp = m_aController.replayInbound ("tx-123");
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
    }
  }

  @Test
  void testGetOutboundHistory ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMock = mockStatic (APJdbcMetaManager.class))
    {
      final IOutboundTransactionManager aTxMgr = mock (IOutboundTransactionManager.class);
      when (aTxMgr.getAllTransactions (0, 50)).thenReturn (new CommonsArrayList <> ());
      aMock.when (APJdbcMetaManager::getOutboundTransactionMgr).thenReturn (aTxMgr);

      final ResponseEntity <List <OutboundTransactionResponse>> aResp = m_aController.getOutboundHistory (0, 50);
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals (0, aResp.getBody ().size ());
    }
  }

  @Test
  void testGetOutboundSize ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMock = mockStatic (APJdbcMetaManager.class))
    {
      final IOutboundTransactionManager aTxMgr = mock (IOutboundTransactionManager.class);
      when (Long.valueOf (aTxMgr.getTransactionCount ())).thenReturn (Long.valueOf (100L));
      aMock.when (APJdbcMetaManager::getOutboundTransactionMgr).thenReturn (aTxMgr);

      final ResponseEntity <Long> aResp = m_aController.getOutboundSize ();
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());
      assertEquals (Long.valueOf (100L), aResp.getBody ());
    }
  }

  @Test
  void testGetOutboundPayloadNotFound ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMock = mockStatic (APJdbcMetaManager.class))
    {
      final IOutboundTransactionManager aTxMgr = mock (IOutboundTransactionManager.class);
      when (aTxMgr.getBySbdhInstanceIDIncludingArchive ("missing-id")).thenReturn (null);
      aMock.when (APJdbcMetaManager::getOutboundTransactionMgr).thenReturn (aTxMgr);

      final ResponseEntity <byte []> aResp = m_aController.getOutboundPayload ("missing-id");
      assertEquals (HttpStatus.NOT_FOUND, aResp.getStatusCode ());
    }
  }

  @Test
  void testGetOutboundPayloadSuccess () throws Exception
  {
    try (final MockedStatic <APJdbcMetaManager> aMockJdbc = mockStatic (APJdbcMetaManager.class);
         final MockedStatic <APBasicMetaManager> aMockBasic = mockStatic (APBasicMetaManager.class))
    {
      final IOutboundTransactionManager aTxMgr = mock (IOutboundTransactionManager.class);
      final IOutboundTransaction aTx = mock (IOutboundTransaction.class);
      when (aTx.getDocumentPath ()).thenReturn ("doc/path/outbound.xml");
      when (aTxMgr.getBySbdhInstanceIDIncludingArchive ("out-123")).thenReturn (aTx);
      aMockJdbc.when (APJdbcMetaManager::getOutboundTransactionMgr).thenReturn (aTxMgr);

      final IDocumentPayloadManager aDocPayloadMgr = mock (IDocumentPayloadManager.class);
      final byte [] aBytes = { 5, 6, 7, 8 };
      when (aDocPayloadMgr.readDocument ("doc/path/outbound.xml")).thenReturn (aBytes);
      aMockBasic.when (APBasicMetaManager::getDocPayloadMgr).thenReturn (aDocPayloadMgr);

      final ResponseEntity <byte []> aResp = m_aController.getOutboundPayload ("out-123");
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());
      assertArrayEquals (aBytes, aResp.getBody ());
    }
  }

  @Test
  void testGetInboundTimelineNotFound ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMock = mockStatic (APJdbcMetaManager.class))
    {
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      when (aTxMgr.getBySbdhInstanceIDIncludingArchive ("missing-id")).thenReturn (null);
      aMock.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final ITransactionAuditManager aAuditMgr = mock (ITransactionAuditManager.class);
      when (aAuditMgr.getTimelineBySbdhInstanceID ("missing-id")).thenReturn (new CommonsArrayList <> ());
      aMock.when (APJdbcMetaManager::getTransactionAuditMgr).thenReturn (aAuditMgr);

      final ResponseEntity <com.helger.phoss.ap.api.dto.TransactionTimelineResponse> aResp = m_aController.getInboundTimeline ("missing-id");
      assertEquals (HttpStatus.NOT_FOUND, aResp.getStatusCode ());
    }
  }

  @Test
  void testGetInboundTimelineSuccess ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMock = mockStatic (APJdbcMetaManager.class))
    {
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      final IInboundTransaction aTx = mock (IInboundTransaction.class);
      when (aTxMgr.getBySbdhInstanceIDIncludingArchive ("tx-123")).thenReturn (aTx);
      aMock.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final ITransactionAuditManager aAuditMgr = mock (ITransactionAuditManager.class);
      final ITransactionAuditItem aItem = mock (ITransactionAuditItem.class);
      when (aItem.getID ()).thenReturn (1L);
      when (aItem.getTransactionID ()).thenReturn ("tx-uuid");
      when (aItem.getSbdhInstanceID ()).thenReturn ("tx-123");
      when (aItem.getDirection ()).thenReturn ("INBOUND");
      when (aItem.getEventType ()).thenReturn ("OPERATOR_ACTION");
      when (aItem.getAction ()).thenReturn ("REPLAY_INBOUND");
      when (aItem.getPerformedBy ()).thenReturn ("operator@agency.gov.au");

      when (aAuditMgr.getTimelineBySbdhInstanceID ("tx-123")).thenReturn (new CommonsArrayList <> (aItem));
      aMock.when (APJdbcMetaManager::getTransactionAuditMgr).thenReturn (aAuditMgr);

      final ResponseEntity <com.helger.phoss.ap.api.dto.TransactionTimelineResponse> aResp = m_aController.getInboundTimeline ("tx-123");
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("tx-123", aResp.getBody ().getSbdhInstanceID ());
      assertEquals ("INBOUND", aResp.getBody ().getDirection ());
      assertEquals (1, aResp.getBody ().getEvents ().size ());
      assertEquals ("OPERATOR_ACTION", aResp.getBody ().getEvents ().get (0).getEventType ());
      assertEquals ("operator@agency.gov.au", aResp.getBody ().getEvents ().get (0).getPerformedBy ());
    }
  }

  @Test
  void testGetOutboundTimelineNotFound ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMock = mockStatic (APJdbcMetaManager.class))
    {
      final IOutboundTransactionManager aTxMgr = mock (IOutboundTransactionManager.class);
      when (aTxMgr.getBySbdhInstanceIDIncludingArchive ("missing-id")).thenReturn (null);
      aMock.when (APJdbcMetaManager::getOutboundTransactionMgr).thenReturn (aTxMgr);

      final ITransactionAuditManager aAuditMgr = mock (ITransactionAuditManager.class);
      when (aAuditMgr.getTimelineBySbdhInstanceID ("missing-id")).thenReturn (null);
      aMock.when (APJdbcMetaManager::getTransactionAuditMgr).thenReturn (aAuditMgr);

      final ResponseEntity <com.helger.phoss.ap.api.dto.TransactionTimelineResponse> aResp = m_aController.getOutboundTimeline ("missing-id");
      assertEquals (HttpStatus.NOT_FOUND, aResp.getStatusCode ());
    }
  }

  @Test
  void testGetOutboundTimelineSuccess ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMock = mockStatic (APJdbcMetaManager.class))
    {
      final IOutboundTransactionManager aTxMgr = mock (IOutboundTransactionManager.class);
      final IOutboundTransaction aTx = mock (IOutboundTransaction.class);
      when (aTxMgr.getBySbdhInstanceIDIncludingArchive ("out-123")).thenReturn (aTx);
      aMock.when (APJdbcMetaManager::getOutboundTransactionMgr).thenReturn (aTxMgr);

      final ITransactionAuditManager aAuditMgr = mock (ITransactionAuditManager.class);
      final ITransactionAuditItem aItem = mock (ITransactionAuditItem.class);
      when (aItem.getID ()).thenReturn (2L);
      when (aItem.getTransactionID ()).thenReturn ("out-uuid");
      when (aItem.getSbdhInstanceID ()).thenReturn ("out-123");
      when (aItem.getDirection ()).thenReturn ("OUTBOUND");
      when (aItem.getEventType ()).thenReturn ("STATUS_CHANGE");
      when (aItem.getFromStatus ()).thenReturn ("PENDING");
      when (aItem.getToStatus ()).thenReturn ("SENT");

      when (aAuditMgr.getTimelineBySbdhInstanceID ("out-123")).thenReturn (new CommonsArrayList <> (aItem));
      aMock.when (APJdbcMetaManager::getTransactionAuditMgr).thenReturn (aAuditMgr);

      final ResponseEntity <com.helger.phoss.ap.api.dto.TransactionTimelineResponse> aResp = m_aController.getOutboundTimeline ("out-123");
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("out-123", aResp.getBody ().getSbdhInstanceID ());
      assertEquals ("OUTBOUND", aResp.getBody ().getDirection ());
      assertEquals (1, aResp.getBody ().getEvents ().size ());
      assertEquals ("STATUS_CHANGE", aResp.getBody ().getEvents ().get (0).getEventType ());
    }
  }

  @Test
  void testReplayInboundWithAuditUser ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMockJdbc = mockStatic (APJdbcMetaManager.class);
         final MockedStatic <InboundOrchestrator> aMockOrchestrator = mockStatic (InboundOrchestrator.class))
    {
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      final IInboundTransaction aTx = mock (IInboundTransaction.class);
      when (aTx.getID ()).thenReturn ("tx-id-999");
      when (aTx.getStatus ()).thenReturn (com.helger.phoss.ap.api.codelist.EInboundStatus.FORWARDED);
      when (aTx.getReportingStatus ()).thenReturn (com.helger.phoss.ap.api.codelist.EReportingStatus.PENDING);
      when (aTx.getMlsType ()).thenReturn (com.helger.peppol.sbdh.EPeppolMLSType.ALWAYS_SEND);
      when (aTx.getAttemptCount ()).thenReturn (1);
      when (aTxMgr.getBySbdhInstanceIDIncludingArchive ("tx-123")).thenReturn (aTx);
      aMockJdbc.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final ITransactionAuditManager aAuditMgr = mock (ITransactionAuditManager.class);
      aMockJdbc.when (APJdbcMetaManager::getTransactionAuditMgr).thenReturn (aAuditMgr);

      aMockOrchestrator.when (() -> InboundOrchestrator.forwardDocument ("API Replay: ", aTx))
                       .thenReturn (ESuccess.SUCCESS);

      final ResponseEntity <InboundTransactionResponse> aResp = m_aController.replayInbound ("tx-123",
                                                                                             "operator_alice");
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());

      org.mockito.Mockito.verify (aAuditMgr).recordAudit ("tx-id-999",
                                                         "tx-123",
                                                         "INBOUND",
                                                         "OPERATOR_ACTION",
                                                         "REPLAY_INBOUND",
                                                         "forwarded",
                                                         null,
                                                         Integer.valueOf (1),
                                                         "operator_alice",
                                                         "Manual operator replay initiated");
    }
  }

  @Test
  void testReverifyAndForwardInboundWithAuditUser ()
  {
    try (final MockedStatic <APJdbcMetaManager> aMockJdbc = mockStatic (APJdbcMetaManager.class);
         final MockedStatic <InboundOrchestrator> aMockOrchestrator = mockStatic (InboundOrchestrator.class))
    {
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      final IInboundTransaction aTx = mock (IInboundTransaction.class);
      when (aTx.getID ()).thenReturn ("tx-id-deferred");
      when (aTx.getStatus ()).thenReturn (com.helger.phoss.ap.api.codelist.EInboundStatus.VERIFICATION_DEFERRED);
      when (aTx.getReportingStatus ()).thenReturn (com.helger.phoss.ap.api.codelist.EReportingStatus.PENDING);
      when (aTx.getMlsType ()).thenReturn (com.helger.peppol.sbdh.EPeppolMLSType.ALWAYS_SEND);
      when (aTx.getAttemptCount ()).thenReturn (0);
      when (aTxMgr.getBySbdhInstanceID ("tx-deferred")).thenReturn (aTx);
      aMockJdbc.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final ITransactionAuditManager aAuditMgr = mock (ITransactionAuditManager.class);
      aMockJdbc.when (APJdbcMetaManager::getTransactionAuditMgr).thenReturn (aAuditMgr);

      aMockOrchestrator.when (() -> InboundOrchestrator.resumeDeferredInboundDocument ("API ReverifyAndForward: ", aTx))
                       .thenReturn (ESuccess.SUCCESS);

      final ResponseEntity <InboundTransactionResponse> aResp = m_aController.reverifyAndForwardInbound ("tx-deferred",
                                                                                                          "irap_auditor_42");
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());

      org.mockito.Mockito.verify (aAuditMgr).recordAudit ("tx-id-deferred",
                                                         "tx-deferred",
                                                         "INBOUND",
                                                         "OPERATOR_ACTION",
                                                         "REVERIFY_AND_FORWARD_INBOUND",
                                                         "verification_deferred",
                                                         null,
                                                         Integer.valueOf (0),
                                                         "irap_auditor_42",
                                                         "Manual operator reverify and forward initiated");
    }
  }
}
