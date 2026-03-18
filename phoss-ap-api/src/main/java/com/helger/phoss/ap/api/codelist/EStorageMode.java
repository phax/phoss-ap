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
package com.helger.phoss.ap.api.codelist;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * Configurable mode for storing the payload of transactions. Defaults to file system as it was the
 * initial solution.
 *
 * @author Philip Helger
 * @since 0.1.1
 */
public enum EStorageMode implements IHasID <String>
{
  /**
   * Store messages on the file system
   */
  FILE_SYSTEM ("filesystem"),
  /**
   * Store messages on S3. Supported since v0.1.1
   */
  S3 ("s3");

  public static final EStorageMode DEFAULT = FILE_SYSTEM;

  private final String m_sID;

  EStorageMode (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * Find the enum constant matching the given ID.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or <code>null</code> if not found.
   */
  @Nullable
  public static EStorageMode getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EStorageMode.class, sID);
  }

  /**
   * Find the enum constant matching the given ID or return the default.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or {@link #DEFAULT} if not found.
   */
  @NonNull
  public static EStorageMode getFromIDOrDefault (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrDefault (EStorageMode.class, sID, DEFAULT);
  }
}
