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
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;

/**
 * SPI interface for deployment-provided document payload storage backends.
 * <p>
 * Implementations are loaded via {@link java.util.ServiceLoader}. When the storage mode is set to
 * <code>spi</code>, the AP selects one provider by matching {@link #getID()} against the configured
 * <code>storage.spi.id</code> value.
 * <p>
 * This is deliberately storage-technology agnostic: the returned {@link IDocumentPayloadManager} may
 * be backed by a database, an object store, a message queue or anything else. Any technology-specific
 * setup (schema creation, migrations, bucket provisioning, …) is the responsibility of the provider
 * implementation itself, not of the AP core.
 *
 * @author Philip Helger
 * @since 0.10.4
 */
@IsSPIInterface
public interface IDocumentPayloadManagerProviderSPI
{
  /**
   * @return The stable provider ID used in configuration. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getID ();

  /**
   * Create a fresh document payload manager instance. The AP calls
   * {@link IDocumentPayloadManager#verifyConfiguration()} on the returned instance afterwards.
   *
   * @return A document payload manager instance. Never <code>null</code>.
   */
  @NonNull
  IDocumentPayloadManager createDocumentPayloadManager ();
}
