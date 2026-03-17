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
package com.helger.phoss.ap.core.helper;

import java.security.MessageDigest;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHex;
import com.helger.security.messagedigest.EMessageDigestAlgorithm;

/**
 * Helper class for computing message digests and hex-encoded hash values.
 *
 * @author Philip Helger
 */
@Immutable
public final class HashHelper
{
  public static final EMessageDigestAlgorithm MD_ALGO = EMessageDigestAlgorithm.SHA_256;

  private HashHelper ()
  {}

  /**
   * Create a new {@link MessageDigest} instance using the configured algorithm ({@link #MD_ALGO}).
   *
   * @return A new {@link MessageDigest} instance. Never <code>null</code>.
   */
  @NonNull
  public static MessageDigest createMessageDigest ()
  {
    return MD_ALGO.createMessageDigest ();
  }

  /**
   * Convert a raw digest byte array to a lowercase hex-encoded string.
   *
   * @param aBytes
   *        The digest bytes to encode. May not be <code>null</code>.
   * @return The hex-encoded string. Never <code>null</code>.
   */
  @NonNull
  public static String getDigestHex (final byte @NonNull [] aBytes)
  {
    return StringHex.getHexEncoded (aBytes);
  }

  /**
   * Finalize the given {@link MessageDigest} and return the result as a hex-encoded string.
   *
   * @param aMD
   *        The message digest to finalize. May not be <code>null</code>.
   * @return The hex-encoded digest string. Never <code>null</code>.
   */
  @NonNull
  public static String getDigestHex (@NonNull final MessageDigest aMD)
  {
    return getDigestHex (aMD.digest ());
  }

  /**
   * Compute the SHA-256 hash of the given byte array and return the result as a hex-encoded string.
   *
   * @param aBytes
   *        The bytes to hash. May not be <code>null</code>.
   * @return The hex-encoded SHA-256 hash. Never <code>null</code>.
   */
  @NonNull
  public static String sha256Hex (final byte @NonNull [] aBytes)
  {
    final byte [] aHash = createMessageDigest ().digest (aBytes);
    return getDigestHex (aHash);
  }
}
