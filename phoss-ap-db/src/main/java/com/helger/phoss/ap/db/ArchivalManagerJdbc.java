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

import java.time.OffsetDateTime;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.ICommonsList;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.ap.api.IArchivalManager;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;

/**
 * JDBC manager dealing with Archival
 *
 * @author Philip Helger
 */
public class ArchivalManagerJdbc extends AbstractAPJdbcManager implements IArchivalManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ArchivalManagerJdbc.class);

  private final String m_sTableNamePrefix;

  /**
   * Constructor.
   *
   * @param aTimestampMgr
   *        The timestamp manager to use. May not be <code>null</code>.
   * @param sTableNamePrefix
   *        The database table name prefix. May not be <code>null</code>.
   */
  public ArchivalManagerJdbc (@NonNull final IAPTimestampManager aTimestampMgr, @NonNull final String sTableNamePrefix)
  {
    super (aTimestampMgr);
    m_sTableNamePrefix = sTableNamePrefix;
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess archiveOutboundTransaction (@Nonempty final String sID)
  {
    ValueEnforcer.notEmpty (sID, "ID");

    final DBExecutor aExecutor = newExecutor ();
    return aExecutor.performInTransaction (() -> {
      // Copy sending attempts first
      aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                        m_sTableNamePrefix +
                                        "outbound_sending_attempt_archive" +
                                        " SELECT * FROM " +
                                        m_sTableNamePrefix +
                                        "outbound_sending_attempt" +
                                        " WHERE outbound_transaction_id=?",
                                        new ConstantPreparedStatementDataProvider (sID));
      aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                        m_sTableNamePrefix +
                                        "outbound_sending_attempt" +
                                        " WHERE outbound_transaction_id=?",
                                        new ConstantPreparedStatementDataProvider (sID));

      // Copy main transaction
      aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                        m_sTableNamePrefix +
                                        "outbound_transaction_archive" +
                                        " SELECT * FROM " +
                                        m_sTableNamePrefix +
                                        "outbound_transaction" +
                                        " WHERE id=?",
                                        new ConstantPreparedStatementDataProvider (sID));
      aExecutor.insertOrUpdateOrDelete ("DELETE FROM " + m_sTableNamePrefix + "outbound_transaction" + " WHERE id=?",
                                        new ConstantPreparedStatementDataProvider (sID));

      LOGGER.info ("Archived outbound transaction '" + sID + "'");
    });
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess archiveInboundTransaction (final String sID)
  {
    ValueEnforcer.notEmpty (sID, "ID");

    final DBExecutor aExecutor = newExecutor ();
    return aExecutor.performInTransaction (() -> {
      // Copy forwarding attempts first
      aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                        m_sTableNamePrefix +
                                        "inbound_forwarding_attempt_archive" +
                                        " SELECT * FROM " +
                                        m_sTableNamePrefix +
                                        "inbound_forwarding_attempt" +
                                        " WHERE inbound_transaction_id=?",
                                        new ConstantPreparedStatementDataProvider (sID));
      aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                        m_sTableNamePrefix +
                                        "inbound_forwarding_attempt" +
                                        " WHERE inbound_transaction_id=?",
                                        new ConstantPreparedStatementDataProvider (sID));

      // Copy main transaction
      aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                        m_sTableNamePrefix +
                                        "inbound_transaction_archive" +
                                        " SELECT * FROM " +
                                        m_sTableNamePrefix +
                                        "inbound_transaction" +
                                        " WHERE id=?",
                                        new ConstantPreparedStatementDataProvider (sID));
      aExecutor.insertOrUpdateOrDelete ("DELETE FROM " + m_sTableNamePrefix + "inbound_transaction" + " WHERE id=?",
                                        new ConstantPreparedStatementDataProvider (sID));

      LOGGER.info ("Archived inbound transaction '" + sID + "'");
    });
  }

  /** {@inheritDoc} */
  @Nonnegative
  public int cleanupOutbound (@NonNull final OffsetDateTime aCutoff,
                              @Nonnegative final int nBatchSize,
                              @NonNull final Predicate <String> aDocumentDeleter)
  {
    ValueEnforcer.notNull (aCutoff, "Cutoff");
    ValueEnforcer.isGT0 (nBatchSize, "BatchSize");
    ValueEnforcer.notNull (aDocumentDeleter, "DocumentDeleter");

    final DBExecutor aExecutor = newExecutor ();
    final ICommonsList <DBResultRow> aRows = aExecutor.queryAll ("SELECT id, document_path FROM " +
                                                                 m_sTableNamePrefix +
                                                                 "outbound_transaction_archive" +
                                                                 " WHERE completed_dt < ?" +
                                                                 " ORDER BY completed_dt" +
                                                                 " LIMIT " +
                                                                 nBatchSize +
                                                                 " FOR UPDATE SKIP LOCKED",
                                                                 new ConstantPreparedStatementDataProvider (DBValueHelper.toTimestamp (aCutoff)));
    if (aRows == null || aRows.isEmpty ())
      return 0;

    int nDeleted = 0;
    for (final DBResultRow aRow : aRows)
    {
      final String sID = aRow.getAsString (0);
      final String sDocumentPath = aRow.getAsString (1);

      if (!aDocumentDeleter.test (sDocumentPath))
        continue;

      final ESuccess eSuccess = aExecutor.performInTransaction (() -> {
        aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                          m_sTableNamePrefix +
                                          "outbound_sending_attempt_archive" +
                                          " WHERE outbound_transaction_id=?",
                                          new ConstantPreparedStatementDataProvider (sID));
        aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                          m_sTableNamePrefix +
                                          "outbound_transaction_archive" +
                                          " WHERE id=?",
                                          new ConstantPreparedStatementDataProvider (sID));
      });
      if (eSuccess.isSuccess ())
      {
        nDeleted++;
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Cleaned up archived outbound transaction '" + sID + "'");
      }
      else
      {
        LOGGER.warn ("Failed to cleanup archived outbound transaction '" + sID + "'");
      }
    }
    return nDeleted;
  }

  /** {@inheritDoc} */
  @Nonnegative
  public int cleanupInbound (@NonNull final OffsetDateTime aCutoff,
                             @Nonnegative final int nBatchSize,
                             @NonNull final Predicate <String> aDocumentDeleter)
  {
    ValueEnforcer.notNull (aCutoff, "Cutoff");
    ValueEnforcer.isGT0 (nBatchSize, "BatchSize");
    ValueEnforcer.notNull (aDocumentDeleter, "DocumentDeleter");

    final DBExecutor aExecutor = newExecutor ();
    final ICommonsList <DBResultRow> aRows = aExecutor.queryAll ("SELECT id, document_path FROM " +
                                                                 m_sTableNamePrefix +
                                                                 "inbound_transaction_archive" +
                                                                 " WHERE completed_dt < ?" +
                                                                 " ORDER BY completed_dt" +
                                                                 " LIMIT " +
                                                                 nBatchSize +
                                                                 " FOR UPDATE SKIP LOCKED",
                                                                 new ConstantPreparedStatementDataProvider (DBValueHelper.toTimestamp (aCutoff)));
    if (aRows == null || aRows.isEmpty ())
      return 0;

    int nDeleted = 0;
    for (final DBResultRow aRow : aRows)
    {
      final String sID = aRow.getAsString (0);
      final String sDocumentPath = aRow.getAsString (1);

      if (!aDocumentDeleter.test (sDocumentPath))
        continue;

      final ESuccess eSuccess = aExecutor.performInTransaction (() -> {
        aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                          m_sTableNamePrefix +
                                          "inbound_forwarding_attempt_archive" +
                                          " WHERE inbound_transaction_id=?",
                                          new ConstantPreparedStatementDataProvider (sID));
        aExecutor.insertOrUpdateOrDelete ("DELETE FROM " +
                                          m_sTableNamePrefix +
                                          "inbound_transaction_archive" +
                                          " WHERE id=?",
                                          new ConstantPreparedStatementDataProvider (sID));
      });
      if (eSuccess.isSuccess ())
      {
        nDeleted++;
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Cleaned up archived inbound transaction '" + sID + "'");
      }
      else
      {
        LOGGER.warn ("Failed to cleanup archived inbound transaction '" + sID + "'");
      }
    }
    return nDeleted;
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("TableNamePrefix", m_sTableNamePrefix)
                            .getToString ();
  }
}
