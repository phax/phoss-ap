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

@Immutable
public final class CPhossAP
{
  public static final Locale DEFAULT_LOCALE = Locale.UK;

  private CPhossAP ()
  {}

  public static boolean isPeppolSeatID (@Nullable final String sSeatID)
  {
    return sSeatID != null && RegExHelper.stringMatchesPattern (PeppolIdentifierHelper.REGEX_SEAT_ID, sSeatID);
  }

  public static boolean isMLS (@NonNull final IDocumentTypeIdentifier aDocTypeID, @NonNull final IProcessIdentifier aProcessID)
  {
    return aDocTypeID.hasSameContent (EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0) &&
           aProcessID.hasSameContent (EPredefinedProcessIdentifier.urn_peppol_edec_mls);
  }
}
