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
package com.helger.phoss.ap.forwarding.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.FilenameHelper;
import com.helger.io.file.SimpleFileIO;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phoss.ap.api.codelist.EForwardingFilesystemLayout;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.dto.InboundTransactionResponse;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.api.otel.PhossAPTelemetry;
import com.helger.phoss.ap.basic.APBasicMetaManager;

import io.opentelemetry.api.trace.SpanKind;

/**
 * Implementation of {@link IDocumentForwarder} that writes the received SBD to a local filesystem
 * directory. Supports two layouts: {@code flat} (all files in a single directory) and
 * {@code per-transaction} (one subdirectory per transaction). A metadata JSON file is written
 * alongside each SBD.
 *
 * @author Philip Helger
 * @since v0.2.0
 */
public class FilesystemDocumentForwarder implements IDocumentForwarder
{
  private static final Logger LOGGER = LoggerFactory.getLogger (FilesystemDocumentForwarder.class);
  private static final int MAX_UNIQUENESS_TRIES = 1_000;

  private File m_aBaseDirectory;
  private EForwardingFilesystemLayout m_eLayout;

  /** {@inheritDoc} */
  @NonNull
  public ESuccess initFromConfiguration (@NonNull final IConfigWithFallback aConfig)
  {
    final String sDirectory = aConfig.getAsString (APConfigurationProperties.FORWARDING_FILESYSTEM_DIRECTORY);
    if (StringHelper.isEmpty (sDirectory))
    {
      LOGGER.error ("The forwarding filesystem directory is missing");
      return ESuccess.FAILURE;
    }

    m_aBaseDirectory = new File (sDirectory);
    FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (m_aBaseDirectory);
    if (!m_aBaseDirectory.isDirectory ())
    {
      LOGGER.error ("The forwarding filesystem directory '" + sDirectory + "' could not be created");
      return ESuccess.FAILURE;
    }

    final String sLayout = aConfig.getAsString (APConfigurationProperties.FORWARDING_FILESYSTEM_LAYOUT,
                                                APConfigurationProperties.FORWARDING_FILESYSTEM_LAYOUT_DEFAULT);
    m_eLayout = EForwardingFilesystemLayout.getFromIDOrDefault (sLayout);

    return ESuccess.SUCCESS;
  }

  /**
   * Generate a unique base name from the SBDH Instance ID. If a file with that name already exists,
   * append a numeric suffix.
   */
  @NonNull
  private static String _getUniqueBaseName (@NonNull final File aTargetDir, @NonNull final String sSbdhInstanceID)
  {
    final String sSafeID = FilenameHelper.getAsSecureValidASCIIFilename (sSbdhInstanceID);

    // Check if the base name is already in use
    if (!new File (aTargetDir, sSafeID + ".xml").exists () && !new File (aTargetDir, sSafeID).exists ())
      return sSafeID;

    // Append numeric suffix until unique
    for (int nSuffix = 1;; nSuffix++)
    {
      final String sCandidate = sSafeID + "-" + nSuffix;
      if (!new File (aTargetDir, sCandidate + ".xml").exists () && !new File (aTargetDir, sCandidate).exists ())
        return sCandidate;

      if (nSuffix >= MAX_UNIQUENESS_TRIES)
      {
        // Avoid endless loop
        throw new IllegalStateException ("The filename '" +
                                         sSafeID +
                                         "' exists alreay with too many suffixes (" +
                                         nSuffix +
                                         ")");
      }
    }
  }

