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

import java.time.Duration;
import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;

/**
 * JDBC manager for querying MLS SLA metrics across inbound/outbound tables. Provides the data
 * needed to calculate MLS-1 (M2 - M1) and MLS-2 (M3 - M1) per Peppol Network Policy.
 *
 * @author Philip Helger
 */
public class MlsMetricsManagerJdbc extends AbstractAPJdbcManager
{
  /** SLA threshold for MLS-1 (M2 - M1): 20 minutes */
  public static final Duration SLA_MLS1_THRESHOLD = Duration.ofMinutes (20);

  /** SLA threshold for MLS-2 (M3 - M1): 25 minutes */
  public static final Duration SLA_MLS2_THRESHOLD = Duration.ofMinutes (25);

  /** SLA compliance target: 99.5% */
  public static final double SLA_COMPLIANCE_PERCENT = 99.5;

  private final String m_sInboundTable;
  private final String m_sOutboundTable;
  private final String m_sOutboundAttemptTable;

  /**
   * Constructor.
   *
   * @param aTimestampMgr
   *        The timestamp manager to use. May not be <code>null</code>.
   * @param sTableNamePrefix
   *        The database table name prefix. May not be <code>null</code>.
   */
  public MlsMetricsManagerJdbc (@NonNull final IAPTimestampManager aTimestampMgr,
                                @NonNull final String sTableNamePrefix)
  {
    super (aTimestampMgr);
    m_sInboundTable = sTableNamePrefix + "inbound_transaction";
    m_sOutboundTable = sTableNamePrefix + "outbound_transaction";
    m_sOutboundAttemptTable = sTableNamePrefix + "outbound_sending_attempt";
  }

  /**
   * An individual MLS SLA measurement data point.
   *
   * @param sbdhInstanceID
   *        SBDH Instance ID
   * @param m1
   *        M1 timestamp
   * @param m2OrM3
   *        M2 or M3 timestamp
   * @param durationSeconds
   *        The duration in seconds between the 2 timestamps
   * @param withinSla
   *        <code>true</code> if the entry is within the SLA, <code>false</code> if not.
   */
  public record MlsSlaEntry (@NonNull String sbdhInstanceID,
                             @NonNull OffsetDateTime m1,
                             @NonNull OffsetDateTime m2OrM3,
                             @Nonnegative long durationSeconds,
                             boolean withinSla)
  {}

  /**
   * Aggregated SLA report.
   *
   * @param entries
   *        All MLS relevant entries
   * @param totalCount
   *        Total amount of MLS entries found. Must be &ge; 0.
   * @param withinSlaCount
   *        How many of them are within the SLA. Must be &ge; 0.
   * @param compliancePercent
   *        The percentage of compliant entries. Always &ge; 0.
   * @param targetPercent
   *        The target percentage needed. Always &ge; 0.
   * @param thresholdSeconds
   *        The threshold in seconds as defined by the PNP. Always &gt; 0.
   */
  public record MlsSlaReport (@NonNull ICommonsList <MlsSlaEntry> entries,
                              @Nonnegative int totalCount,
                              @Nonnegative int withinSlaCount,
                              @Nonnegative double compliancePercent,
                              @Nonnegative double targetPercent,
                              @Nonnegative long thresholdSeconds)
  {
    /**
     * @return {@code true} if the compliance percentage meets or exceeds the target percentage.
     */
    public boolean isMeetingSla ()
    {
      return compliancePercent >= targetPercent;
    }
  }

