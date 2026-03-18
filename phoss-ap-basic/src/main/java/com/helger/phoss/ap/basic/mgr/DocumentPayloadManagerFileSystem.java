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
package com.helger.phoss.ap.basic.mgr;

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
import com.helger.annotation.misc.DevelopersNote;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.IFileOperationManager;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.basic.APBasicConfig;

/**
 * Default implementation of {@link IDocumentPayloadManager} that stores
 * documents as flat files on the local filesystem, organized by date/hour
 * directories.
 *
 * @author Philip Helger
 */
public class DocumentPayloadManagerFileSystem implements IDocumentPayloadManager
{
  /**
   * Default constructor.
   */
  public DocumentPayloadManagerFileSystem ()
  {}

  /**
   * Verify that the configured inbound and outbound storage paths exist, are
   * writable, and can be created if missing. Throws an
   * {@link InitializationException} if any check fails.
   */
  public void verifyConfiguration ()
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
   * Store a document as a file on the filesystem under a date/hour organized
   * directory structure.
   *
   * @param sBaseDir
   *        The base directory for storage. May not be <code>null</code>.
   * @param aReferenceDT
   *        The reference date/time used to derive the subdirectory path. May
   *        not be <code>null</code>.
   * @param sFilename
   *        The filename to use. May not be <code>null</code>.
   * @param aBytes
   *        The document content as byte array. May not be <code>null</code>.
   * @return The absolute path of the stored file. Never <code>null</code>.
   */
  @NonNull
  public String storeDocument (@NonNull final String sBaseDir,
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

  /**
   * Open an {@link OutputStream} for writing a document to the filesystem. The
   * file is placed under a date/hour organized directory structure with a
   * unique filename.
   *
   * @param sBaseDir
   *        The base directory for storage. May not be <code>null</code>.
   * @param aReferenceDT
   *        The reference date/time used to derive the subdirectory path. May
   *        not be <code>null</code>.
   * @param sFilename
   *        The base filename (without extension). May not be empty.
   * @param sFileExt
   *        The file extension including the leading dot (e.g.
   *        {@code ".xml"}). May not be empty.
   * @param aPathConsumer
   *        A consumer that receives the absolute path of the created file.
   *        May not be <code>null</code>.
   * @return A buffered {@link OutputStream} for writing. Never
   *         <code>null</code>.
   */
  @NonNull
  public OutputStream openDocumentStreamForWrite (@NonNull final String sBaseDir,
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

  /**
   * Open an {@link OutputStream} for writing a temporary document with a random
   * UUID-based filename and {@code .tmp} extension.
   *
   * @param sBaseDir
   *        The base directory for storage. May not be <code>null</code>.
   * @param aReferenceDT
   *        The reference date/time used to derive the subdirectory path. May
   *        not be <code>null</code>.
   * @param aPathConsumer
   *        A consumer that receives the absolute path of the created
   *        temporary file. May not be <code>null</code>.
   * @return A buffered {@link OutputStream} for writing. Never
   *         <code>null</code>.
   */
  @NonNull
  public OutputStream openTemporaryDocumentStreamForWrite (@NonNull final String sBaseDir,
                                                           @NonNull final OffsetDateTime aReferenceDT,
                                                           @NonNull final Consumer <String> aPathConsumer)
  {
    // Should be always unique
    return openDocumentStreamForWrite (sBaseDir, aReferenceDT, UUID.randomUUID ().toString (), ".tmp", aPathConsumer);
  }

  /**
   * Rename a file to a new unique name in the target directory.
   *
   * @param sSrcFile
   *        The absolute path of the source file. May not be
   *        <code>null</code>.
   * @param sTargetDir
   *        The target directory. May not be <code>null</code>.
   * @param sBaseName
   *        The desired base filename (without extension). May not be empty.
   * @param sFileExt
   *        The file extension including the leading dot. May not be empty.
   * @return The absolute path of the destination file after renaming. Never
   *         <code>null</code>.
   */
  @NonNull
  public String renameFile (@NonNull final String sSrcFile,
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
    return aDstFile.getAbsolutePath ();
  }

  /**
   * Read an entire document from the filesystem into a byte array.
   *
   * @param sAbsolutePath
   *        The absolute path of the file to read. May not be
   *        <code>null</code>.
   * @return The file contents as byte array. Never <code>null</code>.
   */
  public byte @NonNull [] readDocument (@NonNull final String sAbsolutePath)
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
   * Open a buffered {@link InputStream} for reading a document from the
   * filesystem.
   *
   * @param sAbsolutePath
   *        The absolute path of the file to read. May not be
   *        <code>null</code>.
   * @return A buffered {@link InputStream}. Never <code>null</code>.
   */
  @NonNull
  public InputStream openDocumentStreamForRead (@NonNull final String sAbsolutePath)
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
   * Delete a document from the filesystem if it exists.
   *
   * @param sAbsolutePath
   *        The absolute path of the file to delete. May not be
   *        <code>null</code>.
   * @return {@code true} if the file existed and was deleted, {@code false} if
   *         it did not exist.
   */
  public boolean deleteDocument (@NonNull final String sAbsolutePath)
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
   * Check whether a document exists at the specified path.
   *
   * @param sAbsolutePath
   *        The absolute path to check. May not be <code>null</code>.
   * @return {@code true} if the file exists, {@code false} otherwise.
   */
  public boolean existsDocument (@NonNull final String sAbsolutePath)
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
