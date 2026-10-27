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

import static com.helger.phoss.ap.core.startup.StartupCheckTestHelper.assertErrors;
import static com.helger.phoss.ap.core.startup.StartupCheckTestHelper.performCheck;

import java.util.Map;

import org.junit.Test;

/**
 * Test class for class {@link StartupCheckDatabase}.
 *
 * @author Philip Helger
 */
public final class StartupCheckDatabaseTest
{
  private static final String KEY_DB_TYPE = "phossap.jdbc.database-type";

  private final StartupCheckDatabase m_aCheck = new StartupCheckDatabase ();

  @Test
  public void testValid ()
  {
    // The test configuration uses H2
    assertErrors (m_aCheck, 0, performCheck (m_aCheck, Map.of ()));
    assertErrors (m_aCheck, 0, performCheck (m_aCheck, Map.of (KEY_DB_TYPE, "postgresql")));
  }

  @Test
  public void testInvalid ()
  {
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, Map.of (KEY_DB_TYPE, "oracle-or-so")));
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, Map.of (KEY_DB_TYPE, "")));
  }
}
