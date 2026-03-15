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
package com.helger.phoss.ap.testsender.runner;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.helger.phoss.ap.testsender.config.TestSenderConfig;
import com.helger.phoss.ap.testsender.scenario.ITestScenario;
import com.helger.phoss.ap.testsender.sender.DocumentSender;
import com.helger.phoss.ap.testsender.sender.SendResult;

/**
 * Runs a single document scenario and optionally polls for the final
 * transaction status.
 */
@Component
public class SingleDocumentRunner
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SingleDocumentRunner.class);

  private final DocumentSender m_aSender;
  private final TestSenderConfig m_aConfig;

  public SingleDocumentRunner (@NonNull final DocumentSender aSender, @NonNull final TestSenderConfig aConfig)
  {
    m_aSender = aSender;
    m_aConfig = aConfig;
  }

  @NonNull
  public SendResult run (@NonNull final ITestScenario aScenario)
  {
    LOGGER.info ("Sending single '" + aScenario.getName () + "' document...");

    // Main transmission
    final SendResult aResult = aScenario.execute (m_aSender, 0);

    if (aResult.isSuccess ())
    {
      LOGGER.info ("Send succeeded in " + aResult.getDurationMs () + " ms (HTTP " + aResult.getHttpStatus () + ")");

      if (m_aConfig.getOutput ().isVerbose () && aResult.getResponseBody () != null)
      {
        LOGGER.info ("Response:\n" + aResult.getResponseBody ());
      }

      // Poll for final status if enabled
      if (m_aConfig.getPoll ().isEnabled ())
      {
        LOGGER.info ("Polling status for '" + aResult.getSbdhInstanceID () + "'...");
        final String sStatus = m_aSender.pollStatus (aResult.getSbdhInstanceID (),
                                                     m_aConfig.getPoll ().getTimeoutMs (),
                                                     m_aConfig.getPoll ().getIntervalMs ());
        if (sStatus != null)
          LOGGER.info ("Final status: " + sStatus);
        else
          LOGGER.warn ("Status polling timed out for '" + aResult.getSbdhInstanceID () + "'");
      }
    }
    else
    {
      LOGGER.error ("Send failed in " + aResult.getDurationMs () + " ms: " + aResult.getErrorMessage ());

      if (aResult.getResponseBody () != null)
        LOGGER.error ("Response body:\n" + aResult.getResponseBody ());
    }

    return aResult;
  }
}
