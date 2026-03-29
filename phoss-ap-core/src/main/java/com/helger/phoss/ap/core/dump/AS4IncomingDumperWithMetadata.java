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
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.io.file.SimpleFileIO;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.IAS4IncomingDumperFileProvider;
import com.helger.phase4.incoming.AS4IncomingHelper;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;

/**
 * Extension of {@link AS4IncomingDumperFileBased} that writes a {@code metadata.json} file
 * alongside each incoming AS4 message dump. The metadata JSON is created via
 * {@link AS4IncomingHelper#getIncomingMetadataAsJson(IAS4IncomingMessageMetadata)}.
 *
 * @author Philip Helger
 * @since v0.1.4
 */
public class AS4IncomingDumperWithMetadata extends AS4IncomingDumperFileBased
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4IncomingDumperWithMetadata.class);

  public AS4IncomingDumperWithMetadata ()
  {}

  @Override
  public void onEndRequest (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                            @Nullable final Exception aCaughtException)
  {
    if (aMessageMetadata.getMode ().isRequest ())
    {
      // Write metadata JSON alongside the .as4in dump file
      final String sRelPath = IAS4IncomingDumperFileProvider.getDefaultDirectoryAndFilename (aMessageMetadata);
      // Replace the extension to get the metadata file path
      final String sMetadataPath = sRelPath.substring (0, sRelPath.lastIndexOf ('.')) + ".metadata.json";
      final File aMetadataFile = new File (AS4Configuration.getDumpBasePathFile (),
                                           AS4IncomingDumperFileBased.DEFAULT_BASE_PATH + sMetadataPath);

      final String sJson = AS4IncomingHelper.getIncomingMetadataAsJson (aMessageMetadata)
                                            .getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);
      if (SimpleFileIO.writeFile (aMetadataFile, sJson, StandardCharsets.UTF_8).isFailure ())
        LOGGER.error ("Failed to write metadata to '" + aMetadataFile.getAbsolutePath () + "'");
    }
  }
}
