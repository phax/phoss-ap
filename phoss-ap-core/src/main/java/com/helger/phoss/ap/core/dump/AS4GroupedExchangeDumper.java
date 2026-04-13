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
package com.helger.phoss.ap.core.dump;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.http.CHttp;
import com.helger.http.header.HttpHeaderMap;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.FilenameHelper;
import com.helger.io.file.SimpleFileIO;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.incoming.AS4IncomingHelper;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.messaging.EAS4MessageMode;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;
import com.helger.phoss.ap.basic.APBasicMetaManager;

/**
 * AS4 message dumper that groups correlated messages of a single exchange into a shared directory.
 * Implements both {@link IAS4IncomingDumper} and {@link IAS4OutgoingDumper} so that inbound and
 * outbound legs of the same exchange end up next to each other on disk.
 * <p>
 * Scenario a) Inbound exchange: incoming UserMessage + outgoing SignalMessage response are grouped
 * by the {@code IncomingUniqueID}.<br>
 * Scenario b) Outbound exchange: outgoing UserMessage + incoming SignalMessage response are grouped
 * by the outgoing AS4 Message ID.
 * </p>
 * <p>
 * File layout:
 * </p>
 *
 * <pre>
 * &lt;basepath&gt;/grouped/&lt;date&gt;/
 *   &lt;IncomingUniqueID&gt;/              # Scenario a
 *     usermessage.as4in
 *     signalmessage.as4out
 *   &lt;sanitized-AS4-MessageID&gt;/       # Scenario b
 *     usermessage-0.as4out
 *     signalmessage.as4in
 * </pre>
 *
 * @author Philip Helger
 */
public class AS4GroupedExchangeDumper implements IAS4IncomingDumper, IAS4OutgoingDumper
{
  protected static final String BASE_DIR_GROUPED = "grouped/";

  private static final Logger LOGGER = LoggerFactory.getLogger (AS4GroupedExchangeDumper.class);

  private final File m_aBaseDir;
  /** Maps outgoing AS4 Message ID to its grouped directory (scenario b). */
  private final ConcurrentHashMap <String, File> m_aOutboundDirs = new ConcurrentHashMap <> ();
  /** Maps incoming unique ID to its grouped directory (scenario a) — for metadata writing. */
  private final ConcurrentHashMap <String, File> m_aInboundDirs = new ConcurrentHashMap <> ();

  /**
   * Constructor.
   *
   * @param aBaseDir
   *        The base dump directory (value of {@code phase4.dump.path}). May not be
   *        <code>null</code>.
   */
  public AS4GroupedExchangeDumper (@NonNull final File aBaseDir)
  {
    ValueEnforcer.notNull (aBaseDir, "BaseDir");

    m_aBaseDir = aBaseDir;
  }

  @NonNull
  private File _getDateDir (@NonNull final OffsetDateTime aODT)
  {
    return new File (m_aBaseDir,
                     BASE_DIR_GROUPED +
                                 aODT.getYear () +
                                 "/" +
                                 StringHelper.getLeadingZero (aODT.getMonthValue (), 2) +
                                 "/" +
                                 StringHelper.getLeadingZero (aODT.getDayOfMonth (), 2));
  }

  @Nullable
  private static OutputStream _openAndWriteHeaders (@NonNull final File aFile, @Nullable final HttpHeaderMap aHeaders)
                                                                                                                       throws IOException
  {
    final OutputStream aOS = FileHelper.getBufferedOutputStream (aFile);
    if (aOS == null)
    {
      LOGGER.error ("Failed to open grouped dump file '" + aFile.getAbsolutePath () + "'");
      return null;
    }

    if (aHeaders != null && aHeaders.isNotEmpty ())
    {
      for (final var aEntry : aHeaders)
      {
        final String sKey = aEntry.getKey ();
        for (final String sValue : aEntry.getValue ())
        {
          final boolean bQuoteIfNecessary = false;
          final String sUnifiedValue = HttpHeaderMap.getUnifiedValue (sValue, bQuoteIfNecessary);
          final String sLine = sKey + HttpHeaderMap.SEPARATOR_KEY_VALUE + sUnifiedValue + CHttp.EOL;
          aOS.write (sLine.getBytes (CHttp.HTTP_CHARSET));
        }
      }
      aOS.write (CHttp.EOL.getBytes (CHttp.HTTP_CHARSET));
    }
    return aOS;
  }

