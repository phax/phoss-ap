/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phoss.ap.api;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;

/**
 * Read-only view of a single inbound forwarding attempt. Each attempt represents one forwarding
 * operation to the Receiver Backend.
 *
 * @author Philip Helger
 */
public interface IInboundForwardingAttempt extends IHasID <String>
{
  /**
   * @return The unique attempt ID. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getID ();

  /**
   * @return The ID of the parent inbound transaction. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getInboundTransactionID ();

  /**
   * @return The date/time of this forwarding attempt. Never <code>null</code>.
   */
  @NonNull
  OffsetDateTime getAttemptDT ();

  /**
   * @return The outcome of this attempt. Never <code>null</code>.
   */
  @NonNull
  EAttemptStatus getAttemptStatus ();

  /**
   * @return Machine-readable error code classifying the failure, or <code>null</code> on success.
   */
  @Nullable
  String getErrorCode ();

  /**
   * @return The error message or reason for failure, or <code>null</code> on success.
   */
  @Nullable
  String getErrorDetails ();
}
