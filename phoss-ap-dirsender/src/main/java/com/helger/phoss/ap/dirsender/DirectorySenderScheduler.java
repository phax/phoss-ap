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
import java.time.Duration;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.exception.InitializationException;
import com.helger.io.file.FileOperationManager;

/**
 * Manages the lifecycle of the directory-based SBD sender. Matches the pattern of
 * {@code RetryScheduler} with static {@code start()} / {@code stop()} methods.
 *
 * @author Philip Helger
 * @since v0.2.0
 */
public final class DirectorySenderScheduler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (DirectorySenderScheduler.class);

  private static Timer s_aTimer;

  private DirectorySenderScheduler ()
  {}

  private static void _recoverPendingFiles (final File aWatchDir, final File aPendingDir)
  {
    final File [] aFiles = aPendingDir.listFiles ((dir, name) -> name.endsWith (".xml"));
    if (aFiles != null && aFiles.length > 0)
    {
      LOGGER.info ("Recovering " + aFiles.length + " file(s) from pending directory");
      for (final File aFile : aFiles)
      {
        try
        {
          DirectoryFileProcessor.recoverPendingFile (aWatchDir, aFile);
        }
        catch (final Exception ex)
        {
          LOGGER.error ("Error recovering pending file '" + aFile.getName () + "'", ex);
        }
      }
    }
  }

  /**
   * Start the directory sender scheduler if enabled. Validates configuration, creates
   * subdirectories ({@code pending/}, {@code success/}, {@code error/}), runs startup recovery on
   * pending files, and starts the periodic scan timer.
   */
  public static void start ()
  {
    if (!APDirSenderConfig.isEnabled ())
    {
      LOGGER.info ("Directory sender is disabled");
      return;
    }

    final String sDirectory = APDirSenderConfig.getDirectory ();
    if (sDirectory == null)
      throw new InitializationException ("Directory sender is enabled but 'dirsender.directory' is not configured");

    final File aWatchDir = new File (sDirectory);
    if (!aWatchDir.isDirectory ())
      throw new InitializationException ("Directory sender watch directory '" +
                                         sDirectory +
                                         "' does not exist or is not a directory");

    // Create subdirectories
    final File aPendingDir = new File (aWatchDir, DirectoryFileProcessor.DIR_PENDING);
    final File aSuccessDir = new File (aWatchDir, DirectoryFileProcessor.DIR_SUCCESS);
    final File aErrorDir = new File (aWatchDir, DirectoryFileProcessor.DIR_ERROR);

    if (FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (aPendingDir).isFailure ())
      throw new InitializationException ("Failed to create pending directory '" + aPendingDir.getAbsolutePath () + "'");

    if (FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (aSuccessDir).isFailure ())
      throw new InitializationException ("Failed to create success directory '" + aSuccessDir.getAbsolutePath () + "'");

    if (FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (aErrorDir).isFailure ())
      throw new InitializationException ("Failed to create error directory '" + aErrorDir.getAbsolutePath () + "'");

    // Startup recovery: process files stuck in pending/
    _recoverPendingFiles (aWatchDir, aPendingDir);

    final Duration aScanInterval = APDirSenderConfig.getScanInterval ();
    final Duration aInitialDelay = APDirSenderConfig.getInitialDelay ();

    LOGGER.info ("Starting directory sender: directory='" +
                 sDirectory +
                 "', scan-interval=" +
                 aScanInterval +
                 ", initial-delay=" +
                 aInitialDelay);

    s_aTimer = new Timer ("phoss-ap-dir-sender", true);
    s_aTimer.scheduleAtFixedRate (new DirectoryScanTask (aWatchDir),
                                  aInitialDelay.toMillis (),
                                  aScanInterval.toMillis ());
  }

  /**
   * Stop the directory sender scheduler. If it was not started, this method does nothing.
   */
  public static void stop ()
  {
    if (s_aTimer != null)
    {
      s_aTimer.cancel ();
      s_aTimer = null;
      LOGGER.info ("Directory sender scheduler stopped");
    }
  }
}
