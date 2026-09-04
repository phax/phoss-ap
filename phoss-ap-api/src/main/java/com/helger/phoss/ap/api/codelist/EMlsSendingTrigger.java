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
 * Defines what triggers the <b>positive</b> MLS of a successfully forwarded inbound document. This
 * only affects the success path: the negative MLS (RE) of a verification rejection and the
 * acknowledging MLS (AB) of an exhausted forwarding retry are always sent automatically, because
 * the document never reached the Receiver Backend and it can therefore not report anything.
 * <p>
 * This is an implementation concern of this AP and deliberately not a new value of
 * {@code mls.type}, which maps 1:1 onto the Peppol enum
 * {@link com.helger.peppol.sbdh.EPeppolMLSType}.
 * </p>
 *
 * @author Philip Helger
 * @since 0.13.0
 */
public enum EMlsSendingTrigger implements IHasID <String>
{
  /**
   * The positive MLS is sent immediately after the document was successfully forwarded to C4. This
   * is the default and the behaviour of all versions before 0.13.0.
   */
  AUTO ("auto"),
  /**
   * The positive MLS is only sent when the Receiver Backend reports the outcome via
   * {@code POST /api/mls/send}. If it stays silent, the fallback MLS is sent after
   * {@code mls.sending.api.timeout}.
   */
  API ("api");

  /** The default trigger, if none is configured */
  public static final EMlsSendingTrigger DEFAULT = AUTO;

  private final String m_sID;

  EMlsSendingTrigger (@NonNull @Nonempty final String sID)
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
  public static EMlsSendingTrigger getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EMlsSendingTrigger.class, sID);
  }
}
