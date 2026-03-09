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

import com.helger.io.resource.FileSystemResource;
import com.helger.peppol.sbdh.PeppolSBDHDataReadException;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.ap.testsender.sender.DocumentSender;
import com.helger.phoss.ap.testsender.sender.SendResult;

/**
 * Scenario that sends a prebuilt Standard Business Document (with SBDH envelope).
 */
public class PrebuiltSbdScenario implements ITestScenario
{
  private final Path m_aSbdFile;
  private final String m_sSbdhID;

  public PrebuiltSbdScenario (@NonNull final Path aSbdFile)
  {
    m_aSbdFile = aSbdFile;
    try
    {
      m_sSbdhID = new PeppolSBDHDataReader (PeppolIdentifierFactory.INSTANCE).extractData (new FileSystemResource (aSbdFile.toFile ()))
                                                                             .getInstanceIdentifier ();
    }
    catch (final PeppolSBDHDataReadException ex)
    {
      throw new IllegalStateException ("Failed to parse SBD", ex);
    }
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
    try
    {
      return aSender.sendPrebuiltSbd (m_aSbdFile, m_sSbdhID);
    }
    catch (final Exception ex)
    {
      return SendResult.failure ("sbd", m_sSbdhID, 0, 0, null, ex.getMessage ());
    }
  }
}
