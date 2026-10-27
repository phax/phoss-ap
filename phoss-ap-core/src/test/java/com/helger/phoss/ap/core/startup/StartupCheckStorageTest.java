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

import java.io.File;
import java.util.Map;

import org.junit.Test;

import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for class {@link StartupCheckStorage}.
 *
 * @author Philip Helger
 */
public final class StartupCheckStorageTest
{
  private final StartupCheckStorage m_aCheck = new StartupCheckStorage ();

  @Test
  public void testFileSystem ()
  {
    // Default mode with the test configuration
    assertErrors (m_aCheck, 0, performCheck (m_aCheck, Map.of ()));
    // A path that does not exist yet is fine - it is created later
    assertErrors (m_aCheck,
                  0,
                  performCheck (m_aCheck,
                                Map.of (APConfigurationProperties.STORAGE_INBOUND_PATH,
                                        new File ("target/startup-check-not-yet-existing").getAbsolutePath ())));
    // An existing file is not a directory
    assertErrors (m_aCheck,
                  1,
                  performCheck (m_aCheck,
                                Map.of (APConfigurationProperties.STORAGE_INBOUND_PATH,
                                        new File ("pom.xml").getAbsolutePath ())));
  }

  @Test
  public void testInvalidMode ()
  {
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, Map.of (APConfigurationProperties.STORAGE_MODE, "foo")));
  }

  @Test
  public void testS3 ()
  {
    assertErrors (m_aCheck,
                  0,
                  performCheck (m_aCheck,
                                Map.of (APConfigurationProperties.STORAGE_MODE,
                                        "s3",
                                        APConfigurationProperties.STORAGE_S3_REGION,
                                        "eu-central-1",
                                        APConfigurationProperties.STORAGE_S3_BUCKET,
                                        "bucket")));
    assertErrors (m_aCheck, 2, performCheck (m_aCheck, Map.of (APConfigurationProperties.STORAGE_MODE, "s3")));
  }

  @Test
  public void testSpi ()
  {
    assertErrors (m_aCheck, 1, performCheck (m_aCheck, Map.of (APConfigurationProperties.STORAGE_MODE, "spi")));
    assertErrors (m_aCheck,
                  1,
                  performCheck (m_aCheck,
                                Map.of (APConfigurationProperties.STORAGE_MODE,
                                        "spi",
                                        APConfigurationProperties.STORAGE_SPI_ID,
                                        "does-not-exist")));
  }
}
