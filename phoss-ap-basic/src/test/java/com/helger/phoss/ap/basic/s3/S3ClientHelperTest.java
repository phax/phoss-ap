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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;

/**
 * Test class for class {@link S3ClientHelper}.
 *
 * @author Philip Helger
 */
public final class S3ClientHelperTest
{
  @Test
  public void testMinimal ()
  {
    try (final S3Client aS3Client = S3ClientHelper.createS3Client (Region.EU_CENTRAL_1, null, false, null, null))
    {
      final S3ServiceClientConfiguration aConfig = aS3Client.serviceClientConfiguration ();
      assertEquals (Region.EU_CENTRAL_1, aConfig.region ());
      assertFalse (aConfig.endpointOverride ().isPresent ());
      assertFalse (aConfig.credentialsProvider () instanceof StaticCredentialsProvider);
    }
  }

  @Test
  public void testEndpointAndCredentials ()
  {
    try (final S3Client aS3Client = S3ClientHelper.createS3Client (Region.US_EAST_1,
                                                                   "https://s3.example.org",
                                                                   true,
                                                                   "key-id",
                                                                   "secret"))
    {
      final S3ServiceClientConfiguration aConfig = aS3Client.serviceClientConfiguration ();
      assertEquals (Region.US_EAST_1, aConfig.region ());
      assertEquals (URI.create ("https://s3.example.org"), aConfig.endpointOverride ().get ());
      assertTrue (aConfig.credentialsProvider () instanceof StaticCredentialsProvider);
      final AwsCredentialsIdentity aCredentials = aConfig.credentialsProvider ().resolveIdentity ().join ();
      assertEquals ("key-id", aCredentials.accessKeyId ());
      assertEquals ("secret", aCredentials.secretAccessKey ());
    }
  }

  @Test
  public void testEmptyValuesAreIgnored ()
  {
    // Only the access key ID without the secret access key
    try (final S3Client aS3Client = S3ClientHelper.createS3Client (Region.EU_WEST_1, "", false, "key-id", ""))
    {
      final S3ServiceClientConfiguration aConfig = aS3Client.serviceClientConfiguration ();
      assertFalse (aConfig.endpointOverride ().isPresent ());
      assertFalse (aConfig.credentialsProvider () instanceof StaticCredentialsProvider);
    }
  }
}
