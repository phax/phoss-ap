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

import java.io.File;
import java.util.TimerTask;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.io.file.FileHelper;
import com.helger.phoss.ap.core.APCoreMetaManager;

/**
 * {@link TimerTask} that periodically scans the watch directory for new {@code *.xml} files and
 * processes them. Also cleans up pending files whose DB transactions have reached a final state.
 *
 * @author Philip Helger
 * @since v0.2.0
 */
final class DirectoryScanTask extends TimerTask
{
  private static final Logger LOGGER = LoggerFactory.getLogger (DirectoryScanTask.class);

  private final File m_aWatchDir;

  DirectoryScanTask (@NonNull final File aWatchDir)
  {
    m_aWatchDir = aWatchDir;
  }

  private void _processNewFiles ()
  {
    final File [] aFiles = m_aWatchDir.listFiles ((dir, name) -> name.endsWith (".xml"));
    if (aFiles == null || aFiles.length == 0)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("No new XML files found in '" + m_aWatchDir.getAbsolutePath () + "'");
      return;
    }

    LOGGER.info ("Found " + aFiles.length + " XML file(s) in watch directory");

    for (final File aFile : aFiles)
    {
      // Skip files that are not readable or have size 0 (possibly still being written)
      if (!FileHelper.canReadAndWriteFile (aFile) || aFile.length () == 0)
      {
        LOGGER.debug ("Skipping file '" + aFile.getName () + "' (not ready or empty)");
        continue;
      }

      try
      {
        DirectoryFileProcessor.processFile (m_aWatchDir, aFile);
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Error processing file '" + aFile.getName () + "'", ex);

        for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
          aHandler.onUnexpectedException ("DirectoryScanTask._processNewFiles",
                                          "Error processing file '" + aFile.getName () + "'",
                                          ex);
      }
    }
  }

  private void _cleanupPendingFiles ()
  {
    final File aPendingDir = new File (m_aWatchDir, DirectoryFileProcessor.DIR_PENDING);
    final File [] aFiles = aPendingDir.listFiles ((dir, name) -> name.endsWith (".xml"));
    if (aFiles != null && aFiles.length > 0)
    {
      for (final File aFile : aFiles)
      {
        try
        {
          DirectoryFileProcessor.cleanupPendingFile (m_aWatchDir, aFile);
        }
        catch (final Exception ex)
        {
          LOGGER.error ("Error cleaning up pending file '" + aFile.getName () + "'", ex);
        }
      }
    }
  }

  @Override
  public void run ()
  {
    try
    {
      _processNewFiles ();
      _cleanupPendingFiles ();
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Unexpected error in directory sender scan cycle", ex);

      for (final var aHandler : APCoreMetaManager.getAllNotificationHandlers ())
        aHandler.onUnexpectedException ("DirectoryScanTask.run", "Unexpected error in directory sender scan cycle", ex);
    }
  }
}