  private static void _writeMetadataJson (@NonNull final File aDir,
                                          @NonNull final IAS4IncomingMessageMetadata aMessageMetadata)
  {
    final File aFile = new File (aDir, "metadata.json");
    final String sJson = AS4IncomingHelper.getIncomingMetadataAsJson (aMessageMetadata)
                                          .getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);
    if (SimpleFileIO.writeFile (aFile, sJson, StandardCharsets.UTF_8).isFailure ())
      LOGGER.error ("Failed to write metadata to '" + aFile.getAbsolutePath () + "'");
  }

  // --- IAS4IncomingDumper ---

  /** {@inheritDoc} */
  @Nullable
  public OutputStream onNewRequest (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                    @NonNull final HttpHeaderMap aHttpHeaderMap) throws IOException
  {
    if (aMessageMetadata.getMode ().isRequest ())
    {
      // Request mode

      // Scenario a) — incoming UserMessage
      final String sDirName = FilenameHelper.getAsSecureValidASCIIFilename (aMessageMetadata.getIncomingUniqueID ());
      final File aDir = new File (_getDateDir (aMessageMetadata.getIncomingDT ()), sDirName);
      FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (aDir);

      // Remember for metadata writing in onEndRequest
      m_aInboundDirs.put (aMessageMetadata.getIncomingUniqueID (), aDir);

      return _openAndWriteHeaders (new File (aDir, "usermessage.as4in"), aHttpHeaderMap);
    }

    // Response mode

    // Scenario b) — incoming SignalMessage response to our outgoing UserMessage
    final String sRequestMsgID = aMessageMetadata.getRequestMessageID ();
    File aDir = null;
    if (StringHelper.isNotEmpty (sRequestMsgID))
      aDir = m_aOutboundDirs.get (sRequestMsgID);
    if (aDir == null)
    {
      LOGGER.warn ("Failed to resolve dumping directory for AS4 message ID '" + sRequestMsgID + "'");

      // Fallback: create directory from the request message ID
      final String sDirName = FilenameHelper.getAsSecureValidASCIIFilename (StringHelper.isNotEmpty (sRequestMsgID) ? sRequestMsgID
                                                                                                                    : aMessageMetadata.getIncomingUniqueID ());
      aDir = new File (_getDateDir (aMessageMetadata.getIncomingDT ()), sDirName);
      FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (aDir);
    }
    return _openAndWriteHeaders (new File (aDir, "signalmessage.as4in"), aHttpHeaderMap);
  }

  /** {@inheritDoc} */
  public void onEndRequest (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                            @Nullable final Exception aCaughtException)
  {
    if (aMessageMetadata.getMode ().isRequest ())
    {
      // Request mode — write metadata JSON for incoming UserMessage (scenario a)
      final File aDir = m_aInboundDirs.remove (aMessageMetadata.getIncomingUniqueID ());
      if (aDir != null)
        _writeMetadataJson (aDir, aMessageMetadata);
    }
    else
    {
      // Response mode — cleanup the outbound directory mapping
      final String sRequestMsgID = aMessageMetadata.getRequestMessageID ();
      if (StringHelper.isNotEmpty (sRequestMsgID))
        m_aOutboundDirs.remove (sRequestMsgID);
    }
  }

  // --- IAS4OutgoingDumper ---

  /** {@inheritDoc} */
  @Nullable
  public OutputStream onBeginRequest (@NonNull final EAS4MessageMode eMsgMode,
                                      @Nullable final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                      @Nullable final IAS4IncomingMessageState aIncomingState,
                                      @NonNull @Nonempty final String sMessageID,
                                      @Nullable final HttpHeaderMap aCustomHeaders,
                                      @Nonnegative final int nTry) throws IOException
  {
    if (eMsgMode.isResponse ())
    {
      // Response mode

      // Scenario a) — outgoing SignalMessage response to incoming UserMessage
      if (aIncomingMessageMetadata == null)
      {
        LOGGER.warn ("No incoming metadata for outgoing RESPONSE — cannot correlate");
        return null;
      }

      final String sUniqueID = FilenameHelper.getAsSecureValidASCIIFilename (aIncomingMessageMetadata.getIncomingUniqueID ());
      final File aDir = new File (_getDateDir (aIncomingMessageMetadata.getIncomingDT ()), sUniqueID);
      FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (aDir);
      return _openAndWriteHeaders (new File (aDir, "signalmessage.as4out"), aCustomHeaders);
    }

    // Request mode

    // Scenario b) — outgoing UserMessage
    final IAPTimestampManager aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
    final String sDirName = FilenameHelper.getAsSecureValidASCIIFilename (sMessageID);
    final File aDir = new File (_getDateDir (aTimestampMgr.getCurrentDateTimeUTC ()), sDirName);
    FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (aDir);

    // Store for later correlation when the SignalMessage response arrives
    m_aOutboundDirs.put (sMessageID, aDir);
    return _openAndWriteHeaders (new File (aDir, "usermessage-" + nTry + ".as4out"), aCustomHeaders);
  }

  /** {@inheritDoc} */
  public void onEndRequest (@NonNull final EAS4MessageMode eMsgMode,
                            @Nullable final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                            @Nullable final IAS4IncomingMessageState aIncomingState,
                            @NonNull @Nonempty final String sMessageID,
                            @Nullable final Exception aCaughtException)
  {
    // nothing to do here — cleanup happens in IAS4IncomingDumper.onEndRequest in this class
  }
}
