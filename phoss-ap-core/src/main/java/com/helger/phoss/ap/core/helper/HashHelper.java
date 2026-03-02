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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHex;
import com.helger.security.messagedigest.EMessageDigestAlgorithm;

@Immutable
public final class HashHelper
{
  public static final EMessageDigestAlgorithm MD_ALGO = EMessageDigestAlgorithm.SHA_256;

  private HashHelper ()
  {}

  @NonNull
  public static String sha256Hex (final byte @NonNull [] aBytes)
  {
    final byte [] aHash = MD_ALGO.createMessageDigest ().digest (aBytes);
    return StringHex.getHexEncoded (aHash);
  }
}
