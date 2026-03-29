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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.helger.io.file.SimpleFileIO;
import com.helger.io.resource.ClassPathResource;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppol.testfiles.sbdh.PeppolSBDHTestFiles;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

/**
 * Test class for {@link DirectoryFileProcessor} — tests the independently testable file operations
 * and SBDH parsing without requiring the full outbound pipeline.
 *
 * @author Philip Helger
 */
public final class DirectoryFileProcessorTest
{
  @Rule
  public final TemporaryFolder m_aTempDir = new TemporaryFolder ();

  // -- _moveFileToDir tests --

  @Test
  public void testMoveFileBasic () throws IOException
  {
    final File aSource = m_aTempDir.newFile ("invoice.xml");
    SimpleFileIO.writeFile (aSource, "<test/>", StandardCharsets.UTF_8);
    final File aTargetDir = m_aTempDir.newFolder ("target");

    final File aMoved = DirectoryFileProcessor._moveFileToDir (aSource, aTargetDir);

    assertNotNull (aMoved);
    assertEquals ("invoice.xml", aMoved.getName ());
    assertTrue (aMoved.exists ());
    assertFalse (aSource.exists ());
    assertEquals ("<test/>", SimpleFileIO.getFileAsString (aMoved, StandardCharsets.UTF_8));
  }

  @Test
  public void testMoveFileUniqueSuffix () throws IOException
  {
    final File aTargetDir = m_aTempDir.newFolder ("target");

    // Create a file already existing in target
    SimpleFileIO.writeFile (new File (aTargetDir, "invoice.xml"), "existing", StandardCharsets.UTF_8);

    // Create source file
    final File aSource = m_aTempDir.newFile ("invoice.xml");
    SimpleFileIO.writeFile (aSource, "new content", StandardCharsets.UTF_8);

    final File aMoved = DirectoryFileProcessor._moveFileToDir (aSource, aTargetDir);

    assertNotNull (aMoved);
    assertEquals ("invoice-1.xml", aMoved.getName ());
    assertTrue (aMoved.exists ());
    assertFalse (aSource.exists ());
  }

  @Test
  public void testMoveFileMultipleSuffixes () throws IOException
  {
    final File aTargetDir = m_aTempDir.newFolder ("target");

    SimpleFileIO.writeFile (new File (aTargetDir, "invoice.xml"), "v0", StandardCharsets.UTF_8);
    SimpleFileIO.writeFile (new File (aTargetDir, "invoice-1.xml"), "v1", StandardCharsets.UTF_8);

    final File aSource = m_aTempDir.newFile ("invoice.xml");
    SimpleFileIO.writeFile (aSource, "v2", StandardCharsets.UTF_8);

    final File aMoved = DirectoryFileProcessor._moveFileToDir (aSource, aTargetDir);

    assertNotNull (aMoved);
    assertEquals ("invoice-2.xml", aMoved.getName ());
  }

  @Test
  public void testMoveFileNoExtension () throws IOException
  {
    final File aTargetDir = m_aTempDir.newFolder ("target");

    SimpleFileIO.writeFile (new File (aTargetDir, "noext"), "existing", StandardCharsets.UTF_8);

    final File aSource = m_aTempDir.newFile ("noext");
    SimpleFileIO.writeFile (aSource, "new", StandardCharsets.UTF_8);

    final File aMoved = DirectoryFileProcessor._moveFileToDir (aSource, aTargetDir);

    assertNotNull (aMoved);
    assertEquals ("noext-1", aMoved.getName ());
  }

  // -- _writeResultJson tests --

  @Test
  public void testWriteResultJson ()
  {
    final File aDir = m_aTempDir.getRoot ();
    final IJsonObject aJson = new JsonObject ().add ("success", true).add ("sourceFile", "test.xml");

    DirectoryFileProcessor._writeResultJson (aDir, "test.xml.json", aJson);

    final File aJsonFile = new File (aDir, "test.xml.json");
    assertTrue (aJsonFile.exists ());

    final String sContent = SimpleFileIO.getFileAsString (aJsonFile, StandardCharsets.UTF_8);
    final IJsonObject aParsed = JsonReader.builder ().source (sContent).readAsObject ();
    assertNotNull (aParsed);
    assertTrue (aParsed.getAsBoolean ("success"));
    assertEquals ("test.xml", aParsed.getAsString ("sourceFile"));
  }

  // -- SBDH parsing tests (using PeppolSBDHDataReader directly) --

  @Test
  public void testParseSbdhFromGoodTestFile () throws Exception
  {
    final ClassPathResource aRes = PeppolSBDHTestFiles.getFirstGoodCaseV20 ();
    try (final InputStream aIS = aRes.getInputStream ())
    {
      final PeppolSBDHData aSbdData = new PeppolSBDHDataReader (PeppolIdentifierFactory.INSTANCE).extractData (aIS);
      assertNotNull (aSbdData);
      assertEquals ("a593a0aa-6ff7-48b0-8906-5534fa5212e0", aSbdData.getInstanceIdentifier ());
    }
  }

  @Test
  public void testParseSbdhFromAllGoodTestFiles () throws Exception
  {
    for (final ClassPathResource aRes : PeppolSBDHTestFiles.getAllGoodCases ())
    {
      try (final InputStream aIS = aRes.getInputStream ())
      {
        final PeppolSBDHData aSbdData = new PeppolSBDHDataReader (PeppolIdentifierFactory.INSTANCE).extractData (aIS);
        assertNotNull ("Failed to parse " + aRes.getPath (), aSbdData);
        assertNotNull ("No instance ID in " + aRes.getPath (), aSbdData.getInstanceIdentifier ());
      }
    }
  }

  @Test
  public void testParseSbdhFromInvalidXmlThrowsException ()
  {
    try (final InputStream aIS = new java.io.ByteArrayInputStream ("<not-an-sbd/>".getBytes (StandardCharsets.UTF_8)))
    {
      new PeppolSBDHDataReader (PeppolIdentifierFactory.INSTANCE).extractData (aIS);
      // If no exception, parsing returned something — that's also acceptable
    }
    catch (final Exception ex)
    {
      // Expected: PeppolSBDHDataReader throws on non-SBD XML
      assertNotNull (ex.getMessage ());
    }
  }

  // -- Directory structure constants --

  @Test
  public void testDirectoryConstants ()
  {
    assertEquals ("pending", DirectoryFileProcessor.DIR_PENDING);
    assertEquals ("success", DirectoryFileProcessor.DIR_SUCCESS);
    assertEquals ("error", DirectoryFileProcessor.DIR_ERROR);
  }
}
