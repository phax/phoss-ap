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
import com.helger.base.numeric.mutable.MutableBoolean;
import com.helger.base.state.EChange;
import com.helger.base.state.EContinue;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phase4.duplicate.AS4DuplicateItem;
import com.helger.phase4.duplicate.IAS4DuplicateItem;
import com.helger.phase4.duplicate.IAS4DuplicateManager;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;

/**
 * JDBC-backed implementation of {@link IAS4DuplicateManager}. Stores incoming AS4 message IDs in
 * the <code>as4_duplicate_item</code> table so that duplicate detection survives restarts and works
 * across multiple phoss-ap instances sharing one database.
 *
 * @author Philip Helger
 */
public class AS4DuplicateManagerJdbc extends AbstractAPJdbcManager implements IAS4DuplicateManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4DuplicateManagerJdbc.class);
  private static final String COLS = "message_id, profile_id, pmode_id, created_dt";

  private final String m_sTableName;

  /**
   * Constructor.
   *
   * @param aTimestampMgr
   *        The timestamp manager to use. May not be <code>null</code>.
   * @param sTableNamePrefix
   *        The database table name prefix. May not be <code>null</code>.
   */
  public AS4DuplicateManagerJdbc (@NonNull final IAPTimestampManager aTimestampMgr,
                                  @NonNull final String sTableNamePrefix)
  {
    super (aTimestampMgr);
    m_sTableName = sTableNamePrefix + "as4_duplicate_item";
  }

  @NonNull
  private static IAS4DuplicateItem _toItem (@NonNull final DBResultRow aRow)
  {
    final String sMessageID = aRow.getAsString (0);
    final String sProfileID = aRow.getAsString (1);
    final String sPModeID = aRow.getAsString (2);
    final OffsetDateTime aDT = aRow.getAsOffsetDateTime (3);
    return new AS4DuplicateItem (aDT, sMessageID, sProfileID, sPModeID);
  }

  public boolean isEmpty ()
  {
    return size () == 0;
  }

  @Nonnegative
  public int size ()
  {
    return (int) newExecutor ().queryCount ("SELECT COUNT(*) FROM " + m_sTableName);
  }

  @Nullable
  public IAS4DuplicateItem getItemOfMessageID (@Nullable final String sMessageID)
  {
    if (StringHelper.isEmpty (sMessageID))
      return null;

    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " +
                                                                      COLS +
                                                                      " FROM " +
                                                                      m_sTableName +
                                                                      " WHERE message_id=?",
                                                                      new ConstantPreparedStatementDataProvider (sMessageID));
    if (aRows == null || aRows.isEmpty ())
      return null;
    return _toItem (aRows.getFirstOrNull ());
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IAS4DuplicateItem> getAll ()
  {
    final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT " + COLS + " FROM " + m_sTableName);
    final ICommonsList <IAS4DuplicateItem> ret = new CommonsArrayList <> ();
    if (aRows != null)
      for (final DBResultRow aRow : aRows)
        ret.add (_toItem (aRow));
    return ret;
  }

  @NonNull
  public EContinue registerAndCheck (@Nullable final String sMessageID,
                                     @Nullable final String sProfileID,
                                     @Nullable final String sPModeID)
  {
    if (StringHelper.isEmpty (sMessageID))
    {
      // No message ID present - don't check for duplication
      return EContinue.CONTINUE;
    }

    final DBExecutor aExecutor = newExecutor ();
    final MutableBoolean aIsDup = new MutableBoolean (false);
    if (aExecutor.performInTransaction ( () -> {
      // Check first; the PK constraint serves as a backstop for races
      final long nExisting = aExecutor.queryCount ("SELECT COUNT(*) FROM " + m_sTableName + " WHERE message_id=?",
                                                   new ConstantPreparedStatementDataProvider (sMessageID));
      if (nExisting > 0)
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("AS4 message ID '" + sMessageID + "' is already known - duplicate");
        aIsDup.set (true);
      }
      else
      {
        final long nRowsAffected = aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                                                     m_sTableName +
                                                                     " (" +
                                                                     COLS +
                                                                     ") VALUES (?,?,?,?)",
                                                                     new ConstantPreparedStatementDataProvider (sMessageID,
                                                                                                                sProfileID,
                                                                                                                sPModeID,
                                                                                                                now ()));
        if (nRowsAffected != 1)
        {
          // PK violation race - treat as duplicate
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Insert into " +
                          m_sTableName +
                          " for AS4 message ID '" +
                          sMessageID +
                          "' affected " +
                          nRowsAffected +
                          " rows - treating as duplicate");
          aIsDup.set (true);
        }
      }
    }).isFailure ())
    {
      // Most likely concurrent insert - treat as duplicate
      LOGGER.warn ("Failed to insert AS4 duplicate item for message ID '" +
                   sMessageID +
                   "' (likely concurrent insert) - see the preceeding log");
      return EContinue.BREAK;
    }

    return aIsDup.booleanValue () ? EContinue.BREAK : EContinue.CONTINUE;
  }

  @NonNull
  public EChange clearCache ()
  {
    final long nRowsAffected = newExecutor ().insertOrUpdateOrDelete ("DELETE FROM " + m_sTableName,
                                                                      new ConstantPreparedStatementDataProvider ());
    return EChange.valueOf (nRowsAffected > 0);
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <String> evictAllItemsBefore (@NonNull final OffsetDateTime aRefDT)
  {
    ValueEnforcer.notNull (aRefDT, "RefDT");

    final ICommonsList <String> aEvictedIDs = new CommonsArrayList <> ();
    final DBExecutor aExecutor = newExecutor ();
    aExecutor.performInTransaction ( () -> {
      final ICommonsList <DBResultRow> aRows = aExecutor.queryAll ("SELECT message_id FROM " +
                                                                   m_sTableName +
                                                                   " WHERE created_dt < ?",
                                                                   new ConstantPreparedStatementDataProvider (aRefDT));
      if (aRows != null)
        for (final DBResultRow aRow : aRows)
          aEvictedIDs.add (aRow.getAsString (0));

      if (aEvictedIDs.isNotEmpty ())
      {
        aExecutor.insertOrUpdateOrDelete ("DELETE FROM " + m_sTableName + " WHERE created_dt < ?",
                                          new ConstantPreparedStatementDataProvider (aRefDT));
      }
    });
    return aEvictedIDs;
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("TableName", m_sTableName).getToString ();
  }
}
