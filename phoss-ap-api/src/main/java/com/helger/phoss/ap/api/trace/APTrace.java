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

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;

/**
 * Static facade for the trace abstraction. Resolves the registered {@link IAPTracerSPI} on first
 * use (lazily via {@link ServiceLoader}); falls back to an internal no-op tracer if no
 * implementation is on the classpath.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
@ThreadSafe
public final class APTrace
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APTrace.class);
  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();

  private static volatile IAPTracerSPI s_aTracer;

  private APTrace ()
  {}

  /**
   * Install a custom tracer. Intended primarily for tests. Pass <code>null</code> to revert to the
   * SPI-discovered tracer.
   *
   * @param aTracer
   *        The tracer to install, or <code>null</code> to revert.
   */
  public static void install (@Nullable final IAPTracerSPI aTracer)
  {
    RW_LOCK.writeLocked ( () -> s_aTracer = aTracer);
    if (aTracer != null)
      LOGGER.info ("Installed custom AP tracer: " + aTracer.getClass ().getName ());
  }

  @NonNull
  private static IAPTracerSPI _resolveTracer ()
  {
    final IAPTracerSPI aFast = s_aTracer;
    if (aFast != null)
      return aFast;

    return RW_LOCK.writeLockedGet ( () -> {
      IAPTracerSPI aRet = s_aTracer;
      if (aRet == null)
      {
        final Iterator <IAPTracerSPI> aIt = ServiceLoader.load (IAPTracerSPI.class).iterator ();
        if (aIt.hasNext ())
        {
          aRet = aIt.next ();
          LOGGER.info ("Resolved AP tracer SPI: " + aRet.getClass ().getName ());
        }
        else
        {
          aRet = NoOpAPTracer.INSTANCE;
          LOGGER.debug ("No IAPTracerSPI registered - using no-op tracer");
        }
        s_aTracer = aRet;
      }
      return aRet;
    });
  }

  /**
   * Start a new span. See {@link IAPTracerSPI#startSpan(String, EAPSpanKind)}.
   *
   * @param sName
   *        The span name. Never <code>null</code>.
   * @param eKind
   *        The span kind. Never <code>null</code>.
   * @return A new active span. Never <code>null</code>.
   */
  @NonNull
  public static IAPSpan startSpan (@NonNull final String sName, @NonNull final EAPSpanKind eKind)
  {
    return _resolveTracer ().startSpan (sName, eKind);
  }

  /**
   * Run {@code aBody} inside a fresh span. The span is started, the body is executed with the span
   * passed in (so it can set attributes / status), exceptions are recorded on the span (with status
   * ERROR) and re-thrown, and the span is ended in a {@code finally} block.
   *
   * @param sName
   *        The span name. Never <code>null</code>.
   * @param eKind
   *        The span kind. Never <code>null</code>.
   * @param aBody
   *        The body. Never <code>null</code>. Receives the active span.
   * @param <T>
   *        Body return type.
   * @return The value returned by the body.
   */
  public static <T> T withSpan (@NonNull final String sName,
                                @NonNull final EAPSpanKind eKind,
                                @NonNull final Function <IAPSpan, T> aBody)
  {
    try (final IAPSpan aSpan = startSpan (sName, eKind))
    {
      try
      {
        return aBody.apply (aSpan);
      }
      catch (final RuntimeException ex)
      {
        aSpan.recordException (ex).setStatusError (ex.getMessage ());
        throw ex;
      }
    }
  }

  // === No-op fallback ===

  private static final class NoOpAPTracer implements IAPTracerSPI
  {
    static final NoOpAPTracer INSTANCE = new NoOpAPTracer ();

    private NoOpAPTracer ()
    {}

    @NonNull
    public IAPSpan startSpan (@NonNull final String sName, @NonNull final EAPSpanKind eKind)
    {
      return NoOpAPSpan.INSTANCE;
    }
  }

  private static final class NoOpAPSpan implements IAPSpan
  {
    static final NoOpAPSpan INSTANCE = new NoOpAPSpan ();

    private NoOpAPSpan ()
    {}

    @NonNull
    public IAPSpan setAttribute (@NonNull final String sKey, @Nullable final String sValue)
    {
      return this;
    }

    @NonNull
    public IAPSpan setAttribute (@NonNull final String sKey, final boolean bValue)
    {
      return this;
    }

    @NonNull
    public IAPSpan setAttribute (@NonNull final String sKey, final long nValue)
    {
      return this;
    }

    @NonNull
    public IAPSpan setAttribute (@NonNull final String sKey, final double dValue)
    {
      return this;
    }

    @NonNull
    public IAPSpan recordException (@NonNull final Throwable aException)
    {
      return this;
    }

    @NonNull
    public IAPSpan setStatusOk ()
    {
      return this;
    }

    @NonNull
    public IAPSpan setStatusError (@Nullable final String sMessage)
    {
      return this;
    }

    public void close ()
    {}
  }
}
