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
 * Scenario that sends a plain XML document via the raw submission endpoint.
 */
public class XmlScenario implements ITestScenario
{
  private final Path m_aXmlFile;

  /**
   * Construct a new XML scenario.
   *
   * @param aXmlFile
   *        the path to the XML file to send. May not be {@code null}.
   */
  public XmlScenario (@NonNull final Path aXmlFile)
  {
    m_aXmlFile = aXmlFile;
  }

  /** {@inheritDoc} */
  @Override
  @NonNull
  public String getName ()
  {
    return "xml";
  }

  /** {@inheritDoc} */
  @Override
  @NonNull
  public SendResult execute (@NonNull final DocumentSender aSender, final int nIteration)
  {
    final String sSbdhInstanceID = PeppolSBDHData.createRandomSBDHInstanceIdentifier ();
    try
    {
      return aSender.sendXml (m_aXmlFile, sSbdhInstanceID);
    }
    catch (final Exception ex)
    {
      return SendResult.failure ("xml", sSbdhInstanceID, 0, 0, null, ex.getMessage ());
    }
  }
}
