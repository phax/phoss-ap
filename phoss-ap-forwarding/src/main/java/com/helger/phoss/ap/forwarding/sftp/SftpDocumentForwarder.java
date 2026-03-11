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
package com.helger.phoss.ap.forwarding.sftp;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.base.io.iface.IHasInputStream;
import com.helger.base.io.stream.HasInputStream;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.io.file.FilenameHelper;
import com.helger.jsch.sftp.ChannelSftpHelper;
import com.helger.network.WebExceptionHelper;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.api.spi.IDocumentForwarder;
import com.helger.phoss.ap.basic.storage.DocumentStorageHelper;
import com.helger.photon.connect.sftp.AbstractChannelSftpRunnable;
import com.helger.photon.connect.sftp.ISftpSettings;
import com.helger.photon.connect.sftp.SftpMaxParallelRunner;
import com.helger.photon.connect.sftp.SftpSettings;
import com.helger.photon.connect.sftp.progress.CountingSftpProgressMonitor;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class SftpDocumentForwarder implements IDocumentForwarder
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SftpDocumentForwarder.class);
  private static final String SFTP_DATETIME_PATTERN = "yyyyMMddHHmmss";
  private static final AtomicInteger WRITE_FILE_COUNT = new AtomicInteger (0);

  private ISftpSettings m_aSftpSettings;

  public void initFromConfiguration (@NonNull final IConfigWithFallback aConfig)
  {
    m_aSftpSettings = SftpSettings.createFromConfig (aConfig, "forwarding.sftp");
    if (m_aSftpSettings == null)
      throw new InitializationException ("Failed to initialize SFTP settings from configuration");
  }

  /**
   * Upload a file to the server by first writing the content to a file with the extension ".tmp".
   * Once all data is transfer, the file is renamed to the original destination filename.
   *
   * @param aUploadSettings
   *        The connection settings to use. May not be <code>null</code>.
   * @param sTargetDirectory
   *        The name of the target directory. May not be <code>null</code>.
   * @param sTargetFilename
   *        The name of the uploaded file. May not be <code>null</code>.
   * @param aISP
   *        The input stream to read from. The stream is automatically closed within this method -
   *        no matter whether the upload was successful or not. May not be <code>null</code>.
   * @return The {@link ForwardingResult} to return. Never <code>null</code>.
   */
  @NonNull
  public static ForwardingResult writeUploadedFile (@NonNull final ISftpSettings aUploadSettings,
                                                    @NonNull final String sTargetDirectory,
                                                    @NonNull final String sTargetFilename,
                                                    @NonNull final IHasInputStream aISP)
  {
    ValueEnforcer.notNull (aUploadSettings, "UploadSettings");
    ValueEnforcer.notNull (sTargetDirectory, "TargetDirectory");
    ValueEnforcer.notNull (sTargetFilename, "TargetFilename");
    ValueEnforcer.notNull (aISP, "ISP");

    final String sLogPrefix = aUploadSettings.getLogPrefix ();
    final String sRealTargetDirectory = StringHelper.trimEnd (aUploadSettings.getServerDirectoryUpload (), '/') +
                                        '/' +
                                        StringHelper.trimStart (sTargetDirectory, '/');

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "Trying to upload SFTP file '" + sRealTargetDirectory + "/" + sTargetFilename + "'");

    try
    {
      // do file upload
      final AbstractChannelSftpRunnable aUpload = new AbstractChannelSftpRunnable ("put " +
                                                                                   sRealTargetDirectory +
                                                                                   "/" +
                                                                                   sTargetFilename)
      {
        public void execute (@NonNull final ChannelSftp aChannel) throws SftpException
        {
          // Ensure directory exists
          if (ChannelSftpHelper.mkdir (aChannel, sRealTargetDirectory).isFailure ())
            return;

          // goto "in" directory (will fail
          // if mkdir failed)
          aChannel.cd (sRealTargetDirectory);

          /*
           * First write to the server with a temporary filename, to avoid that unfinished documents
           * are retrieved.The total length is unknown that's why we need to count.
           */
          final String sTargetTempFilename = sTargetFilename + ".tmp";

          WRITE_FILE_COUNT.incrementAndGet ();
          LOGGER.info (sLogPrefix +
                       "transfering file [" +
                       WRITE_FILE_COUNT.get () +
                       "] to server: '" +
                       aChannel.pwd () +
                       "/" +
                       sTargetTempFilename +
                       "'");

          final CountingSftpProgressMonitor aCounter = new CountingSftpProgressMonitor ();
          aChannel.put (aISP.getInputStream (), sTargetTempFilename, aCounter);
          final long nBytesWritten = aCounter.getNumberOfBytes ();

          LOGGER.info (sLogPrefix +
                       "wrote " +
                       nBytesWritten +
                       " bytes; renaming file '" +
                       sTargetTempFilename +
                       "' to '" +
                       sTargetFilename +
                       "'");

          // rename after upload finished -> file is ready to read by handler
          aChannel.rename (sTargetTempFilename, sTargetFilename);

          if (LOGGER.isDebugEnabled ())
            LOGGER.debug (sLogPrefix + "And renamed '" + sTargetTempFilename + "' to '" + sTargetFilename + "'");
        }
      };
      final ESuccess eSuccess = SftpMaxParallelRunner.execute (aUploadSettings, aUpload);
      if (eSuccess.isSuccess ())
        return ForwardingResult.success ();

      return ForwardingResult.failure ("sftp_execution",
                                       "Failed to perform SFTP upload to '" +
                                                         sRealTargetDirectory +
                                                         "/" +
                                                         sTargetFilename +
                                                         "'");
    }
    catch (final JSchException ex)
    {
      // Error sending document to server - keep file!
      final Throwable aCause = ex.getCause ();
      final String sErrorMsg = "Failed to transmit document '" +
                               sRealTargetDirectory +
                               "/" +
                               sTargetFilename +
                               "' to the server";
      if (WebExceptionHelper.isServerNotReachableConnection (aCause))
        LOGGER.error (sLogPrefix + ": " + aCause.getMessage ());
      else
        LOGGER.error (sLogPrefix + "!", ex);
      return ForwardingResult.failure ("sftp_error", sErrorMsg);
    }
  }

  @NonNull
  public ForwardingResult forwardDocument (@NonNull final IInboundTransaction aTransaction)
  {
    try
    {
      // Layout: yyyyMMddHHmmss_(random value).xml
      final String sTargetFilename = DateTimeFormatter.ofPattern (SFTP_DATETIME_PATTERN)
                                                      .format (aTransaction.getReceivedDT ()) +
                                     "_" +
                                     FilenameHelper.getAsSecureValidASCIIFilename (aTransaction.getIncomingID ()) +
                                     ".xml";

      return writeUploadedFile (m_aSftpSettings,
                                "",
                                sTargetFilename,
                                HasInputStream.create (DocumentStorageHelper.readDocument (aTransaction.getDocumentPath ())));
    }
    catch (final Exception ex)
    {
      LOGGER.error ("SFTP forwarding failed for transaction '" + aTransaction.getID () + "'", ex);
      return ForwardingResult.failure ("sftp_exception", ex.getMessage () + " (" + ex.getClass ().getName () + ")");
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("SftpSettings", m_aSftpSettings).getToString ();
  }
}
