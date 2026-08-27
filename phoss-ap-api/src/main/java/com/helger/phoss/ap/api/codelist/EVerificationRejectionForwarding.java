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
 * Defines if an inbound document that did <b>not</b> pass the verification is nevertheless
 * forwarded to C4 - and with which delivery strength. The rejection itself is unaffected by this
 * mode: the verdict {@link EVerificationResult#REJECTED} is always recorded and the negative MLS
 * (RE) is always sent to C2.
 * <p>
 * This is needed for deployments in which C4 must be able to act on a document that failed the
 * content validation - e.g. the UAE Peppol DCTCE model, where C3 has to produce a Tax Data Summary
 * instead of a Tax Data Document for such an invoice.
 * </p>
 *
 * @author Philip Helger
 * @since 0.12.0
 */
public enum EVerificationRejectionForwarding implements IHasID <String>
{
  /**
   * The rejected document is never forwarded and the transaction ends in the status
   * {@link EInboundStatus#REJECTED}. This is the default and the behaviour of all versions before
   * 0.12.0.
   */
  NONE ("none"),
  /**
   * A fire-and-forget copy of the rejected document is dispatched to the primary and to all
   * secondary document forwarders. The transaction still ends in the status
   * {@link EInboundStatus#REJECTED}, no forwarding attempt is recorded and a failing forwarder is
   * only logged - there is no retry.
   */
  BEST_EFFORT ("best-effort"),
  /**
   * The rejected document runs through the regular forwarding state machine, including the
   * forwarding attempts and their retries. The transaction therefore ends in the status
   * {@link EInboundStatus#FORWARDED} or {@link EInboundStatus#PERMANENTLY_FAILED}, while the
   * rejection stays visible in {@link EVerificationResult#REJECTED}.
   */
  RETRY ("retry");

  /** The default mode, if none is configured */
  public static final EVerificationRejectionForwarding DEFAULT = NONE;

  private final String m_sID;

  EVerificationRejectionForwarding (@NonNull @Nonempty final String sID)
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
  public static EVerificationRejectionForwarding getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EVerificationRejectionForwarding.class, sID);
  }
}
