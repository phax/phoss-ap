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

import org.jspecify.annotations.NonNull;

import com.helger.phoss.ap.testsender.sender.DocumentSender;
import com.helger.phoss.ap.testsender.sender.SendResult;

/**
 * A test scenario that sends a document of a specific type.
 */
public interface ITestScenario
{
  /**
   * @return The human-readable name of this scenario (e.g. "xml", "sbd", "pdf").
   */
  @NonNull
  String getName ();

  /**
   * Execute the scenario for a given iteration.
   *
   * @param aSender
   *        The document sender to use. May not be {@code null}.
   * @param nIteration
   *        The iteration number, used to generate unique SBDH instance IDs.
   * @return The send result. Never {@code null}.
   */
  @NonNull
  SendResult execute (@NonNull DocumentSender aSender, int nIteration);
}
