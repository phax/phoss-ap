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
package com.helger.phoss.ap.core.startup;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.spi.APStartupCheckError;
import com.helger.phoss.ap.api.spi.IDocumentVerifier;
import com.helger.phoss.ap.core.APCoreMetaManager;

/**
 * Check that the IDs of all loaded inbound and outbound document verifiers are set and unique, so
 * that a verifier can be identified by its ID. The verifiers are loaded by
 * {@link APCoreMetaManager#initVerifierSPIs()} before this check runs. See
 * <a href="https://github.com/phax/phoss-ap/issues/80">issue 80</a>.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckDocumentVerifiers extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "document-verifiers";

  public StartupCheckDocumentVerifiers ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.POST_INIT);
  }

  /**
   * Check that the IDs of all inbound and outbound verifiers are unique. A verifier class that
   * implements both SPIs is loaded twice - once per SPI - and therefore contributes the same ID
   * twice, which is not a duplicate.
   *
   * @param aAllVerifiers
   *        All loaded inbound and outbound verifiers. May not be <code>null</code>.
   * @param aErrors
   *        The error list to add to. May not be <code>null</code>.
   */
  @VisibleForTesting
  static void checkVerifierIDsAreUnique (@NonNull final Iterable <? extends IDocumentVerifier> aAllVerifiers,
                                         @NonNull final ErrorList aErrors)
  {
    // Maps the verifier ID to the implementation class that uses it
    final ICommonsMap <String, Class <?>> aIDToClass = new CommonsHashMap <> ();

    for (final IDocumentVerifier aVerifier : aAllVerifiers)
    {
      final String sID = aVerifier.getID ();
      if (StringHelper.isEmpty (sID))
      {
        aErrors.add (APStartupCheckError.create (CHECK_NAME,
                                                 "The document verifier " +
                                                             aVerifier.getClass ().getName () +
                                                             " provides an empty ID"));
        continue;
      }

      final Class <?> aExistingClass = aIDToClass.get (sID);
      if (aExistingClass == null)
        aIDToClass.put (sID, aVerifier.getClass ());
      else
        if (!aExistingClass.equals (aVerifier.getClass ()))
        {
          // The same ID is used by two different implementations
          aErrors.add (APStartupCheckError.create (CHECK_NAME,
                                                   "The document verifier ID '" +
                                                               sID +
                                                               "' is used by " +
                                                               aExistingClass.getName () +
                                                               " and by " +
                                                               aVerifier.getClass ().getName () +
                                                               ". Verifier IDs must be unique over all inbound and outbound verifiers."));
        }
    }
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final ICommonsList <IDocumentVerifier> aAllVerifiers = new CommonsArrayList <> ();
    aAllVerifiers.addAll (APCoreMetaManager.getAllInboundVerifiers ());
    aAllVerifiers.addAll (APCoreMetaManager.getAllOutboundVerifiers ());
    checkVerifierIDsAreUnique (aAllVerifiers, aErrors);
  }
}
