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

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.misc.DevelopersNote;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.IFileOperationManager;
import com.helger.phoss.ap.basic.APBasicConfig;

/**
 * Utility class for reading and writing document files on disk. Documents are
 * stored as flat files rather than as BYTEA columns in the database.
 *
 * @author Philip Helger
 */
@Immutable
public final class DocumentStorageHelper
{
  private DocumentStorageHelper ()
  {}

  public static void verifyConfiguration ()
  {
    final IFileOperationManager aFOM = FileOperationManager.INSTANCE;

    {
      final String sInboundPath = APBasicConfig.getStorageInboundPath ();
      if (StringHelper.isEmpty (sInboundPath))
        throw new InitializationException ("No Storage Inbound Path provided");
      final File aInboundPath = new File (sInboundPath);
      if (aFOM.createDirRecursiveIfNotExisting (aInboundPath).isFailure ())
        throw new InitializationException ("Failed to create the Storage Inbound Path '" + sInboundPath + "'");
      if (!aInboundPath.canWrite ())
        throw new InitializationException ("The Storage Inbound Path '" +
                                           sInboundPath +
                                           "' is not writable by the application user");
    }

    {
      final String sOutboundPath = APBasicConfig.getStorageOutboundPath ();
      if (StringHelper.isEmpty (sOutboundPath))
        throw new InitializationException ("No Storage Outbound Path provided");
      final File aOutboundPath = new File (sOutboundPath);
      if (aFOM.createDirRecursiveIfNotExisting (aOutboundPath).isFailure ())
        throw new InitializationException ("Failed to create the Storage Outbound Path '" + sOutboundPath + "'");
      if (!aOutboundPath.canWrite ())
        throw new InitializationException ("The Storage Outbound Path '" +
                                           sOutboundPath +
                                           "' is not writable by the application user");
    }
  }

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
   * Write bytes to a file under the given base directory, using the provided
   * filename. Creates the directory if needed.
   *
   * @param sBaseDir
   *        The base directory to store the file in. May not be
   *        <code>null</code>.
   * @param aReferenceDT
   *        The reference date time to which the message should be stored.
   * @param sFilename
   *        The filename to use. May not be <code>null</code>.
   * @param aBytes
   *        The bytes to write. May not be <code>null</code>.
   * @return The absolute path of the stored file.
   */
  @NonNull
  public static String storeDocument (@NonNull final String sBaseDir,
                                      @NonNull final OffsetDateTime aReferenceDT,
                                      @NonNull final String sFilename,
                                      final byte @NonNull [] aBytes)
  {
    ValueEnforcer.notNull (sBaseDir, "BaseDir");
    ValueEnforcer.notNull (aReferenceDT, "ReferenceDT");
    ValueEnforcer.notNull (sFilename, "Filename");
    ValueEnforcer.notNull (aBytes, "Bytes");

    final File aEffectiveBaseDir = _getStorageDir (new File (sBaseDir), aReferenceDT);
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
  private static File _ensureUniqueFile (@NonNull final File aBaseDir,
                                         @NonNull @Nonempty final String sFilename,
                                         @NonNull @Nonempty final String sFileExt)
  {
    File aFilePath = new File (aBaseDir, sFilename + sFileExt);

    // Make sure the file does not exist yet
    if (aFilePath.exists ())
    {
      // Filename is not unique
      int nSuffix = 1;
      do
      {
        aFilePath = new File (aBaseDir, sFilename + "-" + nSuffix + sFileExt);
        nSuffix++;
        if (nSuffix >= 1_000)
        {
          // Avoid endless loop
          throw new IllegalStateException ("The filename '" +
                                           sFileExt +
                                           "' exists alreay with too many suffixes (" +
                                           nSuffix +
                                           ")");
        }
      } while (aFilePath.exists ());
    }
    return aFilePath;
  }

  @NonNull
  public static OutputStream openDocumentStream (@NonNull final String sBaseDir,
                                                 @NonNull final OffsetDateTime aReferenceDT,
                                                 @NonNull @DevelopersNote final String sFilename,
                                                 @NonNull final String sFileExt,
                                                 @NonNull final Consumer <String> aPathConsumer)
  {
    ValueEnforcer.notNull (sBaseDir, "BaseDir");
    ValueEnforcer.notNull (aReferenceDT, "ReferenceDT");
    ValueEnforcer.notEmpty (sFilename, "Filename");
    ValueEnforcer.notEmpty (sFileExt, "FileExt");
    ValueEnforcer.isTrue ( () -> sFileExt.startsWith ("."), "FileExt must start with a dot");
    ValueEnforcer.notNull (aPathConsumer, "PathConsumer");

    final File aEffectiveBaseDir = _getStorageDir (new File (sBaseDir), aReferenceDT);
    try
    {
      // Create base directory structure if needed
      Files.createDirectories (aEffectiveBaseDir.toPath ());

      // Get the absolute path
      final File aFilePath = _ensureUniqueFile (aEffectiveBaseDir, sFilename, sFileExt);

      aPathConsumer.accept (aFilePath.getAbsolutePath ());
      return FileHelper.getBufferedOutputStream (aFilePath);
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
  public static OutputStream openTemporaryDocumentStream (@NonNull final String sBaseDir,
                                                          @NonNull final OffsetDateTime aReferenceDT,
                                                          @NonNull final Consumer <String> aPathConsumer)
  {
    // Should be always unique
    return openDocumentStream (sBaseDir, aReferenceDT, UUID.randomUUID ().toString (), ".tmp", aPathConsumer);
  }

  @NonNull
  public static File renameFile (@NonNull final String sSrcFile,
                                 @NonNull final String sTargetDir,
                                 @NonNull @Nonempty final String sBaseName,
                                 @NonNull @Nonempty final String sFileExt)
  {
    final File aSrcFile = new File (sSrcFile);
    final File aDstFile = _ensureUniqueFile (new File (sTargetDir), sBaseName, sFileExt);

    if (FileOperationManager.INSTANCE.renameFile (aSrcFile, aDstFile).isFailure ())
      throw new IllegalStateException ("Failed to rename file '" +
                                       aSrcFile.getAbsolutePath () +
                                       "' to '" +
                                       aDstFile.getAbsolutePath () +
                                       "'");
    return aDstFile;
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
   * Open an {@link InputStream} for the file at the given absolute path. The
   * caller is responsible for closing the stream.
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
   * @return <code>true</code> if the file was deleted, <code>false</code> if it
   *         did not exist.
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

  /**
   * Check if the document file at the given path exists or not.
   *
   * @param sAbsolutePath
   *        The absolute path of the file. May not be <code>null</code>.
   * @return <code>true</code> if the file exists, <code>false</code> if it does
   *         not exist.
   */
  public static boolean existsDocument (@NonNull final String sAbsolutePath)
  {
    ValueEnforcer.notNull (sAbsolutePath, "AbsolutePath");

    try
    {
      return Files.exists (Path.of (sAbsolutePath));
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to check existence of document at '" + sAbsolutePath + "'", ex);
    }
  }
}
