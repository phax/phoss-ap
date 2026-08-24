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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Duration;

import com.helger.base.location.SimpleLocation;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.resource.ClassPathResource;
import com.helger.peppol.mls.CPeppolMLS;
import com.helger.peppol.mls.EPeppolMLSStatusReasonCode;
import com.helger.phive.api.EValidationType;
import com.helger.phive.api.IValidationType;
import com.helger.phive.api.artefact.IValidationArtefact;
import com.helger.phive.api.artefact.ValidationArtefact;
import com.helger.phive.api.result.ValidationResult;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.EExtendedValidity;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.codelist.EVerificationIssueLevel;
import com.helger.phoss.ap.api.codelist.EVerificationIssueType;
import com.helger.phoss.ap.api.model.MlsOutcomeIssue;
import com.helger.phoss.ap.api.model.VerificationIssue;

import org.junit.Test;

/**
 * Test class for class {@link PhiveToVerificationMapper}.
 *
 * @author Philip Helger
 */
public final class PhiveToVerificationMapperTest
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
  public void testNoErrorsYieldsNoIssues ()
  {
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.XSD, EExtendedValidity.VALID));
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.VALID));

    assertTrue (PhiveToVerificationMapper.toVerificationIssues (aList, CPhossAP.DEFAULT_LOCALE).isEmpty ());
  }

  @Test
  public void testWarningOnValidDocumentIsPreserved ()
  {
    // The MLS-based predecessor dropped every warning as soon as the document was valid, because an
    // MLS acceptance cannot carry line issues. The neutral model has no such limit
    final IError aWarn = SingleError.builderWarn ()
                                    .errorID ("BR-W-01")
                                    .errorFieldName ("/Invoice/Note")
                                    .errorText ("Soft warning")
                                    .build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.VALID, aWarn));

    final ICommonsList <VerificationIssue> aIssues = PhiveToVerificationMapper.toVerificationIssues (aList,
                                                                                                     CPhossAP.DEFAULT_LOCALE);
    assertEquals (1, aIssues.size ());
    final VerificationIssue aIssue = aIssues.getFirstOrNull ();
    assertSame (EVerificationIssueLevel.WARNING, aIssue.getLevel ());
    assertSame (EVerificationIssueType.BUSINESS_RULE, aIssue.getType ());
    assertEquals ("BR-W-01", aIssue.getCode ());
  }

  @Test
  public void testXsdErrorMapsToSyntax ()
  {
    final IError aErr = SingleError.builderError ()
                                   .errorID ("XSD-001")
                                   .errorLocation (new SimpleLocation ("invoice.xml", 42, 5))
                                   .errorText ("Element 'cbc:Foo' not allowed here")
                                   .build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.XSD, EExtendedValidity.INVALID, aErr));

    final ICommonsList <VerificationIssue> aIssues = PhiveToVerificationMapper.toVerificationIssues (aList,
                                                                                                     CPhossAP.DEFAULT_LOCALE);
    assertEquals (1, aIssues.size ());
    final VerificationIssue aIssue = aIssues.getFirstOrNull ();
    assertSame (EVerificationIssueType.SYNTAX, aIssue.getType ());
    assertSame (EVerificationIssueLevel.ERROR, aIssue.getLevel ());
    assertEquals ("invoice.xml(42:5)", aIssue.getLocation ());
    // The rule ID is a field of its own - not mashed into the description
    assertEquals ("XSD-001", aIssue.getCode ());
    assertEquals ("Element 'cbc:Foo' not allowed here", aIssue.getDescription ());
  }

  @Test
  public void testSchematronErrorMapsToBusinessRule ()
  {
    final IError aErr = SingleError.builderError ()
                                   .errorID ("BR-CO-15")
                                   .errorFieldName ("/Invoice/cbc:ID")
                                   .errorText ("Sum mismatch")
                                   .build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.INVALID, aErr));

    final VerificationIssue aIssue = PhiveToVerificationMapper.toVerificationIssues (aList, CPhossAP.DEFAULT_LOCALE)
                                                              .getFirstOrNull ();
    assertSame (EVerificationIssueType.BUSINESS_RULE, aIssue.getType ());
    assertSame (EVerificationIssueLevel.ERROR, aIssue.getLevel ());
    assertEquals ("/Invoice/cbc:ID", aIssue.getLocation ());
    assertEquals ("BR-CO-15", aIssue.getCode ());
    assertEquals ("Sum mismatch", aIssue.getDescription ());
  }

  @Test
  public void testWarningAndErrorKeepTheirLevels ()
  {
    final IError aWarn = SingleError.builderWarn ()
                                    .errorFieldName ("/Invoice/cbc:Note")
                                    .errorText ("Soft warning")
                                    .build ();
    final IError aErr = SingleError.builderError ()
                                   .errorFieldName ("/Invoice/cbc:ID")
                                   .errorText ("Fatal rule")
                                   .build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.INVALID, aWarn, aErr));

    final ICommonsList <VerificationIssue> aIssues = PhiveToVerificationMapper.toVerificationIssues (aList,
                                                                                                     CPhossAP.DEFAULT_LOCALE);
    assertEquals (2, aIssues.size ());
    // Unlike the MLS grouping by error field, the neutral list keeps the phive order
    assertSame (EVerificationIssueLevel.WARNING, aIssues.get (0).getLevel ());
    assertEquals ("/Invoice/cbc:Note", aIssues.get (0).getLocation ());
    assertSame (EVerificationIssueLevel.ERROR, aIssues.get (1).getLevel ());
    assertEquals ("/Invoice/cbc:ID", aIssues.get (1).getLocation ());
  }

  @Test
  public void testUnknownLocationIsNull ()
  {
    // No field name, no location. The MLS "NA" sentinel is only added when mapping to MLS
    final IError aErr = SingleError.builderError ().errorText ("Bad thing happened").build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.INVALID, aErr));

    final VerificationIssue aIssue = PhiveToVerificationMapper.toVerificationIssues (aList, CPhossAP.DEFAULT_LOCALE)
                                                              .getFirstOrNull ();
    assertNull (aIssue.getLocation ());
    assertNull (aIssue.getCode ());
    assertEquals ("Bad thing happened", aIssue.getDescription ());

    // ... and the MLS projection re-adds it
    assertEquals (CPeppolMLS.LINE_ID_NOT_AVAILABLE, MlsOutcomeIssue.fromVerificationIssue (aIssue).getErrorField ());
  }

  @Test
  public void testSkippedResultsAreIgnored ()
  {
    final IError aErr = SingleError.builderError ().errorFieldName ("/x").errorText ("nope").build ();
    final ValidationResultList aList = new ValidationResultList (null);
    // A skipped result that nominally carries an error must not contribute issues
    aList.add (ValidationResult.createSkippedResult (_artefact (EValidationType.SCHEMATRON_XSLT2)));
    aList.add (_result (EValidationType.XSD, EExtendedValidity.INVALID, aErr));

    final ICommonsList <VerificationIssue> aIssues = PhiveToVerificationMapper.toVerificationIssues (aList,
                                                                                                     CPhossAP.DEFAULT_LOCALE);
    assertEquals (1, aIssues.size ());
    assertSame (EVerificationIssueType.SYNTAX, aIssues.getFirstOrNull ().getType ());
  }

  @Test
  public void testNullLocaleUsesDefault ()
  {
    final IError aErr = SingleError.builderError ().errorFieldName ("/x").errorText ("hello").build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.INVALID, aErr));

    assertEquals ("hello",
                  PhiveToVerificationMapper.toVerificationIssues (aList, null).getFirstOrNull ().getDescription ());
  }

  @Test
  public void testMlsProjectionKeepsPreviousWireFormat ()
  {
    // The MLS output must be unchanged by the introduction of the neutral issue model: the rule ID
    // is prefixed back into the description and the severity decides the status reason code
    final IError aErr = SingleError.builderError ()
                                   .errorID ("BR-CO-15")
                                   .errorFieldName ("/Invoice/cbc:ID")
                                   .errorText ("Sum mismatch")
                                   .build ();
    final ValidationResultList aList = new ValidationResultList (null);
    aList.add (_result (EValidationType.SCHEMATRON_XSLT2, EExtendedValidity.INVALID, aErr));

    final VerificationIssue aIssue = PhiveToVerificationMapper.toVerificationIssues (aList, CPhossAP.DEFAULT_LOCALE)
                                                              .getFirstOrNull ();
    final MlsOutcomeIssue aMlsIssue = MlsOutcomeIssue.fromVerificationIssue (aIssue);
    assertSame (EPeppolMLSStatusReasonCode.BUSINESS_RULE_VIOLATION_FATAL, aMlsIssue.getStatusReasonCode ());
    assertEquals ("/Invoice/cbc:ID", aMlsIssue.getErrorField ());
    assertEquals ("[BR-CO-15] Sum mismatch", aMlsIssue.getDescription ());
  }
}
