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
package com.helger.phoss.ap.core.inbound;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.spi.IDocumentForwarderProviderSPI;

/**
 * Provides the {@link MockControlledForwarder} for <code>forwarding.mode=spi</code>.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class MockControlledForwarderProviderSPI implements IDocumentForwarderProviderSPI
{
  @NonNull
  @Nonempty
  public String getID ()
  {
    return MockControlledForwarder.ID;
  }

  @NonNull
  public IDocumentForwarder createDocumentForwarder ()
  {
    return new MockControlledForwarder ();
  }
}
