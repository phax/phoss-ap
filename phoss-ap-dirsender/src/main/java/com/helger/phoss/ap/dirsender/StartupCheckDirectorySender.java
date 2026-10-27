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
package com.helger.phoss.ap.dirsender;

import java.io.File;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.core.startup.AbstractAPStartupCheck;

/**
 * Check the configuration of the directory sender. Only evaluated if the directory sender is
 * enabled. Registered via SPI.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
@IsSPIImplementation
public class StartupCheckDirectorySender extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "dirsender-configuration";

  public StartupCheckDirectorySender ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    if (!APDirSenderConfig.isEnabled ())
      return;

    final String sDirectory = APDirSenderConfig.getDirectory ();
    if (sDirectory == null)
    {
      addError (aErrors,
                "Directory sender is enabled but '" +
                         APConfigurationProperties.DIRSENDER_DIRECTORY +
                         "' is not configured");
      return;
    }

    if (!new File (sDirectory).isDirectory ())
      addError (aErrors,
                "Directory sender watch directory '" + sDirectory + "' does not exist or is not a directory");
  }
}
