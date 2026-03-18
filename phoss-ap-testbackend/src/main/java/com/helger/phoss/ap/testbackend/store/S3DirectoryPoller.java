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
package com.helger.phoss.ap.testbackend.store;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Polls a local directory for files uploaded via S3 (e.g. a MinIO data directory or an S3-mounted
 * filesystem). New files are registered in the {@link DocumentStore} under the "s3" channel.
 * <p>
 * Configure the directory via {@code testbackend.s3poll.directory} and the polling interval via
 * {@code testbackend.s3poll.interval-ms}.
 * </p>
 *
 * @author Philip Helger
 */
@Component
@ConditionalOnProperty (name = "testbackend.s3poll.enabled", havingValue = "true", matchIfMissing = true)
public class S3DirectoryPoller
{
  private static final Logger LOGGER = LoggerFactory.getLogger (S3DirectoryPoller.class);

  @Value ("${testbackend.s3poll.directory:generated/s3-inbox/}")
  private String m_sPollDirectory;

  @Autowired
  private DocumentStore m_aDocumentStore;

  private final Set <String> m_aKnownFiles = new HashSet <> ();

  /**
   * Initialize the S3 poll directory. If the directory already exists, all existing files are
   * registered so they are not re-detected on the first poll.
   */
  @PostConstruct
  public void init ()
  {
    final File aDir = new File (m_sPollDirectory);
    if (!aDir.exists ())
    {
      aDir.mkdirs ();
      LOGGER.info ("Created S3 poll directory: " + aDir.getAbsolutePath ());
    }
    else
    {
      // Register already-existing files so we don't re-detect them
      final File [] aFiles = aDir.listFiles ();
      if (aFiles != null)
      {
        for (final File aFile : aFiles)
          if (aFile.isFile ())
            m_aKnownFiles.add (aFile.getAbsolutePath ());
      }
      LOGGER.info ("S3 poller initialized with " +
                   m_aKnownFiles.size () +
                   " existing files in " +
                   aDir.getAbsolutePath ());
    }
  }

  /**
   * Poll the configured S3 directory for new files and register any newly discovered files in the
   * {@link DocumentStore}.
   */
  @Scheduled (fixedDelayString = "${testbackend.s3poll.interval-ms:5000}")
  public void poll ()
  {
    final File aDir = new File (m_sPollDirectory);
    if (!aDir.exists () || !aDir.isDirectory ())
      return;

    _scanDirectory (aDir);
  }

  private void _scanDirectory (final File aDir)
  {
    final File [] aFiles = aDir.listFiles ();
    if (aFiles == null)
      return;

    for (final File aFile : aFiles)
    {
      if (aFile.isDirectory ())
      {
        // Recurse into subdirectories (S3 key prefixes create subdirectories)
        _scanDirectory (aFile);
      }
      else
        if (aFile.isFile () && m_aKnownFiles.add (aFile.getAbsolutePath ()))
        {
          LOGGER.info ("S3 poller detected new file: " + aFile.getName ());
          m_aDocumentStore.registerExternalDocument ("s3", aFile.getName (), aFile.length (), aFile.getAbsolutePath ());
        }
    }
  }
}
