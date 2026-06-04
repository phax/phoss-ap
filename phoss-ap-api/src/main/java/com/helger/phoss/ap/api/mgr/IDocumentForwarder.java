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

import com.helger.base.state.ESuccess;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IInboundTransaction;

/**
 * Interface for forwarding received inbound documents to the Receiver Backend (C4). Built-in
 * implementations are selected by <code>forwarding.mode</code>; deployment-provided implementations
 * can be exposed through {@link com.helger.phoss.ap.api.spi.IDocumentForwarderProviderSPI}.
 *
 * @author Philip Helger
 */
public interface IDocumentForwarder
{
  /**
   * The default configuration key prefix used for the primary forwarder.
   *
   * @since 0.9.0
   */
  String DEFAULT_CONFIG_KEY_PREFIX = "forwarding.";

  /**
   * Initialize the forwarder from the provided configuration using the default "forwarding." key
   * prefix.
   *
   * @param aConfig
   *        The configuration object to init from. Never <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  default ESuccess initFromConfiguration (@NonNull final IConfigWithFallback aConfig)
  {
    return initFromConfiguration (aConfig, DEFAULT_CONFIG_KEY_PREFIX);
  }

  /**
   * Initialize the forwarder from the provided configuration using the given configuration key
   * prefix. This allows the same implementation to be used for both the primary forwarder (with
   * prefix <code>"forwarding."</code>) and secondary forwarders (with prefix
   * <code>"forwarding.secondary.{n}."</code>).
   *
   * @param aConfig
   *        The configuration object to init from. Never <code>null</code>.
   * @param sKeyPrefix
   *        The configuration key prefix to use, including the trailing dot (e.g.
   *        <code>"forwarding."</code> or <code>"forwarding.secondary.1."</code>). Never
   *        <code>null</code>.
   * @return {@link ESuccess}
   * @since 0.9.0
   */
  @NonNull
  ESuccess initFromConfiguration (@NonNull IConfigWithFallback aConfig, @NonNull String sKeyPrefix);

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
