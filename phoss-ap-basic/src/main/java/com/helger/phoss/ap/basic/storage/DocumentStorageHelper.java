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
package com.helger.phoss.ap.basic.storage;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingBufferedOutputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;

/**
 * Utility class for reading and writing document files on disk. Documents are stored as flat files
 * rather than as BYTEA columns in the database.
 *
 * @author Philip Helger
 */
@Immutable
public final class DocumentStorageHelper
{
  private DocumentStorageHelper ()
  {}

  @NonNull
  private static File _getStorageDir (@NonNull final File aBaseDir, @NonNull final OffsetDateTime aReferenceDT)
  {
    return new File (aBaseDir,
                     Integer.toString (aReferenceDT.getYear ()) +
                               "/" +
                               StringHelper.getLeadingZero (aReferenceDT.getMonthValue (), 2) +
                               "/" +
                               StringHelper.getLeadingZero (aReferenceDT.getDayOfMonth (), 2) +
                               "/" +
                               StringHelper.getLeadingZero (aReferenceDT.getHour (), 2));
  }

  /**
   * Write bytes to a file under the given base directory, using the provided filename. Creates the
   * directory if needed.
   *
   * @param aBaseDir
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
  public static String storeDocument (@NonNull final File aBaseDir,
                                      @NonNull final OffsetDateTime aReferenceDT,
                                      @NonNull final String sFilename,
                                      final byte @NonNull [] aBytes)
  {
    ValueEnforcer.notNull (aBaseDir, "BaseDir");
    ValueEnforcer.notNull (aReferenceDT, "ReferenceDT");
    ValueEnforcer.notNull (sFilename, "Filename");
    ValueEnforcer.notNull (aBytes, "Bytes");

    final File aEffectiveBaseDir = _getStorageDir (aBaseDir, aReferenceDT);
    try
    {
      Files.createDirectories (aEffectiveBaseDir.toPath ());
      final Path aFilePath = aEffectiveBaseDir.toPath ().resolve (sFilename);
      Files.write (aFilePath, aBytes);
      return aFilePath.toAbsolutePath ().toString ();
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to store document '" +
                                       sFilename +
                                       "' in " +
                                       aEffectiveBaseDir.getAbsolutePath () +
                                       "'",
                                       ex);
    }
  }

  @NonNull
  public static OutputStream openDocumentStream (@NonNull final File aBaseDir,
                                                 @NonNull final OffsetDateTime aReferenceDT,
                                                 @NonNull final String sFilename,
                                                 @NonNull final Consumer <File> aFileConsumer)
  {
    ValueEnforcer.notNull (aBaseDir, "BaseDir");
    ValueEnforcer.notNull (aReferenceDT, "ReferenceDT");
    ValueEnforcer.notNull (aFileConsumer, "FileConsumer");

    final File aEffectiveBaseDir = _getStorageDir (aBaseDir, aReferenceDT);
    try
    {
      Files.createDirectories (aEffectiveBaseDir.toPath ());
      final Path aFilePath = aEffectiveBaseDir.toPath ().resolve (sFilename);
      aFileConsumer.accept (aFilePath.toFile ());
      return new NonBlockingBufferedOutputStream (Files.newOutputStream (aFilePath));
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to create file '" +
                                       sFilename +
                                       "' in " +
                                       aEffectiveBaseDir.getAbsolutePath () +
                                       "'",
                                       ex);
    }
  }

  @NonNull
  public static OutputStream openTemporaryDocumentStream (@NonNull final File aBaseDir,
                                                          @NonNull final OffsetDateTime aReferenceDT,
                                                          @NonNull final Consumer <File> aFileConsumer)
  {
    return openDocumentStream (aBaseDir, aReferenceDT, UUID.randomUUID ().toString () + ".tmp", aFileConsumer);
  }

  /**
   * Read all bytes from the file at the given absolute path.
   *
   * @param sAbsolutePath
   *        The absolute path of the file. May not be <code>null</code>.
   * @return The file contents as a byte array.
   */
  public static byte @NonNull [] readDocument (@NonNull final String sAbsolutePath)
  {
    ValueEnforcer.notNull (sAbsolutePath, "AbsolutePath");

    try
    {
      return Files.readAllBytes (Path.of (sAbsolutePath));
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to read document from '" + sAbsolutePath + "'", ex);
    }
  }

  /**
   * Open an {@link InputStream} for the file at the given absolute path. The caller is responsible
   * for closing the stream.
   *
   * @param sAbsolutePath
   *        The absolute path of the file. May not be <code>null</code>.
   * @return An open input stream.
   */
  @NonNull
  public static InputStream openDocumentStream (@NonNull final String sAbsolutePath)
  {
    ValueEnforcer.notNull (sAbsolutePath, "AbsolutePath");

    try
    {
      return StreamHelper.getBuffered (Files.newInputStream (Path.of (sAbsolutePath)));
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to open document stream for '" + sAbsolutePath + "'", ex);
    }
  }

  /**
   * Delete the document file at the given path.
   *
   * @param sAbsolutePath
   *        The absolute path of the file. May not be <code>null</code>.
   * @return <code>true</code> if the file was deleted, <code>false</code> if it did not exist.
   */
  public static boolean deleteDocument (@NonNull final String sAbsolutePath)
  {
    ValueEnforcer.notNull (sAbsolutePath, "AbsolutePath");

    try
    {
      return Files.deleteIfExists (Path.of (sAbsolutePath));
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to delete document at '" + sAbsolutePath + "'", ex);
    }
  }
}
