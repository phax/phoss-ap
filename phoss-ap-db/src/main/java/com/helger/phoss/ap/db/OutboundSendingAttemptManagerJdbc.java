/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.ap.api.IOutboundSendingAttemptManager;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.model.IOutboundSendingAttempt;
import com.helger.phoss.ap.db.dto.OutboundSendingAttemptRow;

public class OutboundSendingAttemptManagerJdbc extends AbstractAPJdbcManager implements IOutboundSendingAttemptManager
{
  private static final String COLS = "id, outbound_transaction_id, as4_message_id, as4_timestamp," +
                                     " receipt_message_id, http_status_code, attempt_dt, attempt_status, error_details";

  private final String m_sTableNameName;

  public OutboundSendingAttemptManagerJdbc (@NonNull final IAPTimestampManager aTimestampMgr,
                                            @NonNull final String sTableNamePrefix)
  {
    super (aTimestampMgr);
    m_sTableNameName = sTableNamePrefix + "outbound_sending_attempt";
  }

  @Nullable
  public String create (@NonNull final String sOutboundTransactionID,
                        @NonNull final String sAS4MessageID,
                        @NonNull final OffsetDateTime aAS4Timestamp,
                        @Nullable final String sReceiptMessageID,
                        @Nullable final Integer aHttpStatusCode,
                        @NonNull final EAttemptStatus eAttemptStatus,
                        @Nullable final String sErrorDetails)
  {
    final String sID = createUniqueRowID ();
    final OffsetDateTime aNow = now ();

    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("INSERT INTO " +
                                                                      m_sTableNameName +
                                                                      " (" +
                                                                      COLS +
                                                                      ")" +
                                                                      " VALUES (?,?,?,?,?,?,?,?,?)",
                                                                      new ConstantPreparedStatementDataProvider (sID,
                                                                                                                 sOutboundTransactionID,
                                                                                                                 sAS4MessageID,
                                                                                                                 aAS4Timestamp,
                                                                                                                 sReceiptMessageID,
                                                                                                                 aHttpStatusCode,
                                                                                                                 aNow,
                                                                                                                 eAttemptStatus.getID (),
                                                                                                                 sErrorDetails));
    return nRowsAffected == 0 ? null : sID;
  }

  @NonNull
  public ICommonsList <IOutboundSendingAttempt> getByTransactionID (@NonNull final String sOutboundTransactionID)
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableNameName +
                                                                      " WHERE outbound_transaction_id=?" +
                                                                      " ORDER BY attempt_dt",
                                                                      new ConstantPreparedStatementDataProvider (sOutboundTransactionID));
    final ICommonsList <IOutboundSendingAttempt> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new OutboundSendingAttemptRow (aRow));
    return ret;
  }
}