  @NonNull
  private static MlsSlaReport _buildReport (@Nullable final ICommonsList <DBResultRow> aRows,
                                            @NonNull final Duration aThreshold)
  {
    final ICommonsList <MlsSlaEntry> aEntries = new CommonsArrayList <> ();
    int nWithinSla = 0;

    if (aRows != null)
    {
      for (final DBResultRow aRow : aRows)
      {
        final String sSbdhInstanceID = aRow.getAsString (0);
        final OffsetDateTime aM1 = aRow.getAsOffsetDateTime (1);
        final OffsetDateTime aM2OrM3 = aRow.getAsOffsetDateTime (2);

        if (aM1 != null && aM2OrM3 != null)
        {
          final Duration aDuration = Duration.between (aM1, aM2OrM3);
          final long nDurationSeconds = aDuration.toSeconds ();
          final boolean bWithinSla = aDuration.compareTo (aThreshold) <= 0;

          if (bWithinSla)
            nWithinSla++;

          aEntries.add (new MlsSlaEntry (sSbdhInstanceID, aM1, aM2OrM3, nDurationSeconds, bWithinSla));
        }
      }
    }

    final int nTotal = aEntries.size ();
    final double dCompliancePercent = nTotal > 0 ? (nWithinSla * 100.0) / nTotal : 100.0;

    return new MlsSlaReport (aEntries,
                             nTotal,
                             nWithinSla,
                             dCompliancePercent,
                             SLA_COMPLIANCE_PERCENT,
                             aThreshold.toSeconds ());
  }

  /**
   * Calculate MLS-1 SLA (M2 - M1) for the receiving side. M1 = AS4 timestamp of the received
   * inbound business document. M2 = AS4 timestamp of the successful sending attempt of the MLS
   * response outbound transaction.
   * <p>
   * Joins: inbound_transaction → (mls_outbound_transaction_id) → outbound_sending_attempt (success)
   *
   * @return The SLA report. Never <code>null</code>.
   */
  @NonNull
  public MlsSlaReport getMls1Report ()
  {
    // Query: join inbound tx (business docs with MLS sent) with outbound
    // attempt (successful MLS send)
    final String sSQL = "SELECT i.sbdh_instance_id, i.as4_timestamp, a.as4_timestamp" +
                        " FROM " +
                        m_sInboundTable +
                        " i" +
                        " JOIN " +
                        m_sOutboundAttemptTable +
                        " a ON a.outbound_transaction_id = i.mls_outbound_transaction_id" +
                        " WHERE i.mls_outbound_transaction_id IS NOT NULL" +
                        " AND NOT (i.doc_type_id=? AND i.process_id=?)" +
                        " AND a.attempt_status=?" +
                        " ORDER BY i.as4_timestamp";

    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll (sSQL,
                                                                      new ConstantPreparedStatementDataProvider (EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getURIEncoded (),
                                                                                                                 EPredefinedProcessIdentifier.urn_peppol_edec_mls.getURIEncoded (),
                                                                                                                 EAttemptStatus.SUCCESS.getID ()));

    return _buildReport (aRows, SLA_MLS1_THRESHOLD);
  }

  /**
   * Calculate MLS-2 SLA (M3 - M1) for the sending side. M1 = AS4 timestamp of the successful
   * sending attempt of the outbound business document. M3 = mls_received_dt on the outbound
   * transaction.
   * <p>
   * Joins: outbound_transaction (business docs with MLS received) → outbound_sending_attempt
   * (success)
   *
   * @return The SLA report. Never <code>null</code>.
   */
  @NonNull
  public MlsSlaReport getMls2Report ()
  {
    // Query: join outbound tx (business docs with MLS received) with sending
    // attempt (successful send)
    final String sSQL = "SELECT o.sbdh_instance_id, a.as4_timestamp, o.mls_received_dt" +
                        " FROM " +
                        m_sOutboundTable +
                        " o" +
                        " JOIN " +
                        m_sOutboundAttemptTable +
                        " a ON a.outbound_transaction_id = o.id" +
                        " WHERE o.transaction_type=?" +
                        " AND o.mls_received_dt IS NOT NULL" +
                        " AND a.attempt_status=?" +
                        " ORDER BY a.as4_timestamp";

    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll (sSQL,
                                                                      new ConstantPreparedStatementDataProvider (ETransactionType.BUSINESS_DOCUMENT.getID (),
                                                                                                                 EAttemptStatus.SUCCESS.getID ()));

    return _buildReport (aRows, SLA_MLS2_THRESHOLD);
  }
}
