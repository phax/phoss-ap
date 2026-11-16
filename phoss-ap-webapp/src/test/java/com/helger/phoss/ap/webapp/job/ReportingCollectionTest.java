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
package com.helger.phoss.ap.webapp.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.peppol.reporting.api.PeppolReportingItem;

final class ReportingCollectionTest
{
  private static final YearMonth PERIOD = YearMonth.of (2026, 1);
  private static final List <String> KEYS = List.of ("database-type", "url", "user", "password", "driver", "schema");
  private String m_sURL;
  private ReportingCollectionStore m_aStore;

  @BeforeEach
  void setup () throws Exception
  {
    m_sURL = "jdbc:h2:mem:collection_" + UUID.randomUUID () + ";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    final String [] aValues = { "h2", m_sURL, "sa", "", "org.h2.Driver", "public" };
    for (int i = 0; i < KEYS.size (); ++i)
      System.setProperty ("peppol.reporting.jdbc." + KEYS.get (i), aValues[i]);
    m_aStore = new ReportingCollectionStore (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
    m_aStore.initialize ();
    sql ("CREATE TABLE peppol_reporting_item (exchangedt timestamp(3) NOT NULL, sending boolean NOT NULL," +
         " c2id varchar(64) NOT NULL, c3id varchar(64) NOT NULL, dtscheme varchar(64) NOT NULL," +
         " dtvalue varchar(500) NOT NULL, procscheme varchar(64) NOT NULL, procvalue varchar(200) NOT NULL," +
         " tp varchar(64) NOT NULL, c1cc varchar(2) NOT NULL, c4cc varchar(2), enduserid varchar(256) NOT NULL)");
  }

  @AfterEach
  void cleanup () throws Exception
  {
    sql ("DROP ALL OBJECTS");
    for (final String sKey : KEYS)
      System.clearProperty ("peppol.reporting.jdbc." + sKey);
  }

  private void sql (final String sSQL) throws Exception
  {
    try (final Connection aConnection = DriverManager.getConnection (m_sURL, "sa", "");
         final Statement aStatement = aConnection.createStatement ())
    {
      aStatement.execute (sSQL);
    }
  }

  private int count (final String sTable) throws Exception
  {
    try (final Connection aConnection = DriverManager.getConnection (m_sURL, "sa", "");
         final Statement aStatement = aConnection.createStatement ();
         final ResultSet aRows = aStatement.executeQuery ("SELECT COUNT(*) FROM " + sTable))
    {
      aRows.next ();
      return aRows.getInt (1);
    }
  }

  private List <PeppolReportingItem> records ()
  {
    final String sJSON = "[{\"version\":1,\"exchangedt\":\"2026-01-04T10:15:30Z\",\"direction\":\"send\"," +
                          "\"c2id\":\"POP000001\",\"c3id\":\"POP000002\",\"dtidscheme\":\"busdox-docid-qns\"," +
                          "\"dtidvalue\":\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1\"," +
                          "\"procidscheme\":\"cenbii-procid-ubl\",\"procidvalue\":\"billing\"," +
                          "\"transportid\":\"peppol-transport-as4-v2_0\",\"c1cc\":\"FI\",\"enduserid\":\"user\"}]";
    return ReportingCollectionJob.parseRecords (sJSON, PERIOD);
  }

  @Test
  void testAtomicMoveAndResumeWithoutRefetch () throws Exception
  {
    final var aClaim = m_aStore.claim ("ap-a", PERIOD, Instant.now (), Duration.ofHours (12));
    final var aRecords = records ();
    m_aStore.stage (aClaim, List.of (aRecords.get (0), aRecords.get (0)));
    sql ("UPDATE peppol_reporting_collection_queue SET payload_json = REPLACE(payload_json, '\"version\":1', '\"version\":999') WHERE item_index = 1");
    assertThrows (IllegalArgumentException.class, () -> m_aStore.transfer (aClaim));
    assertEquals (0, count ("peppol_reporting_item"));
    assertEquals (2, count ("peppol_reporting_collection_queue"));
    sql ("UPDATE peppol_reporting_collection_queue SET payload_json = REPLACE(payload_json, '\"version\":999', '\"version\":1') WHERE item_index = 1");
    sql ("ALTER TABLE peppol_reporting_collection ADD CONSTRAINT fail_done CHECK(state <> 'DONE')");
    assertThrows (java.sql.SQLException.class, () -> m_aStore.transfer (aClaim));
    assertEquals (0, count ("peppol_reporting_item"));
    assertEquals (2, count ("peppol_reporting_collection_queue"));
    m_aStore.fail (aClaim);
    final var aRetry = m_aStore.claim ("ap-a", PERIOD, Instant.now (), Duration.ofHours (12));
    assertTrue (aRetry.fetched ());
    sql ("ALTER TABLE peppol_reporting_collection DROP CONSTRAINT fail_done");
    m_aStore.transfer (aRetry);
    assertEquals (2, count ("peppol_reporting_item"));
    assertEquals (0, count ("peppol_reporting_collection_queue"));
    assertNull (m_aStore.claim ("ap-a", PERIOD, Instant.now (), Duration.ofHours (12)));
    final var aEmpty = m_aStore.claim ("ap-b", PERIOD, Instant.now (), Duration.ofHours (12));
    m_aStore.stage (aEmpty, List.of ());
    m_aStore.transfer (aEmpty);
    assertNull (m_aStore.claim ("ap-b", PERIOD, Instant.now (), Duration.ofHours (12)));
  }

  @Test
  void testExpiredOwnerCannotStageOrImport () throws Exception
  {
    final var aOld = m_aStore.claim ("ap-a", PERIOD, Instant.now (), Duration.ofHours (12));
    assertNull (m_aStore.claim ("ap-a", PERIOD, Instant.now (), Duration.ofHours (12)));
    sql ("UPDATE peppol_reporting_collection SET lease_until = TIMESTAMP '2000-01-01 00:00:00'");
    final var aNew = m_aStore.claim ("ap-a", PERIOD, Instant.now (), Duration.ofHours (12));
    assertThrows (java.sql.SQLException.class, () -> m_aStore.stage (aOld, records ()));
    m_aStore.stage (aNew, records ());
    assertThrows (java.sql.SQLException.class, () -> m_aStore.transfer (aOld));
    m_aStore.transfer (aNew);
    assertEquals (1, count ("peppol_reporting_item"));
  }

  @Test
  void testCollectionWindowAndMalformedSource ()
  {
    assertTrue (ReportingCollectionJob.isCollectionWindow (ZonedDateTime.parse ("2026-02-01T00:00:00Z"), 2, 6, 7));
    assertFalse (ReportingCollectionJob.isCollectionWindow (ZonedDateTime.parse ("2026-02-02T06:07:00Z"), 2, 6, 7));
    assertFalse (ReportingCollectionJob.isCollectionWindow (ZonedDateTime.parse ("2026-02-01T00:30:00+02:00"), 2, 6, 7));
    assertThrows (IllegalArgumentException.class, () -> ReportingCollectionJob.parseRecords ("[{}", PERIOD));
    assertThrows (IllegalArgumentException.class, () -> ReportingCollectionJob.parseRecords ("[null]", PERIOD));
  }
}
