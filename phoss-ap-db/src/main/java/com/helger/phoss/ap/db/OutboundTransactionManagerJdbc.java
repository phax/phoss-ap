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
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.peppol.sbdh.EPeppolMLSType;
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
                                     " sbdh_standard, sbdh_type_version, sbdh_type, payload_mime_type," +
                                     " custom1, custom2, custom3, mls_type, requested_as4_message_id";

  private final String m_sTableName;

  /**
   * Constructor.
   *
   * @param aTimestampMgr
   *        The timestamp manager to use. May not be <code>null</code>.
   * @param sTableNamePrefix
   *        The database table name prefix. May not be <code>null</code>.
   */
  public OutboundTransactionManagerJdbc (@NonNull final IAPTimestampManager aTimestampMgr,
                                         @NonNull final String sTableNamePrefix)
  {
    super (aTimestampMgr);
    m_sTableName = sTableNamePrefix + "outbound_transaction";
  }

  /**
   * Get the SQL condition that an outbound transaction must fulfil to be archived. It is shared by
   * {@link #getAllForArchival(int)} and the archival itself, so that the archival re-checks exactly
   * the criteria of the candidate selection.
   *
   * @param aParams
   *        The parameters to which the values of the placeholders of the condition are appended.
   *        May not be <code>null</code>.
   * @return The SQL condition without a leading <code>AND</code>. Never <code>null</code>.
   */
  @NonNull
  static String getArchivalCondition (@NonNull final ConstantPreparedStatementDataProvider aParams)
  {
    aParams.addValue (EOutboundStatus.REJECTED.getID ())
           .addValue (EOutboundStatus.SENT.getID ())
           .addValue (EOutboundStatus.PERMANENTLY_FAILED.getID ())
           .addValue (EReportingStatus.REPORTED.getID ())
           .addValue (EReportingStatus.EXCLUDED.getID ());
    return "status IN (?,?,?) AND reporting_status IN (?,?)";
  }

  /** {@inheritDoc} */
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
                        @Nullable final String sPayloadMimeType,
                        @Nullable final String sCustom1,
                        @Nullable final String sCustom2,
                        @Nullable final String sCustom3,
                        @Nullable final EPeppolMLSType eMlsType,
                        @Nullable final String sRequestedAS4MessageID)
  {
    final String sID = createUniqueRowID ();

    final DBExecutor aExecutor = newExecutor ();
    final long nRowsAffected = aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                                                 m_sTableName +
                                                                 " (" +
                                                                 COLS +
                                                                 ")" +
                                                                 " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
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
                                                                                                            DBValueHelper.toTimestamp (aCreationTD),
                                                                                                            null,
                                                                                                            EReportingStatus.PENDING.getID (),
                                                                                                            null,
                                                                                                            null,
                                                                                                            sMlsTo,
                                                                                                            eTransactionType == ETransactionType.BUSINESS_DOCUMENT ? EMlsReceptionStatus.PENDING.getID ()
                                                                                                                                                                   : null,
                                                                                                            null,
                                                                                                            null,
                                                                                                            sMlsInboundTransactionID,
                                                                                                            sSbdhStandard,
                                                                                                            sSbdhTypeVersion,
                                                                                                            sSbdhType,
                                                                                                            sPayloadMimeType,
                                                                                                            sCustom1,
                                                                                                            sCustom2,
                                                                                                            sCustom3,
                                                                                                            eMlsType == null ? null
                                                                                                                             : eMlsType.getID (),
                                                                                                            sRequestedAS4MessageID));
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Stored new outbound transaction in DB. " + nRowsAffected + " rows affected.");

    return nRowsAffected == 0 ? null : sID;
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Nullable
  public IOutboundTransaction getByIDIncludingArchive (@NonNull final String sID)
  {
    // Active table first
    final IOutboundTransaction aActive = getByID (sID);
    if (aActive != null)
      return aActive;

    // Fall back to archive table
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      "_archive" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (sID));
    if (aRows != null)
    {
      if (aRows.size () == 1)
        return new OutboundTransactionRow (aRows.getFirstOrNull ());
      if (aRows.size () > 1)
        LOGGER.warn ("Found " +
                     aRows.size () +
                     " archived transactions that all match the Transaction Identifier '" +
                     sID +
                     "'");
    }
    return null;
  }

  /** {@inheritDoc} */
  public boolean containsTransactionWithID (@NonNull final String sID)
  {
    final long nAffectedRows = newExecutor ().queryCount ("SELECT COUNT(*)" + " FROM " + m_sTableName + " WHERE id=?",
                                                          new ConstantPreparedStatementDataProvider (sID));
    return nAffectedRows > 0;
  }

  /** {@inheritDoc} */
  public boolean containsRequestedAS4MessageIDIncludingArchive (@NonNull final String sAS4MessageID)
  {
    ValueEnforcer.notNull (sAS4MessageID, "AS4MessageID");

    final DBExecutor aExecutor = newExecutor ();
    // Active table first
    if (aExecutor.queryCount ("SELECT COUNT(*) FROM " + m_sTableName + " WHERE requested_as4_message_id=?",
                              new ConstantPreparedStatementDataProvider (sAS4MessageID)) > 0)
      return true;

    // Fall back to archive table
    return aExecutor.queryCount ("SELECT COUNT(*) FROM " +
                                 m_sTableName +
                                 "_archive" +
                                 " WHERE requested_as4_message_id=?",
                                 new ConstantPreparedStatementDataProvider (sAS4MessageID)) > 0;
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Nullable
  public IOutboundTransaction getBySbdhInstanceIDIncludingArchive (@NonNull final String sSbdhInstanceID)
  {
    // Active table first
    final IOutboundTransaction aActive = getBySbdhInstanceID (sSbdhInstanceID);
    if (aActive != null)
      return aActive;

    // Fall back to archive table
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      "_archive" +
                                                                      " WHERE sbdh_instance_id=?",
                                                                      new ConstantPreparedStatementDataProvider (sSbdhInstanceID));
    if (aRows != null)
    {
      if (aRows.size () == 1)
        return new OutboundTransactionRow (aRows.getFirstOrNull ());
      if (aRows.size () > 1)
        LOGGER.warn ("Found " +
                     aRows.size () +
                     " archived transactions that all match the SBDH Instance Identifier '" +
                     sSbdhInstanceID +
                     "'");
    }
    return null;
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @NonNull
  public ESuccess claimForRetry (@NonNull final String sID)
  {
    ValueEnforcer.notNull (sID, "ID");

    // "next_retry_dt" keeps an instance with an old batch from claiming a transaction that was
    // retried and failed again in the meantime
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET status=?" +
                                                                      " WHERE id=? AND status IN (?,?) AND next_retry_dt <= ?",
                                                                      new ConstantPreparedStatementDataProvider (EOutboundStatus.SENDING.getID (),
                                                                                                                 sID,
                                                                                                                 EOutboundStatus.FAILED.getID (),
                                                                                                                 EOutboundStatus.RECOVERING.getID (),
                                                                                                                 DBValueHelper.toTimestamp (now ())));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  /** {@inheritDoc} */
  @Nonnegative
  public long markAllInFlightAsRecovering (@NonNull final OffsetDateTime aRecoveryDueDT)
  {
    ValueEnforcer.notNull (aRecoveryDueDT, "RecoveryDueDT");

    // Rows that are being processed right now are marked as well - their next regular status update
    // overwrites the mark again
    return newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                  m_sTableName +
                                                  " SET status=?, next_retry_dt=?" +
                                                  " WHERE status IN (?,?)",
                                                  new ConstantPreparedStatementDataProvider (EOutboundStatus.RECOVERING.getID (),
                                                                                             DBValueHelper.toTimestamp (aRecoveryDueDT),
                                                                                             EOutboundStatus.PENDING.getID (),
                                                                                             EOutboundStatus.SENDING.getID ()));
  }

  /** {@inheritDoc} */
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
                                                                                                                 DBValueHelper.toTimestamp (aNextRetryDT),
                                                                                                                 sErrorDetails,
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess updateStatusCompleted (@NonNull final String sID, @NonNull final EOutboundStatus eStatus)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET status=?, completed_dt=?" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (eStatus.getID (),
                                                                                                                 DBValueHelper.toTimestamp (now ()),
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess updateMlsStatus (@NonNull final String sID,
                                   @NonNull final EMlsReceptionStatus eMlsStatus,
                                   @Nullable final OffsetDateTime aMlsReceivedDT,
                                   @Nullable final String sMlsID,
                                   @Nullable final String sMlsInboundTransactionID)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET mls_status=?, mls_received_dt=?, mls_id=?, mls_inbound_transaction_id=?" +
                                                                      " WHERE id=? AND (mls_status IS NULL OR mls_status=?)",
                                                                      new ConstantPreparedStatementDataProvider (eMlsStatus.getID (),
                                                                                                                 DBValueHelper.toTimestamp (aMlsReceivedDT),
                                                                                                                 sMlsID,
                                                                                                                 sMlsInboundTransactionID,
                                                                                                                 sID,
                                                                                                                 EMlsReceptionStatus.PENDING.getID ()));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IOutboundTransaction> getAllInTransmission ()
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE status IN (?,?,?,?)",
                                                                      new ConstantPreparedStatementDataProvider (EOutboundStatus.PENDING.getID (),
                                                                                                                 EOutboundStatus.SENDING.getID (),
                                                                                                                 EOutboundStatus.FAILED.getID (),
                                                                                                                 EOutboundStatus.RECOVERING.getID ()));
    final ICommonsList <IOutboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new OutboundTransactionRow (aRow));
    return ret;
  }

  /** {@inheritDoc} */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IOutboundTransaction> getAllForRetry (@Nonnegative final int nBatchSize)
  {
    ValueEnforcer.isGT0 (nBatchSize, "BatchSize");

    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE status IN (?,?) AND next_retry_dt <= ?" +
                                                                      " ORDER BY next_retry_dt" +
                                                                      " LIMIT " +
                                                                      nBatchSize,
                                                                      new ConstantPreparedStatementDataProvider (EOutboundStatus.FAILED.getID (),
                                                                                                                 EOutboundStatus.RECOVERING.getID (),
                                                                                                                 DBValueHelper.toTimestamp (now ())));
    final ICommonsList <IOutboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new OutboundTransactionRow (aRow));
    return ret;
  }

  /** {@inheritDoc} */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IOutboundTransaction> getAllForArchival (@Nonnegative final int nBatchSize)
  {
    final ConstantPreparedStatementDataProvider aParams = new ConstantPreparedStatementDataProvider ();
    final String sCondition = getArchivalCondition (aParams);
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE " +
                                                                      sCondition +
                                                                      " ORDER BY completed_dt" +
                                                                      " LIMIT " +
                                                                      nBatchSize,
                                                                      aParams);
    final ICommonsList <IOutboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new OutboundTransactionRow (aRow));
    return ret;
  }

  /** {@inheritDoc} */
  @NonNull
  public ICommonsList <IOutboundTransaction> getAllTransactions (@Nonnegative final int nOffset,
                                                                 @Nonnegative final int nLimit)
  {
    ValueEnforcer.isGE0 (nOffset, "Offset");
    ValueEnforcer.isGE0 (nLimit, "Limit");

    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " ORDER BY created_dt DESC" +
                                                                      " LIMIT " +
                                                                      nLimit +
                                                                      " OFFSET " +
                                                                      nOffset);
    final ICommonsList <IOutboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new OutboundTransactionRow (aRow));
    return ret;
  }

  /** {@inheritDoc} */
  @Nonnegative
  public long getTransactionCount ()
  {
    return newExecutor ().queryCount ("SELECT COUNT(*) FROM " + m_sTableName);
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("TableName", m_sTableName).getToString ();
  }
}
