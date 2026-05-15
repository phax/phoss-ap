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
package com.helger.phoss.ap.api.trace;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.IsSPIInterface;

/**
 * SPI for starting trace spans. Loaded via {@link java.util.ServiceLoader}. The first registered
 * implementation wins; if none is registered, {@link APTrace} falls back to a no-op tracer that
 * makes all span operations cheap and side-effect-free.
 * <p>
 * Implementations must:
 * <ul>
 * <li>Make the returned {@link IAPSpan} the current span on the calling thread for its
 * lifetime.</li>
 * <li>Restore the previously-current span when the returned {@link IAPSpan} is closed.</li>
 * <li>Be thread-safe: {@link #startSpan(String, EAPSpanKind)} may be called concurrently from any
 * thread.</li>
 * </ul>
 *
 * @author Philip Helger
 * @since 0.9.0
 */
@IsSPIInterface
public interface IAPTracerSPI
{
  /**
   * Start a new span. The returned span is implicitly made the current span on the calling thread;
   * it stays current until {@link IAPSpan#close()} is invoked.
   *
   * @param sName
   *        The span name (typically a constant from CPhossAPOtel). Never <code>null</code>.
   * @param eKind
   *        The kind of operation. Never <code>null</code>.
   * @return A new active span. Never <code>null</code>.
   */
  @NonNull
  IAPSpan startSpan (@NonNull String sName, @NonNull EAPSpanKind eKind);
}
