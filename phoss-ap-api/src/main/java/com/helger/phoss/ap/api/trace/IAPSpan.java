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
import org.jspecify.annotations.Nullable;

/**
 * A single trace span. Returned by {@link IAPTracerSPI#startSpan(String, EAPSpanKind)}, which
 * implicitly makes the span the <em>current</em> span on the calling thread for the entire lifetime
 * of this {@link IAPSpan}. Calling {@link #close()} both detaches the current-span association and
 * ends the span — so the idiomatic usage is:
 *
 * <pre>
 *   try (IAPSpan aSpan = APTrace.startSpan ("phoss.ap.outbound.send", EAPSpanKind.PRODUCER)
 *                               .setAttribute ("phoss.ap.transaction.id", sTxID))
 *   {
 *     ... business work ...
 *     aSpan.setStatusOk ();
 *   }
 *   catch (final RuntimeException ex)
 *   {
 *     aSpan.recordException (ex).setStatusError (ex.getMessage ());
 *     throw ex;
 *   }
 * </pre>
 *
 * All mutator methods return {@code this} for fluent chaining. All methods must be safe to call
 * after {@link #close()} as no-ops (so a misordered {@code finally} cannot crash the AP). The
 * default {@link AutoCloseable#close()} contract is overridden to declare no checked exceptions.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
public interface IAPSpan extends AutoCloseable
{
  /**
   * Set a string-valued attribute on this span.
   *
   * @param sKey
   *        The attribute key (typically a constant from CPhossAPOtel). Never <code>null</code>.
   * @param sValue
   *        The value. May be <code>null</code>, in which case the attribute is not recorded.
   * @return this. Never <code>null</code>.
   */
  @NonNull
  IAPSpan setAttribute (@NonNull String sKey, @Nullable String sValue);

  /**
   * Set a boolean-valued attribute on this span.
   *
   * @param sKey
   *        The attribute key. Never <code>null</code>.
   * @param bValue
   *        The value.
   * @return this. Never <code>null</code>.
   */
  @NonNull
  IAPSpan setAttribute (@NonNull String sKey, boolean bValue);

  /**
   * Set a long-valued attribute on this span.
   *
   * @param sKey
   *        The attribute key. Never <code>null</code>.
   * @param nValue
   *        The value.
   * @return this. Never <code>null</code>.
   */
  @NonNull
  IAPSpan setAttribute (@NonNull String sKey, long nValue);

  /**
   * Set a long-valued attribute on this span.
   *
   * @param sKey
   *        The attribute key. Never <code>null</code>.
   * @param dValue
   *        The value.
   * @return this. Never <code>null</code>.
   */
  @NonNull
  IAPSpan setAttribute (@NonNull String sKey, double dValue);

  /**
   * Attach an exception as a span event.
   *
   * @param aException
   *        The exception. Never <code>null</code>.
   * @return this. Never <code>null</code>.
   */
  @NonNull
  IAPSpan recordException (@NonNull Throwable aException);

  /**
   * Mark this span as having completed successfully. Optional — leaving the status unset is also
   * treated as success by most observability backends.
   *
   * @return this. Never <code>null</code>.
   */
  @NonNull
  IAPSpan setStatusOk ();

  /**
   * Mark this span as having failed.
   *
   * @param sMessage
   *        Optional message describing the failure. May be <code>null</code>.
   * @return this. Never <code>null</code>.
   */
  @NonNull
  IAPSpan setStatusError (@Nullable String sMessage);

  /**
   * End this span and detach it from the current thread context. After this call all mutator
   * methods are no-ops.
   */
  void close ();
}
