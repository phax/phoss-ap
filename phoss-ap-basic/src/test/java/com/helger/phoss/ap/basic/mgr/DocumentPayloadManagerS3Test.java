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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Unit tests for S3 stream lifecycle handling in {@link DocumentPayloadManagerS3}.
 */
public final class DocumentPayloadManagerS3Test
{
  private static final String BUCKET = "test-bucket";
  private static final OffsetDateTime REFERENCE_DT = OffsetDateTime.of (2026,
                                                                        6,
                                                                        23,
                                                                        10,
                                                                        0,
                                                                        0,
                                                                        0,
                                                                        ZoneOffset.UTC);

  @Test
  public void testReadDocumentClosesS3ResponseStream ()
  {
    final byte [] aPayload = "payload".getBytes (StandardCharsets.UTF_8);
    final AtomicBoolean aClosed = new AtomicBoolean (false);
    final ByteArrayInputStream aSourceIS = new ByteArrayInputStream (aPayload)
    {
      @Override
      public void close ()
      {
        aClosed.set (true);
      }
    };
    final S3Client aS3Client = _createS3Client ( (sMethodName, aArgs) -> {
      if ("getObject".equals (sMethodName))
        return new ResponseInputStream <> (GetObjectResponse.builder ().build (), aSourceIS);
      throw new UnsupportedOperationException (sMethodName);
    });

    final DocumentPayloadManagerS3 aManager = new DocumentPayloadManagerS3 (aS3Client, BUCKET);
    assertArrayEquals (aPayload, aManager.readDocument ("s3://" + BUCKET + "/document.xml"));
    assertTrue ("The S3 response stream must be closed after reading", aClosed.get ());
  }

  @Test
  public void testUploadStreamCloseIsIdempotent () throws Exception
  {
    final AtomicInteger aPutCount = new AtomicInteger ();
    final S3Client aS3Client = _createS3Client ( (sMethodName, aArgs) -> {
      if ("putObject".equals (sMethodName))
      {
        aPutCount.incrementAndGet ();
        return PutObjectResponse.builder ().build ();
      }
      throw new UnsupportedOperationException (sMethodName);
    });

    final DocumentPayloadManagerS3 aManager = new DocumentPayloadManagerS3 (aS3Client, BUCKET);
    final OutputStream aOS = aManager.openDocumentStreamForWrite ("outbound",
                                                                  REFERENCE_DT,
                                                                  "document",
                                                                  ".xml",
                                                                  sPath -> {});
    aOS.write ("payload".getBytes (StandardCharsets.UTF_8));
    aOS.close ();
    aOS.close ();

    assertEquals ("Closing the stream twice must upload only once", 1, aPutCount.get ());
  }

  @FunctionalInterface
  private interface IS3InvocationHandler
  {
    Object invoke (String sMethodName, Object [] aArgs) throws Throwable;
  }

  private static S3Client _createS3Client (final IS3InvocationHandler aHandler)
  {
    return (S3Client) Proxy.newProxyInstance (DocumentPayloadManagerS3Test.class.getClassLoader (),
                                               new Class <?> [] { S3Client.class },
                                               (aProxy, aMethod, aArgs) -> aHandler.invoke (aMethod.getName (), aArgs));
  }
}
