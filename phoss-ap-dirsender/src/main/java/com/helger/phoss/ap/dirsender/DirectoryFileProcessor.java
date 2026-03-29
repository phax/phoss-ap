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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.string.StringHelper;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.FilenameHelper;
import com.helger.io.file.SimpleFileIO;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.outbound.OutboundOrchestrator;
import com.helger.phoss.ap.db.APJdbcMetaManager;

/**
 * Processes a single SBD file from the watch directory: moves it to the pending directory, submits
 * it via the outbound pipeline, and moves it to the success or error directory based on the
 * outcome.
 *
 * @author Philip Helger
 * @since v0.1.4
 */
@Immutable
public final class DirectoryFileProcessor
{
  static final String DIR_PENDING = "pending";
  static final String DIR_SUCCESS = "success";
  static final String DIR_ERROR = "error";

  private static final Logger LOGGER = LoggerFactory.getLogger (DirectoryFileProcessor.class);

  private DirectoryFileProcessor ()
  {}

  /**
   * Move a file to the target directory. If a file with the same name already exists, a numeric
   * suffix is appended (e.g. {@code file-1.xml}, {@code file-2.xml}).
   *
   * @param aFile
   *        The file to move. May not be <code>null</code>.
   * @param aTargetDir
   *        The target directory. May not be <code>null</code>.
   * @return The moved file, or <code>null</code> if the move failed.
   */
  @Nullable
  @VisibleForTesting
  static File _moveFileToDir (@NonNull final File aFile, @NonNull final File aTargetDir)
  {
    final String sBaseName = FilenameHelper.getBaseName (aFile.getName ());
    final String sExtension = FilenameHelper.getExtension (aFile.getName ());
    final String sDotExt = StringHelper.isNotEmpty (sExtension) ? "." + sExtension : "";

    File aTarget = new File (aTargetDir, aFile.getName ());
    int nSuffix = 0;
    while (aTarget.exists ())
    {
      nSuffix++;
      aTarget = new File (aTargetDir, sBaseName + "-" + nSuffix + sDotExt);
    }

    if (FileOperationManager.INSTANCE.renameFile (aFile, aTarget).isSuccess ())
      return aTarget;

    LOGGER.error ("Failed to move file '" + aFile.getAbsolutePath () + "' to '" + aTarget.getAbsolutePath () + "'");
    return null;
  }

