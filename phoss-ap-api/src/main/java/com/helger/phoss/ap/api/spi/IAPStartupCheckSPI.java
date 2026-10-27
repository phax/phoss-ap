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

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIInterface;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;

/**
 * SPI interface for checks that are run when the phoss AP starts. All checks of a phase are run,
 * even after the first error, so that an operator learns about all problems at once. A finding at
 * error level prevents the start, a finding at warning level is only logged.
 * <p>
 * Use {@link APStartupCheckError} to create the findings, so that every finding names the check it
 * comes from. A check that throws an exception counts as a failed check, never as a passed one.
 * <p>
 * Implementations are loaded via {@link java.util.ServiceLoader}. Multiple checks may be registered.
 * They are run after the built-in checks of the same phase, ordered by {@link #getCheckName()}.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
@IsSPIInterface
public interface IAPStartupCheckSPI
{
  /**
   * @return A short name of the check for the log, e.g. <code>outbound-tls-truststore</code>. Never
   *         <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getCheckName ();

  /**
   * @return The startup phase in which this check runs. Never <code>null</code>.
   */
  @NonNull
  EAPStartupCheckPhase getPhase ();

  /**
   * Run the check.
   *
   * @param aErrors
   *        Receives the findings. Never <code>null</code>. Every entry at error level prevents the
   *        start; a warning does not.
   */
  void performCheck (@NonNull ErrorList aErrors);
}
