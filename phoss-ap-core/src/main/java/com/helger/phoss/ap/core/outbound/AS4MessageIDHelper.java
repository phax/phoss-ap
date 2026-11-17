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
package com.helger.phoss.ap.core.outbound;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.string.StringHelper;

/**
 * Checks an AS4 message ID chosen by the submitter instead of a random one. A backend that keeps its
 * own message IDs (e.g. to trace a message across systems, or to let the receiver detect a resent
 * message as a duplicate) needs the same ID on the wire.
 */
public final class AS4MessageIDHelper
{
  /** AS4 message IDs end up in XML attributes and in the database, so they are kept reasonably short. */
  public static final int MAX_LENGTH = 255;

  private AS4MessageIDHelper ()
  {}

  /**
   * @return The trimmed message ID, or <code>null</code> if none was provided.
   */
  @Nullable
  public static String getTrimmedOrNull (@Nullable final String sMessageID)
  {
    if (sMessageID == null)
      return null;
    final String sTrimmed = sMessageID.trim ();
    return StringHelper.isNotEmpty (sTrimmed) ? sTrimmed : null;
  }

  /**
   * @return An error text if the message ID is not usable, <code>null</code> otherwise.
   */
  @Nullable
  public static String getValidationError (@NonNull final String sMessageID)
  {
    // ebMS 3.0: the MessageId must be globally unique and conform to the RFC 2822 "msg-id" form
    final int nAt = sMessageID.indexOf ('@');
    if (nAt <= 0 || nAt == sMessageID.length () - 1 || sMessageID.indexOf ('@', nAt + 1) >= 0)
      return "The AS4 message ID must have the form 'local@domain'";
    if (sMessageID.length () > MAX_LENGTH)
      return "The AS4 message ID must not be longer than " + MAX_LENGTH + " characters";
    if (!sMessageID.chars ().allMatch (c -> c > 0x20 && c != 0x7f))
      return "The AS4 message ID must not contain whitespace or control characters";
    return null;
  }
}
