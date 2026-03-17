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
 * The method used to deliver received documents to the Receiver Backend.
 * Configured per AP instance.
 *
 * @author Philip Helger
 */
public enum EForwardingMode implements IHasID <String>
{
  /**
   * SBD is POSTed to the Receiver Backend; reporting is triggered
   * asynchronously later.
   */
  HTTP_POST_ASYNC ("http_post_async"),
  /**
   * SBD is POSTed to the Receiver Backend; C4 country code is returned
   * synchronously.
   */
  HTTP_POST_SYNC ("http_post_sync"),
  /**
   * SBD is stored in S3; a link/reference is forwarded to the Receiver Backend.
   */
  S3_LINK ("s3_link"),
  /** SBD is uploaded to the Receiver Backend via SFTP. */
  SFTP ("sftp");

  private final String m_sID;

  EForwardingMode (@NonNull @Nonempty final String sID)
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
   * @return <code>true</code> if this forwarding mode provides delivery
   *         confirmation (HTTP modes), <code>false</code> if it does not (SFTP,
   *         S3).
   */
  public boolean isWithDeliveryConfirmation ()
  {
    return this == HTTP_POST_SYNC || this == HTTP_POST_ASYNC;
  }

  /**
   * Find the enum constant matching the given ID.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or <code>null</code> if not found.
   */
  @Nullable
  public static EForwardingMode getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EForwardingMode.class, sID);
  }
}