  /**
   * Write a JSON result file alongside the processed file.
   *
   * @param aTargetDir
   *        The directory to write to. May not be <code>null</code>.
   * @param sTargetFilename
   *        The target filename to write. May not be <code>null</code>.
   * @param aJson
   *        The JSON object to write. May not be <code>null</code>.
   */
  @VisibleForTesting
  static void _writeResultJson (@NonNull final File aTargetDir,
                                @NonNull final String sTargetFilename,
                                @NonNull final IJsonObject aJson)
  {
    final File aJsonFile = new File (aTargetDir, sTargetFilename);
    final String sJson = new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aJson);
    if (SimpleFileIO.writeFile (aJsonFile, sJson, StandardCharsets.UTF_8).isFailure ())
      LOGGER.error ("Failed to write result JSON to '" + aJsonFile.getAbsolutePath () + "'");
  }

  /**
   * Parse an SBD file to extract the SBDH Instance Identifier without fully consuming the stream
   * for storage.
   *
   * @param aFile
   *        The SBD file. May not be <code>null</code>.
   * @return The SBDH Instance Identifier, or <code>null</code> if parsing failed.
   */
  @Nullable
  @VisibleForTesting
  static String _parseSbdhInstanceID (@NonNull final File aFile)
  {
    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();
    try (final InputStream aIS = FileHelper.getBufferedInputStream (aFile))
    {
      final PeppolSBDHData aSbdData = new PeppolSBDHDataReader (aIF).extractData (aIS);
      return aSbdData != null ? aSbdData.getInstanceIdentifier () : null;
    }
    catch (final Exception ex)
    {
      LOGGER.warn ("Failed to parse SBDH Instance ID from '" + aFile.getAbsolutePath () + "': " + ex.getMessage ());
      return null;
    }
  }

  @NonNull
  private static IJsonObject _createSuccessJson (@NonNull final String sSourceFile,
                                                 @NonNull final IOutboundTransaction aTx)
  {
    return new JsonObject ().add ("success", true)
                            .add ("sourceFile", sSourceFile)
                            .add ("sbdhInstanceID", aTx.getSbdhInstanceID ())
                            .add ("transactionID", aTx.getID ())
                            .add ("status", aTx.getStatus ().getID ())
                            .add ("processedDT",
                                  APBasicMetaManager.getTimestampMgr ().getCurrentDateTime ().toString ());
  }

  @NonNull
  private static IJsonObject _createErrorJson (@NonNull final String sSourceFile,
                                               @Nullable final String sSbdhInstanceID,
                                               @Nullable final String sTransactionID,
                                               @Nullable final String sStatus,
                                               @NonNull final String sErrorDetails)
  {
    final IJsonObject ret = new JsonObject ().add ("success", false).add ("sourceFile", sSourceFile);
    if (sSbdhInstanceID != null)
      ret.add ("sbdhInstanceID", sSbdhInstanceID);
    if (sTransactionID != null)
      ret.add ("transactionID", sTransactionID);
    if (sStatus != null)
      ret.add ("status", sStatus);
    ret.add ("errorDetails", sErrorDetails);
    ret.add ("processedDT", APBasicMetaManager.getTimestampMgr ().getCurrentDateTime ().toString ());
    return ret;
  }

  /**
   * Process a single SBD file from the watch directory.
   *
   * @param aWatchDir
   *        The watch directory. May not be <code>null</code>.
   * @param aFile
   *        The SBD file to process. May not be <code>null</code>.
   */
  public static void processFile (@NonNull final File aWatchDir, @NonNull final File aFile)
  {
    final String sLogPrefix = "[DirSender] ";
    final String sOriginalName = aFile.getName ();
    LOGGER.info (sLogPrefix + "Processing file '" + sOriginalName + "'");

    final File aPendingDir = new File (aWatchDir, DIR_PENDING);
    final File aSuccessDir = new File (aWatchDir, DIR_SUCCESS);
    final File aErrorDir = new File (aWatchDir, DIR_ERROR);

    // Step 1: Move to pending
    final File aPendingFile = _moveFileToDir (aFile, aPendingDir);
    if (aPendingFile == null)
      return;

    // Step 2: Submit pre-built SBD
    final IOutboundTransaction aTx;
    try (final InputStream aIS = FileHelper.getBufferedInputStream (aPendingFile))
    {
      aTx = OutboundOrchestrator.submitPrebuiltSBD (sLogPrefix, aIS, null);
    }
    catch (final Exception ex)
    {
      LOGGER.error (sLogPrefix + "Failed to read file '" + aPendingFile.getAbsolutePath () + "'", ex);
      final File aMoved = _moveFileToDir (aPendingFile, aErrorDir);
      if (aMoved != null)
        _writeResultJson (aErrorDir,
                          aMoved.getName () + ".json",
                          _createErrorJson (sOriginalName, null, null, null, ex.getMessage ()));
      return;
    }

    if (aTx == null)
    {
      // Parse failure
      LOGGER.error (sLogPrefix + "Failed to parse SBD from '" + sOriginalName + "'");
      final File aMoved = _moveFileToDir (aPendingFile, aErrorDir);
      if (aMoved != null)
        _writeResultJson (aErrorDir,
                          aMoved.getName () + ".json",
                          _createErrorJson (sOriginalName, null, null, null, "Failed to parse SBD"));
      return;
    }

    // Step 3: Send via AS4
    final Phase4PeppolSendingReport aSendingReport = OutboundOrchestrator.processPendingOutbound (sLogPrefix, aTx);

    // Step 4: Re-read transaction to get final status
    final IOutboundTransaction aTxFinal = APJdbcMetaManager.getOutboundTransactionMgr ().getByID (aTx.getID ());
    final EOutboundStatus eStatus = aTxFinal != null ? aTxFinal.getStatus () : EOutboundStatus.FAILED;

    if (eStatus == EOutboundStatus.SENT)
    {
      final File aMoved = _moveFileToDir (aPendingFile, aSuccessDir);
      if (aMoved != null)
        _writeResultJson (aSuccessDir, aMoved.getName () + ".json", _createSuccessJson (sOriginalName, aTxFinal));
      _writeResultJson (aSuccessDir, aMoved.getName () + ".report.json", aSendingReport.getAsJsonObject ());
      LOGGER.info (sLogPrefix + "Successfully sent '" + sOriginalName + "' (tx=" + aTx.getID () + ")");
    }
    else
      if (eStatus.isFinalState ())
      {
        // PERMANENTLY_FAILED or REJECTED
        final String sError = aTxFinal != null && aTxFinal.getErrorDetails () != null ? aTxFinal.getErrorDetails ()
                                                                                      : "Sending failed with status " +
                                                                                        eStatus.getID ();
        final File aMoved = _moveFileToDir (aPendingFile, aErrorDir);
        if (aMoved != null)
          _writeResultJson (aErrorDir,
                            aMoved.getName () + ".json",
                            _createErrorJson (sOriginalName,
                                              aTxFinal != null ? aTxFinal.getSbdhInstanceID () : null,
                                              aTx.getID (),
                                              eStatus.getID (),
                                              sError));
        _writeResultJson (aErrorDir, aMoved.getName () + ".report.json", aSendingReport.getAsJsonObject ());
        LOGGER.warn (sLogPrefix + "Failed to send '" + sOriginalName + "' (tx=" + aTx.getID () + "): " + sError);
      }
      else
      {
        // Non-final (PENDING, FAILED) — leave in pending for cleanup cycle
        LOGGER.info (sLogPrefix +
                     "Transaction '" +
                     aTx.getID () +
                     "' for file '" +
                     sOriginalName +
                     "' is in non-final state '" +
                     eStatus.getID () +
                     "' — leaving in pending for cleanup");
      }
  }

  /**
   * Recover a file found in the pending directory at startup. Checks the DB to determine the
   * correct target directory.
   *
   * @param aWatchDir
   *        The watch directory. May not be <code>null</code>.
   * @param aPendingFile
   *        The file in the pending directory. May not be <code>null</code>.
   */
  public static void recoverPendingFile (@NonNull final File aWatchDir, @NonNull final File aPendingFile)
  {
    final String sLogPrefix = "[DirSender-Recovery] ";
    final String sFileName = aPendingFile.getName ();

    final File aSuccessDir = new File (aWatchDir, DIR_SUCCESS);
    final File aErrorDir = new File (aWatchDir, DIR_ERROR);

    final String sSbdhInstanceID = _parseSbdhInstanceID (aPendingFile);
    if (sSbdhInstanceID == null)
    {
      // Cannot parse — move to error
      LOGGER.warn (sLogPrefix + "Cannot parse SBDH from pending file '" + sFileName + "' — moving to error");

      final File aMoved = _moveFileToDir (aPendingFile, aErrorDir);
      if (aMoved != null)
        _writeResultJson (aErrorDir,
                          aMoved.getName () + ".recovery.json",
                          _createErrorJson (sFileName, null, null, null, "Cannot parse SBDH during recovery"));
      return;
    }

    final IOutboundTransaction aTx = APJdbcMetaManager.getOutboundTransactionMgr ()
                                                      .getBySbdhInstanceID (sSbdhInstanceID);
    if (aTx == null)
    {
      // No DB entry — move back to watch directory for reprocessing
      LOGGER.info (sLogPrefix +
                   "No DB entry for '" +
                   sFileName +
                   "' (SBDH ID '" +
                   sSbdhInstanceID +
                   "') — moving back for reprocessing");
      _moveFileToDir (aPendingFile, aWatchDir);
      return;
    }

    final EOutboundStatus eStatus = aTx.getStatus ();
    if (eStatus == EOutboundStatus.SENT)
    {
      LOGGER.info (sLogPrefix + "Recovering '" + sFileName + "' to success (status=" + eStatus.getID () + ")");

      final File aMoved = _moveFileToDir (aPendingFile, aSuccessDir);
      if (aMoved != null)
        _writeResultJson (aSuccessDir, aMoved.getName () + ".json", _createSuccessJson (sFileName, aTx));
    }
    else
      if (eStatus == EOutboundStatus.PERMANENTLY_FAILED || eStatus == EOutboundStatus.REJECTED)
      {
        final String sError = aTx.getErrorDetails () != null ? aTx.getErrorDetails () : "Status " + eStatus.getID ();
        LOGGER.info (sLogPrefix + "Recovering '" + sFileName + "' to error (status=" + eStatus.getID () + ")");

        final File aMoved = _moveFileToDir (aPendingFile, aErrorDir);
        if (aMoved != null)
          _writeResultJson (aErrorDir,
                            aMoved.getName () + ".json",
                            _createErrorJson (sFileName, sSbdhInstanceID, aTx.getID (), eStatus.getID (), sError));
      }
      else
      {
        // Non-final — leave in pending, RetryScheduler will handle it
        // The file itself will be recovered in one of the next rounds of recovery
        LOGGER.warn (sLogPrefix +
                     "Pending file '" +
                     sFileName +
                     "' has non-final DB status '" +
                     eStatus.getID () +
                     "' — leaving in pending for cleanup");
      }
  }

  /**
   * Cleanup a file in the pending directory whose DB transaction may have reached a final state
   * since it was left there. Called periodically by the scan task.
   *
   * @param aWatchDir
   *        The watch directory. May not be <code>null</code>.
   * @param aPendingFile
   *        The file in the pending directory. May not be <code>null</code>.
   */
  public static void cleanupPendingFile (@NonNull final File aWatchDir, @NonNull final File aPendingFile)
  {
    final String sLogPrefix = "[DirSender-Cleanup] ";
    final String sFileName = aPendingFile.getName ();

    final String sSbdhInstanceID = _parseSbdhInstanceID (aPendingFile);
    if (sSbdhInstanceID == null)
      return;

    final IOutboundTransaction aTx = APJdbcMetaManager.getOutboundTransactionMgr ()
                                                      .getBySbdhInstanceID (sSbdhInstanceID);
    if (aTx == null)
      return;

    final EOutboundStatus eStatus = aTx.getStatus ();
    if (!eStatus.isFinalState ())
      return;

    // Transaction reached a final state — move the file
    final File aSuccessDir = new File (aWatchDir, DIR_SUCCESS);
    final File aErrorDir = new File (aWatchDir, DIR_ERROR);

    if (eStatus == EOutboundStatus.SENT)
    {
      LOGGER.info (sLogPrefix + "Moving '" + sFileName + "' to success (status=" + eStatus.getID () + ")");

      final File aMoved = _moveFileToDir (aPendingFile, aSuccessDir);
      if (aMoved != null)
        _writeResultJson (aSuccessDir, aMoved.getName () + ".json", _createSuccessJson (sFileName, aTx));
    }
    else
    {
      final String sError = aTx.getErrorDetails () != null ? aTx.getErrorDetails () : "Status " + eStatus.getID ();
      LOGGER.info (sLogPrefix + "Moving '" + sFileName + "' to error (status=" + eStatus.getID () + ")");

      final File aMoved = _moveFileToDir (aPendingFile, aErrorDir);
      if (aMoved != null)
        _writeResultJson (aErrorDir,
                          aMoved.getName () + ".json",
                          _createErrorJson (sFileName, sSbdhInstanceID, aTx.getID (), eStatus.getID (), sError));
    }
  }
}
