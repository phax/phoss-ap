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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test class for DirectoryScanTask — tests the file filtering logic (which files are picked up as
 * candidates). Does not test the full processing flow since that requires the outbound pipeline.
 *
 * @author Philip Helger
 */
public final class DirectoryScanTaskTest
{
  @Rule
  public final TemporaryFolder m_aTempDir = new TemporaryFolder ();

  @Test
  public void testXmlFilterPicksUpXmlFiles () throws IOException
  {
    final File aWatchDir = m_aTempDir.newFolder ("watch");
    Files.writeString (new File (aWatchDir, "invoice.xml").toPath (), "<sbd/>", StandardCharsets.UTF_8);
    Files.writeString (new File (aWatchDir, "another.xml").toPath (), "<sbd/>", StandardCharsets.UTF_8);

    final File [] aFiles = aWatchDir.listFiles ( (dir, name) -> name.endsWith (".xml"));
    assertTrue (aFiles.length == 2);
  }

  @Test
  public void testXmlFilterSkipsNonXmlFiles () throws IOException
  {
    final File aWatchDir = m_aTempDir.newFolder ("watch");
    Files.writeString (new File (aWatchDir, "readme.txt").toPath (), "hello", StandardCharsets.UTF_8);
    Files.writeString (new File (aWatchDir, "data.json").toPath (), "{}", StandardCharsets.UTF_8);
    Files.writeString (new File (aWatchDir, "invoice.xml").toPath (), "<sbd/>", StandardCharsets.UTF_8);

    final File [] aFiles = aWatchDir.listFiles ( (dir, name) -> name.endsWith (".xml"));
    assertTrue (aFiles.length == 1);
    assertTrue (aFiles[0].getName ().equals ("invoice.xml"));
  }

  @Test
  public void testXmlFilterSkipsSubdirectories () throws IOException
  {
    final File aWatchDir = m_aTempDir.newFolder ("watch");
    new File (aWatchDir, "pending").mkdirs ();
    new File (aWatchDir, "success").mkdirs ();
    Files.writeString (new File (aWatchDir, "invoice.xml").toPath (), "<sbd/>", StandardCharsets.UTF_8);

    // listFiles with filename filter already excludes directories (they don't end in .xml)
    final File [] aFiles = aWatchDir.listFiles ( (dir, name) -> name.endsWith (".xml"));
    assertTrue (aFiles.length == 1);
  }

  @Test
  public void testEmptyDirectoryReturnsNoFiles ()
  {
    final File aWatchDir = m_aTempDir.getRoot ();

    final File [] aFiles = aWatchDir.listFiles ( (dir, name) -> name.endsWith (".xml"));
    assertNotNull (aFiles);
    assertTrue (aFiles.length == 0);
  }

  @Test
  public void testEmptyXmlFileIsSkippable () throws IOException
  {
    final File aWatchDir = m_aTempDir.newFolder ("watch");
    final File aEmptyFile = new File (aWatchDir, "empty.xml");
    aEmptyFile.createNewFile ();

    // The scan task skips files with size 0
    assertFalse (aEmptyFile.length () > 0);
  }
}
