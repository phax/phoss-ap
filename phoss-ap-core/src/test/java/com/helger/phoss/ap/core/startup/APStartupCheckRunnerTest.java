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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Locale;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.exception.InitializationException;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.spi.APStartupCheckError;
import com.helger.phoss.ap.api.spi.IAPStartupCheckSPI;

/**
 * Test class for class {@link APStartupCheckRunner}.
 *
 * @author Philip Helger
 */
public final class APStartupCheckRunnerTest
{
  private static final class MockCheck extends AbstractAPStartupCheck
  {
    private final Consumer <ErrorList> m_aAction;

    MockCheck (@NonNull final String sCheckName,
               @NonNull final EAPStartupCheckPhase ePhase,
               @NonNull final Consumer <ErrorList> aAction)
    {
      super (sCheckName, ePhase);
      m_aAction = aAction;
    }

    public void performCheck (@NonNull final ErrorList aErrors)
    {
      m_aAction.accept (aErrors);
    }
  }

  @NonNull
  private static IAPStartupCheckSPI _passing (@NonNull final String sName)
  {
    return new MockCheck (sName, EAPStartupCheckPhase.PRE_INIT, x -> {});
  }

  @NonNull
  private static IAPStartupCheckSPI _failing (@NonNull final String sName, @NonNull final EAPStartupCheckPhase ePhase)
  {
    return new MockCheck (sName, ePhase, x -> x.add (APStartupCheckError.create (sName, "Failure of " + sName)));
  }

  @NonNull
  private static IAPStartupCheckSPI _warning (@NonNull final String sName)
  {
    return new MockCheck (sName,
                          EAPStartupCheckPhase.PRE_INIT,
                          x -> x.add (APStartupCheckError.createWarning (sName, "Warning of " + sName)));
  }

  @Test
  public void testNoChecks ()
  {
    final ICommonsList <IAPStartupCheckSPI> aChecks = new CommonsArrayList <> ();
    for (final EAPStartupCheckPhase ePhase : EAPStartupCheckPhase.values ())
    {
      assertTrue (APStartupCheckRunner.performAllChecks (aChecks, ePhase).isEmpty ());
      APStartupCheckRunner.performAllChecksOrThrow (aChecks, ePhase);
    }
  }

  @Test
  public void testAllFindingsAreReported ()
  {
    final ICommonsList <IAPStartupCheckSPI> aChecks = new CommonsArrayList <> (_failing ("first",
                                                                                         EAPStartupCheckPhase.PRE_INIT),
                                                                               _passing ("ok"),
                                                                               _warning ("warn"),
                                                                               _failing ("second",
                                                                                         EAPStartupCheckPhase.PRE_INIT));
    final ErrorList aFindings = APStartupCheckRunner.performAllChecks (aChecks, EAPStartupCheckPhase.PRE_INIT);
    assertEquals (3, aFindings.size ());
    assertEquals (2, aFindings.getAllErrors ().size ());
    // Order of the checks is kept
    assertEquals ("first", aFindings.get (0).getErrorFieldName ());
    assertEquals ("warn", aFindings.get (1).getErrorFieldName ());
    assertEquals ("second", aFindings.get (2).getErrorFieldName ());

    try
    {
      APStartupCheckRunner.performAllChecksOrThrow (aChecks, EAPStartupCheckPhase.PRE_INIT);
      fail ();
    }
    catch (final InitializationException ex)
    {
      // One exception with all errors, but without the warning
      assertTrue (ex.getMessage (), ex.getMessage ().contains ("2 error(s)"));
      assertTrue (ex.getMessage (), ex.getMessage ().contains ("first: Failure of first"));
      assertTrue (ex.getMessage (), ex.getMessage ().contains ("second: Failure of second"));
      assertFalse (ex.getMessage (), ex.getMessage ().contains ("Warning of warn"));
    }
  }

  @Test
  public void testWarningOnlyDoesNotThrow ()
  {
    final ICommonsList <IAPStartupCheckSPI> aChecks = new CommonsArrayList <> (_warning ("warn"), _passing ("ok"));
    final ErrorList aFindings = APStartupCheckRunner.performAllChecks (aChecks, EAPStartupCheckPhase.PRE_INIT);
    assertEquals (1, aFindings.size ());
    assertFalse (aFindings.containsAtLeastOneError ());
    APStartupCheckRunner.performAllChecksOrThrow (aChecks, EAPStartupCheckPhase.PRE_INIT);
  }

  @Test
  public void testThrowingCheckIsAFailedCheck ()
  {
    final IllegalStateException aEx = new IllegalStateException ("boom");
    final ICommonsList <IAPStartupCheckSPI> aChecks = new CommonsArrayList <> (new MockCheck ("thrower",
                                                                                              EAPStartupCheckPhase.PRE_INIT,
                                                                                              x -> {
                                                                                                throw aEx;
                                                                                              }),
                                                                               _failing ("after",
                                                                                         EAPStartupCheckPhase.PRE_INIT));
    final ErrorList aFindings = APStartupCheckRunner.performAllChecks (aChecks, EAPStartupCheckPhase.PRE_INIT);
    // The checks after the throwing one still run
    assertEquals (2, aFindings.size ());

    final IError aFinding = aFindings.get (0);
    assertTrue (aFinding.isError ());
    assertEquals ("thrower", aFinding.getErrorFieldName ());
    assertEquals ("The check itself failed: IllegalStateException: boom", aFinding.getErrorText (Locale.ROOT));
    assertSame (aEx, aFinding.getLinkedException ());
  }

  @Test
  public void testPhasesAreIndependent ()
  {
    final ICommonsList <IAPStartupCheckSPI> aChecks = new CommonsArrayList <> (_failing ("pre",
                                                                                         EAPStartupCheckPhase.PRE_INIT),
                                                                               _failing ("post",
                                                                                         EAPStartupCheckPhase.POST_INIT));
    final ErrorList aPre = APStartupCheckRunner.performAllChecks (aChecks, EAPStartupCheckPhase.PRE_INIT);
    assertEquals (1, aPre.size ());
    assertEquals ("pre", aPre.get (0).getErrorFieldName ());

    final ErrorList aPost = APStartupCheckRunner.performAllChecks (aChecks, EAPStartupCheckPhase.POST_INIT);
    assertEquals (1, aPost.size ());
    assertEquals ("post", aPost.get (0).getErrorFieldName ());

    // Only POST_INIT checks pass - PRE_INIT must still be fine
    final ICommonsList <IAPStartupCheckSPI> aOnlyPost = new CommonsArrayList <> (_failing ("post",
                                                                                           EAPStartupCheckPhase.POST_INIT));
    APStartupCheckRunner.performAllChecksOrThrow (aOnlyPost, EAPStartupCheckPhase.PRE_INIT);
    try
    {
      APStartupCheckRunner.performAllChecksOrThrow (aOnlyPost, EAPStartupCheckPhase.POST_INIT);
      fail ();
    }
    catch (final InitializationException ex)
    {
      assertNotNull (ex.getMessage ());
    }
  }
}
