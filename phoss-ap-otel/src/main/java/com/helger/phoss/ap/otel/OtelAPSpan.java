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
package com.helger.phoss.ap.otel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.phoss.ap.api.trace.IAPSpan;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * {@link IAPSpan} backed by an OpenTelemetry {@link Span} that has been made the current span on
 * the calling thread via {@link Span#makeCurrent()}. The returned {@link Scope} is kept inside this
 * wrapper and closed together with the span on {@link #close()}.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
final class OtelAPSpan implements IAPSpan
{
  private final Span m_aSpan;
  private final Scope m_aScope;
  private boolean m_bClosed;

  OtelAPSpan (@NonNull final Span aSpan)
  {
    m_aSpan = aSpan;
    m_aScope = aSpan.makeCurrent ();
  }

  @NonNull
  public IAPSpan setAttribute (@NonNull final String sKey, @Nullable final String sValue)
  {
    if (sValue != null)
      m_aSpan.setAttribute (sKey, sValue);
    return this;
  }

  @NonNull
  public IAPSpan setAttribute (@NonNull final String sKey, final boolean bValue)
  {
    m_aSpan.setAttribute (sKey, bValue);
    return this;
  }

  @NonNull
  public IAPSpan setAttribute (@NonNull final String sKey, final long nValue)
  {
    m_aSpan.setAttribute (sKey, nValue);
    return this;
  }

  @NonNull
  public IAPSpan setAttribute (@NonNull final String sKey, final double dValue)
  {
    m_aSpan.setAttribute (sKey, dValue);
    return this;
  }

  @NonNull
  public IAPSpan recordException (@NonNull final Throwable aException)
  {
    m_aSpan.recordException (aException);
    return this;
  }

  @NonNull
  public IAPSpan setStatusOk ()
  {
    m_aSpan.setStatus (StatusCode.OK);
    return this;
  }

  @NonNull
  public IAPSpan setStatusError (@Nullable final String sMessage)
  {
    if (sMessage != null)
      m_aSpan.setStatus (StatusCode.ERROR, sMessage);
    else
      m_aSpan.setStatus (StatusCode.ERROR);
    return this;
  }

  public void close ()
  {
    if (!m_bClosed)
    {
      m_bClosed = true;
      // First detach the scope (so the previous current span is restored), then end the span.
      m_aScope.close ();
      m_aSpan.end ();
    }
  }
}
