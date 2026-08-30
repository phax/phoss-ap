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
package com.helger.phoss.ap.forwarding.s3;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.mime.CMimeType;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IForwardableDocument;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.Telemetry;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Implementation of {@link IDocumentForwarder} for using Amazon S3.
 *
 * @author Philip Helger
 */
public class S3DocumentForwarder implements IDocumentForwarder
{
  private static final Logger LOGGER = LoggerFactory.getLogger (S3DocumentForwarder.class);
  // Configuration key suffixes (relative to the configured base prefix)
  private static final String SUFFIX_S3_REGION = "s3.region";
  private static final String SUFFIX_S3_BUCKET = "s3.bucket";
  private static final String SUFFIX_S3_ACCESS_KEY_ID = "s3.access-key-id";
  private static final String SUFFIX_S3_SECRET_ACCESS_KEY = "s3.secret-access-key";
  private static final String SUFFIX_S3_ENDPOINT = "s3.endpoint";
  private static final String SUFFIX_S3_PATH_STYLE_ACCESS = "s3.path-style-access";
  private static final String SUFFIX_S3_KEY_PREFIX = "s3.key-prefix";
  private static final String SUFFIX_S3_WRITE_METADATA = "s3.write-metadata";

  private Region m_aRegion;
  private String m_sBucket;
  private String m_sAccessKeyId;
  private String m_sSecretAccessKey;
  private String m_sKeyPrefix;
  private String m_sEndpoint;
  private boolean m_bPathStyleAccess;
  private boolean m_bWriteMetadata;

  /** {@inheritDoc} */
  @NonNull
  public ESuccess initFromConfiguration (@NonNull final IConfigWithFallback aConfig, @NonNull final String sKeyPrefix)
  {
    ValueEnforcer.notNull (sKeyPrefix, "KeyPrefix");

    final String sRegion = aConfig.getAsString (sKeyPrefix + SUFFIX_S3_REGION);
    m_aRegion = Region.of (sRegion);
    if (m_aRegion == null)
    {
      LOGGER.error ("Configured S3 region '" + sRegion + "' is invalid");
      return ESuccess.FAILURE;
    }

    final String sBucketKey = sKeyPrefix + SUFFIX_S3_BUCKET;
    m_sBucket = aConfig.getAsString (sBucketKey);
    if (StringHelper.isEmpty (m_sBucket))
    {
      LOGGER.error ("S3 bucket at '" + sBucketKey + "' is not configured");
      return ESuccess.FAILURE;
    }

    m_sAccessKeyId = aConfig.getAsString (sKeyPrefix + SUFFIX_S3_ACCESS_KEY_ID);
    m_sSecretAccessKey = aConfig.getAsString (sKeyPrefix + SUFFIX_S3_SECRET_ACCESS_KEY);
    m_sEndpoint = aConfig.getAsString (sKeyPrefix + SUFFIX_S3_ENDPOINT);
    m_bPathStyleAccess = aConfig.getAsBoolean (sKeyPrefix + SUFFIX_S3_PATH_STYLE_ACCESS,
                                               APConfigurationProperties.FORWARDING_S3_PATH_STYLE_ACCESS_DEFAULT);
    m_bWriteMetadata = aConfig.getAsBoolean (sKeyPrefix + SUFFIX_S3_WRITE_METADATA,
                                             APConfigurationProperties.FORWARDING_S3_WRITE_METADATA_DEFAULT);

    m_sKeyPrefix = aConfig.getAsString (sKeyPrefix + SUFFIX_S3_KEY_PREFIX);
    if (StringHelper.isNotEmpty (m_sKeyPrefix))
    {
      if (!m_sKeyPrefix.endsWith ("/"))
        m_sKeyPrefix += '/';
    }
    else
      m_sKeyPrefix = "";
    return ESuccess.SUCCESS;
  }

