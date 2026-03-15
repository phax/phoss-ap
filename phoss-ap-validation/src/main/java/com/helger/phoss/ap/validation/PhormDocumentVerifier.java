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
package com.helger.phoss.ap.validation;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.config.IConfig;
import com.helger.http.CHttpHeader;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.json.IJsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.EExtendedValidity;
import com.helger.phive.result.json.PhiveJsonHelper;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IOutboundDocumentVerifierSPI;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.phoss.ap.basic.storage.DocumentStorageHelper;

/**
 * Document verifier implementation that calls the phorm Validation Service to validate documents.
 * The validation service automatically detects the document type and validates it against the
 * appropriate rules. This class implements both inbound and outbound verification SPIs.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PhormDocumentVerifier implements IInboundDocumentVerifierSPI, IOutboundDocumentVerifierSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PhormDocumentVerifier.class);

  @NonNull
  private ESuccess _callPhorm (@NonNull @Nonempty final String sDocumentPath)
  {
    final IConfig aConfig = APConfigProvider.getConfig ();
    final String sPhormBaseURL = aConfig.getAsString (APConfigurationProperties.VERIFICATION_PHORM_URL);
    final String sPhormToken = aConfig.getAsString (APConfigurationProperties.VERIFICATION_PHORM_TOKEN);

    if (StringHelper.isEmpty (sPhormBaseURL))
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Phorm URL is not configured ('" + APConfigurationProperties.VERIFICATION_PHORM_URL + "')");
      // Don't break document processing
      return ESuccess.SUCCESS;
    }

    if (URLHelper.getAsURL (sPhormBaseURL) == null)
    {
      LOGGER.error ("Phorm URL '" + sPhormBaseURL + "' is not a valid URL");
      return ESuccess.FAILURE;
    }

    if (StringHelper.isEmpty (sPhormToken))
    {
      LOGGER.error ("Phorm URL '" + sPhormBaseURL + "' looks okay but the Token is not configured");
      return ESuccess.FAILURE;
    }

    final String sURL = StringHelper.trimEnd (sPhormBaseURL, '/') + "/api/dd_and_validate/";
    if (!DocumentStorageHelper.existsDocument (sDocumentPath))
    {
      LOGGER.error ("Document path '" + sDocumentPath + "' does not exist");
      return ESuccess.FAILURE;
    }

    final HttpClientSettings aHCS = new HttpClientSettings ();
    APBasicConfig.applyHttpProxySettings (aHCS);

    try (final HttpClientManager aHttpClientMgr = HttpClientManager.create (aHCS);
         final InputStream aDocumentIS = DocumentStorageHelper.openDocumentStreamForRead (sDocumentPath))
    {
      final HttpPost aPost = new HttpPost (sURL);
      aPost.setEntity (new InputStreamEntity (aDocumentIS, ContentType.APPLICATION_XML));
      aPost.setHeader (CHttpHeader.ACCEPT, ContentType.APPLICATION_JSON.getMimeType ());
      aPost.setHeader ("X-Token", sPhormToken);

      LOGGER.info ("Calling Phorm at '" + sURL + "' for document '" + sDocumentPath + "'");

      final byte [] aResponseBytes = aHttpClientMgr.execute (aPost, new ResponseHandlerByteArray ());
      if (aResponseBytes == null)
      {
        LOGGER.error ("Phorm returned null response for '" + sDocumentPath + "'");
        return ESuccess.FAILURE;
      }

      final IJsonObject aJson = JsonReader.builder ().source (aResponseBytes).readAsObject ();
      if (aJson == null)
      {
        LOGGER.error ("Failed to parse Phorm response as JSON for '" + sDocumentPath + "'");
        return ESuccess.FAILURE;
      }

      final ValidationResultList aResultList = PhiveJsonHelper.getAsValidationResultList (aJson);
      if (aResultList == null)
      {
        LOGGER.error ("Failed to extract validation results from Phorm response for '" + sDocumentPath + "'");
        return ESuccess.FAILURE;
      }

      final EExtendedValidity eValidity = aResultList.getOverallValidity ();
      if (eValidity == EExtendedValidity.INVALID)
      {
        LOGGER.warn ("Document '" +
                     sDocumentPath +
                     "' failed validation. " +
                     aResultList.getAllErrors ().size () +
                     " error(s) found");
        if (LOGGER.isDebugEnabled ())
          aResultList.getAllErrors ()
                     .forEach (e -> LOGGER.debug ("  Validation error: " + e.getErrorText (CPhossAP.DEFAULT_LOCALE)));
        return ESuccess.FAILURE;
      }

      LOGGER.info ("Document '" + sDocumentPath + "' passed validation (validity=" + eValidity + ")");
      return ESuccess.SUCCESS;
    }
    catch (final ExtendedHttpResponseException ex)
    {
      LOGGER.error ("Phorm returned HTTP error for '" + sDocumentPath + "': " + ex.getMessage ());
      return ESuccess.FAILURE;
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to call Phorm for '" +
                    sDocumentPath +
                    "': " +
                    ex.getMessage () +
                    " (" +
                    ex.getClass ().getName () +
                    ")");
      return ESuccess.FAILURE;
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Unexpected error calling Phorm for '" + sDocumentPath + "'", ex);
      return ESuccess.FAILURE;
    }
  }

  @NonNull
  public ESuccess verifyInboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                         @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                         @NonNull final IProcessIdentifier aProcessID)
  {
    return _callPhorm (sDocumentPath);
  }

  @NonNull
  public ESuccess verifyOutboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                          @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                          @NonNull final IProcessIdentifier aProcessID)
  {
    return _callPhorm (sDocumentPath);
  }
}
