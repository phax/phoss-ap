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
package com.helger.phoss.ap.validation;

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.location.ILocation;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.level.EErrorLevel;
import com.helger.phive.api.EValidationBaseType;
import com.helger.phive.api.result.ValidationResult;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.codelist.EVerificationIssueLevel;
import com.helger.phoss.ap.api.codelist.EVerificationIssueType;
import com.helger.phoss.ap.api.model.VerificationIssue;

/**
 * Maps a phive {@link ValidationResultList} to transport-neutral {@link VerificationIssue}s.
 * <p>
 * The validation artefact's base type selects the {@link EVerificationIssueType} (XSD yields
 * {@link EVerificationIssueType#SYNTAX}, Schematron and others yield
 * {@link EVerificationIssueType#BUSINESS_RULE}) and the phive error level selects the
 * {@link EVerificationIssueLevel}. Entries below WARN are dropped, as are Saxon transformation
 * warnings.
 * </p>
 * <p>
 * Unlike the MLS-specific predecessor of this class, the phive error ID is kept as a separate
 * {@link VerificationIssue#getCode()} instead of being prefixed to the description as
 * <code>[ID] text</code>. That is what allows a client to react to a specific rule - e.g.
 * <code>PEPPOL-EN16931-R001</code> - without parsing human-readable text. The MLS representation
 * still re-adds the prefix, see
 * {@link com.helger.phoss.ap.api.model.MlsOutcomeIssue#fromVerificationIssue(VerificationIssue)}.
 * </p>
 *
 * @author Philip Helger
 * @since 0.10.0 - mapped to MLS until 0.12.0
 */
@Immutable
public final class PhiveToVerificationMapper
{
  private PhiveToVerificationMapper ()
  {}

  @Nullable
  private static String _location (@NonNull final IError aError)
  {
    // Phive Schematron populates errorFieldName with the failed assertion's XPath context
    final String sFieldName = aError.getErrorFieldName ();
    if (StringHelper.isNotEmpty (sFieldName))
      return sFieldName;

    // Fall back to formatted location (resource:line:column) if any info is present
    final ILocation aLocation = aError.getErrorLocation ();
    if (aLocation != null && aLocation.isAnyInformationPresent ())
    {
      final String sLocation = aLocation.getAsString ();
      if (StringHelper.isNotEmpty (sLocation))
        return sLocation;
    }

    // Unknown - deliberately null and not the MLS "NA" sentinel
    return null;
  }

  @NonNull
  private static VerificationIssue _toIssue (@NonNull final IError aError,
                                             @NonNull final EValidationBaseType eBaseType,
                                             @NonNull final Locale aDisplayLocale)
  {
    // XSD: every failure is a syntax violation. Schematron and others are business rules
    final EVerificationIssueType eType = eBaseType.isXSD () ? EVerificationIssueType.SYNTAX
                                                            : EVerificationIssueType.BUSINESS_RULE;
    final EVerificationIssueLevel eLevel = aError.getErrorLevel ().isError () ? EVerificationIssueLevel.ERROR
                                                                              : EVerificationIssueLevel.WARNING;

    final String sText = aError.getErrorText (aDisplayLocale);
    // VerificationIssue requires a non-empty description
    final String sDescription = StringHelper.isNotEmpty (sText) ? sText : "Validation failure";

    return new VerificationIssue (eLevel, eType, aError.getErrorID (), _location (aError), sDescription);
  }

  private static boolean _isSaxonTransformationWarning (@NonNull final IError aError)
  {
    return aError.getErrorLevel ().isEQ (EErrorLevel.WARN) &&
           "Transformation warning".equals (aError.getErrorText (CPhossAP.DEFAULT_LOCALE)) &&
           aError.getLinkedException () != null;
  }

  /**
   * Map a phive {@link ValidationResultList} to the contained verification issues. All WARN-and-
   * above entries are mapped, independent of the overall validity - so the warnings of a valid
   * document are preserved as well. Use {@link ValidationResultList#containsNoError()} to decide
   * whether the issues are warnings or the reason of a rejection.
   *
   * @param aResultList
   *        The phive validation result list to map. May not be <code>null</code>.
   * @param aDisplayLocale
   *        The locale used to render error texts. May be <code>null</code> in which case
   *        {@link CPhossAP#DEFAULT_LOCALE} is used.
   * @return Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <VerificationIssue> toVerificationIssues (@NonNull final ValidationResultList aResultList,
                                                                       @Nullable final Locale aDisplayLocale)
  {
    final Locale aEffectiveLocale = aDisplayLocale != null ? aDisplayLocale : CPhossAP.DEFAULT_LOCALE;
    final ICommonsList <VerificationIssue> ret = new CommonsArrayList <> ();
    for (final ValidationResult aResult : aResultList)
    {
      // Ignore skipped layer
      if (aResult.getValidity ().isSkipped ())
        continue;

      final EValidationBaseType eBaseType = aResult.getValidationArtefact ().getValidationType ().getBaseType ();
      for (final IError aError : aResult.getErrorList ())
      {
        // We only care about warning or higher
        if (aError.getErrorLevel ().isLT (EErrorLevel.WARN))
          continue;

        if (_isSaxonTransformationWarning (aError))
          continue;

        ret.add (_toIssue (aError, eBaseType, aEffectiveLocale));
      }
    }
    return ret;
  }
}
