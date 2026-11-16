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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.PeppolReportingJsonHelper;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreConfig;

final class PeppolReportingRecordsTest
{
  private static final String PATH = "/api/reporting/records/2026/1";
  private final MockMvc m_aMvc = MockMvcBuilders.standaloneSetup (new PeppolReportingController ())
                                               .addFilters (new ApiTokenFilter ())
                                               .build ();

  @Test
  void testRejectsUnconfiguredToken () throws Exception
  {
    try (final MockedStatic <APCoreConfig> aConfig = mockStatic (APCoreConfig.class))
    {
      aConfig.when (APCoreConfig::getPhase4ApiRequiredToken).thenReturn ("");
      m_aMvc.perform (get (PATH).servletPath (PATH)).andExpect (status ().isUnauthorized ());
    }
  }

  @Test
  void testExportAndReadFailure () throws Exception
  {
    final IPeppolReportingBackendSPI aBackend = mock (IPeppolReportingBackendSPI.class,
                                                    org.mockito.Mockito.CALLS_REAL_METHODS);
    when (aBackend.isInitialized ()).thenReturn (true);
    final String sRecord = "{\"version\":1,\"exchangedt\":\"2026-01-04T10:15:30Z\",\"direction\":\"send\"," +
                           "\"c2id\":\"POP000001\",\"c3id\":\"POP000002\",\"dtidscheme\":\"busdox-docid-qns\"," +
                           "\"dtidvalue\":\"invoice\",\"procidscheme\":\"cenbii-procid-ubl\",\"procidvalue\":\"billing\"," +
                           "\"transportid\":\"peppol-transport-as4-v2_0\",\"c1cc\":\"FI\",\"enduserid\":\"test-user\"}";
    final PeppolReportingItem aItem = PeppolReportingJsonHelper.toDomain (sRecord);
    doReturn (List.of (aItem)).when (aBackend).iterateReportingItems (any (YearMonth.class));
    final IAPTimestampManager aClock = () -> OffsetDateTime.parse ("2026-02-01T00:00:00Z");
    try (final MockedStatic <APCoreConfig> aConfig = mockStatic (APCoreConfig.class);
         final MockedStatic <APBasicMetaManager> aBasic = mockStatic (APBasicMetaManager.class);
         final MockedStatic <PeppolReportingBackend> aBackendAccess = mockStatic (PeppolReportingBackend.class,
                                                                                org.mockito.Mockito.CALLS_REAL_METHODS))
    {
      aConfig.when (APCoreConfig::getPhase4ApiRequiredToken).thenReturn ("test-secret");
      aBasic.when (APBasicMetaManager::getTimestampMgr).thenReturn (aClock);
      aBackendAccess.when (PeppolReportingBackend::getBackendService).thenReturn (aBackend);
      final String sBody = m_aMvc.perform (get ("/ap" + PATH).contextPath ("/ap").servletPath (PATH)
                                                          .header ("X-Token", "test-secret"))
                                .andExpect (status ().isOk ())
                                .andExpect (content ().contentTypeCompatibleWith ("application/json"))
                                .andReturn ().getResponse ().getContentAsString ();
      assertEquals ("[" + PeppolReportingJsonHelper.toJsonString (aItem) + "]", sBody);
      doReturn (List.of ()).when (aBackend).iterateReportingItems (any (YearMonth.class));
      m_aMvc.perform (get (PATH).servletPath (PATH).header ("X-Token", "test-secret"))
            .andExpect (status ().isOk ()).andExpect (content ().string ("[]"));
      doAnswer (aInvocation -> {
        final Consumer <PeppolReportingItem> aConsumer = aInvocation.getArgument (1);
        aConsumer.accept (aItem);
        throw new PeppolReportingBackendException ("Read failed after the first record");
      }).when (aBackend).forEachReportingItem (any (YearMonth.class), any ());
      m_aMvc.perform (get (PATH).servletPath (PATH).header ("X-Token", "test-secret"))
            .andExpect (status ().isInternalServerError ())
            .andExpect (content ().string ("{\"error\":\"Failed to read reporting records\"}"));
    }
  }
}
