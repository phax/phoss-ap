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
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.EVerificationResult;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.db.dto.InboundTransactionRow;

/**
 * JDBC implementation of {@link IInboundTransactionManager}.
 *
 * @author Philip Helger
 */
public class InboundTransactionManagerJdbc extends AbstractAPJdbcManager implements IInboundTransactionManager
{
  // All columns, in the order expected by the InboundTransactionRow constructor. This is also used
  // as the INSERT column list.
  private static final String COLS = "id, incoming_id, c2_seat_id, c3_seat_id, signing_cert_cn," +
                                     " sender_id, receiver_id, doc_type_id, process_id," +
                                     " document_path, document_size, document_hash," +
                                     " as4_message_id, as4_timestamp, sbdh_instance_id," +
                                     " c1_country_code, c4_country_code, is_duplicate_as4, is_duplicate_sbdh," +
                                     " status, attempt_count, received_dt, completed_dt," +
                                     " reporting_status, next_retry_dt, error_details," +
                                     " mls_to, mls_type, mls_response_code, mls_outbound_transaction_id," +
                                     " verification_result, verification_details, mls_correlated_outbound_transaction_id";
  private static final Logger LOGGER = LoggerFactory.getLogger (InboundTransactionManagerJdbc.class);

  private final String m_sTableName;

  /**
   * Constructor.
   *
   * @param aTimestampMgr
   *        The timestamp manager to use. May not be <code>null</code>.
   * @param sTableNamePrefix
   *        The database table name prefix. May not be <code>null</code>.
   */
  public InboundTransactionManagerJdbc (@NonNull final IAPTimestampManager aTimestampMgr,
                                        @NonNull final String sTableNamePrefix)
  {
    super (aTimestampMgr);
    m_sTableName = sTableNamePrefix + "inbound_transaction";
  }

