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
package com.helger.phoss.ap.api.mgr;

import org.jspecify.annotations.NonNull;

import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;

/**
 * SPI interface for forwarding received inbound documents to the Receiver Backend (C4).
 * Implementations are loaded via {@link java.util.ServiceLoader}. Exactly one implementation must
 * be present on the classpath at runtime.
 *
 * @author Philip Helger
 */
public interface IDocumentForwarder
{
  /**
   * Initialize the forwarded from the provided configuration.
   *
   * @param aConfig
   *        The configuration object to init from. Never <code>null</code>.
   */
  void initFromConfiguration (@NonNull IConfigWithFallback aConfig);

  /**
   * Forward the given inbound transaction's document to the Receiver Backend. This method should
   * never throw an exception.
   *
   * @param aTransaction
   *        The inbound transaction whose document bytes should be forwarded. Never
   *        <code>null</code>.
   * @return {@link ForwardingResult#success()} if forwarding succeeded, or
   *         {@link ForwardingResult#failure(String, String)} with error details otherwise.
   */
  @NonNull
  ForwardingResult forwardDocument (@NonNull IInboundTransaction aTransaction);
}
