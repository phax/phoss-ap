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
package com.helger.phoss.ap.api;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.codelist.EAttemptStatus;
import com.helger.phoss.ap.api.model.IOutboundSendingAttempt;

/**
 * Manager interface for creating and querying outbound transaction attempts.
 *
 * @author Philip Helger
 */
public interface IOutboundSendingAttemptManager
{
  @Nullable
  String create (@NonNull String sOutboundTransactionID,
                 @NonNull String sAS4MessageID,
                 @NonNull OffsetDateTime aAS4Timestamp,
                 @Nullable String sReceiptMessageID,
                 @Nullable Integer aHttpStatusCode,
                 @NonNull EAttemptStatus eAttemptStatus,
                 @Nullable String sErrorDetails,
                 @Nullable String sSendingReport);

  @Nullable
  String createSuccess (@NonNull String sOutboundTransactionID,
                        @NonNull String sAS4MessageID,
                        @NonNull OffsetDateTime aAS4Timestamp,
                        @NonNull String sReceiptMessageID,
                        @Nullable String sSendingReport);

  @NonNull
  ICommonsList <IOutboundSendingAttempt> getByTransactionID (@NonNull String sOutboundTransactionID);
}
