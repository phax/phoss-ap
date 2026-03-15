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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.mime.CMimeType;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;
import com.helger.phoss.ap.basic.APBasicMetaManager;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3DocumentForwarder implements IDocumentForwarder
{
  private static final Logger LOGGER = LoggerFactory.getLogger (S3DocumentForwarder.class);

  private Region m_aRegion;
  private String m_sBucket;
  private String m_sAccessKeyId;
  private String m_sSecretAccessKey;
  private String m_sKeyPrefix;

  @NonNull
  public ESuccess initFromConfiguration (@NonNull final IConfigWithFallback aConfig)
  {
    final String sRegion = aConfig.getAsString (APConfigurationProperties.FORWARDING_S3_REGION);
    m_aRegion = Region.of (sRegion);
    if (m_aRegion == null)
    {
      LOGGER.error ("Configured S3 region '" + sRegion + "' is invalid");
      return ESuccess.FAILURE;
    }

    m_sBucket = aConfig.getAsString (APConfigurationProperties.FORWARDING_S3_BUCKET);
    if (StringHelper.isEmpty (m_sBucket))
    {
      LOGGER.error ("S3 bucket is not configured");
      return ESuccess.FAILURE;
    }

    m_sAccessKeyId = aConfig.getAsString (APConfigurationProperties.FORWARDING_S3_ACCESS_KEY_ID);
    m_sSecretAccessKey = aConfig.getAsString (APConfigurationProperties.FORWARDING_S3_SECRET_ACCESS_KEY);

    m_sKeyPrefix = aConfig.getAsString (APConfigurationProperties.FORWARDING_S3_KEY_PREFIX);
    if (StringHelper.isNotEmpty (m_sKeyPrefix))
    {
      if (!m_sKeyPrefix.endsWith ("/"))
        m_sKeyPrefix += '/';
    }
    else
      m_sKeyPrefix = "";
    return ESuccess.SUCCESS;
  }

  @NonNull
  public ForwardingResult forwardDocument (@NonNull final IInboundTransaction aTransaction)
  {
    final IDocumentPayloadManager aDocPayloadMgr = APBasicMetaManager.getDocPayloadMgr ();

    try
    {
      final S3ClientBuilder aBuilder = S3Client.builder ().region (m_aRegion);
      if (StringHelper.isNotEmpty (m_sAccessKeyId) && StringHelper.isNotEmpty (m_sSecretAccessKey))
      {
        aBuilder.credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create (m_sAccessKeyId,
                                                                                                    m_sSecretAccessKey)));
      }

      try (final S3Client aS3Client = aBuilder.build ())
      {
        final String sKey = m_sKeyPrefix + aTransaction.getSbdhInstanceID () + ".xml";

        final PutObjectRequest aPutReq = PutObjectRequest.builder ()
                                                         .bucket (m_sBucket)
                                                         .key (sKey)
                                                         .contentType (CMimeType.APPLICATION_XML.getAsString ())
                                                         .build ();

        final var aResult = aS3Client.putObject (aPutReq,
                                                 RequestBody.fromInputStream (aDocPayloadMgr.openDocumentStreamForRead (aTransaction.getDocumentPath ()),
                                                                              aTransaction.getDocumentSize ()));
        if (!aResult.sdkHttpResponse ().isSuccessful ())
        {
          LOGGER.error ("Failed to uploaded transaction '" +
                        aTransaction.getID () +
                        "' to S3 bucket '" +
                        m_sBucket +
                        "' and key '" +
                        sKey +
                        "'");
          return ForwardingResult.failure ("s3-error", "SDK Http response error: " + aResult.sdkHttpResponse ());
        }

        LOGGER.info ("Uploaded transaction '" +
                     aTransaction.getID () +
                     "' to S3 bucket '" +
                     m_sBucket +
                     "' and key '" +
                     sKey +
                     "'");
        return ForwardingResult.success ();
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("S3 forwarding failed for transaction '" +
                    aTransaction.getID () +
                    "' to bucket '" +
                    m_sBucket +
                    "'",
                    ex);
      return ForwardingResult.failure ("s3_error", ex.getMessage () + " (" + ex.getClass ().getName () + ")");
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
