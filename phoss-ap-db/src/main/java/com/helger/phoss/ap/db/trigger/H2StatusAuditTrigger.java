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
package com.helger.phoss.ap.db.trigger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.h2.api.Trigger;

/**
 * H2 Database Trigger to automatically capture transaction lifecycle status and retry
 * count transitions into the {@code transaction_audit_log} table.
 *
 * @author Philip Helger
 */
public class H2StatusAuditTrigger implements Trigger
{
  private String m_sSchemaName;
  private String m_sTableName;

  @Override
  public void init (final Connection aConn,
                    final String sSchemaName,
                    final String sTriggerName,
                    final String sTableName,
                    final boolean bBefore,
                    final int nType) throws SQLException
  {
    m_sSchemaName = sSchemaName;
    m_sTableName = sTableName;
  }

  @Override
  public void fire (final Connection aConn, final Object [] aOldRow, final Object [] aNewRow) throws SQLException
  {
    if (aOldRow == null || aNewRow == null)
      return;

    final String sDirection;
    final String sTxId = (String) aNewRow[0];
    final String sSbdhId;
    final String sOldStatus;
    final String sNewStatus;
    final Integer aOldAttempt;
    final Integer aNewAttempt;
    final String sDetails;

    if ("INBOUND_TRANSACTION".equalsIgnoreCase (m_sTableName))
    {
      sDirection = "INBOUND";
      sSbdhId = (String) aNewRow[14];
      sOldStatus = (String) aOldRow[19];
      sNewStatus = (String) aNewRow[19];
      aOldAttempt = (Integer) aOldRow[20];
      aNewAttempt = (Integer) aNewRow[20];
      sDetails = (String) aNewRow[25];
    }
    else if ("OUTBOUND_TRANSACTION".equalsIgnoreCase (m_sTableName))
    {
      sDirection = "OUTBOUND";
      sSbdhId = (String) aNewRow[6];
      sOldStatus = (String) aOldRow[12];
      sNewStatus = (String) aNewRow[12];
      aOldAttempt = (Integer) aOldRow[13];
      aNewAttempt = (Integer) aNewRow[13];
      sDetails = (String) aNewRow[18];
    }
    else
    {
      return;
    }

    if (!Objects.equals (sOldStatus, sNewStatus) || !Objects.equals (aOldAttempt, aNewAttempt))
    {
      final String sTargetTable = (m_sSchemaName != null && !m_sSchemaName.isEmpty () ? m_sSchemaName + "." : "") +
                                  "transaction_audit_log";
      final String sSql = "INSERT INTO " +
                          sTargetTable +
                          " (" +
                          "transaction_id, sbdh_instance_id, direction, event_type, action, " +
                          "from_status, to_status, attempt_count, performed_by, performed_dt, details) " +
                          "VALUES (?, ?, ?, 'STATUS_CHANGE', 'STATUS_UPDATE', ?, ?, ?, 'SYSTEM', CURRENT_TIMESTAMP, ?)";
      try (final PreparedStatement aPS = aConn.prepareStatement (sSql))
      {
        aPS.setString (1, sTxId);
        aPS.setString (2, sSbdhId);
        aPS.setString (3, sDirection);
        aPS.setString (4, sOldStatus);
        aPS.setString (5, sNewStatus);
        if (aNewAttempt != null)
          aPS.setInt (6, aNewAttempt.intValue ());
        else
          aPS.setNull (6, Types.INTEGER);
        aPS.setString (7, sDetails);
        aPS.executeUpdate ();
      }
    }
  }

  @Override
  public void close () throws SQLException
  {}

  @Override
  public void remove () throws SQLException
  {}
}
