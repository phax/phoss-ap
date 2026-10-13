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
import org.slf4j.MDC;

import com.helger.annotation.Nonempty;
import com.helger.base.state.ESuccess;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.ap.api.ITransactionAuditManager;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.api.model.ITransactionAuditItem;
import com.helger.phoss.ap.db.dto.TransactionAuditItemRow;

/**
 * JDBC implementation of {@link ITransactionAuditManager}.
 * Stores transaction lifecycle history and operator action audit records in the database
 * and concurrently streams audit events to Elasticsearch in Elastic Common Schema (ECS) format via SLF4J MDC.
 *
 * @author Philip Helger
 */
public class TransactionAuditManagerJdbc extends AbstractAPJdbcManager implements ITransactionAuditManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (TransactionAuditManagerJdbc.class);
  private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger ("AUDIT_TRAIL");

  private static final String COLS = "id, transaction_id, sbdh_instance_id, direction, event_type, action, " +
                                     "from_status, to_status, attempt_count, performed_by, performed_dt, details";

  private final String m_sTableName;

  public TransactionAuditManagerJdbc (@NonNull final IAPTimestampManager aTimestampMgr,
                                      @NonNull final String sTableNamePrefix)
  {
    super (aTimestampMgr);
    m_sTableName = sTableNamePrefix + "transaction_audit_log";
  }

  @Override
  @NonNull
  public ESuccess recordAudit (@Nullable final String sTransactionID,
                               @NonNull @Nonempty final String sSbdhInstanceID,
                               @NonNull @Nonempty final String sDirection,
                               @NonNull @Nonempty final String sEventType,
                               @NonNull @Nonempty final String sAction,
                               @Nullable final String sFromStatus,
                               @Nullable final String sToStatus,
                               @Nullable final Integer aAttemptCount,
                               @NonNull @Nonempty final String sPerformedBy,
                               @Nullable final String sDetails)
  {
    final OffsetDateTime aNow = now ();

    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("INSERT INTO " +
                                                                      m_sTableName +
                                                                      " (transaction_id, sbdh_instance_id, direction, event_type, action, " +
                                                                      "from_status, to_status, attempt_count, performed_by, performed_dt, details)" +
                                                                      " VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                                                                      new ConstantPreparedStatementDataProvider (sTransactionID,
                                                                                                                 sSbdhInstanceID,
                                                                                                                 sDirection,
                                                                                                                 sEventType,
                                                                                                                 sAction,
                                                                                                                 sFromStatus,
                                                                                                                 sToStatus,
                                                                                                                 aAttemptCount,
                                                                                                                 sPerformedBy,
                                                                                                                 DBValueHelper.toTimestamp (aNow),
                                                                                                                 sDetails));

    final boolean bSuccess = nRowsAffected == 1;

    // Stream audit event to Elasticsearch / SIEM in ECS 8.x format via Logback MDC
    try
    {
      MDC.put ("user.id", sPerformedBy);
      MDC.put ("event.category", "audit");
      MDC.put ("event.action", sAction);
      MDC.put ("event.outcome", bSuccess ? "success" : "failure");
      MDC.put ("peppol.sbdh_instance_id", sSbdhInstanceID);
      MDC.put ("peppol.direction", sDirection);
      if (sTransactionID != null)
        MDC.put ("peppol.transaction_id", sTransactionID);
      if (sFromStatus != null)
        MDC.put ("peppol.from_status", sFromStatus);
      if (sToStatus != null)
        MDC.put ("peppol.to_status", sToStatus);

      AUDIT_LOGGER.info ("AUDIT: action='{}' sbdhInstanceID='{}' direction='{}' performedBy='{}' outcome='{}' details='{}'",
                         sAction,
                         sSbdhInstanceID,
                         sDirection,
                         sPerformedBy,
                         bSuccess ? "success" : "failure",
                         sDetails != null ? sDetails : "");
    }
    finally
    {
      MDC.remove ("user.id");
      MDC.remove ("event.category");
      MDC.remove ("event.action");
      MDC.remove ("event.outcome");
      MDC.remove ("peppol.sbdh_instance_id");
      MDC.remove ("peppol.direction");
      MDC.remove ("peppol.transaction_id");
      MDC.remove ("peppol.from_status");
      MDC.remove ("peppol.to_status");
    }

    return ESuccess.valueOf (bSuccess);
  }

  @Override
  @NonNull
  public ICommonsList <ITransactionAuditItem> getTimelineBySbdhInstanceID (@NonNull @Nonempty final String sSbdhInstanceID)
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE sbdh_instance_id=?" +
                                                                      " ORDER BY performed_dt ASC, id ASC",
                                                                      new ConstantPreparedStatementDataProvider (sSbdhInstanceID));
    final ICommonsList <ITransactionAuditItem> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new TransactionAuditItemRow (aRow));
    return ret;
  }

  @Override
  @NonNull
  public ICommonsList <ITransactionAuditItem> getTimelineByTransactionID (@NonNull @Nonempty final String sTransactionID)
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE transaction_id=?" +
                                                                      " ORDER BY performed_dt ASC, id ASC",
                                                                      new ConstantPreparedStatementDataProvider (sTransactionID));
    final ICommonsList <ITransactionAuditItem> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (new TransactionAuditItemRow (aRow));
    return ret;
  }

  @Override
  @NonNull
  public ICommonsMap <String, Integer> getReplayCountsBySbdhInstanceID ()
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll (
        "SELECT sbdh_instance_id, COUNT(*) FROM " + m_sTableName +
        " WHERE action LIKE '%REPLAY%' OR action LIKE '%REVERIFY%' GROUP BY sbdh_instance_id");
    final ICommonsMap <String, Integer> ret = new CommonsHashMap <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
      {
        final String sID = aRow.getAsString (0);
        final Integer nCount = aRow.getAsInt (1);
        if (sID != null && !sID.trim ().isEmpty () && nCount != null)
          ret.put (sID, nCount);
      }
    return ret;
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("TableName", m_sTableName).getToString ();
  }
}
