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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.helger.io.file.FilenameHelper;
import com.helger.phoss.ap.testbackend.model.ReceivedDocument;

import jakarta.annotation.PostConstruct;

/**
 * In-memory store for received documents. Persists document content to disk and
 * maintains metadata in a {@link ConcurrentHashMap}.
 *
 * @author Philip Helger
 */
@Component
public class DocumentStore
{
  private static final Logger LOGGER = LoggerFactory.getLogger (DocumentStore.class);

  private final Map <String, ReceivedDocument> m_aDocuments = new ConcurrentHashMap <> ();

  @Value ("${testbackend.storage.base-dir:generated/received/}")
  private String m_sBaseDir;

  /**
   * Initialize the storage base directory, creating it if it does not exist.
   */
  @PostConstruct
  public void init ()
  {
    final File aDir = new File (m_sBaseDir);
    if (!aDir.exists ())
    {
      aDir.mkdirs ();
      LOGGER.info ("Created storage directory: " + aDir.getAbsolutePath ());
    }
  }

  /**
   * Store a document by writing its content to disk and registering it in the
   * in-memory map.
   *
   * @param sChannel
   *        The forwarding channel name (e.g. "http-sync", "http-async").
   * @param sFilename
   *        The original filename.
   * @param aContent
   *        The raw document bytes.
   * @param sSbdhID
   *        The SBDH Instance Identifier for logging purposes.
   * @return The stored {@link ReceivedDocument}, or {@code null} if writing to
   *         disk failed.
   */
  public ReceivedDocument storeDocument (final String sChannel,
                                         final String sFilename,
                                         final byte [] aContent,
                                         final String sSbdhID)
  {
    final String sID = UUID.randomUUID ().toString ();
    final File aChannelDir = new File (m_sBaseDir, sChannel);
    if (!aChannelDir.exists ())
      aChannelDir.mkdirs ();

    final File aTargetFile = new File (aChannelDir,
                                       FilenameHelper.getAsSecureValidASCIIFilename (sID + "_" + sFilename));
    try
    {
      Files.write (aTargetFile.toPath (), aContent);
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to store document '" + sFilename + "'", ex);
      return null;
    }

    final ReceivedDocument aDoc = new ReceivedDocument (sID,
                                                        sChannel,
                                                        sFilename,
                                                        aContent.length,
                                                        OffsetDateTime.now (),
                                                        aTargetFile.getAbsolutePath ());
    m_aDocuments.put (sID, aDoc);
    LOGGER.info ("Stored document [" +
                 sChannel +
                 "] '" +
                 sFilename +
                 "' (" +
                 aContent.length +
                 " bytes) as '" +
                 sID +
                 "' from SBDH '" +
                 sSbdhID +
                 "'");
    return aDoc;
  }

  /**
   * Register a document that was stored externally (e.g. via SFTP or S3)
   * without copying its content.
   *
   * @param sChannel
   *        The forwarding channel name (e.g. "sftp", "s3").
   * @param sFilename
   *        The original filename.
   * @param nSizeBytes
   *        The file size in bytes.
   * @param sAbsolutePath
   *        The absolute path to the file on disk.
   * @return The registered {@link ReceivedDocument}.
   */
  public ReceivedDocument registerExternalDocument (final String sChannel,
                                                    final String sFilename,
                                                    final long nSizeBytes,
                                                    final String sAbsolutePath)
  {
    final String sID = UUID.randomUUID ().toString ();
    final ReceivedDocument aDoc = new ReceivedDocument (sID,
                                                        sChannel,
                                                        sFilename,
                                                        nSizeBytes,
                                                        OffsetDateTime.now (),
                                                        sAbsolutePath);
    m_aDocuments.put (sID, aDoc);
    LOGGER.info ("Registered external document [" + sChannel + "] '" + sFilename + "' (" + nSizeBytes + " bytes)");
    return aDoc;
  }

  /**
   * Get a received document by its ID.
   *
   * @param sID
   *        The document ID.
   * @return The matching document, or {@code null} if not found.
   */
  public ReceivedDocument getByID (final String sID)
  {
    return m_aDocuments.get (sID);
  }

  /**
   * @return An unmodifiable list of all received documents.
   */
  public List <ReceivedDocument> getAll ()
  {
    return Collections.unmodifiableList (new ArrayList <> (m_aDocuments.values ()));
  }

  /**
   * Get all received documents for a specific forwarding channel.
   *
   * @param sChannel
   *        The channel name to filter by.
   * @return A list of documents matching the given channel.
   */
  public List <ReceivedDocument> getAllByChannel (final String sChannel)
  {
    return m_aDocuments.values ().stream ().filter (d -> d.getChannel ().equals (sChannel)).toList ();
  }

  /**
   * Read the raw content of a stored document from disk.
   *
   * @param sID
   *        The document ID.
   * @return The file content as a byte array, or {@code null} if no document
   *         with the given ID exists.
   * @throws IOException
   *         If reading the file fails.
   */
  public byte [] readContent (final String sID) throws IOException
  {
    final ReceivedDocument aDoc = m_aDocuments.get (sID);
    if (aDoc == null)
      return null;
    return Files.readAllBytes (Path.of (aDoc.getStoragePath ()));
  }

  /**
   * @return The total number of stored documents.
   */
  public int getCount ()
  {
    return m_aDocuments.size ();
  }

  /**
   * Remove all documents from the in-memory store. Does not delete files from
   * disk.
   */
  public void clear ()
  {
    m_aDocuments.clear ();
    LOGGER.info ("Document store cleared");
  }
}
