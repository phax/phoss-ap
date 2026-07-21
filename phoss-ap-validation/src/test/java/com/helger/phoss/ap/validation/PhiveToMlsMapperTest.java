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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.Test;

import com.helger.base.location.SimpleLocation;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.resource.ClassPathResource;
import com.helger.peppol.mls.CPeppolMLS;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.mls.EPeppolMLSStatusReasonCode;
import com.helger.phive.api.EValidationType;
import com.helger.phive.api.IValidationType;
import com.helger.phive.api.artefact.IValidationArtefact;
import com.helger.phive.api.artefact.ValidationArtefact;
import com.helger.phive.api.result.ValidationResult;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.EExtendedValidity;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.model.MlsOutcomeIssue;

/**
 * Test class for class {@link PhiveToMlsMapper}.
 *
 * @author Philip Helger
 */
public final class PhiveToMlsMapperTest
{
  private static IValidationArtefact _artefact (final IValidationType aType)
  {
    return new ValidationArtefact (aType, new ClassPathResource ("dummy.xml"));
  }

  private static ValidationResult _result (final IValidationType aType,
                                           final EExtendedValidity eValidity,
                                           final IError... aErrors)
  {
    return new ValidationResult (_artefact (aType), new ErrorList (aErrors), eValidity, Duration.ZERO);
  }

