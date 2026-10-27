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
package com.helger.phoss.ap.api.spi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.SingleError;

/**
 * Builds the findings of an {@link IAPStartupCheckSPI}. One place, so that every check names itself
 * the same way and a log line is readable without knowing which check wrote it.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
@Immutable
public final class APStartupCheckError
{
  /** The error ID every finding of a startup check carries. */
  public static final String ERROR_ID = "startup-check-failed";

  private APStartupCheckError ()
  {}

  /**
   * @param sCheckName
   *        The name of the check. May neither be <code>null</code> nor empty.
   * @param sText
   *        What is wrong and how to fix it. May neither be <code>null</code> nor empty.
   * @return The finding at error level - it prevents the start. Never <code>null</code>.
   */
  @NonNull
  public static IError create (@NonNull @Nonempty final String sCheckName, @NonNull @Nonempty final String sText)
  {
    return create (sCheckName, sText, null);
  }

  /**
   * @param sCheckName
   *        The name of the check. May neither be <code>null</code> nor empty.
   * @param sText
   *        What is wrong and how to fix it. May neither be <code>null</code> nor empty.
   * @param aCause
   *        The exception that caused the finding. May be <code>null</code>.
   * @return The finding at error level - it prevents the start. Never <code>null</code>.
   */
  @NonNull
  public static IError create (@NonNull @Nonempty final String sCheckName,
                               @NonNull @Nonempty final String sText,
                               @Nullable final Throwable aCause)
  {
    ValueEnforcer.notEmpty (sCheckName, "CheckName");
    ValueEnforcer.notEmpty (sText, "Text");
    return SingleError.builderError ()
                      .errorFieldName (sCheckName)
                      .errorID (ERROR_ID)
                      .errorText (sText)
                      .linkedException (aCause)
                      .build ();
  }

  /**
   * @param sCheckName
   *        The name of the check. May neither be <code>null</code> nor empty.
   * @param sText
   *        What is noteworthy. May neither be <code>null</code> nor empty.
   * @return The finding at warning level - it does not prevent the start. Never <code>null</code>.
   */
  @NonNull
  public static IError createWarning (@NonNull @Nonempty final String sCheckName,
                                      @NonNull @Nonempty final String sText)
  {
    ValueEnforcer.notEmpty (sCheckName, "CheckName");
    ValueEnforcer.notEmpty (sText, "Text");
    return SingleError.builderWarn ().errorFieldName (sCheckName).errorID (ERROR_ID).errorText (sText).build ();
  }
}
