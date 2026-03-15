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
package com.helger.phoss.ap.db.testhelper;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.db.jdbc.executor.DBResultField;
import com.helger.db.jdbc.executor.DBResultRow;

/**
 * Helper to build {@link DBResultRow} instances for unit tests.
 */
public final class DBResultRowHelper
{
  /**
   * Subclass to expose the protected {@code internalAdd} method.
   */
  private static final class TestDBResultRow extends DBResultRow
  {
    TestDBResultRow (final int nCols)
    {
      super (nCols);
    }

    void addField (@NonNull final DBResultField aField)
    {
      internalAdd (aField);
    }
  }

  private DBResultRowHelper ()
  {}

  @Nullable
  private static Object _convertValue (@Nullable final Object aVal)
  {
    // DBResultRow.getAsOffsetDateTime goes through getAsTimestamp, so we must
    // store OffsetDateTime values as java.sql.Timestamp for the conversion
    // chain
    // to work correctly.
    if (aVal instanceof final OffsetDateTime aODT)
      return Timestamp.from (aODT.toInstant ());
    return aVal;
  }

  private static int _inferType (@Nullable final Object aVal)
  {
    if (aVal == null)
      return Types.NULL;
    if (aVal instanceof String)
      return Types.VARCHAR;
    if (aVal instanceof Integer)
      return Types.INTEGER;
    if (aVal instanceof Long)
      return Types.BIGINT;
    if (aVal instanceof Boolean)
      return Types.BOOLEAN;
    if (aVal instanceof byte [])
      return Types.VARBINARY;
    if (aVal instanceof Timestamp)
      return Types.TIMESTAMP;
    if (aVal instanceof Double)
      return Types.DOUBLE;
    return Types.OTHER;
  }

  /**
   * Create a {@link DBResultRow} from the given values. Each value becomes a
   * column named {@code "col0"}, {@code "col1"}, etc. The JDBC type is inferred
   * from the Java type.
   *
   * @param aValues
   *        Values to add.
   * @return never <code>null</code>
   */
  @NonNull
  public static DBResultRow createRow (@NonNull final Object @Nullable... aValues)
  {
    final TestDBResultRow aRow = new TestDBResultRow (aValues.length);
    for (int i = 0; i < aValues.length; i++)
    {
      final Object aVal = _convertValue (aValues[i]);
      final int nType = _inferType (aVal);
      aRow.addField (new DBResultField ("col" + i, nType, aVal));
    }
    return aRow;
  }
}