  @Test
  public void testNoErrorsYieldsAcceptance ()
  {
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.XSD, EExtendedValidity.VALID));
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.VALID));

    final MlsOutcome aOutcome = PhiveToMlsMapper.toMlsOutcome (aList, CPhossAP.DEFAULT_LOCALE, "ignored");
    assertSame (EPeppolMLSResponseCode.ACCEPTANCE, aOutcome.getResponseCode ());
    assertFalse (aOutcome.hasIssues ());
  }

  @Test
  public void testWarningOnlyYieldsAcceptance ()
  {
    // Warnings without errors must not produce a rejection
    final IError aWarn = SingleError.builderWarn ()
                                    .errorFieldName ("/Invoice/Note")
                                    .errorText ("Soft warning")
                                    .build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.VALID, aWarn));

    final MlsOutcome aOutcome = PhiveToMlsMapper.toMlsOutcome (aList, CPhossAP.DEFAULT_LOCALE, null);
    assertSame (EPeppolMLSResponseCode.ACCEPTANCE, aOutcome.getResponseCode ());
    assertFalse (aOutcome.hasIssues ());
  }

  @Test
  public void testXsdErrorMapsToSyntaxViolation ()
  {
    final IError aErr = SingleError.builderError ()
                                   .errorID ("XSD-001")
                                   .errorLocation (new SimpleLocation ("invoice.xml", 42, 5))
                                   .errorText ("Element 'cbc:Foo' not allowed here")
                                   .build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.XSD, EExtendedValidity.INVALID, aErr));

    final MlsOutcome aOutcome = PhiveToMlsMapper.toMlsOutcome (aList, CPhossAP.DEFAULT_LOCALE, "schema fail");
    assertSame (EPeppolMLSResponseCode.REJECTION, aOutcome.getResponseCode ());
    assertEquals ("schema fail", aOutcome.getResponseText ());
    final List <MlsOutcomeIssue> aIssues = aOutcome.getIssues ();
    assertEquals (1, aIssues.size ());
    final MlsOutcomeIssue aIssue = aIssues.get (0);
    assertSame (EPeppolMLSStatusReasonCode.SYNTAX_VIOLATION, aIssue.getStatusReasonCode ());
    assertEquals ("invoice.xml(42:5)", aIssue.getErrorField ());
    assertEquals ("[XSD-001] Element 'cbc:Foo' not allowed here", aIssue.getDescription ());
  }

  @Test
  public void testSchematronErrorMapsToBusinessRuleFatal ()
  {
    final IError aErr = SingleError.builderError ()
                                   .errorID ("BR-CO-15")
                                   .errorFieldName ("/Invoice/cbc:ID")
                                   .errorText ("Sum mismatch")
                                   .build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.INVALID, aErr));

    final MlsOutcome aOutcome = PhiveToMlsMapper.toMlsOutcome (aList, CPhossAP.DEFAULT_LOCALE, null);
    assertSame (EPeppolMLSResponseCode.REJECTION, aOutcome.getResponseCode ());
    final List <MlsOutcomeIssue> aIssues = aOutcome.getIssues ();
    assertEquals (1, aIssues.size ());
    final MlsOutcomeIssue aIssue = aIssues.get (0);
    assertSame (EPeppolMLSStatusReasonCode.BUSINESS_RULE_VIOLATION_FATAL, aIssue.getStatusReasonCode ());
    assertEquals ("/Invoice/cbc:ID", aIssue.getErrorField ());
    assertEquals ("[BR-CO-15] Sum mismatch", aIssue.getDescription ());
  }

  @Test
  public void testSchematronWarningAlongsideErrorBecomesBW ()
  {
    final IError aWarn = SingleError.builderWarn ()
                                    .errorID ("BR-W-01")
                                    .errorFieldName ("/Invoice/cbc:Note")
                                    .errorText ("Soft warning")
                                    .build ();
    final IError aErr = SingleError.builderError ()
                                   .errorFieldName ("/Invoice/cbc:ID")
                                   .errorText ("Fatal rule")
                                   .build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.INVALID, aWarn, aErr));

    final MlsOutcome aOutcome = PhiveToMlsMapper.toMlsOutcome (aList, CPhossAP.DEFAULT_LOCALE, null);
    assertSame (EPeppolMLSResponseCode.REJECTION, aOutcome.getResponseCode ());
    final List <MlsOutcomeIssue> aIssues = aOutcome.getIssues ();
    assertEquals (2, aIssues.size ());
    // Issues are grouped by errorField inside MlsOutcome (LinkedHashMap by first-seen field)
    // and getIssues returns them in that group order. Warning's field came first.
    assertSame (EPeppolMLSStatusReasonCode.BUSINESS_RULE_VIOLATION_WARNING, aIssues.get (0).getStatusReasonCode ());
    assertEquals ("/Invoice/cbc:Note", aIssues.get (0).getErrorField ());
    assertSame (EPeppolMLSStatusReasonCode.BUSINESS_RULE_VIOLATION_FATAL, aIssues.get (1).getStatusReasonCode ());
    assertEquals ("/Invoice/cbc:ID", aIssues.get (1).getErrorField ());
  }

  @Test
  public void testErrorFieldFallbackToNA ()
  {
    // No field name, no location -> NA
    final IError aErr = SingleError.builderError ().errorText ("Bad thing happened").build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.INVALID, aErr));

    final MlsOutcome aOutcome = PhiveToMlsMapper.toMlsOutcome (aList, CPhossAP.DEFAULT_LOCALE, null);
    final List <MlsOutcomeIssue> aIssues = aOutcome.getIssues ();
    assertEquals (1, aIssues.size ());
    assertEquals (CPeppolMLS.LINE_ID_NOT_AVAILABLE, aIssues.get (0).getErrorField ());
    assertEquals ("Bad thing happened", aIssues.get (0).getDescription ());
  }

  @Test
  public void testSkippedResultsAreIgnored ()
  {
    final IError aErr = SingleError.builderError ().errorFieldName ("/x").errorText ("nope").build ();
    final ValidationResultList aList = new ValidationResultList (null);
    // A skipped result that nominally carries an error must not contribute issues
    aList.add (ValidationResult.createSkippedResult (_artefact (EValidationType.SCHEMATRON_XSLT2)));
    aList.add (_result (EValidationType.XSD, EExtendedValidity.INVALID, aErr));

    final MlsOutcome aOutcome = PhiveToMlsMapper.toMlsOutcome (aList, CPhossAP.DEFAULT_LOCALE, null);
    assertTrue (aOutcome.hasIssues ());
    assertEquals (1, aOutcome.getIssues ().size ());
    assertSame (EPeppolMLSStatusReasonCode.SYNTAX_VIOLATION, aOutcome.getIssues ().get (0).getStatusReasonCode ());
  }

  @Test
  public void testNullLocaleUsesDefault ()
  {
    final IError aErr = SingleError.builderError ().errorFieldName ("/x").errorText ("hello").build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.INVALID, aErr));

    final MlsOutcome aOutcome = PhiveToMlsMapper.toMlsOutcome (aList, null, null);
    assertEquals ("hello", aOutcome.getIssues ().get (0).getDescription ());
  }
}
