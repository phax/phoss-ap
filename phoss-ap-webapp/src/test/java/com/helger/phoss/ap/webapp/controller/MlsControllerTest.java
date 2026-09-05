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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.helger.base.state.ESuccess;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.dto.MlsSendIssue;
import com.helger.phoss.ap.api.dto.MlsSendRequest;
import com.helger.phoss.ap.api.dto.ReportResponse;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.mls.MlsCreationResult;
import com.helger.phoss.ap.core.mls.MlsHandler;
import com.helger.phoss.ap.core.outbound.MlsSmpFallback;
import com.helger.phoss.ap.db.APJdbcMetaManager;

/**
 * Test class for {@link MlsController}.
 *
 * @author Philip Helger
 */
final class MlsControllerTest
{
  private static final String SBDH_ID = "550e8400-e29b-41d4-a716-446655440000";

  private final MlsController m_aController = new MlsController ();

  private static MlsSendRequest _request (final String sResponseCode)
  {
    final MlsSendRequest ret = new MlsSendRequest ();
    ret.setSbdhInstanceID (SBDH_ID);
    ret.setResponseCode (sResponseCode);
    return ret;
  }

  private static IInboundTransaction _businessDocumentTx ()
  {
    final IInboundTransaction ret = mock (IInboundTransaction.class);
    when (ret.getID ()).thenReturn ("tx-123");
    when (ret.getSbdhInstanceID ()).thenReturn (SBDH_ID);
    when (ret.getDocTypeID ()).thenReturn ("busdox-docid-qns::urn:test:invoice");
    when (ret.getProcessID ()).thenReturn ("cenbii-procid-ubl::urn:test:process");
    when (ret.getMlsType ()).thenReturn (EPeppolMLSType.ALWAYS_SEND);
    return ret;
  }

