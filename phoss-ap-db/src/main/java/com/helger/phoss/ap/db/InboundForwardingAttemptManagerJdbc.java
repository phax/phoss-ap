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
import com.helger.phoss.ap.api.IInboundForwardingAttemptManager;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.model.IInboundForwardingAttempt;
import com.helger.phoss.ap.db.dto.InboundForwardingAttemptRow;

public class InboundForwardingAttemptManagerJdbc extends AbstractAPJdbcManager implements IInboundForwardingAttemptManager
{
  private static final String COLS = "id, inbound_transaction_id, attempt_dt, attempt_status, error_code, error_details";

  public InboundForwardingAttemptManagerJdbc (@NonNull final IAPTimestampManager aTimestampMgr)
  {
    super (aTimestampMgr);
  }

  @Nullable
  public String create (@NonNull final String sInboundTransactionID,
                        @NonNull final EAttemptStatus eAttemptStatus,
                        @Nullable final String sErrorCode,
                        @Nullable final String sErrorDetails)
  {
    final String sID = createUniqueRowID ();
    final OffsetDateTime aNow = now ();

    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("INSERT INTO inbound_forwarding_attempt (" +
                                                                      COLS +
                                                                      ")" +
                                                                      " VALUES (?,?,?,?,?,?)",
                                                                      new ConstantPreparedStatementDataProvider (sID,
                                                                                                                 sInboundTransactionID,
                                                                                                                 aNow,
                                                                                                                 eAttemptStatus.getID (),
                                                                                                                 sErrorCode,
                                                                                                                 sErrorDetails));
    return nRowsAffected == 0 ? null : sID;
  }

  public String createSuccess (final String sInboundTransactionID)
  {
    return create (sInboundTransactionID, EAttemptStatus.SUCCESS, null, null);
  }

  public String createFailure (final String sInboundTransactionID,
                               final String sErrorCode,
                               final String sErrorDetails)
  {
    return create (sInboundTransactionID, EAttemptStatus.FAILED, sErrorCode, sErrorDetails);
  }

  public ICommonsList <IInboundForwardingAttempt> getByTransactionID (final String sInboundTransactionID)
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM inbound_forwarding_attempt" +
                                                                      " WHERE inbound_transaction_id=?" +
                                                                      " ORDER BY attempt_dt",
                                                                      new ConstantPreparedStatementDataProvider (sInboundTransactionID));
    final ICommonsList <IInboundForwardingAttempt> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new InboundForwardingAttemptRow (aRow));
    return ret;
  }
}
