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
package com.helger.phoss.ap.extension.demo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.spi.APStartupCheckError;
import com.helger.phoss.ap.api.spi.IAPStartupCheckSPI;

/**
 * Demo implementation of {@link IAPStartupCheckSPI}. It runs in the configuration-only phase and
 * emits a warning if the default charset of the JVM is not UTF-8. A warning is logged but does not
 * prevent the start; use {@link APStartupCheckError#create(String, String)} for a finding that
 * should.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class DemoStartupCheckSPI implements IAPStartupCheckSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (DemoStartupCheckSPI.class);
  private static final String CHECK_NAME = "extension-demo";

  public DemoStartupCheckSPI ()
  {
    LOGGER.info ("[extension-demo] " + DemoStartupCheckSPI.class.getSimpleName () + " was loaded via SPI");
  }

  @NonNull
  @Nonempty
  public String getCheckName ()
  {
    return CHECK_NAME;
  }

  @NonNull
  public EAPStartupCheckPhase getPhase ()
  {
    return EAPStartupCheckPhase.PRE_INIT;
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    final Charset aDefaultCharset = Charset.defaultCharset ();
    if (!StandardCharsets.UTF_8.equals (aDefaultCharset))
      aErrors.add (APStartupCheckError.createWarning (CHECK_NAME,
                                                      "The default charset of the JVM is '" +
                                                                  aDefaultCharset.name () +
                                                                  "' and not UTF-8"));
  }
}