  @Test
  void testSendMlsGloballyDisabled ()
  {
    try (final MockedStatic <APCoreConfig> aMockConfig = mockStatic (APCoreConfig.class))
    {
      aMockConfig.when (APCoreConfig::isMlsSendingEnabled).thenReturn (false);

      final ResponseEntity <ReportResponse> aResp = m_aController.sendMls (_request ("AP"));
      assertEquals (HttpStatus.SERVICE_UNAVAILABLE, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("disabled", aResp.getBody ().getStatus ());
    }
  }

  @Test
  void testSendMlsWithoutSbdhInstanceID ()
  {
    try (final MockedStatic <APCoreConfig> aMockConfig = mockStatic (APCoreConfig.class))
    {
      aMockConfig.when (APCoreConfig::isMlsSendingEnabled).thenReturn (true);

      final MlsSendRequest aRequest = _request ("AP");
      aRequest.setSbdhInstanceID (null);

      final ResponseEntity <ReportResponse> aResp = m_aController.sendMls (aRequest);
      assertEquals (HttpStatus.BAD_REQUEST, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("invalid", aResp.getBody ().getStatus ());
    }
  }

  @Test
  void testSendMlsWithUnknownResponseCode ()
  {
    try (final MockedStatic <APCoreConfig> aMockConfig = mockStatic (APCoreConfig.class))
    {
      aMockConfig.when (APCoreConfig::isMlsSendingEnabled).thenReturn (true);

      final ResponseEntity <ReportResponse> aResp = m_aController.sendMls (_request ("XX"));
      assertEquals (HttpStatus.BAD_REQUEST, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("invalid", aResp.getBody ().getStatus ());
    }
  }

  @Test
  void testSendMlsRejectionWithoutIssues ()
  {
    try (final MockedStatic <APCoreConfig> aMockConfig = mockStatic (APCoreConfig.class))
    {
      aMockConfig.when (APCoreConfig::isMlsSendingEnabled).thenReturn (true);

      final ResponseEntity <ReportResponse> aResp = m_aController.sendMls (_request ("RE"));
      assertEquals (HttpStatus.BAD_REQUEST, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("invalid", aResp.getBody ().getStatus ());
    }
  }

  @Test
  void testSendMlsWithUnknownStatusReasonCode ()
  {
    try (final MockedStatic <APCoreConfig> aMockConfig = mockStatic (APCoreConfig.class))
    {
      aMockConfig.when (APCoreConfig::isMlsSendingEnabled).thenReturn (true);

      final MlsSendIssue aIssue = new MlsSendIssue ();
      aIssue.setStatusReasonCode ("XX");
      aIssue.setErrorField ("NA");
      aIssue.setDescription ("Whatever");

      final MlsSendRequest aRequest = _request ("RE");
      aRequest.setIssues (List.of (aIssue));

      final ResponseEntity <ReportResponse> aResp = m_aController.sendMls (aRequest);
      assertEquals (HttpStatus.BAD_REQUEST, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("invalid", aResp.getBody ().getStatus ());
    }
  }

  @Test
  void testSendMlsUnknownTransaction ()
  {
    try (final MockedStatic <APCoreConfig> aMockConfig = mockStatic (APCoreConfig.class);
         final MockedStatic <APJdbcMetaManager> aMockJdbc = mockStatic (APJdbcMetaManager.class))
    {
      aMockConfig.when (APCoreConfig::isMlsSendingEnabled).thenReturn (true);

      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      when (aTxMgr.getBySbdhInstanceID (SBDH_ID)).thenReturn (null);
      aMockJdbc.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final ResponseEntity <ReportResponse> aResp = m_aController.sendMls (_request ("AP"));
      assertEquals (HttpStatus.NOT_FOUND, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("not-found", aResp.getBody ().getStatus ());
    }
  }

  @Test
  void testSendMlsForInboundMls ()
  {
    try (final MockedStatic <APCoreConfig> aMockConfig = mockStatic (APCoreConfig.class);
         final MockedStatic <APJdbcMetaManager> aMockJdbc = mockStatic (APJdbcMetaManager.class))
    {
      aMockConfig.when (APCoreConfig::isMlsSendingEnabled).thenReturn (true);

      final IInboundTransaction aTx = _businessDocumentTx ();
      when (aTx.getDocTypeID ()).thenReturn (EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getURIEncoded ());
      when (aTx.getProcessID ()).thenReturn (EPredefinedProcessIdentifier.urn_peppol_edec_mls.getURIEncoded ());

      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      when (aTxMgr.getBySbdhInstanceID (SBDH_ID)).thenReturn (aTx);
      aMockJdbc.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final ResponseEntity <ReportResponse> aResp = m_aController.sendMls (_request ("AP"));
      assertEquals (HttpStatus.UNPROCESSABLE_ENTITY, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("not-eligible", aResp.getBody ().getStatus ());
    }
  }

  @Test
  void testSendMlsTwice ()
  {
    try (final MockedStatic <APCoreConfig> aMockConfig = mockStatic (APCoreConfig.class);
         final MockedStatic <APJdbcMetaManager> aMockJdbc = mockStatic (APJdbcMetaManager.class);
         final MockedStatic <MlsHandler> aMockHandler = mockStatic (MlsHandler.class))
    {
      aMockConfig.when (APCoreConfig::isMlsSendingEnabled).thenReturn (true);

      final IInboundTransaction aTx = _businessDocumentTx ();
      when (aTx.getMlsResponseCode ()).thenReturn (EPeppolMLSResponseCode.ACCEPTANCE);

      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      when (aTxMgr.getBySbdhInstanceID (SBDH_ID)).thenReturn (aTx);
      aMockJdbc.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final ResponseEntity <ReportResponse> aResp = m_aController.sendMls (_request ("AP"));
      assertEquals (HttpStatus.CONFLICT, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("conflict", aResp.getBody ().getStatus ());

      // Nothing was created and nothing was sent
      aMockHandler.verify (() -> MlsHandler.createInboundResultMls (any (), any ()), never ());
    }
  }

  @Test
  void testSendMlsSuccess ()
  {
    try (final MockedStatic <APCoreConfig> aMockConfig = mockStatic (APCoreConfig.class);
         final MockedStatic <APJdbcMetaManager> aMockJdbc = mockStatic (APJdbcMetaManager.class);
         final MockedStatic <MlsHandler> aMockHandler = mockStatic (MlsHandler.class))
    {
      aMockConfig.when (APCoreConfig::isMlsSendingEnabled).thenReturn (true);

      final IInboundTransaction aTx = _businessDocumentTx ();
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      when (aTxMgr.getBySbdhInstanceID (SBDH_ID)).thenReturn (aTx);
      aMockJdbc.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final IOutboundTransaction aMlsTx = mock (IOutboundTransaction.class);
      when (aMlsTx.getID ()).thenReturn ("mls-tx-456");
      final MlsSmpFallback aFallback = new MlsSmpFallback (mock (IParticipantIdentifier.class), SBDH_ID);
      final MlsCreationResult aCreationResult = MlsCreationResult.created (EPeppolMLSResponseCode.ACCEPTANCE,
                                                                          aMlsTx,
                                                                          aFallback);
      aMockHandler.when (() -> MlsHandler.createInboundResultMls (any (), any ())).thenReturn (aCreationResult);

      final ResponseEntity <ReportResponse> aResp = m_aController.sendMls (_request ("AP"));
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("sent", aResp.getBody ().getStatus ());
      assertEquals ("tx-123", aResp.getBody ().getTransactionID ());

      // The AS4 transmission happens in the background
      aMockHandler.verify (() -> MlsHandler.sendCreatedMlsAsync (aCreationResult), times (1));
    }
  }

  @Test
  void testSendMlsRecordedOnly ()
  {
    try (final MockedStatic <APCoreConfig> aMockConfig = mockStatic (APCoreConfig.class);
         final MockedStatic <APJdbcMetaManager> aMockJdbc = mockStatic (APJdbcMetaManager.class);
         final MockedStatic <MlsHandler> aMockHandler = mockStatic (MlsHandler.class))
    {
      aMockConfig.when (APCoreConfig::isMlsSendingEnabled).thenReturn (true);

      final IInboundTransaction aTx = _businessDocumentTx ();
      when (aTx.getMlsType ()).thenReturn (EPeppolMLSType.FAILURE_ONLY);

      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      when (aTxMgr.getBySbdhInstanceID (SBDH_ID)).thenReturn (aTx);
      aMockJdbc.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      final MlsCreationResult aCreationResult = MlsCreationResult.suppressed (ESuccess.SUCCESS,
                                                                             EPeppolMLSResponseCode.ACCEPTANCE);
      aMockHandler.when (() -> MlsHandler.createInboundResultMls (any (), any ())).thenReturn (aCreationResult);

      final ResponseEntity <ReportResponse> aResp = m_aController.sendMls (_request ("AP"));
      assertEquals (HttpStatus.OK, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("recorded", aResp.getBody ().getStatus ());

      aMockHandler.verify (() -> MlsHandler.sendCreatedMlsAsync (any ()), never ());
    }
  }

  @Test
  void testSendMlsCreationFailed ()
  {
    try (final MockedStatic <APCoreConfig> aMockConfig = mockStatic (APCoreConfig.class);
         final MockedStatic <APJdbcMetaManager> aMockJdbc = mockStatic (APJdbcMetaManager.class);
         final MockedStatic <MlsHandler> aMockHandler = mockStatic (MlsHandler.class))
    {
      aMockConfig.when (APCoreConfig::isMlsSendingEnabled).thenReturn (true);

      final IInboundTransaction aTx = _businessDocumentTx ();
      final IInboundTransactionManager aTxMgr = mock (IInboundTransactionManager.class);
      when (aTxMgr.getBySbdhInstanceID (SBDH_ID)).thenReturn (aTx);
      aMockJdbc.when (APJdbcMetaManager::getInboundTransactionMgr).thenReturn (aTxMgr);

      aMockHandler.when (() -> MlsHandler.createInboundResultMls (any (), any ()))
                  .thenReturn (MlsCreationResult.failure (EPeppolMLSResponseCode.ACCEPTANCE));

      final ResponseEntity <ReportResponse> aResp = m_aController.sendMls (_request ("AP"));
      assertEquals (HttpStatus.INTERNAL_SERVER_ERROR, aResp.getStatusCode ());
      assertNotNull (aResp.getBody ());
      assertEquals ("failed", aResp.getBody ().getStatus ());

      aMockHandler.verify (() -> MlsHandler.sendCreatedMlsAsync (any ()), never ());
    }
  }
}
