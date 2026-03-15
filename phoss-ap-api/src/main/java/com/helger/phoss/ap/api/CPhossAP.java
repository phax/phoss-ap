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
package com.helger.phoss.ap.api;

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.cache.regex.RegExHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;

/**
 * Global constants for phoss AP.
 *
 * @author Philip Helger
 */
@Immutable
public final class CPhossAP
{
  /** The default locale used for display messages and error formatting. */
  public static final Locale DEFAULT_LOCALE = Locale.UK;

  private CPhossAP ()
  {}

  /**
   * Check if the provided string looks like a valid Peppol Seat ID.
   *
   * @param sSeatID
   *        The Seat ID to check. May be <code>null</code>.
   * @return <code>true</code> if the value matches the Peppol Seat ID pattern.
   */
  public static boolean isPeppolSeatID (@Nullable final String sSeatID)
  {
    return sSeatID != null && RegExHelper.stringMatchesPattern (PeppolIdentifierHelper.REGEX_SEAT_ID, sSeatID);
  }

  /**
   * Check if the provided document type and process identifiers represent a
   * Peppol MLS message.
   *
   * @param aDocTypeID
   *        The document type identifier. May not be <code>null</code>.
   * @param aProcessID
   *        The process identifier. May not be <code>null</code>.
   * @return <code>true</code> if both identifiers match the Peppol MLS 1.0
   *         document type and process.
   */
  public static boolean isMLS (@NonNull final IDocumentTypeIdentifier aDocTypeID,
                               @NonNull final IProcessIdentifier aProcessID)
  {
    return aDocTypeID.hasSameContent (EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0) &&
           aProcessID.hasSameContent (EPredefinedProcessIdentifier.urn_peppol_edec_mls);
  }

  /**
   * Check if the provided URI-encoded document type and process identifier
   * strings represent a Peppol MLS message.
   *
   * @param sDocTypeID
   *        The URI-encoded document type identifier. May not be
   *        <code>null</code>.
   * @param sProcessID
   *        The URI-encoded process identifier. May not be <code>null</code>.
   * @return <code>true</code> if both identifiers match the Peppol MLS 1.0
   *         document type and process.
   */
  public static boolean isMLS (@NonNull final String sDocTypeID, @NonNull final String sProcessID)
  {
    return EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getURIEncoded ().equals (sDocTypeID) &&
           EPredefinedProcessIdentifier.urn_peppol_edec_mls.getURIEncoded ().equals (sProcessID);
  }
}
