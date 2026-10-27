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

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diagnostics.error.list.IErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.spi.APStartupCheckError;
import com.helger.phoss.ap.api.spi.IAPStartupCheckSPI;

/**
 * Runs the startup checks of one {@link EAPStartupCheckPhase} and aborts the start if one of them
 * reports an error.
 * <p>
 * All checks of the phase run even after the first error. An operator who has three things to fix
 * is better served by learning all three at once than by three restarts.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public final class APStartupCheckRunner
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APStartupCheckRunner.class);

  private APStartupCheckRunner ()
  {}

  /**
   * Run all checks of the provided phase and collect the findings.
   *
   * @param aChecks
   *        The checks to consider, in the order they should run. Checks of other phases are
   *        ignored. May not be <code>null</code>.
   * @param ePhase
   *        The phase to run. May not be <code>null</code>.
   * @return The findings of all checks. Never <code>null</code>. Empty means everything is in
   *         order.
   */
  @NonNull
  public static ErrorList performAllChecks (@NonNull final Iterable <? extends IAPStartupCheckSPI> aChecks,
                                            @NonNull final EAPStartupCheckPhase ePhase)
  {
    ValueEnforcer.notNull (aChecks, "Checks");
    ValueEnforcer.notNull (ePhase, "Phase");

    final ErrorList ret = new ErrorList ();
    for (final IAPStartupCheckSPI aCheck : aChecks)
      if (aCheck.getPhase () == ePhase)
      {
        final int nBefore = ret.size ();
        try
        {
          aCheck.performCheck (ret);
        }
        catch (final RuntimeException ex)
        {
          // A check that throws instead of reporting is a defect in the check, not a passed check.
          // Swallowing it would turn the defect into a green start
          ret.add (APStartupCheckError.create (aCheck.getCheckName (),
                                               "The check itself failed: " +
                                                                       ex.getClass ().getSimpleName () +
                                                                       ": " +
                                                                       ex.getMessage (),
                                               ex));
        }
        if (ret.size () == nBefore)
          LOGGER.info ("Startup check '" + aCheck.getCheckName () + "' passed");
      }
    return ret;
  }

  /**
   * Run all checks of the provided phase that are registered at the {@link APStartupCheckManager}
   * and collect the findings.
   *
   * @param ePhase
   *        The phase to run. May not be <code>null</code>.
   * @return The findings of all checks. Never <code>null</code>. Empty means everything is in
   *         order.
   */
  @NonNull
  public static ErrorList performAllChecks (@NonNull final EAPStartupCheckPhase ePhase)
  {
    return performAllChecks (APStartupCheckManager.getAllChecks (), ePhase);
  }

  /**
   * Run all checks of the provided phase and abort the start if one of them reports an error.
   * Warnings are logged but do not abort.
   *
   * @param aChecks
   *        The checks to consider, in the order they should run. Checks of other phases are
   *        ignored. May not be <code>null</code>.
   * @param ePhase
   *        The phase to run. May not be <code>null</code>.
   * @throws InitializationException
   *         if at least one check reported an error. The message lists all errors.
   */
  public static void performAllChecksOrThrow (@NonNull final Iterable <? extends IAPStartupCheckSPI> aChecks,
                                              @NonNull final EAPStartupCheckPhase ePhase)
  {
    LOGGER.info ("Running the " + ePhase.getID () + " startup checks");

    final ErrorList aFindings = performAllChecks (aChecks, ePhase);

    // A warning does not prevent the start, but it must not be invisible either
    for (final IError aFinding : aFindings)
      if (!aFinding.isError ())
        LOGGER.warn ("Startup check '" +
                     aFinding.getErrorFieldName () +
                     "': " +
                     aFinding.getErrorText (Locale.ROOT));

    if (aFindings.containsAtLeastOneError ())
    {
      final IErrorList aErrors = aFindings.getAllErrors ();
      final StringBuilder aSB = new StringBuilder ();
      for (final IError aError : aErrors)
        aSB.append ("\n  * ")
           .append (aError.getErrorFieldName ())
           .append (": ")
           .append (aError.getErrorText (Locale.ROOT));
      final String sMsg = "phoss AP does not start: " +
                          aErrors.size () +
                          " error(s) in the " +
                          ePhase.getID () +
                          " startup checks - fix the configuration" +
                          aSB;
      LOGGER.error (sMsg);
      throw new InitializationException (sMsg);
    }
    LOGGER.info ("All " + ePhase.getID () + " startup checks passed");
  }

  /**
   * Run all checks of the provided phase that are registered at the {@link APStartupCheckManager}
   * and abort the start if one of them reports an error. Warnings are logged but do not abort.
   *
   * @param ePhase
   *        The phase to run. May not be <code>null</code>.
   * @throws InitializationException
   *         if at least one check reported an error. The message lists all errors.
   */
  public static void performAllChecksOrThrow (@NonNull final EAPStartupCheckPhase ePhase)
  {
    performAllChecksOrThrow (APStartupCheckManager.getAllChecks (), ePhase);
  }
}
