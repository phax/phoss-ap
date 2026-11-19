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
package com.helger.phoss.ap.basic.s3;

import java.net.URI;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Creates the S3 clients for document storage, S3 forwarding and outbound S3 submission, so that
 * all of them interpret the S3 configuration properties in the same way.
 *
 * @author Philip Helger
 * @since v0.15.0
 */
@Immutable
public final class S3ClientHelper
{
  private S3ClientHelper ()
  {}

  /**
   * Create a new S3 client. The caller owns the client and is responsible for closing it.
   *
   * @param aRegion
   *        The AWS region to use. May not be <code>null</code>.
   * @param sEndpoint
   *        The optional custom endpoint URL for S3-compatible providers (e.g. MinIO, Garage). May be
   *        <code>null</code> or empty for standard AWS S3.
   * @param bPathStyleAccess
   *        <code>true</code> to use path-style access instead of virtual-hosted style.
   * @param sAccessKeyID
   *        The optional access key ID. May be <code>null</code> or empty.
   * @param sSecretAccessKey
   *        The optional secret access key. May be <code>null</code> or empty.
   * @return The new S3 client. Never <code>null</code>. Static credentials are only used if both the
   *         access key ID and the secret access key are present - otherwise the default credentials
   *         provider chain of the AWS SDK applies (e.g. for IAM role-based authentication).
   */
  @NonNull
  public static S3Client createS3Client (@NonNull final Region aRegion,
                                         @Nullable final String sEndpoint,
                                         final boolean bPathStyleAccess,
                                         @Nullable final String sAccessKeyID,
                                         @Nullable final String sSecretAccessKey)
  {
    ValueEnforcer.notNull (aRegion, "Region");

    final S3ClientBuilder aBuilder = S3Client.builder ().region (aRegion);
    if (StringHelper.isNotEmpty (sEndpoint))
      aBuilder.endpointOverride (URI.create (sEndpoint));
    if (bPathStyleAccess)
      aBuilder.forcePathStyle (Boolean.TRUE);
    if (StringHelper.isNotEmpty (sAccessKeyID) && StringHelper.isNotEmpty (sSecretAccessKey))
    {
      aBuilder.credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create (sAccessKeyID,
                                                                                                  sSecretAccessKey)));
    }
    return aBuilder.build ();
  }
}