  private static void _writeMetadataJson (@NonNull final File aJsonFile, @NonNull final IInboundTransaction aTx)
  {
    final String sJson = InboundTransactionResponse.fromDomain (aTx)
                                                   .getAsJson ()
                                                   .getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);
    if (SimpleFileIO.writeFile (aJsonFile, sJson, StandardCharsets.UTF_8).isFailure ())
      LOGGER.error ("Failed to write metadata JSON to '" + aJsonFile.getAbsolutePath () + "'");
  }

  @NonNull
  private ForwardingResult _forwardFlat (@NonNull final IDocumentPayloadManager aDocPayloadMgr,
                                         @NonNull final IInboundTransaction aTx,
                                         @NonNull final String sBaseName) throws IOException
  {
    final File aTmpFile = new File (m_aBaseDirectory, sBaseName + ".xml.tmp");
    final File aFinalFile = new File (m_aBaseDirectory, sBaseName + ".xml");
    final File aJsonFile = new File (m_aBaseDirectory, sBaseName + ".json");

    // Atomic write: .tmp → .xml
    try (final InputStream aIS = aDocPayloadMgr.openDocumentStreamForRead (aTx.getDocumentPath ());
         final OutputStream aOS = FileHelper.getBufferedOutputStream (aTmpFile))
    {
      if (aOS == null)
        return ForwardingResult.failure ("filesystem_io_error",
                                         "Failed to open temporary file '" + aTmpFile.getAbsolutePath () + "'");

      if (StreamHelper.copyByteStream ().from (aIS).closeFrom (false).to (aOS).closeTo (false).build ().isFailure ())
        return ForwardingResult.failure ("filesystem_io_error",
                                         "Failed to copy data to temporary file '" + aTmpFile.getAbsolutePath () + "'");
    }

    if (FileOperationManager.INSTANCE.renameFile (aTmpFile, aFinalFile).isFailure ())
    {
      FileOperationManager.INSTANCE.deleteFile (aTmpFile);
      return ForwardingResult.failure ("filesystem_io_error",
                                       "Failed to rename temporary file to '" + aFinalFile.getAbsolutePath () + "'");
    }

    LOGGER.info ("Forwarded transaction '" + aTx.getID () + "' to filesystem: '" + aFinalFile.getAbsolutePath () + "'");

    _writeMetadataJson (aJsonFile, aTx);
    return ForwardingResult.success ();
  }

  @NonNull
  private ForwardingResult _forwardPerTransaction (@NonNull final IDocumentPayloadManager aDocPayloadMgr,
                                                   @NonNull final IInboundTransaction aTx,
                                                   @NonNull final String sBaseName) throws IOException
  {
    final File aTxDir = new File (m_aBaseDirectory, sBaseName);
    FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (aTxDir);

    final File aTmpFile = new File (aTxDir, "sbd.xml.tmp");
    final File aFinalFile = new File (aTxDir, "sbd.xml");
    final File aJsonFile = new File (aTxDir, "metadata.json");

    // Atomic write: .tmp → .xml
    try (final InputStream aIS = aDocPayloadMgr.openDocumentStreamForRead (aTx.getDocumentPath ());
         final OutputStream aOS = FileHelper.getBufferedOutputStream (aTmpFile))
    {
      if (aOS == null)
        return ForwardingResult.failure ("filesystem_io_error",
                                         "Failed to open temporary file '" + aTmpFile.getAbsolutePath () + "'");

      if (StreamHelper.copyByteStream ().from (aIS).closeFrom (false).to (aOS).closeTo (false).build ().isFailure ())
        return ForwardingResult.failure ("filesystem_io_error",
                                         "Failed to copy data to temporary file '" + aTmpFile.getAbsolutePath () + "'");
    }

    if (FileOperationManager.INSTANCE.renameFile (aTmpFile, aFinalFile).isFailure ())
    {
      FileOperationManager.INSTANCE.deleteFile (aTmpFile);
      return ForwardingResult.failure ("filesystem_io_error",
                                       "Failed to rename temporary file to '" + aFinalFile.getAbsolutePath () + "'");
    }

    LOGGER.info ("Forwarded transaction '" + aTx.getID () + "' to filesystem: '" + aFinalFile.getAbsolutePath () + "'");

    _writeMetadataJson (aJsonFile, aTx);
    return ForwardingResult.success ();
  }

  /** {@inheritDoc} */
  @NonNull
  public ForwardingResult forwardDocument (@NonNull final IInboundTransaction aTransaction)
  {
    return PhossAPTelemetry.withSpan (PhossAPTelemetry.tracer ()
                                                      .spanBuilder (CPhossAPOtel.SPAN_FORWARDER_DISPATCH)
                                                      .setSpanKind (SpanKind.CLIENT)
                                                      .setAttribute (CPhossAPOtel.ATTR_FORWARDER_TYPE, "filesystem")
                                                      .setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID,
                                                                     aTransaction.getID ()),
                                      () -> _doForwardDocument (aTransaction));
  }

  @NonNull
  private ForwardingResult _doForwardDocument (@NonNull final IInboundTransaction aTransaction)
  {
    final IDocumentPayloadManager aDocPayloadMgr = APBasicMetaManager.getDocPayloadMgr ();
    final String sBaseName = _getUniqueBaseName (m_aBaseDirectory, aTransaction.getSbdhInstanceID ());

    try
    {
      return switch (m_eLayout)
      {
        case FLAT -> _forwardFlat (aDocPayloadMgr, aTransaction, sBaseName);
        case PER_TRANSACTION -> _forwardPerTransaction (aDocPayloadMgr, aTransaction, sBaseName);
      };
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Filesystem forwarding failed for transaction '" + aTransaction.getID () + "'", ex);
      return ForwardingResult.failure ("filesystem_io_error",
                                       ex.getMessage () + " (" + ex.getClass ().getName () + ")");
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Filesystem forwarding failed for transaction '" + aTransaction.getID () + "'", ex);
      return ForwardingResult.failure ("filesystem_error", ex.getMessage () + " (" + ex.getClass ().getName () + ")");
    }
  }

  /** {@inheritDoc} */
  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("BaseDirectory", m_aBaseDirectory)
                                       .append ("Layout", m_eLayout)
                                       .getToString ();
  }
}
