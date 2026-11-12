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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import com.helger.config.IConfig;
import com.helger.db.api.helper.DBSystemHelper;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.PeppolReportingHelper;
import com.helger.peppol.reporting.api.PeppolReportingJsonHelper;
import com.helger.peppol.reporting.backend.sql.ReportingJdbcConfiguration;

/** Persists source-period leases and queue records in the reporting database. */
final class ReportingCollectionStore
{
  record Claim (String source, YearMonth period, String owner, boolean fetched)
  {}

  @FunctionalInterface
  private interface Transaction <T>
  {
    T run (Connection aConnection) throws SQLException;
  }

  private static final String COLUMNS = "exchangedt, sending, c2id, c3id, dtscheme, dtvalue, procscheme, procvalue, tp, c1cc, c4cc, enduserid";
  private static final String KEY = "source_id = ? AND report_period = ?";
  private final IConfig m_aConfig;
  private final ReportingJdbcConfiguration m_aJdbc;
  private final String m_sPrefix;
  private final String m_sDialect;

  ReportingCollectionStore (final IConfig aConfig)
  {
    m_aConfig = aConfig;
    m_aJdbc = new ReportingJdbcConfiguration (aConfig);
    if (m_aJdbc.getJdbcDatabaseSystemType () == null)
      throw new IllegalArgumentException ("A reporting database type is required for collection");
    m_sDialect = m_aJdbc.getJdbcDatabaseSystemType ().getID ();
    m_sPrefix = DBSystemHelper.getTableNamePrefix (m_aJdbc.getJdbcDatabaseSystemType (), m_aJdbc.getJdbcSchema ());
  }

  void initialize () throws SQLException
  {
    final String sDriver = m_aConfig.getAsString ("peppol.reporting.jdbc.driver");
    if (sDriver != null && !sDriver.isBlank ())
      try
      {
        Class.forName (sDriver);
      }
      catch (final ClassNotFoundException ex)
      {
        throw new SQLException ("Reporting JDBC driver is unavailable", ex);
      }
    if (m_aConfig.getAsBoolean ("peppol.reporting.flyway.enabled", true))
    {
      final FluentConfiguration aFlyway = Flyway.configure ()
                                               .dataSource (m_aJdbc.getJdbcUrl (), m_aJdbc.getJdbcUser (), m_aJdbc.getJdbcPassword ())
                                               .locations ("classpath:db/reporting-collection-" + m_sDialect)
                                               .table ("flyway_reporting_collection")
                                               .baselineOnMigrate (true)
                                               .baselineVersion ("0");
      final String sSchema = m_aJdbc.getJdbcSchema ();
      if (sSchema != null && !sSchema.isBlank ())
        aFlyway.schemas (sSchema).defaultSchema (sSchema);
      aFlyway.load ().migrate ();
    }
  }

  private String _table (final String sName)
  {
    return m_sPrefix + "peppol_reporting_" + sName;
  }

  private <T> T _transaction (final Transaction <T> aAction) throws SQLException
  {
    try (final Connection aConnection = DriverManager.getConnection (m_aJdbc.getJdbcUrl (),
                                                                    m_aJdbc.getJdbcUser (),
                                                                    m_aJdbc.getJdbcPassword ()))
    {
      aConnection.setAutoCommit (false);
      try
      {
        final T aResult = aAction.run (aConnection);
        aConnection.commit ();
        return aResult;
      }
      catch (final SQLException | RuntimeException ex)
      {
        try
        {
          aConnection.rollback ();
        }
        catch (final SQLException aRollbackError)
        {
          ex.addSuppressed (aRollbackError);
        }
        throw ex;
      }
    }
  }

  private static PreparedStatement _prepare (final Connection aConnection,
                                             final String sSQL,
                                             final Object... aParameters) throws SQLException
  {
    final PreparedStatement aStatement = aConnection.prepareStatement (sSQL);
    try
    {
      for (int i = 0; i < aParameters.length; ++i)
        aStatement.setObject (i + 1, aParameters[i]);
      return aStatement;
    }
    catch (final SQLException ex)
    {
      aStatement.close ();
      throw ex;
    }
  }

  private static int _update (final Connection aConnection, final String sSQL, final Object... aParameters) throws SQLException
  {
    try (final PreparedStatement aStatement = _prepare (aConnection, sSQL, aParameters))
    {
      return aStatement.executeUpdate ();
    }
  }

  private static Timestamp _utc (final Instant aInstant)
  {
    return Timestamp.valueOf (LocalDateTime.ofInstant (aInstant, ZoneOffset.UTC));
  }

