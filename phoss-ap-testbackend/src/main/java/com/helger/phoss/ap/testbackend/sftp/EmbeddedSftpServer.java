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
package com.helger.phoss.ap.testbackend.sftp;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.helger.phoss.ap.testbackend.store.DocumentStore;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Embedded SFTP server using Apache MINA SSHD. Accepts uploads from phoss-ap's
 * {@code SftpDocumentForwarderSPI} and registers received files in the
 * {@link DocumentStore}.
 *
 * @author Philip Helger
 */
@Component
@ConditionalOnProperty (name = "testbackend.sftp.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddedSftpServer
{
  private static final Logger LOGGER = LoggerFactory.getLogger (EmbeddedSftpServer.class);

  @Value ("${testbackend.sftp.port:2222}")
  private int m_nPort;

  @Value ("${testbackend.sftp.user:testuser}")
  private String m_sUser;

  @Value ("${testbackend.sftp.password:testpass}")
  private String m_sPassword;

  @Value ("${testbackend.sftp.host-key-file:generated/hostkey.ser}")
  private String m_sHostKeyFile;

  @Value ("${testbackend.storage.base-dir:generated/received/}")
  private String m_sBaseDir;

  @Autowired
  private DocumentStore m_aDocumentStore;

  private SshServer m_aSshd;
  private Thread m_aWatcherThread;
  private volatile boolean m_bRunning;

  /**
   * Start the embedded SFTP server and begin watching the SFTP root directory
   * for newly uploaded files.
   *
   * @throws IOException
   *         If the SSH server fails to start.
   */
  @PostConstruct
  public void start () throws IOException
  {
    final Path aSftpRoot = Path.of (m_sBaseDir, "sftp");
    aSftpRoot.toFile ().mkdirs ();

    m_aSshd = SshServer.setUpDefaultServer ();
    m_aSshd.setPort (m_nPort);

    // Host key
    final File aHostKeyFile = new File (m_sHostKeyFile);
    aHostKeyFile.getParentFile ().mkdirs ();
    final KeyPairProvider aKeyPairProvider = new SimpleGeneratorHostKeyProvider (aHostKeyFile.toPath ());
    m_aSshd.setKeyPairProvider (aKeyPairProvider);

    // Password authentication
    final PasswordAuthenticator aAuthenticator = (sUsername, sPassword, aSession) -> m_sUser.equals (sUsername) &&
                                                                                     m_sPassword.equals (sPassword);
    m_aSshd.setPasswordAuthenticator (aAuthenticator);

    // SFTP subsystem
    m_aSshd.setSubsystemFactories (Collections.singletonList (new SftpSubsystemFactory ()));

    // Virtual filesystem rooted at sftp directory
    m_aSshd.setFileSystemFactory (new VirtualFileSystemFactory (aSftpRoot));

    m_aSshd.start ();
    LOGGER.info ("Embedded SFTP server started on port " +
                 m_nPort +
                 " (user=" +
                 m_sUser +
                 ", root=" +
                 aSftpRoot.toAbsolutePath () +
                 ")");

    // Start a background watcher for new files
    m_bRunning = true;
    m_aWatcherThread = new Thread ( () -> _watchDirectory (aSftpRoot), "sftp-file-watcher");
    m_aWatcherThread.setDaemon (true);
    m_aWatcherThread.start ();
  }

  private void _watchDirectory (final Path aDir)
  {
    try (final WatchService aWatcher = FileSystems.getDefault ().newWatchService ())
    {
      aDir.register (aWatcher, StandardWatchEventKinds.ENTRY_CREATE);

      while (m_bRunning)
      {
        final WatchKey aKey;
        try
        {
          aKey = aWatcher.take ();
        }
        catch (final InterruptedException ex)
        {
          Thread.currentThread ().interrupt ();
          return;
        }

        for (final WatchEvent <?> aEvent : aKey.pollEvents ())
        {
          if (aEvent.kind () == StandardWatchEventKinds.ENTRY_CREATE)
          {
            final Path aFilePath = aDir.resolve ((Path) aEvent.context ());
            final File aFile = aFilePath.toFile ();

            // Skip .tmp files (SFTP writes to .tmp then renames)
            if (aFile.getName ().endsWith (".tmp"))
              continue;

            // Small delay to let rename complete
            try
            {
              Thread.sleep (200);
            }
            catch (final InterruptedException ex)
            {
              Thread.currentThread ().interrupt ();
              return;
            }

            if (aFile.exists () && aFile.isFile ())
            {
              m_aDocumentStore.registerExternalDocument ("sftp",
                                                         aFile.getName (),
                                                         aFile.length (),
                                                         aFile.getAbsolutePath ());
            }
          }
        }
        aKey.reset ();
      }
    }
    catch (final IOException ex)
    {
      LOGGER.error ("SFTP directory watcher failed", ex);
    }
  }

  /**
   * Stop the embedded SFTP server and the file watcher thread.
   *
   * @throws IOException
   *         If the SSH server fails to stop cleanly.
   */
  @PreDestroy
  public void stop () throws IOException
  {
    m_bRunning = false;
    if (m_aWatcherThread != null)
      m_aWatcherThread.interrupt ();
    if (m_aSshd != null)
    {
      m_aSshd.stop ();
      LOGGER.info ("Embedded SFTP server stopped");
    }
  }
}