  /**
   * Write a JSON metadata sidecar object next to the uploaded SBD. The content mirrors the metadata
   * JSON written by the filesystem and SFTP forwarders. A failure to write the sidecar is logged
   * but does not fail the overall forwarding.
   *
   * @param aS3Client
   *        The S3 client to use for the upload. May not be <code>null</code>.
   * @param aDocument
   *        The document to write the metadata for. May not be <code>null</code>.
   * @param sMetaKey
   *        The S3 object key of the sidecar (including the ".json" extension). May not be
   *        <code>null</code>.
   */
  private void _writeMetadataSidecar (@NonNull final S3Client aS3Client,
                                      @NonNull final IForwardableDocument aDocument,
                                      @NonNull final String sMetaKey)
  {
    final byte [] aJsonBytes = aDocument.metadataJson ().get ().getBytes (StandardCharsets.UTF_8);

    final PutObjectRequest aMetaReq = PutObjectRequest.builder ()
                                                      .bucket (m_sBucket)
                                                      .key (sMetaKey)
                                                      .contentType (CMimeType.APPLICATION_JSON.getAsString ())
                                                      .build ();

    final var aMetaResult = aS3Client.putObject (aMetaReq, RequestBody.fromBytes (aJsonBytes));
    if (!aMetaResult.sdkHttpResponse ().isSuccessful ())
      LOGGER.error ("Failed to write S3 metadata sidecar for transaction '" +
                    aDocument.id () +
                    "' to S3 bucket '" +
                    m_sBucket +
                    "' and key '" +
                    sMetaKey +
                    "'");
  }

  @NonNull
  private ForwardingResult _doForwardDocument (@NonNull final IForwardableDocument aDocument)
  {
    final IDocumentPayloadManager aDocPayloadMgr = APBasicMetaManager.getDocPayloadMgr ();

    try
    {
      final S3ClientBuilder aBuilder = S3Client.builder ().region (m_aRegion);
      if (StringHelper.isNotEmpty (m_sEndpoint))
        aBuilder.endpointOverride (URI.create (m_sEndpoint));
      if (m_bPathStyleAccess)
        aBuilder.forcePathStyle (Boolean.TRUE);
      if (StringHelper.isNotEmpty (m_sAccessKeyId) && StringHelper.isNotEmpty (m_sSecretAccessKey))
      {
        aBuilder.credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create (m_sAccessKeyId,
                                                                                                    m_sSecretAccessKey)));
      }

      try (final S3Client aS3Client = aBuilder.build ();
           final InputStream aDocumentIS = aDocPayloadMgr.openDocumentStreamForRead (aDocument.documentPath ()))
      {
        final String sBaseKey = m_sKeyPrefix + aDocument.sbdhInstanceID ();
        final String sKey = sBaseKey + ".xml";

        final PutObjectRequest aPutReq = PutObjectRequest.builder ()
                                                         .bucket (m_sBucket)
                                                         .key (sKey)
                                                         .contentType (CMimeType.APPLICATION_XML.getAsString ())
                                                         .build ();

        final var aResult = aS3Client.putObject (aPutReq,
                                                 RequestBody.fromInputStream (aDocumentIS,
                                                                              aDocument.documentSize ()));
        if (!aResult.sdkHttpResponse ().isSuccessful ())
        {
          LOGGER.error ("Failed to uploaded transaction '" +
                        aDocument.id () +
                        "' to S3 bucket '" +
                        m_sBucket +
                        "' and key '" +
                        sKey +
                        "'");
          return ForwardingResult.failure ("s3-error", "SDK Http response error: " + aResult.sdkHttpResponse ());
        }

        LOGGER.info ("Uploaded transaction '" +
                     aDocument.id () +
                     "' to S3 bucket '" +
                     m_sBucket +
                     "' and key '" +
                     sKey +
                     "'");

        // Optionally write a metadata JSON sidecar next to the uploaded SBD
        if (m_bWriteMetadata && aDocument.metadataJson () != null)
          _writeMetadataSidecar (aS3Client, aDocument, sBaseKey + ".json");

        return ForwardingResult.success ();
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("S3 forwarding failed for transaction '" +
                    aDocument.id () +
                    "' to bucket '" +
                    m_sBucket +
                    "'",
                    ex);
      return ForwardingResult.failure ("s3_error", ex.getMessage () + " (" + ex.getClass ().getName () + ")");
    }
  }

  /** {@inheritDoc} */
  @NonNull
  public ForwardingResult forwardDocument (@NonNull final IForwardableDocument aDocument)
  {
    return Telemetry.withSpan (CPhossAPOtel.SPAN_FORWARDER_DISPATCH, ETelemetrySpanKind.CLIENT, aSpan -> {
      aSpan.setAttribute (CPhossAPOtel.ATTR_FORWARDER_TYPE, "s3")
           .setAttribute (CPhossAPOtel.ATTR_TRANSACTION_ID, aDocument.id ());
      return _doForwardDocument (aDocument);
    });
  }

  public boolean isWithDeliveryConfirmation ()
  {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Region", m_aRegion)
                                       .append ("Bucket", m_sBucket)
                                       .append ("PathStyleAccess", m_bPathStyleAccess)
                                       .append ("WriteMetadata", m_bWriteMetadata)
                                       .getToString ();
  }
}