  Claim claim (final String sSource, final YearMonth aPeriod, final Instant aNow, final Duration aLease) throws SQLException
  {
    final String sOwner = UUID.randomUUID ().toString ();
    try
    {
      return _transaction (aConnection -> {
        final int nClaimed = _update (aConnection,
                                     "UPDATE " + _table ("collection") +
                                                  " SET lease_owner = ?, lease_until = ?, revision = revision + 1," +
                                                  " state = CASE WHEN state = 'FETCHED' THEN 'FETCHED' ELSE 'FETCHING' END" +
                                                  " WHERE " + KEY + " AND state <> 'DONE' AND (lease_owner IS NULL OR lease_until <= ?)",
                                     sOwner, _utc (aNow.plus (aLease)), sSource, aPeriod.toString (), _utc (aNow));
        if (nClaimed == 0)
        {
          try (final PreparedStatement aStatement = _prepare (aConnection,
                                                              "SELECT state FROM " + _table ("collection") + " WHERE " + KEY,
                                                              sSource, aPeriod.toString ());
               final ResultSet aRows = aStatement.executeQuery ())
          {
            if (aRows.next ())
              return null;
          }
          _update (aConnection,
                   "INSERT INTO " + _table ("collection") +
                   " (source_id, report_period, state, lease_owner, lease_until, record_count, revision) VALUES (?, ?, 'FETCHING', ?, ?, 0, 0)",
                   sSource, aPeriod.toString (), sOwner, _utc (aNow.plus (aLease)));
        }
        try (final PreparedStatement aStatement = _prepare (aConnection,
                                                            "SELECT state FROM " + _table ("collection") + " WHERE " + KEY,
                                                            sSource, aPeriod.toString ());
             final ResultSet aRows = aStatement.executeQuery ())
        {
          if (!aRows.next ())
            throw new SQLException ("Collection claim disappeared");
          return new Claim (sSource, aPeriod, sOwner, "FETCHED".equals (aRows.getString (1)));
        }
      });
    }
    catch (final SQLException ex)
    {
      // Concurrent first claims race on the source-period primary key; only one owns the job.
      if (ex.getSQLState () != null && ex.getSQLState ().startsWith ("23"))
        return null;
      throw ex;
    }
  }

  private void _lock (final Connection aConnection, final Claim aClaim) throws SQLException
  {
    if (_update (aConnection,
                 "UPDATE " + _table ("collection") + " SET revision = revision + 1 WHERE " + KEY +
                 " AND lease_owner = ? AND lease_until > ? AND state IN ('FETCHING', 'FETCHED')",
                 aClaim.source (), aClaim.period ().toString (), aClaim.owner (), _utc (Instant.now ())) != 1)
      throw new SQLException ("Reporting collection lease is no longer owned by this worker");
  }

  private static void _length (final String sValue, final int nMaximum)
  {
    if (sValue != null && sValue.length () > nMaximum)
      throw new IllegalArgumentException ("A reporting record exceeds the database field limit");
  }

  private static void _validate (final Claim aClaim, final PeppolReportingItem aItem)
  {
    if (aItem == null)
      throw new IllegalArgumentException ("Invalid queued reporting record");
    if (!aClaim.period ().equals (YearMonth.from (aItem.getExchangeDTUTC ().withOffsetSameInstant (ZoneOffset.UTC))))
      throw new IllegalArgumentException ("A reporting record is outside the requested month");
    if (!PeppolReportingHelper.isDocumentTypeEligableForReporting (aItem.getDocTypeIDScheme (), aItem.getDocTypeIDValue ()))
      throw new IllegalArgumentException ("A reporting record is not eligible for reporting");
    _length (aItem.getC2ID (), PeppolReportingItem.MAX_LEN_C2_ID);
    _length (aItem.getC3ID (), PeppolReportingItem.MAX_LEN_C3_ID);
    _length (aItem.getDocTypeIDScheme (), PeppolReportingItem.MAX_LEN_DOCTYPE_SCHEME);
    _length (aItem.getDocTypeIDValue (), PeppolReportingItem.MAX_LEN_DOCTYPE_VALUE);
    _length (aItem.getProcessIDScheme (), PeppolReportingItem.MAX_LEN_PROCESS_SCHEME);
    _length (aItem.getProcessIDValue (), PeppolReportingItem.MAX_LEN_PROCESS_VALUE);
    _length (aItem.getTransportProtocol (), PeppolReportingItem.MAX_LEN_TRANSPORT_PROTOCOL);
    _length (aItem.getC1CountryCode (), PeppolReportingItem.MAX_LEN_C1_COUNTRY_CODE);
    _length (aItem.getC4CountryCode (), PeppolReportingItem.MAX_LEN_C4_COUNTRY_CODE);
    _length (aItem.getEndUserID (), PeppolReportingItem.MAX_LEN_END_USER_ID);
  }

  void stage (final Claim aClaim, final List <PeppolReportingItem> aItems) throws SQLException
  {
    for (final PeppolReportingItem aItem : aItems)
      _validate (aClaim, aItem);
    _transaction (aConnection -> {
      _lock (aConnection, aClaim);
      _update (aConnection, "DELETE FROM " + _table ("collection_queue") + " WHERE " + KEY,
               aClaim.source (), aClaim.period ().toString ());
      final String sSQL = "INSERT INTO " + _table ("collection_queue") +
                          " (source_id, report_period, item_index, payload_json) VALUES (?, ?, ?, ?)";
      int nIndex = 0;
      for (final PeppolReportingItem aItem : aItems)
        _update (aConnection, sSQL, aClaim.source (), aClaim.period ().toString (), nIndex++,
                 PeppolReportingJsonHelper.toJsonString (aItem));
      _update (aConnection, "UPDATE " + _table ("collection") + " SET state = 'FETCHED', record_count = ? WHERE " + KEY,
               aItems.size (), aClaim.source (), aClaim.period ().toString ());
      return null;
    });
  }

