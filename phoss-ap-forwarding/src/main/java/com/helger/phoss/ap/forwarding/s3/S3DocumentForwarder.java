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

import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.mime.CMimeType;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
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

  public void initFromConfiguration (@NonNull final IConfigWithFallback aConfig)
  {
    m_aRegion = Region.of (aConfig.getAsString (APConfigurationProperties.FORWARDING_S3_REGION));
    m_sBucket = aConfig.getAsString (APConfigurationProperties.FORWARDING_S3_BUCKET);
    m_sAccessKeyId = aConfig.getAsString (APConfigurationProperties.FORWARDING_S3_ACCESS_KEY_ID);
    m_sSecretAccessKey = aConfig.getAsString (APConfigurationProperties.FORWARDING_S3_SECRET_ACCESS_KEY);
    m_sKeyPrefix = aConfig.getAsString (APConfigurationProperties.FORWARDING_S3_KEY_PREFIX);
  }

  @NonNull
  public ForwardingResult forwardDocument (@NonNull final IInboundTransaction aTransaction)
  {
    if (m_aRegion == null)
    {
      LOGGER.error ("S3 region not configured");
      return ForwardingResult.failure ("s3_configuration_error", "S3 region not configured");
    }
    if (StringHelper.isEmpty (m_sBucket))
    {
      LOGGER.error ("S3 rbucket not configured");
      return ForwardingResult.failure ("s3_configuration_error", "S3 bucket not configured");
    }

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
        final String sKey = (StringHelper.isNotEmpty (m_sKeyPrefix) ? m_sKeyPrefix + "/" : "") +
                            aTransaction.getSbdhInstanceID () +
                            ".xml";

        final PutObjectRequest aPutReq = PutObjectRequest.builder ()
                                                         .bucket (m_sBucket)
                                                         .key (sKey)
                                                         .contentType (CMimeType.APPLICATION_XML.getAsString ())
                                                         .build ();

        aS3Client.putObject (aPutReq,
                             RequestBody.fromInputStream (APBasicMetaManager.getDocStorageProvider ().openDocumentStreamForRead (aTransaction.getDocumentPath ()),
                                                          aTransaction.getDocumentSize ()));

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
      LOGGER.error ("S3 forwarding failed for transaction '" + aTransaction.getID () + "'", ex);
      return ForwardingResult.failure ("s3_error", ex.getMessage () + " (" + ex.getClass ().getName () + ")");
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
