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
 * The persisted verdict of the inbound document verification. It is deliberately kept separate from
 * {@link EInboundStatus}, because the status describes the lifecycle of a transaction (and is
 * overwritten by every forwarding attempt), whereas this verdict must survive for auditing - also
 * for a document that is forwarded despite having been rejected.
 * <p>
 * A value of <code>null</code> means that no verification was performed (yet) - either because
 * <code>verification.inbound.enabled</code> is <code>false</code>, or because the verification is
 * still deferred (see {@link EInboundStatus#VERIFICATION_DEFERRED}).
 * </p>
 *
 * @author Philip Helger
 * @since 0.12.0
 */
public enum EVerificationResult implements IHasID <String>
{
  /** The document was inspected by all verifiers and accepted. */
  PASSED ("passed"),
  /**
   * The document was inspected and rejected - either by an explicit verifier rejection, or because
   * a verifier was unavailable and {@link EVerificationFailMode#CLOSED} was configured.
   */
  REJECTED ("rejected"),
  /**
   * The document was never inspected, because a verifier was unavailable and
   * {@link EVerificationFailMode#OPEN} was configured - it was forwarded unverified.
   */
  UNVERIFIED ("unverified");

  private final String m_sID;

  EVerificationResult (@NonNull @Nonempty final String sID)
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
   * @return <code>true</code> if the document was inspected and rejected.
   */
  public boolean isRejected ()
  {
    return this == REJECTED;
  }

  /**
   * Find the enum constant matching the given ID.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or <code>null</code> if not found.
   */
  @Nullable
  public static EVerificationResult getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EVerificationResult.class, sID);
  }
}