  void transfer (final Claim aClaim) throws SQLException
  {
    _transaction (aConnection -> {
      _lock (aConnection, aClaim);
      final int nExpected;
      try (final PreparedStatement aStatement = _prepare (aConnection,
                                                          "SELECT record_count FROM " + _table ("collection") + " WHERE " + KEY + " AND state = 'FETCHED'",
                                                          aClaim.source (), aClaim.period ().toString ());
           final ResultSet aRows = aStatement.executeQuery ())
      {
        if (!aRows.next ())
          throw new SQLException ("Reporting records have not been fetched completely");
        nExpected = aRows.getInt (1);
      }
      final List <PeppolReportingItem> aItems = new ArrayList <> ();
      try (final PreparedStatement aStatement = _prepare (aConnection,
                                                          "SELECT payload_json FROM " + _table ("collection_queue") +
                                                          " WHERE " + KEY + " ORDER BY item_index",
                                                          aClaim.source (), aClaim.period ().toString ());
           final ResultSet aRows = aStatement.executeQuery ())
      {
        while (aRows.next ())
        {
          final PeppolReportingItem aItem = PeppolReportingJsonHelper.toDomain (aRows.getString (1));
          _validate (aClaim, aItem);
          aItems.add (aItem);
        }
      }
      if (aItems.size () != nExpected)
        throw new SQLException ("Reporting collection queue is incomplete");
      final String sSQL = "INSERT INTO " + _table ("item") + " (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      int nInserted = 0;
      for (final PeppolReportingItem aItem : aItems)
        nInserted += _update (aConnection, sSQL,
                 Timestamp.valueOf (aItem.getExchangeDTUTC ().withOffsetSameInstant (ZoneOffset.UTC).toLocalDateTime ()),
                 "oracle".equals (m_sDialect) || "db2".equals (m_sDialect) ? Integer.valueOf (aItem.isSending () ? 1 : 0) : Boolean.valueOf (aItem.isSending ()),
                 DBValueHelper.getTrimmedToLength (aItem.getC2ID (), PeppolReportingItem.MAX_LEN_C2_ID),
                 DBValueHelper.getTrimmedToLength (aItem.getC3ID (), PeppolReportingItem.MAX_LEN_C3_ID),
                 DBValueHelper.getTrimmedToLength (aItem.getDocTypeIDScheme (), PeppolReportingItem.MAX_LEN_DOCTYPE_SCHEME),
                 DBValueHelper.getTrimmedToLength (aItem.getDocTypeIDValue (), PeppolReportingItem.MAX_LEN_DOCTYPE_VALUE),
                 DBValueHelper.getTrimmedToLength (aItem.getProcessIDScheme (), PeppolReportingItem.MAX_LEN_PROCESS_SCHEME),
                 DBValueHelper.getTrimmedToLength (aItem.getProcessIDValue (), PeppolReportingItem.MAX_LEN_PROCESS_VALUE),
                 DBValueHelper.getTrimmedToLength (aItem.getTransportProtocol (), PeppolReportingItem.MAX_LEN_TRANSPORT_PROTOCOL),
                 DBValueHelper.getTrimmedToLength (aItem.getC1CountryCode (), PeppolReportingItem.MAX_LEN_C1_COUNTRY_CODE),
                 DBValueHelper.getTrimmedToLength (aItem.getC4CountryCode (), PeppolReportingItem.MAX_LEN_C4_COUNTRY_CODE),
                 DBValueHelper.getTrimmedToLength (aItem.getEndUserID (), PeppolReportingItem.MAX_LEN_END_USER_ID));
      if (nInserted != nExpected)
        throw new SQLException ("Reporting collection import is incomplete");
      _update (aConnection, "DELETE FROM " + _table ("collection_queue") + " WHERE " + KEY,
               aClaim.source (), aClaim.period ().toString ());
      _update (aConnection,
               "UPDATE " + _table ("collection") + " SET state = 'DONE', lease_owner = NULL, lease_until = NULL WHERE " + KEY,
               aClaim.source (), aClaim.period ().toString ());
      return null;
    });
  }

  void fail (final Claim aClaim) throws SQLException
  {
    _transaction (aConnection -> {
      _update (aConnection,
               "UPDATE " + _table ("collection") +
               " SET lease_owner = NULL, lease_until = NULL, state = CASE WHEN state = 'FETCHED' THEN 'FETCHED' ELSE 'FAILED' END WHERE " +
               KEY + " AND lease_owner = ? AND state <> 'DONE'",
               aClaim.source (), aClaim.period ().toString (), aClaim.owner ());
      return null;
    });
  }
}
