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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.db.dto.OutboundTransactionRow;

/**
 * Implementation of {@link IOutboundTransactionManager} for JDBC backend.
 *
 * @author Philip Helger
 */
public class OutboundTransactionManagerJdbc extends AbstractAPJdbcManager implements IOutboundTransactionManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (OutboundTransactionManagerJdbc.class);

  private static final String COLS = "id, transaction_type, sender_id, receiver_id, doc_type_id, process_id," +
                                     " sbdh_instance_id, source_type, document_path, document_size, document_hash," +
                                     " c1_country_code, status, attempt_count, created_dt, completed_dt," +
                                     " reporting_status, next_retry_dt, error_details, mls_to, mls_status," +
                                     " mls_received_dt, mls_id, mls_inbound_transaction_id," +
                                     " sbdh_standard, sbdh_type_version, sbdh_type, payload_mime_type";

  private final String m_sTableName;

  public OutboundTransactionManagerJdbc (@NonNull final IAPTimestampManager aTimestampMgr,
                                         @NonNull final String sTableNamePrefix)
  {
    super (aTimestampMgr);
    m_sTableName = sTableNamePrefix + "outbound_transaction";
  }

  @Nullable
  public String create (@NonNull final ETransactionType eTransactionType,
                        @NonNull final String sSenderID,
                        @NonNull final String sReceiverID,
                        @NonNull final String sDocTypeID,
                        @NonNull final String sProcessID,
                        @NonNull final String sSbdhInstanceID,
                        @NonNull final ESourceType eSourceType,
                        @NonNull final String sDocumentPath,
                        @Nonnegative final long nDocumentSize,
                        @NonNull final String sDocumentHash,
                        @NonNull final String sC1CountryCode,
                        @NonNull final OffsetDateTime aCreationTD,
                        @Nullable final String sMlsTo,
                        @Nullable final String sMlsInboundTransactionID,
                        @Nullable final String sSbdhStandard,
                        @Nullable final String sSbdhTypeVersion,
                        @Nullable final String sSbdhType,
                        @Nullable final String sPayloadMimeType)
  {
    final String sID = createUniqueRowID ();

    final DBExecutor aExecutor = newExecutor ();
    final long nRowsAffected = aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                                                 m_sTableName +
                                                                 " (" +
                                                                 COLS +
                                                                 ")" +
                                                                 " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                                                 new ConstantPreparedStatementDataProvider (sID,
                                                                                                            eTransactionType.getID (),
                                                                                                            sSenderID,
                                                                                                            sReceiverID,
                                                                                                            sDocTypeID,
                                                                                                            sProcessID,
                                                                                                            sSbdhInstanceID,
                                                                                                            eSourceType.getID (),
                                                                                                            sDocumentPath,
                                                                                                            Long.valueOf (nDocumentSize),
                                                                                                            sDocumentHash,
                                                                                                            sC1CountryCode,
                                                                                                            EOutboundStatus.PENDING.getID (),
                                                                                                            Integer.valueOf (0),
                                                                                                            aCreationTD,
                                                                                                            null,
                                                                                                            EReportingStatus.PENDING.getID (),
                                                                                                            null,
                                                                                                            null,
                                                                                                            sMlsTo,
                                                                                                            eTransactionType ==
                                                                                                                    ETransactionType.BUSINESS_DOCUMENT ? EMlsReceptionStatus.PENDING.getID ()
                                                                                                                                                       : null,
                                                                                                            null,
                                                                                                            null,
                                                                                                            sMlsInboundTransactionID,
                                                                                                            sSbdhStandard,
                                                                                                            sSbdhTypeVersion,
                                                                                                            sSbdhType,
                                                                                                            sPayloadMimeType));
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Stored new outbound transaction in DB. " + nRowsAffected + " rows affected.");

    return nRowsAffected == 0 ? null : sID;
  }

  @Nullable
  public IOutboundTransaction getByID (@NonNull final String sID)
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (sID));
    if (aRows != null)
    {
      if (aRows.size () == 1)
        return new OutboundTransactionRow (aRows.getFirstOrNull ());
      LOGGER.warn ("Found " + aRows.size () + " transactions that all match the Transaction Identifier '" + sID + "'");
    }
    return null;
  }

  public boolean containsTransactionWithID (@NonNull final String sID)
  {
    final long nAffectedRows = newExecutor ().queryCount ("SELECT COUNT(*)" + " FROM " + m_sTableName + " WHERE id=?",
                                                          new ConstantPreparedStatementDataProvider (sID));
    return nAffectedRows > 0;
  }

  @Nullable
  public IOutboundTransaction getBySbdhInstanceID (@NonNull final String sSbdhInstanceID)
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE sbdh_instance_id=?",
                                                                      new ConstantPreparedStatementDataProvider (sSbdhInstanceID));
    if (aRows != null)
    {
      if (aRows.size () == 1)
        return new OutboundTransactionRow (aRows.getFirstOrNull ());
      LOGGER.warn ("Found " +
                   aRows.size () +
                   " transactions that all match the SBDH Instance Identifier '" +
                   sSbdhInstanceID +
                   "'");
    }
    return null;
  }

  @NonNull
  public ESuccess updateStatus (@NonNull final String sID, @NonNull final EOutboundStatus eStatus)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET status=?" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (eStatus.getID (),
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  @NonNull
  public ESuccess updateStatusAndRetry (@NonNull final String sID,
                                        @NonNull final EOutboundStatus eStatus,
                                        @Nonnegative final int nAttemptCount,
                                        @Nullable final OffsetDateTime aNextRetryDT,
                                        @Nullable final String sErrorDetails)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET status=?, attempt_count=?, next_retry_dt=?, error_details=?" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (eStatus.getID (),
                                                                                                                 Integer.valueOf (nAttemptCount),
                                                                                                                 aNextRetryDT,
                                                                                                                 sErrorDetails,
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  @NonNull
  public ESuccess updateStatusCompleted (@NonNull final String sID, @NonNull final EOutboundStatus eStatus)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET status=?, completed_dt=?" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (eStatus.getID (),
                                                                                                                 now (),
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  @NonNull
  public ESuccess updateMlsStatus (@NonNull final String sID,
                                   @NonNull final EMlsReceptionStatus eMlsStatus,
                                   @Nullable final OffsetDateTime aMlsReceivedDT,
                                   @Nullable final String sMlsID)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET mls_status=?, mls_received_dt=?, mls_id=?" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (eMlsStatus.getID (),
                                                                                                                 aMlsReceivedDT,
                                                                                                                 sMlsID,
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  @NonNull
  public ESuccess updateReportingStatus (@NonNull final String sID, @NonNull final EReportingStatus eReportingStatus)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET reporting_status=?" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (eReportingStatus.getID (),
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IOutboundTransaction> getAllInTransmission ()
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE status IN (?,?,?)",
                                                                      new ConstantPreparedStatementDataProvider (EOutboundStatus.PENDING.getID (),
                                                                                                                 EOutboundStatus.SENDING.getID (),
                                                                                                                 EOutboundStatus.FAILED.getID ()));
    final ICommonsList <IOutboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new OutboundTransactionRow (aRow));
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IOutboundTransaction> getAllForRetry (@Nonnegative final int nBatchSize)
  {
    ValueEnforcer.isGT0 (nBatchSize, "BatchSize");

    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE status=? AND next_retry_dt <= NOW()" +
                                                                      " ORDER BY next_retry_dt" +
                                                                      " LIMIT " +
                                                                      nBatchSize +
                                                                      " FOR UPDATE SKIP LOCKED",
                                                                      new ConstantPreparedStatementDataProvider (EOutboundStatus.FAILED.getID ()));
    final ICommonsList <IOutboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new OutboundTransactionRow (aRow));
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IOutboundTransaction> getAllForArchival (@Nonnegative final int nBatchSize)
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE status IN (?,?,?) AND reporting_status=?" +
                                                                      " ORDER BY completed_dt" +
                                                                      " LIMIT " +
                                                                      nBatchSize +
                                                                      " FOR UPDATE SKIP LOCKED",
                                                                      new ConstantPreparedStatementDataProvider (EOutboundStatus.REJECTED.getID (),
                                                                                                                 EOutboundStatus.SENT.getID (),
                                                                                                                 EOutboundStatus.PERMANENTLY_FAILED.getID (),
                                                                                                                 EReportingStatus.REPORTED.getID ()));
    final ICommonsList <IOutboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new OutboundTransactionRow (aRow));
    return ret;
  }
}
