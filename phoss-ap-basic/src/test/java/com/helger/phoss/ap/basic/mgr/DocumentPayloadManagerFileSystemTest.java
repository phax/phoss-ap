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
package com.helger.phoss.ap.basic.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.Test;

import com.helger.io.file.FilenameHelper;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;

/**
 * Contract test for {@link DocumentPayloadManagerFileSystem}. Runs as a standard unit test (no
 * Docker required) using a temporary directory.
 *
 * @author Philip Helger
 */
public final class DocumentPayloadManagerFileSystemTest extends AbstractDocumentPayloadManagerContractTest
{
  private Path m_aTempDir;

  @Override
  protected IDocumentPayloadManager createManager ()
  {
    try
    {
      m_aTempDir = Files.createTempDirectory ("phoss-ap-fs-test");
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to create temp directory", ex);
    }
    return new DocumentPayloadManagerFileSystem ();
  }

  @Override
  protected String getBaseDir ()
  {
    return m_aTempDir.toAbsolutePath ().toString ();
  }

  @Override
  protected String getNonExistentPath ()
  {
    return m_aTempDir.resolve ("does-not-exist.xml").toAbsolutePath ().toString ();
  }

  @Override
  protected String extractDirectory (final String sPath)
  {
    return FilenameHelper.getPath (sPath);
  }

  @Override
  protected void cleanup ()
  {
    if (m_aTempDir != null)
    {
      try
      {
        // Recursively delete the temp directory
        Files.walk (m_aTempDir).sorted (Comparator.reverseOrder ()).forEach (aPath -> {
          try
          {
            Files.deleteIfExists (aPath);
          }
          catch (final IOException ex)
          {
            // Ignore cleanup errors
          }
        });
      }
      catch (final IOException ex)
      {
        // Ignore cleanup errors
      }
    }
  }

  @Test
  public void testGetSafeFilenameReplacesPathSeparatorsAndSpaces ()
  {
    assertEquals ("IM_2026_10_4807678.sbd",
                  DocumentPayloadManagerFileSystem.getSafeFilename ("IM 2026/10/4807678.sbd"));
    assertEquals ("plain.sbd", DocumentPayloadManagerFileSystem.getSafeFilename ("plain.sbd"));
  }

  @Test
  public void testStoreDocumentWithSlashySbdhInstanceId ()
  {
    // Real-world Digiteal SBDH Instance Identifier used as the inbound filename
    final byte [] aPayload = "<SBD/>".getBytes (StandardCharsets.UTF_8);
    final IDocumentPayloadManager aMgr = createManager ();
    final String sPath = aMgr.storeDocument (getBaseDir (),
                                             REFERENCE_DT,
                                             "IM 2026/10/4807678.sbd",
                                             aPayload);
    assertTrue (sPath.endsWith ("IM_2026_10_4807678.sbd"));
    assertFalse ("Must not create nested directories from the instance id",
                 sPath.contains ("IM 2026") || sPath.contains ("/10/4807678"));
    assertArrayEquals (aPayload, aMgr.readDocument (sPath));
  }
}
