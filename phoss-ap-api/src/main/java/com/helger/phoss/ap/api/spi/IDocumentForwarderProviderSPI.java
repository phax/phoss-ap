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
package com.helger.phoss.ap.api.spi;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIInterface;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;

/**
 * SPI interface for deployment-provided document forwarders.
 * <p>
 * Implementations are loaded via {@link java.util.ServiceLoader}. The AP selects one provider by
 * matching {@link #getID()} against the configured <code>forwarding.spi.id</code> value.
 *
 * @author Philip Helger
 * @since 0.9.1
 */
@IsSPIInterface
public interface IDocumentForwarderProviderSPI
{
  /**
   * @return The stable provider ID used in configuration. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getID ();

  /**
   * Create a fresh document forwarder instance. The AP initializes it afterwards with the active
   * forwarding configuration prefix.
   *
   * @return A document forwarder instance. Never <code>null</code>.
   */
  @NonNull
  IDocumentForwarder createDocumentForwarder ();
}
