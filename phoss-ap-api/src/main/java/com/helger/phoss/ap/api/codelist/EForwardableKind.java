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
 * The nature of a document that is handed to a document forwarder. This is the piece of information
 * that the document itself cannot provide: C4 can tell an MLS <em>received</em> from the Peppol
 * network apart from an MLS this AP <em>sent</em> itself, which matters as soon as both end up on
 * the same endpoint.
 *
 * @author Philip Helger
 * @since 0.12.0
 */
public enum EForwardableKind implements IHasID <String>
{
  /** A business document received from the Peppol network. */
  INBOUND_DOCUMENT ("inbound-document"),
  /** A Message Level Status received from the Peppol network. */
  INBOUND_MLS ("inbound-mls"),
  /** A copy of a Message Level Status that this AP generated and sent itself. */
  OUTBOUND_MLS_COPY ("outbound-mls-copy");

  private final String m_sID;

  EForwardableKind (@NonNull @Nonempty final String sID)
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
   * @return <code>true</code> if the document was received from the Peppol network, and
   *         <code>false</code> if this AP created it itself.
   */
  public boolean isInbound ()
  {
    return this == INBOUND_DOCUMENT || this == INBOUND_MLS;
  }

  /**
   * Find the enum constant matching the given ID.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or <code>null</code> if not found.
   */
  @Nullable
  public static EForwardableKind getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EForwardableKind.class, sID);
  }
}
