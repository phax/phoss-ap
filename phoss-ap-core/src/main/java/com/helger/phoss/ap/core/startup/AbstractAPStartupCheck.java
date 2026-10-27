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

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.spi.APStartupCheckError;
import com.helger.phoss.ap.api.spi.IAPStartupCheckSPI;

/**
 * Abstract base class for the built-in startup checks, holding the name and the phase.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public abstract class AbstractAPStartupCheck implements IAPStartupCheckSPI
{
  private final String m_sCheckName;
  private final EAPStartupCheckPhase m_ePhase;

  protected AbstractAPStartupCheck (@NonNull @Nonempty final String sCheckName,
                                    @NonNull final EAPStartupCheckPhase ePhase)
  {
    ValueEnforcer.notEmpty (sCheckName, "CheckName");
    ValueEnforcer.notNull (ePhase, "Phase");
    m_sCheckName = sCheckName;
    m_ePhase = ePhase;
  }

  @NonNull
  @Nonempty
  public final String getCheckName ()
  {
    return m_sCheckName;
  }

  @NonNull
  public final EAPStartupCheckPhase getPhase ()
  {
    return m_ePhase;
  }

  /**
   * Add a finding at error level for this check.
   *
   * @param aErrors
   *        The error list to add to. May not be <code>null</code>.
   * @param sText
   *        What is wrong and how to fix it. May neither be <code>null</code> nor empty.
   */
  protected final void addError (@NonNull final ErrorList aErrors, @NonNull @Nonempty final String sText)
  {
    aErrors.add (APStartupCheckError.create (m_sCheckName, sText));
  }

  /**
   * Add a finding at warning level for this check.
   *
   * @param aErrors
   *        The error list to add to. May not be <code>null</code>.
   * @param sText
   *        What is noteworthy. May neither be <code>null</code> nor empty.
   */
  protected final void addWarning (@NonNull final ErrorList aErrors, @NonNull @Nonempty final String sText)
  {
    aErrors.add (APStartupCheckError.createWarning (m_sCheckName, sText));
  }
}
