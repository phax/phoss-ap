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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
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

  public ArchivalManagerJdbc (@NonNull final IAPTimestampManager aTimestampMgr)
  {
    super (aTimestampMgr);
  }

  public ESuccess archiveOutboundTransaction (@Nonempty final String sID)
  {
    ValueEnforcer.notEmpty (sID, "ID");

    final DBExecutor aExecutor = newExecutor ();
    return aExecutor.performInTransaction ( () -> {
      // Copy sending attempts first
      aExecutor.insertOrUpdateOrDelete ("INSERT INTO outbound_sending_attempt_archive" +
                                        " SELECT * FROM outbound_sending_attempt WHERE outbound_transaction_id=?",
                                        new ConstantPreparedStatementDataProvider (sID));
      aExecutor.insertOrUpdateOrDelete ("DELETE FROM outbound_sending_attempt WHERE outbound_transaction_id=?",
                                        new ConstantPreparedStatementDataProvider (sID));

      // Copy main transaction
      aExecutor.insertOrUpdateOrDelete ("INSERT INTO outbound_transaction_archive" +
                                        " SELECT * FROM outbound_transaction WHERE id=?",
                                        new ConstantPreparedStatementDataProvider (sID));
      aExecutor.insertOrUpdateOrDelete ("DELETE FROM outbound_transaction WHERE id=?",
                                        new ConstantPreparedStatementDataProvider (sID));

      LOGGER.info ("Archived outbound transaction '" + sID + "'");
    });
  }

  public ESuccess archiveInboundTransaction (final String sID)
  {
    ValueEnforcer.notEmpty (sID, "ID");

    final DBExecutor aExecutor = newExecutor ();
    return aExecutor.performInTransaction ( () -> {
      // Copy forwarding attempts first
      aExecutor.insertOrUpdateOrDelete ("INSERT INTO inbound_forwarding_attempt_archive" +
                                        " SELECT * FROM inbound_forwarding_attempt WHERE inbound_transaction_id=?",
                                        new ConstantPreparedStatementDataProvider (sID));
      aExecutor.insertOrUpdateOrDelete ("DELETE FROM inbound_forwarding_attempt WHERE inbound_transaction_id=?",
                                        new ConstantPreparedStatementDataProvider (sID));

      // Copy main transaction
      aExecutor.insertOrUpdateOrDelete ("INSERT INTO inbound_transaction_archive" +
                                        " SELECT * FROM inbound_transaction WHERE id=?",
                                        new ConstantPreparedStatementDataProvider (sID));
      aExecutor.insertOrUpdateOrDelete ("DELETE FROM inbound_transaction WHERE id=?",
                                        new ConstantPreparedStatementDataProvider (sID));

      LOGGER.info ("Archived inbound transaction '" + sID + "'");
    });
  }
}
