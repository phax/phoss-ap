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
import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.collection.commons.ICommonsList;
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
import com.helger.phoss.ap.api.model.VerificationIssue;
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
     * The request could not be created or sent at all - Phorm is misconfigured or the document
     * could not be read
     */
    REQUEST_ERROR,
    /** Phorm could not be reached or reported itself as unavailable */
    SERVICE_UNAVAILABLE,
    /** Phorm answered, but the response could not be used */
    RESPONSE_ERROR,
    // Note: REQUEST_ERROR, SERVICE_UNAVAILABLE and RESPONSE_ERROR all mean that the call did not
    // produce a verdict about the document. They are therefore all mapped to
    // VerificationOutcome.serviceUnavailable (...) and only differ in the message
    /** Phorm call completed - {@link PhormCallResult#results} is non-null */
    COMPLETED
  }

  private static record PhormCallResult (@NonNull EPhormCallState state, @Nullable ValidationResultList results)
  {
    @NonNull
    static final PhormCallResult SKIPPED = new PhormCallResult (EPhormCallState.SKIPPED, null);
    @NonNull
    static final PhormCallResult REQUEST_ERROR = new PhormCallResult (EPhormCallState.REQUEST_ERROR, null);
    @NonNull
    static final PhormCallResult SERVICE_UNAVAILABLE = new PhormCallResult (EPhormCallState.SERVICE_UNAVAILABLE, null);
    @NonNull
    static final PhormCallResult RESPONSE_ERROR = new PhormCallResult (EPhormCallState.RESPONSE_ERROR, null);

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
      return PhormCallResult.REQUEST_ERROR;
    }

    if (StringHelper.isEmpty (sPhormToken))
    {
      LOGGER.error ("Phorm URL '" + sPhormBaseURL + "' looks okay but the Token is not configured");
      return PhormCallResult.REQUEST_ERROR;
    }

    final String sURL = StringHelper.trimEnd (sPhormBaseURL, '/') + "/api/dd_and_validate/";
    if (!aDocPayloadMgr.existsDocument (sDocumentPath))
    {
      LOGGER.error ("Document path '" + sDocumentPath + "' does not exist");
      return PhormCallResult.REQUEST_ERROR;
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
        // Server side error (HTTP >= 500) or an empty response entity
        LOGGER.error ("Phorm returned null response for '" + sDocumentPath + "' with code " + aStatusCode.intValue ());
        return PhormCallResult.SERVICE_UNAVAILABLE;
      }

      final IJsonObject aJson = JsonReader.builder ().source (aResponseBytes).readAsObject ();
      if (aJson == null)
      {
        // Phorm answered, but not with something usable
        LOGGER.error ("Failed to parse Phorm response as JSON for '" +
                      sDocumentPath +
                      "' with code " +
                      aStatusCode.intValue ());
        return PhormCallResult.RESPONSE_ERROR;
      }

      // Parse JSON back to data structure
      final ValidationResultList aResultList = PhiveJsonHelper.getAsValidationResultList (aJson);
      if (aResultList == null)
      {
        LOGGER.error ("Failed to extract validation results from Phorm response for '" +
                      sDocumentPath +
                      "' with code " +
                      aStatusCode.intValue ());
        return PhormCallResult.RESPONSE_ERROR;
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
      // A response was received, but with an error status code
      LOGGER.error ("Phorm returned HTTP error for '" + sDocumentPath + "': " + ex.getMessage ());
      return PhormCallResult.RESPONSE_ERROR;
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
      return PhormCallResult.SERVICE_UNAVAILABLE;
    }
    catch (final Exception ex)
    {
      // We don't know whether the request or the response was at fault
      LOGGER.error ("Unexpected error calling Phorm for '" + sDocumentPath + "'", ex);
      return PhormCallResult.SERVICE_UNAVAILABLE;
    }
  }

  /**
   * Turn a completed Phorm call into a verification outcome. The issues are the same in both
   * directions - only how they are reported to the outside world differs.
   *
   * @param aCall
   *        The completed Phorm call. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  private static VerificationOutcome _toOutcome (@NonNull final PhormCallResult aCall)
  {
    final ICommonsList <VerificationIssue> aIssues = PhiveToVerificationMapper.toVerificationIssues (aCall.results (),
                                                                                                     CPhossAP.DEFAULT_LOCALE);
    if (aCall.results ().containsNoError ())
    {
      // Valid - any remaining issues are warnings
      return VerificationOutcome.passed (aIssues);
    }

    // The result list said there is at least one error, so the mapping must have produced issues
    return VerificationOutcome.rejected ("Document validation failed", aIssues);
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
      case REQUEST_ERROR -> VerificationOutcome.serviceUnavailable ("Phorm validation service call could not be sent - see server log for details");
      case SERVICE_UNAVAILABLE -> VerificationOutcome.serviceUnavailable ("Phorm validation service is not available - see server log for details");
      case RESPONSE_ERROR -> VerificationOutcome.serviceUnavailable ("Phorm validation service response could not be used - see server log for details");
      case COMPLETED -> _toOutcome (aCall);
    };
  }

  /** {@inheritDoc} */
  @NonNull
  public VerificationOutcome verifyOutboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                                     @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                     @NonNull final IProcessIdentifier aProcessID)
  {
    final PhormCallResult aCall = _callPhorm (sDocumentPath);
    return switch (aCall.state ())
    {
      case SKIPPED -> VerificationOutcome.passed ();
      // Outbound verification has no fail mode - a verifier without a verdict stays fail closed.
      // The outcome is nevertheless "service unavailable" and not "rejected", so that the caller
      // can tell the submitter that the document was not actually found to be invalid
      case REQUEST_ERROR -> VerificationOutcome.serviceUnavailable ("Phorm validation service call could not be sent - see server log for details");
      case SERVICE_UNAVAILABLE -> VerificationOutcome.serviceUnavailable ("Phorm validation service is not available - see server log for details");
      case RESPONSE_ERROR -> VerificationOutcome.serviceUnavailable ("Phorm validation service response could not be used - see server log for details");
      case COMPLETED -> _toOutcome (aCall);
    };
  }
}