  /** {@inheritDoc} */
  @Nullable
  public String create (@NonNull final String sIncomingID,
                        @NonNull final String sC2SeatID,
                        @NonNull final String sC3SeatID,
                        @NonNull final String sSigningCertCN,
                        @NonNull final String sSenderID,
                        @NonNull final String sReceiverID,
                        @NonNull final String sDocTypeID,
                        @NonNull final String sProcessID,
                        @NonNull final String sDocumentPath,
                        @Nonnegative final long nDocumentSize,
                        @NonNull final String sDocumentHash,
                        @NonNull final String sAS4MessageID,
                        @NonNull final OffsetDateTime aAS4Timestamp,
                        @NonNull final String sSbdhInstanceID,
                        @NonNull final String sC1CountryCode,
                        final boolean bIsDuplicateAS4,
                        final boolean bIsDuplicateSBDH,
                        @Nullable final String sMlsTo,
                        @NonNull final EPeppolMLSType eMlsType)
  {
    final String sID = createUniqueRowID ();
    final OffsetDateTime aNow = now ();

    final DBExecutor aExecutor = newExecutor ();
    final long nRowsAffected = aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                                                 m_sTableName +
                                                                 " (" +
                                                                 COLS +
                                                                 ")" +
                                                                 " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                                                 new ConstantPreparedStatementDataProvider (sID,
                                                                                                            sIncomingID,
                                                                                                            sC2SeatID,
                                                                                                            sC3SeatID,
                                                                                                            sSigningCertCN,
                                                                                                            sSenderID,
                                                                                                            sReceiverID,
                                                                                                            sDocTypeID,
                                                                                                            sProcessID,
                                                                                                            sDocumentPath,
                                                                                                            Long.valueOf (nDocumentSize),
                                                                                                            sDocumentHash,
                                                                                                            sAS4MessageID,
                                                                                                            DBValueHelper.toTimestamp (aAS4Timestamp),
                                                                                                            sSbdhInstanceID,
                                                                                                            sC1CountryCode,
                                                                                                            null,
                                                                                                            Boolean.valueOf (bIsDuplicateAS4),
                                                                                                            Boolean.valueOf (bIsDuplicateSBDH),
                                                                                                            EInboundStatus.RECEIVED.getID (),
                                                                                                            Integer.valueOf (0),
                                                                                                            DBValueHelper.toTimestamp (aNow),
                                                                                                            null,
                                                                                                            EReportingStatus.PENDING.getID (),
                                                                                                            null,
                                                                                                            null,
                                                                                                            sMlsTo,
                                                                                                            eMlsType.getID (),
                                                                                                            null,
                                                                                                            null,
                                                                                                            null,
                                                                                                            null,
                                                                                                            null));

    return nRowsAffected == 0 ? null : sID;
  }

  /** {@inheritDoc} */
  @Nullable
  public IInboundTransaction getByID (@NonNull final String sID)
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
        return new InboundTransactionRow (aRows.getFirstOrNull ());
      LOGGER.warn ("Found " + aRows.size () + " transactions that all match the Transaction Identifier '" + sID + "'");
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
  public boolean containsByAS4MessageID (@NonNull final String sAS4MessageID)
  {
    final long nAffectedRows = newExecutor ().queryCount ("SELECT COUNT(*)" +
                                                          " FROM " +
                                                          m_sTableName +
                                                          " WHERE as4_message_id=?",
                                                          new ConstantPreparedStatementDataProvider (sAS4MessageID));
    return nAffectedRows > 0;
  }

  /** {@inheritDoc} */
  @Nullable
  public IInboundTransaction getByAS4MessageID (@NonNull final String sAS4MessageID)
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE as4_message_id=?",
                                                                      new ConstantPreparedStatementDataProvider (sAS4MessageID));
    if (aRows != null)
    {
      if (aRows.size () == 1)
        return new InboundTransactionRow (aRows.getFirstOrNull ());
      LOGGER.warn ("Found " +
                   aRows.size () +
                   " transactions that all match the AS4 Message ID '" +
                   sAS4MessageID +
                   "'");
    }
    return null;
  }

  /** {@inheritDoc} */
  public boolean containsBySbdhInstanceID (@NonNull final String sSbdhInstanceID)
  {
    final long nAffectedRows = newExecutor ().queryCount ("SELECT COUNT(*)" +
                                                          " FROM " +
                                                          m_sTableName +
                                                          " WHERE sbdh_instance_id=?",
                                                          new ConstantPreparedStatementDataProvider (sSbdhInstanceID));
    return nAffectedRows > 0;
  }

  /** {@inheritDoc} */
  @Nullable
  public IInboundTransaction getBySbdhInstanceID (@NonNull final String sSbdhInstanceID)
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
        return new InboundTransactionRow (aRows.getFirstOrNull ());
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
  public IInboundTransaction getBySbdhInstanceIDIncludingArchive (@NonNull final String sSbdhInstanceID)
  {
    // Active table first
    final IInboundTransaction aActive = getBySbdhInstanceID (sSbdhInstanceID);
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
        return new InboundTransactionRow (aRows.getFirstOrNull ());
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
  public ESuccess updateStatus (@NonNull final String sID, @NonNull final EInboundStatus eStatus)
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
  public ESuccess updateStatusAndRetry (@NonNull final String sID,
                                        @NonNull final EInboundStatus eStatus,
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
  public ESuccess updateStatusAndNextRetry (@NonNull final String sID,
                                            @NonNull final EInboundStatus eStatus,
                                            @Nullable final OffsetDateTime aNextRetryDT,
                                            @Nullable final String sErrorDetails)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET status=?, next_retry_dt=?, error_details=?" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (eStatus.getID (),
                                                                                                                 DBValueHelper.toTimestamp (aNextRetryDT),
                                                                                                                 sErrorDetails,
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess updateStatusCompleted (@NonNull final String sID, @NonNull final EInboundStatus eStatus)
  {
    // The next retry and the error details of the previous failure or deferral are cleared, so that
    // a completed transaction does not look like it still has something pending
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET status=?, completed_dt=?," +
                                                                      " next_retry_dt=NULL, error_details=NULL" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (eStatus.getID (),
                                                                                                                 DBValueHelper.toTimestamp (now ()),
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess updateC4CountryCode (@NonNull final String sID, @NonNull final String sC4CountryCode)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET c4_country_code=?" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (sC4CountryCode,
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess updateMlsFields (@NonNull final String sID,
                                   @Nullable final EPeppolMLSResponseCode eMlsResponseCode,
                                   @Nullable final String sMlsOutboundTransactionID)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET mls_response_code=?, mls_outbound_transaction_id=?" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (eMlsResponseCode != null ? eMlsResponseCode.getID ()
                                                                                                                                          : null,
                                                                                                                 sMlsOutboundTransactionID,
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess claimMlsResponseCode (@NonNull final String sID,
                                        @NonNull final EPeppolMLSResponseCode eMlsResponseCode)
  {
    ValueEnforcer.notNull (sID, "ID");
    ValueEnforcer.notNull (eMlsResponseCode, "MlsResponseCode");

    // The "IS NULL" condition is the whole point: the first writer wins and every later one gets 0
    // affected rows, so that no second MLS can be created for the same business document
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET mls_response_code=?" +
                                                                      " WHERE id=? AND mls_response_code IS NULL",
                                                                      new ConstantPreparedStatementDataProvider (eMlsResponseCode.getID (),
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess releaseMlsResponseCodeClaim (@NonNull final String sID)
  {
    ValueEnforcer.notNull (sID, "ID");

    // The "IS NULL" condition on the outbound transaction ID makes this safe: the response code of
    // an MLS that made it to an outbound transaction can never be erased by a release
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET mls_response_code=NULL" +
                                                                      " WHERE id=? AND mls_outbound_transaction_id IS NULL",
                                                                      new ConstantPreparedStatementDataProvider (sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess updateVerificationResult (@NonNull final String sID,
                                            @NonNull final EVerificationResult eVerificationResult,
                                            @Nullable final String sVerificationDetails)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET verification_result=?, verification_details=?" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (eVerificationResult.getID (),
                                                                                                                 sVerificationDetails,
                                                                                                                 sID));
    return ESuccess.valueOf (nRowsAffected == 1);
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess updateMlsCorrelatedOutboundTransactionID (@NonNull final String sID,
                                                            @NonNull final String sCorrelatedOutboundTransactionID)
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                      m_sTableName +
                                                                      " SET mls_correlated_outbound_transaction_id=?" +
                                                                      " WHERE id=?",
                                                                      new ConstantPreparedStatementDataProvider (sCorrelatedOutboundTransactionID,
                                                                                                                 sID));
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
  public ICommonsList <IInboundTransaction> getAllInProcessing ()
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE status IN (?,?,?,?)",
                                                                      new ConstantPreparedStatementDataProvider (EInboundStatus.RECEIVED.getID (),
                                                                                                                 EInboundStatus.VERIFICATION_DEFERRED.getID (),
                                                                                                                 EInboundStatus.FORWARDING.getID (),
                                                                                                                 EInboundStatus.FORWARD_FAILED.getID ()));
    final ICommonsList <IInboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new InboundTransactionRow (aRow));
    return ret;
  }

  /** {@inheritDoc} */
  @NonNull
  public ICommonsList <IInboundTransaction> getAllForRetry (@Nonnegative final int nBatchSize)
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE status=? AND next_retry_dt <= NOW()" +
                                                                      " ORDER BY next_retry_dt" +
                                                                      " LIMIT " +
                                                                      nBatchSize +
                                                                      " FOR UPDATE SKIP LOCKED",
                                                                      new ConstantPreparedStatementDataProvider (EInboundStatus.FORWARD_FAILED.getID ()));
    final ICommonsList <IInboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new InboundTransactionRow (aRow));
    return ret;
  }

  /** {@inheritDoc} */
  @NonNull
  public ICommonsList <IInboundTransaction> getAllForVerificationRetry (@Nonnegative final int nBatchSize)
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE status=? AND next_retry_dt <= NOW()" +
                                                                      " ORDER BY next_retry_dt" +
                                                                      " LIMIT " +
                                                                      nBatchSize +
                                                                      " FOR UPDATE SKIP LOCKED",
                                                                      new ConstantPreparedStatementDataProvider (EInboundStatus.VERIFICATION_DEFERRED.getID ()));
    final ICommonsList <IInboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new InboundTransactionRow (aRow));
    return ret;
  }

  /** {@inheritDoc} */
  @NonNull
  public ICommonsList <IInboundTransaction> getAllForMlsApiTimeout (@Nonnegative final int nBatchSize,
                                                                    @NonNull final OffsetDateTime aMaxAS4Timestamp)
  {
    ValueEnforcer.isGT0 (nBatchSize, "BatchSize");
    ValueEnforcer.notNull (aMaxAS4Timestamp, "MaxAS4Timestamp");

    // Forwarded business documents (neither MLS nor MLR) that were received long enough ago and
    // for which no MLS response code was determined yet. The age is deliberately measured on the
    // AS4 timestamp and not on "completed_dt", because MLS-1 is measured from the reception of the
    // document - anchoring on the forwarding would add the forwarding duration on top of the SLA
    // budget instead of consuming it from it. A document that was rejected by the verification is
    // excluded, because C2 already received the negative MLS (RE) of that rejection, and so is one
    // with the MLS type FAILURE_ONLY, which never gets a positive MLS at all - recording a fallback
    // response code for it would only block the rejection the Receiver Backend may still report
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE status=? AND mls_response_code IS NULL" +
                                                                      " AND as4_timestamp < ?" +
                                                                      " AND mls_type <> ?" +
                                                                      " AND NOT (doc_type_id=? AND process_id=?)" +
                                                                      " AND NOT (doc_type_id=? AND process_id=?)" +
                                                                      " AND (verification_result IS NULL OR verification_result <> ?)" +
                                                                      " ORDER BY as4_timestamp" +
                                                                      " LIMIT " +
                                                                      nBatchSize +
                                                                      " FOR UPDATE SKIP LOCKED",
                                                                      new ConstantPreparedStatementDataProvider (EInboundStatus.FORWARDED.getID (),
                                                                                                                 DBValueHelper.toTimestamp (aMaxAS4Timestamp),
                                                                                                                 EPeppolMLSType.FAILURE_ONLY.getID (),
                                                                                                                 EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getURIEncoded (),
                                                                                                                 EPredefinedProcessIdentifier.urn_peppol_edec_mls.getURIEncoded (),
                                                                                                                 EPredefinedDocumentTypeIdentifier.APPLICATIONRESPONSE_FDC_PEPPOL_EU_POACC_TRNS_MLR_3.getURIEncoded (),
                                                                                                                 EPredefinedProcessIdentifier.BIS3_MLR.getURIEncoded (),
                                                                                                                 EVerificationResult.REJECTED.getID ()));
    final ICommonsList <IInboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new InboundTransactionRow (aRow));
    return ret;
  }

  /** {@inheritDoc} */
  @NonNull
  public ICommonsList <IInboundTransaction> getAllForArchival (@Nonnegative final int nBatchSize,
                                                               final boolean bExcludeMlsPending)
  {
    ValueEnforcer.isGT0 (nBatchSize, "BatchSize");

    final ConstantPreparedStatementDataProvider aParams = new ConstantPreparedStatementDataProvider (EInboundStatus.REJECTED.getID (),
                                                                                                    EInboundStatus.FORWARDED.getID (),
                                                                                                    EInboundStatus.PERMANENTLY_FAILED.getID (),
                                                                                                    EReportingStatus.REPORTED.getID (),
                                                                                                    EReportingStatus.EXCLUDED.getID ());
    String sMlsPendingCondition = "";
    if (bExcludeMlsPending)
    {
      // Same criteria as getAllForMlsApiTimeout
      sMlsPendingCondition = " AND NOT (status=? AND mls_response_code IS NULL" +
                             " AND mls_type <> ?" +
                             " AND NOT (doc_type_id=? AND process_id=?)" +
                             " AND NOT (doc_type_id=? AND process_id=?)" +
                             " AND (verification_result IS NULL OR verification_result <> ?))";
      aParams.addValue (EInboundStatus.FORWARDED.getID ())
             .addValue (EPeppolMLSType.FAILURE_ONLY.getID ())
             .addValue (EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getURIEncoded ())
             .addValue (EPredefinedProcessIdentifier.urn_peppol_edec_mls.getURIEncoded ())
             .addValue (EPredefinedDocumentTypeIdentifier.APPLICATIONRESPONSE_FDC_PEPPOL_EU_POACC_TRNS_MLR_3.getURIEncoded ())
             .addValue (EPredefinedProcessIdentifier.BIS3_MLR.getURIEncoded ())
             .addValue (EVerificationResult.REJECTED.getID ());
    }

    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE status IN (?,?,?) AND reporting_status IN (?,?)" +
                                                                      sMlsPendingCondition +
                                                                      " ORDER BY completed_dt" +
                                                                      " LIMIT " +
                                                                      nBatchSize +
                                                                      " FOR UPDATE SKIP LOCKED",
                                                                      aParams);
    final ICommonsList <IInboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new InboundTransactionRow (aRow));
    return ret;
  }

  /** {@inheritDoc} */
  @NonNull
  public ICommonsList <IInboundTransaction> getAllWithoutMlsResponse ()
  {
    // Inbound business documents (not MLS) where no MLS response code has been
    // set yet
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE mls_response_code IS NULL" +
                                                                      " AND NOT (doc_type_id=? AND process_id=?)" +
                                                                      " ORDER BY as4_timestamp",
                                                                      new ConstantPreparedStatementDataProvider (EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getURIEncoded (),
                                                                                                                 EPredefinedProcessIdentifier.urn_peppol_edec_mls.getURIEncoded ()));
    final ICommonsList <IInboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new InboundTransactionRow (aRow));
    return ret;
  }

  /** {@inheritDoc} */
  @NonNull
  public ICommonsList <IInboundTransaction> getAllWithoutC4CountryCode ()
  {
    // Forwarded transactions where the C4 country code has not yet been set and
    // reporting is still pending
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE c4_country_code IS NULL" +
                                                                      " AND status=?" +
                                                                      " AND reporting_status=?" +
                                                                      " ORDER BY received_dt",
                                                                      new ConstantPreparedStatementDataProvider (EInboundStatus.FORWARDED.getID (),
                                                                                                                 EReportingStatus.PENDING.getID ()));
    final ICommonsList <IInboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new InboundTransactionRow (aRow));
    return ret;
  }

  /** {@inheritDoc} */
  @NonNull
  public ICommonsList <IInboundTransaction> getAllTransactions (@Nonnegative final int nOffset,
                                                                @Nonnegative final int nLimit)
  {
    ValueEnforcer.isGE0 (nOffset, "Offset");
    ValueEnforcer.isGE0 (nLimit, "Limit");

    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " ORDER BY received_dt DESC" +
                                                                      " LIMIT " +
                                                                      nLimit +
                                                                      " OFFSET " +
                                                                      nOffset);
    final ICommonsList <IInboundTransaction> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new InboundTransactionRow (aRow));
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
