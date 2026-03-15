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
package com.helger.phoss.ap.testsender.scenario;

import java.nio.file.Path;

import org.jspecify.annotations.NonNull;

import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.phoss.ap.testsender.sender.DocumentSender;
import com.helger.phoss.ap.testsender.sender.SendResult;

/**
 * Scenario that sends a prebuilt Standard Business Document (with SBDH
 * envelope).
 */
public class PrebuiltSbdScenario implements ITestScenario
{
  private final Path m_aSbdFile;

  public PrebuiltSbdScenario (@NonNull final Path aSbdFile)
  {
    m_aSbdFile = aSbdFile;
  }

  @Override
  @NonNull
  public String getName ()
  {
    return "sbd";
  }

  @Override
  @NonNull
  public SendResult execute (@NonNull final DocumentSender aSender, final int nIteration)
  {
    final String sSbdhInstanceID = PeppolSBDHData.createRandomSBDHInstanceIdentifier ();
    try
    {
      return aSender.sendPrebuiltSbd (m_aSbdFile, sSbdhInstanceID);
    }
    catch (final Exception ex)
    {
      return SendResult.failure ("sbd", sSbdhInstanceID, 0, 0, null, ex.getMessage ());
    }
  }
}
