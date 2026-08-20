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
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.base.numeric.mutable.MutableInt;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.config.IConfig;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.level.EErrorLevel;
import com.helger.http.CHttpHeader;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.json.IJsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.result.json.PhiveJsonHelper;
import com.helger.phoss.ap.api.CPhossAP;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.model.VerificationOutcome;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IOutboundDocumentVerifierSPI;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.phoss.ap.basic.APBasicMetaManager;

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
  private static final String HTTP_HEADER_X_TOKEN = "X-Token";
  private static final Logger LOGGER = LoggerFactory.getLogger (PhormDocumentVerifier.class);

  private enum EPhormCallState
  {
    /** Phorm is not configured - skip verification */
    SKIPPED,
    /**
     * Phorm call could not be completed (configuration error, HTTP, parse) - the service is
     * considered unavailable
     */
    FAILED,
    /** Phorm call completed - {@link PhormCallResult#results} is non-null */
    COMPLETED
  }

  private static record PhormCallResult (@NonNull EPhormCallState state, @Nullable ValidationResultList results)
  {
    @NonNull
    static final PhormCallResult SKIPPED = new PhormCallResult (EPhormCallState.SKIPPED, null);
    @NonNull
    static final PhormCallResult FAILED = new PhormCallResult (EPhormCallState.FAILED, null);

    @NonNull
    static PhormCallResult completed (@NonNull final ValidationResultList aResults)
    {
      return new PhormCallResult (EPhormCallState.COMPLETED, aResults);
    }
  }

  @NonNull
  private PhormCallResult _callPhorm (@NonNull @Nonempty final String sDocumentPath)
  {
    final IDocumentPayloadManager aDocPayloadMgr = APBasicMetaManager.getDocPayloadMgr ();
    final IConfig aConfig = APConfigProvider.getConfig ();
    final String sPhormBaseURL = aConfig.getAsString (APConfigurationProperties.VERIFICATION_PHORM_URL);
    final String sPhormToken = aConfig.getAsString (APConfigurationProperties.VERIFICATION_PHORM_TOKEN);

    if (StringHelper.isEmpty (sPhormBaseURL))
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Phorm URL is not configured ('" + APConfigurationProperties.VERIFICATION_PHORM_URL + "')");

      // Don't break document processing if Phorm is not used
      return PhormCallResult.SKIPPED;
    }

    if (URLHelper.getAsURL (sPhormBaseURL) == null)
    {
      LOGGER.error ("Phorm URL '" + sPhormBaseURL + "' is not a valid URL");
      return PhormCallResult.FAILED;
    }

    if (StringHelper.isEmpty (sPhormToken))
    {
      LOGGER.error ("Phorm URL '" + sPhormBaseURL + "' looks okay but the Token is not configured");
      return PhormCallResult.FAILED;
    }

    final String sURL = StringHelper.trimEnd (sPhormBaseURL, '/') + "/api/dd_and_validate/";
    if (!aDocPayloadMgr.existsDocument (sDocumentPath))
    {
      LOGGER.error ("Document path '" + sDocumentPath + "' does not exist");
      return PhormCallResult.FAILED;
    }

    final HttpClientSettings aHCS = new HttpClientSettings ();
    APBasicConfig.applyHttpProxySettings (aHCS);

    try (final HttpClientManager aHttpClientMgr = HttpClientManager.create (aHCS);
         final InputStream aDocumentIS = aDocPayloadMgr.openDocumentStreamForRead (sDocumentPath))
    {
      final HttpPost aPost = new HttpPost (sURL);
      aPost.setEntity (new InputStreamEntity (aDocumentIS, ContentType.APPLICATION_XML));
      aPost.setHeader (CHttpHeader.ACCEPT, ContentType.APPLICATION_JSON.getMimeType ());
      if (StringHelper.isNotEmpty (sPhormToken))
        aPost.setHeader (HTTP_HEADER_X_TOKEN, sPhormToken);

      LOGGER.info ("Calling Phorm at '" + sURL + "' for document '" + sDocumentPath + "'");

      final MutableInt aStatusCode = new MutableInt (0);
      final byte [] aResponseBytes = aHttpClientMgr.execute (aPost, aHttpResponse -> {
        final StatusLine aStatusLine = new StatusLine (aHttpResponse);
        aStatusCode.set (aStatusLine.getStatusCode ());
        // Skip all server side errors
        if (aStatusLine.getStatusCode () >= 500)
          return null;

        // Phorm return 400 in case of invalid validations
        final HttpEntity aEntity = aHttpResponse.getEntity ();
        return EntityUtils.toByteArray (aEntity);
      });
      if (aResponseBytes == null)
      {
        LOGGER.error ("Phorm returned null response for '" + sDocumentPath + "' with code " + aStatusCode.intValue ());
        return PhormCallResult.FAILED;
      }

      final IJsonObject aJson = JsonReader.builder ().source (aResponseBytes).readAsObject ();
      if (aJson == null)
      {
        LOGGER.error ("Failed to parse Phorm response as JSON for '" +
                      sDocumentPath +
                      "' with code " +
                      aStatusCode.intValue ());
        return PhormCallResult.FAILED;
      }

      // Parse JSON back to data structure
      final ValidationResultList aResultList = PhiveJsonHelper.getAsValidationResultList (aJson);
      if (aResultList == null)
      {
        LOGGER.error ("Failed to extract validation results from Phorm response for '" +
                      sDocumentPath +
                      "' with code " +
                      aStatusCode.intValue ());
        return PhormCallResult.FAILED;
      }

      if (aResultList.containsAtLeastOneError ())
      {
        final int nErrors = aResultList.getAllCount (IError::isError);
        final int nWarns = aResultList.getAllCount (x -> x.getErrorLevel ().isEQ (EErrorLevel.WARN));
        LOGGER.warn ("Document '" +
                     sDocumentPath +
                     "' failed validation. " +
                     nErrors +
                     (nErrors == 1 ? " error" : " errors") +
                     (nWarns == 0 ? "" : " and " + nWarns + (nWarns == 1 ? " warning" : " warnings")) +
                     " found");
        if (LOGGER.isDebugEnabled ())
        {
          aResultList.getAllErrors ()
                     .forEach (e -> LOGGER.debug ("  Validation error: " + e.getErrorText (CPhossAP.DEFAULT_LOCALE)));
        }
      }
      else
      {
        LOGGER.info ("Document '" +
                     sDocumentPath +
                     "' passed validation (validity=" +
                     aResultList.getOverallValidity () +
                     ")");
      }
      return PhormCallResult.completed (aResultList);
    }
    catch (final ExtendedHttpResponseException ex)
    {
      LOGGER.error ("Phorm returned HTTP error for '" + sDocumentPath + "': " + ex.getMessage ());
      return PhormCallResult.FAILED;
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
      return PhormCallResult.FAILED;
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Unexpected error calling Phorm for '" + sDocumentPath + "'", ex);
      return PhormCallResult.FAILED;
    }
  }

  /** {@inheritDoc} */
  @NonNull
  public VerificationOutcome verifyInboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                                    @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                    @NonNull final IProcessIdentifier aProcessID)
  {
    final PhormCallResult aCall = _callPhorm (sDocumentPath);
    return switch (aCall.state ())
    {
      case SKIPPED -> VerificationOutcome.passed ();
      // The document was not validated at all, so this is no rejection of the document itself.
      // Depending on the configured EVerificationFailMode this leads to a deferral, a rejection or
      // an acceptance
      case FAILED -> VerificationOutcome.serviceUnavailable ("Phorm validation service call failed - see server log for details");
      case COMPLETED ->
      {
        final MlsOutcome aMlsOutcome = PhiveToMlsMapper.toMlsOutcome (aCall.results (),
                                                                      CPhossAP.DEFAULT_LOCALE,
                                                                      "Document validation failed");
        yield aMlsOutcome.getResponseCode ().isFailure () ? VerificationOutcome.rejected (aMlsOutcome)
                                                          : VerificationOutcome.passed ();
      }
    };
  }

  /** {@inheritDoc} */
  @NonNull
  public ESuccess verifyOutboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                          @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                          @NonNull final IProcessIdentifier aProcessID)
  {
    final PhormCallResult aCall = _callPhorm (sDocumentPath);
    return switch (aCall.state ())
    {
      case SKIPPED -> ESuccess.SUCCESS;
      case FAILED -> ESuccess.FAILURE;
      case COMPLETED -> ESuccess.valueOf (aCall.results ().getOverallValidity ().isValid ());
    };
  }
}
