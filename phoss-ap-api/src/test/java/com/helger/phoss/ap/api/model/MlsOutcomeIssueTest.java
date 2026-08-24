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
package com.helger.phoss.ap.api.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.helger.peppol.mls.CPeppolMLS;
import com.helger.peppol.mls.EPeppolMLSStatusReasonCode;

/**
 * Test class for class {@link MlsOutcomeIssue}.
 *
 * @author Philip Helger
 */
public final class MlsOutcomeIssueTest
{
  @Test
  public void testBusinessRuleViolation ()
  {
    final MlsOutcomeIssue a = MlsOutcomeIssue.businessRuleViolation ("field1", "desc1");
    assertEquals ("field1", a.getErrorField ());
    assertSame (EPeppolMLSStatusReasonCode.BUSINESS_RULE_VIOLATION_FATAL, a.getStatusReasonCode ());
    assertEquals ("desc1", a.getDescription ());
  }

  @Test
  public void testBusinessRuleWarning ()
  {
    final MlsOutcomeIssue a = MlsOutcomeIssue.businessRuleWarning ("field2", "desc2");
    assertEquals ("field2", a.getErrorField ());
    assertSame (EPeppolMLSStatusReasonCode.BUSINESS_RULE_VIOLATION_WARNING, a.getStatusReasonCode ());
    assertEquals ("desc2", a.getDescription ());
  }

  @Test
  public void testFailureOfDelivery ()
  {
    final MlsOutcomeIssue a = MlsOutcomeIssue.failureOfDelivery ("desc3");
    assertEquals (CPeppolMLS.LINE_ID_NOT_AVAILABLE, a.getErrorField ());
    assertSame (EPeppolMLSStatusReasonCode.FAILURE_OF_DELIVERY, a.getStatusReasonCode ());
    assertEquals ("desc3", a.getDescription ());
  }

  @Test
  public void testSyntaxViolation ()
  {
    final MlsOutcomeIssue a = MlsOutcomeIssue.syntaxViolation ("field4", "desc4");
    assertEquals ("field4", a.getErrorField ());
    assertSame (EPeppolMLSStatusReasonCode.SYNTAX_VIOLATION, a.getStatusReasonCode ());
    assertEquals ("desc4", a.getDescription ());
  }

  @Test
  public void testOfNA ()
  {
    final MlsOutcomeIssue a = MlsOutcomeIssue.ofNA (EPeppolMLSStatusReasonCode.FAILURE_OF_DELIVERY, "desc5");
    assertEquals (CPeppolMLS.LINE_ID_NOT_AVAILABLE, a.getErrorField ());
    assertSame (EPeppolMLSStatusReasonCode.FAILURE_OF_DELIVERY, a.getStatusReasonCode ());
    assertEquals ("desc5", a.getDescription ());
  }
}
