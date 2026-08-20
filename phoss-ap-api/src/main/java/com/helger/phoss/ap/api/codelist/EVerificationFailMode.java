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
 * Defines how an inbound document is handled, if a document verifier cannot make a verdict,
 * because its backend service is unavailable (see
 * {@link EVerificationOutcomeCategory#SERVICE_UNAVAILABLE}).
 * <p>
 * This mode is only applied to unavailable verifier services. A verifier that inspected the
 * document and rejected it, always leads to a rejection, independent of this mode.
 * </p>
 *
 * @author Philip Helger
 * @since 0.12.0
 */
public enum EVerificationFailMode implements IHasID <String>
{
  /**
   * Treat the unavailability like a rejection: the document is rejected and a negative MLS is sent
   * back to C2. This is the default and the behaviour of all versions before 0.12.0.
   */
  CLOSED ("closed"),
  /**
   * Treat the unavailability like a successful verification: the document is forwarded to C4
   * without having been verified. A warning is logged for every such document.
   */
  OPEN ("open"),
  /**
   * Keep the document in the state {@link EInboundStatus#VERIFICATION_DEFERRED} and re-verify it
   * later, until the verifier service is available again. No MLS is sent to C2 while the
   * verification is deferred.
   */
  DEFERRED ("deferred");

  /** The default mode, if none is configured */
  public static final EVerificationFailMode DEFAULT = CLOSED;

  private final String m_sID;

  EVerificationFailMode (@NonNull @Nonempty final String sID)
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
  public static EVerificationFailMode getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EVerificationFailMode.class, sID);
  }
}
