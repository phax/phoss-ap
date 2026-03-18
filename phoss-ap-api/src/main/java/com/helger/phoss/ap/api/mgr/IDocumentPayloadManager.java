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
package com.helger.phoss.ap.api.mgr;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;

/**
 * Interface for reading and writing document files. Documents are stored as flat files rather than
 * as BYTEA columns in the database. Implementations may use local disk, network storage, or other
 * backends.
 *
 * @author Philip Helger
 */
public interface IDocumentPayloadManager
{
  /**
   * Verify that the storage configuration is valid and the storage directories are accessible.
   *
   * @throws RuntimeException
   *         if the configuration is invalid or the directories are not writable.
   */
  void verifyConfiguration ();

  /**
   * Write bytes to a file under the given base directory, using the provided filename. Creates the
   * directory if needed.
   *
   * @param sBaseDir
   *        The base directory to store the file in. May not be <code>null</code>.
   * @param aReferenceDT
   *        The reference date time to which the message should be stored.
   * @param sFilename
   *        The filename to use. May not be <code>null</code>.
   * @param aBytes
   *        The bytes to write. May not be <code>null</code>.
   * @return The absolute path of the stored file.
   */
  @NonNull
  String storeDocument (@NonNull String sBaseDir,
                        @NonNull OffsetDateTime aReferenceDT,
                        @NonNull String sFilename,
                        byte @NonNull [] aBytes);

  /**
   * Open an {@link OutputStream} for writing a document file. The caller is responsible for closing
   * the stream.
   *
   * @param sBaseDir
   *        The base directory to store the file in. May not be <code>null</code>.
   * @param aReferenceDT
   *        The reference date time to which the message should be stored.
   * @param sFilename
   *        The filename to use. May not be <code>null</code>.
   * @param sFileExt
   *        The file extension including the leading dot. May not be <code>null</code>.
   * @param aPathConsumer
   *        A consumer that receives the absolute path of the file being written. May not be
   *        <code>null</code>.
   * @return An open output stream.
   */
  @NonNull
  OutputStream openDocumentStreamForWrite (@NonNull String sBaseDir,
                                           @NonNull OffsetDateTime aReferenceDT,
                                           @NonNull String sFilename,
                                           @NonNull String sFileExt,
                                           @NonNull Consumer <String> aPathConsumer);

  /**
   * Open an {@link OutputStream} for writing a temporary document file. The caller is responsible
   * for closing the stream.
   *
   * @param sBaseDir
   *        The base directory to store the file in. May not be <code>null</code>.
   * @param aReferenceDT
   *        The reference date time to which the message should be stored.
   * @param aPathConsumer
   *        A consumer that receives the absolute path of the file being written. May not be
   *        <code>null</code>.
   * @return An open output stream.
   */
  @NonNull
  OutputStream openTemporaryDocumentStreamForWrite (@NonNull String sBaseDir,
                                                    @NonNull OffsetDateTime aReferenceDT,
                                                    @NonNull Consumer <String> aPathConsumer);

  /**
   * Rename a file to a new name in the target directory.
   *
   * @param sSrcFile
   *        The absolute path of the source file. May not be <code>null</code>.
   * @param sTargetDir
   *        The target directory. May not be <code>null</code>.
   * @param sBaseName
   *        The base name (without extension) of the target file. May not be <code>null</code>.
   * @param sFileExt
   *        The file extension including the leading dot. May not be <code>null</code>.
   * @return The new path of the renamed document (changed in 0.1.1 from File to String)
   */
  @NonNull
  String renameFile (@NonNull String sSrcFile,
                     @NonNull String sTargetDir,
                     @NonNull @Nonempty String sBaseName,
                     @NonNull @Nonempty String sFileExt);

  /**
   * Read all bytes from the file at the given absolute path.
   *
   * @param sAbsolutePath
   *        The absolute path of the file. May not be <code>null</code>.
   * @return The file contents as a byte array.
   */
  byte @NonNull [] readDocument (@NonNull String sAbsolutePath);

  /**
   * Open an {@link InputStream} for the file at the given absolute path. The caller is responsible
   * for closing the stream.
   *
   * @param sAbsolutePath
   *        The absolute path of the file. May not be <code>null</code>.
   * @return An open input stream.
   */
  @NonNull
  InputStream openDocumentStreamForRead (@NonNull String sAbsolutePath);

  /**
   * Delete the document file at the given path.
   *
   * @param sAbsolutePath
   *        The absolute path of the file. May not be <code>null</code>.
   * @return <code>true</code> if the file was deleted, <code>false</code> if it did not exist.
   */
  boolean deleteDocument (@NonNull String sAbsolutePath);

  /**
   * Check if the document file at the given path exists or not.
   *
   * @param sAbsolutePath
   *        The absolute path of the file. May not be <code>null</code>.
   * @return <code>true</code> if the file exists, <code>false</code> if it does not exist.
   */
  boolean existsDocument (@NonNull String sAbsolutePath);
}
