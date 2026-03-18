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
package com.helger.phoss.ap.basic.mgr;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.string.StringHelper;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.basic.APBasicConfig;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Implementation of {@link IDocumentPayloadManager} that stores documents in Amazon S3. Document
 * paths are returned as {@code s3://bucket/key} URIs which are self-contained and opaque to
 * callers.
 *
 * @author Philip Helger
 * @since 0.1.1
 */
public class DocumentPayloadManagerS3 implements IDocumentPayloadManager
{
  /**
   * Internal {@link ByteArrayOutputStream} that uploads its buffered content to S3 when
   * {@link #close()} is called.
   */
  private final class S3UploadOutputStream extends NonBlockingByteArrayOutputStream
  {
    private final String m_sKey;

    S3UploadOutputStream (@NonNull final String sKey)
    {
      m_sKey = sKey;
    }

    @Override
    public void close ()
    {
      try
      {
        super.close ();
        m_aS3Client.putObject (PutObjectRequest.builder ().bucket (m_sBucket).key (m_sKey).build (),
                               RequestBody.fromByteBuffer (ByteBuffer.wrap (m_aBuf, 0, m_nCount)));
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to upload buffered document to S3 '" +
                                         m_sBucket +
                                         "' / '" +
                                         m_sKey +
                                         "'",
                                         ex);
      }
    }
  }

  private static record BucketAndKey (String bucket, String key)
  {}

  public static final String S3_URI_PREFIX = "s3://";
  private static final Logger LOGGER = LoggerFactory.getLogger (DocumentPayloadManagerS3.class);

  private final S3Client m_aS3Client;
  private final String m_sBucket;

  /**
   * Default constructor. Reads S3 configuration from {@link APBasicConfig}.
   */
  public DocumentPayloadManagerS3 ()
  {
    final String sRegion = APBasicConfig.getStorageS3Region ();
    if (StringHelper.isEmpty (sRegion))
      throw new InitializationException ("No S3 storage region configured (storage.s3.region)");
    final Region aRegion = Region.of (sRegion);
    if (aRegion == null)
      throw new InitializationException ("The S3 storage region configuration '" + sRegion + "' is invalid!");

    m_sBucket = APBasicConfig.getStorageS3Bucket ();
    if (StringHelper.isEmpty (m_sBucket))
      throw new InitializationException ("No S3 storage bucket configured (storage.s3.bucket)");

    final S3ClientBuilder aBuilder = S3Client.builder ().region (aRegion);

    final String sAccessKeyID = APBasicConfig.getStorageS3AccessKeyID ();
    final String sSecretAccessKey = APBasicConfig.getStorageS3SecretAccessKey ();
    if (StringHelper.isNotEmpty (sAccessKeyID) && StringHelper.isNotEmpty (sSecretAccessKey))
    {
      aBuilder.credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create (sAccessKeyID,
                                                                                                  sSecretAccessKey)));
    }

    m_aS3Client = aBuilder.build ();
    LOGGER.info ("S3 DocumentPayloadManager initialized with region '" + sRegion + "' and bucket '" + m_sBucket + "'");
  }

  public void verifyConfiguration ()
  {
    try
    {
      m_aS3Client.headBucket (HeadBucketRequest.builder ().bucket (m_sBucket).build ());
      LOGGER.info ("S3 bucket '" + m_sBucket + "' is accessible");
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("S3 bucket '" + m_sBucket + "' is not accessible", ex);
    }
  }

  @NonNull
  private static String _buildKey (@NonNull final String sBaseDir,
                                   @NonNull final OffsetDateTime aDT,
                                   @NonNull final String sFilename)
  {
    return StringHelper.trimEnd (sBaseDir, '/') +
           "/" +
           aDT.getYear () +
           "/" +
           StringHelper.getLeadingZero (aDT.getMonthValue (), 2) +
           "/" +
           StringHelper.getLeadingZero (aDT.getDayOfMonth (), 2) +
           "/" +
           StringHelper.getLeadingZero (aDT.getHour (), 2) +
           "/" +
           sFilename;
  }

  @NonNull
  private String _toS3URI (@NonNull final String sKey)
  {
    return S3_URI_PREFIX + m_sBucket + "/" + sKey;
  }

  @NonNull
  private static BucketAndKey _extractBucketAndKey (@NonNull final String sS3URI)
  {
    // s3://bucket/key
    final String sWithoutPrefix = sS3URI.substring (S3_URI_PREFIX.length ());
    final int nSlash = sWithoutPrefix.indexOf ('/');
    if (nSlash < 0)
      throw new IllegalArgumentException ("Invalid S3 URI (no key): " + sS3URI);
    return new BucketAndKey (sWithoutPrefix.substring (0, nSlash), sWithoutPrefix.substring (nSlash + 1));
  }

  @NonNull
  public String storeDocument (@NonNull final String sBaseDir,
                               @NonNull final OffsetDateTime aReferenceDT,
                               @NonNull final String sFilename,
                               final byte @NonNull [] aBytes)
  {
    ValueEnforcer.notNull (sBaseDir, "BaseDir");
    ValueEnforcer.notNull (aReferenceDT, "ReferenceDT");
    ValueEnforcer.notNull (sFilename, "Filename");
    ValueEnforcer.notNull (aBytes, "Bytes");

    final String sKey = _buildKey (sBaseDir, aReferenceDT, sFilename);
    try
    {
      m_aS3Client.putObject (PutObjectRequest.builder ().bucket (m_sBucket).key (sKey).build (),
                             RequestBody.fromBytes (aBytes));
      return _toS3URI (sKey);
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to store document '" + sFilename + "' to S3 key '" + sKey + "'", ex);
    }
  }

  @NonNull
  public OutputStream openDocumentStreamForWrite (@NonNull final String sBaseDir,
                                                  @NonNull final OffsetDateTime aReferenceDT,
                                                  @NonNull final String sFilename,
                                                  @NonNull final String sFileExt,
                                                  @NonNull final Consumer <String> aPathConsumer)
  {
    ValueEnforcer.notNull (sBaseDir, "BaseDir");
    ValueEnforcer.notNull (aReferenceDT, "ReferenceDT");
    ValueEnforcer.notEmpty (sFilename, "Filename");
    ValueEnforcer.notEmpty (sFileExt, "FileExt");
    ValueEnforcer.isTrue ( () -> sFileExt.startsWith ("."), "FileExt must start with a dot");
    ValueEnforcer.notNull (aPathConsumer, "PathConsumer");

    final String sKey = _buildKey (sBaseDir, aReferenceDT, sFilename + sFileExt);
    final String sS3URI = _toS3URI (sKey);

    // Notify the consumer of the final path immediately so the caller can store it
    aPathConsumer.accept (sS3URI);

    // Return a ByteArrayOutputStream that uploads to S3 on close
    return new S3UploadOutputStream (sKey);
  }

  @NonNull
  public OutputStream openTemporaryDocumentStreamForWrite (@NonNull final String sBaseDir,
                                                           @NonNull final OffsetDateTime aReferenceDT,
                                                           @NonNull final Consumer <String> aPathConsumer)
  {
    return openDocumentStreamForWrite (sBaseDir, aReferenceDT, UUID.randomUUID ().toString (), ".tmp", aPathConsumer);
  }

  @NonNull
  public String renameFile (@NonNull final String sSrcFile,
                            @NonNull final String sTargetDir,
                            @NonNull @Nonempty final String sBaseName,
                            @NonNull @Nonempty final String sFileExt)
  {
    final BucketAndKey aBucketAndKey = _extractBucketAndKey (sSrcFile);
    final String sSrcKey = aBucketAndKey.key ();
    final String sBucket = aBucketAndKey.bucket ();

    // Derive the new key: replace the filename portion but keep the directory
    final int nLastSlash = sSrcKey.lastIndexOf ('/');
    final String sNewKey = (nLastSlash >= 0 ? sSrcKey.substring (0, nLastSlash + 1) : "") + sBaseName + sFileExt;

    try
    {
      // S3 CopyObject + DeleteObject
      m_aS3Client.copyObject (CopyObjectRequest.builder ()
                                               .sourceBucket (sBucket)
                                               .sourceKey (sSrcKey)
                                               .destinationBucket (sBucket)
                                               .destinationKey (sNewKey)
                                               .build ());
      m_aS3Client.deleteObject (DeleteObjectRequest.builder ().bucket (sBucket).key (sSrcKey).build ());

      return S3_URI_PREFIX + sBucket + "/" + sNewKey;
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to rename S3 object '" + sSrcFile + "' to key '" + sNewKey + "'", ex);
    }
  }

  public byte @NonNull [] readDocument (@NonNull final String sAbsolutePath)
  {
    ValueEnforcer.notNull (sAbsolutePath, "AbsolutePath");

    try
    {
      final BucketAndKey aBucketAndKey = _extractBucketAndKey (sAbsolutePath);
      final ResponseInputStream <GetObjectResponse> aIS = m_aS3Client.getObject (GetObjectRequest.builder ()
                                                                                                 .bucket (aBucketAndKey.bucket ())
                                                                                                 .key (aBucketAndKey.key ())
                                                                                                 .build ());
      return aIS.readAllBytes ();
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to read document from '" + sAbsolutePath + "'", ex);
    }
  }

  @NonNull
  public InputStream openDocumentStreamForRead (@NonNull final String sAbsolutePath)
  {
    ValueEnforcer.notNull (sAbsolutePath, "AbsolutePath");

    try
    {
      final BucketAndKey aBucketAndKey = _extractBucketAndKey (sAbsolutePath);
      return m_aS3Client.getObject (GetObjectRequest.builder ()
                                                    .bucket (aBucketAndKey.bucket ())
                                                    .key (aBucketAndKey.key ())
                                                    .build ());
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to open document stream for '" + sAbsolutePath + "'", ex);
    }
  }

  public boolean deleteDocument (@NonNull final String sAbsolutePath)
  {
    ValueEnforcer.notNull (sAbsolutePath, "AbsolutePath");

    try
    {
      // Check existence first since S3 DeleteObject does not report whether the
      // object existed
      if (!existsDocument (sAbsolutePath))
        return false;

      final BucketAndKey aBucketAndKey = _extractBucketAndKey (sAbsolutePath);
      m_aS3Client.deleteObject (DeleteObjectRequest.builder ()
                                                   .bucket (aBucketAndKey.bucket ())
                                                   .key (aBucketAndKey.key ())
                                                   .build ());
      return true;
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to delete document at '" + sAbsolutePath + "'", ex);
    }
  }

  public boolean existsDocument (@NonNull final String sAbsolutePath)
  {
    ValueEnforcer.notNull (sAbsolutePath, "AbsolutePath");

    try
    {
      final BucketAndKey aBucketAndKey = _extractBucketAndKey (sAbsolutePath);
      m_aS3Client.headObject (HeadObjectRequest.builder ()
                                               .bucket (aBucketAndKey.bucket ())
                                               .key (aBucketAndKey.key ())
                                               .build ());
      return true;
    }
    catch (final NoSuchKeyException ex)
    {
      return false;
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to check existence of document at '" + sAbsolutePath + "'", ex);
    }
  }
}
