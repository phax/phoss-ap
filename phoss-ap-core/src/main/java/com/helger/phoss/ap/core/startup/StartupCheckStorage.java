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
package com.helger.phoss.ap.core.startup;

import java.io.File;
import java.util.ServiceLoader;

import org.jspecify.annotations.NonNull;

import com.helger.base.string.StringHelper;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phoss.ap.api.codelist.EAPStartupCheckPhase;
import com.helger.phoss.ap.api.codelist.EStorageMode;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.spi.IDocumentPayloadManagerProviderSPI;
import com.helger.phoss.ap.basic.APBasicConfig;

/**
 * Check the document storage configuration: the storage mode is valid, and the settings the
 * selected mode requires are present. For the file system, an existing path must be a writable
 * directory; a missing one is created later on. Whether an S3 bucket is reachable is verified when
 * the storage is initialized.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public class StartupCheckStorage extends AbstractAPStartupCheck
{
  public static final String CHECK_NAME = "storage";

  public StartupCheckStorage ()
  {
    super (CHECK_NAME, EAPStartupCheckPhase.PRE_INIT);
  }

  private void _checkPath (@NonNull final ErrorList aErrors,
                           @NonNull final String sConfigKey,
                           @NonNull final String sPath)
  {
    if (StringHelper.isEmpty (sPath))
    {
      addError (aErrors, "No storage path is configured in '" + sConfigKey + "'");
      return;
    }

    final File aPath = new File (sPath);
    if (aPath.exists ())
    {
      if (!aPath.isDirectory ())
        addError (aErrors, "The storage path '" + sPath + "' (" + sConfigKey + ") exists but is not a directory");
      else
        if (!aPath.canWrite ())
          addError (aErrors,
                    "The storage path '" + sPath + "' (" + sConfigKey + ") is not writable by the application user");
    }
  }

  public void performCheck (@NonNull final ErrorList aErrors)
  {
    // The getter silently falls back to the default for an invalid value
    final String sConfiguredMode = APConfigProvider.getConfig ().getAsString (APConfigurationProperties.STORAGE_MODE);
    if (StringHelper.isNotEmpty (sConfiguredMode) && EStorageMode.getFromIDOrNull (sConfiguredMode) == null)
    {
      addError (aErrors,
                "The configured storage mode '" +
                         sConfiguredMode +
                         "' (" +
                         APConfigurationProperties.STORAGE_MODE +
                         ") is invalid");
      return;
    }

    switch (APBasicConfig.getStorageMode ())
    {
      case FILE_SYSTEM:
        _checkPath (aErrors, APConfigurationProperties.STORAGE_INBOUND_PATH, APBasicConfig.getStorageInboundPath ());
        _checkPath (aErrors, APConfigurationProperties.STORAGE_OUTBOUND_PATH, APBasicConfig.getStorageOutboundPath ());
        break;
      case S3:
        if (StringHelper.isEmpty (APBasicConfig.getStorageS3Region ()))
          addError (aErrors,
                    "Storage mode 's3' requires configuration property '" +
                             APConfigurationProperties.STORAGE_S3_REGION +
                             "' to be set");
        if (StringHelper.isEmpty (APBasicConfig.getStorageS3Bucket ()))
          addError (aErrors,
                    "Storage mode 's3' requires configuration property '" +
                             APConfigurationProperties.STORAGE_S3_BUCKET +
                             "' to be set");
        break;
      case SPI:
      {
        final String sProviderID = APBasicConfig.getStorageSpiID ();
        if (StringHelper.isEmpty (sProviderID))
        {
          addError (aErrors,
                    "Storage mode 'spi' requires configuration property '" +
                             APConfigurationProperties.STORAGE_SPI_ID +
                             "' to be set");
          break;
        }

        boolean bFound = false;
        for (final IDocumentPayloadManagerProviderSPI aProvider : ServiceLoader.load (IDocumentPayloadManagerProviderSPI.class))
          if (sProviderID.equals (aProvider.getID ()))
          {
            bFound = true;
            break;
          }
        if (!bFound)
          addError (aErrors,
                    "No document payload manager provider SPI found for ID '" +
                             sProviderID +
                             "' from configuration property '" +
                             APConfigurationProperties.STORAGE_SPI_ID +
                             "'");
        break;
      }
    }
  }
}
