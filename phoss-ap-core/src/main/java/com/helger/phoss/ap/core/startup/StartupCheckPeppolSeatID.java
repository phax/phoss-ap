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

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.core.APCoreConfig;

/**
 * Check that the configured Peppol Seat ID matches the syntactical requirements.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckPeppolSeatID extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "peppol-seat-id";

  public StartupCheckPeppolSeatID ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final String sSeatID = APCoreConfig.getPeppolOwnerSeatID ();
    if (!CPhossAP.isPeppolSeatID (sSeatID))
      addError (aErrors, "The configured Peppol Seat ID '" + sSeatID + "' does not match the syntactical requirements.");
  }
}
