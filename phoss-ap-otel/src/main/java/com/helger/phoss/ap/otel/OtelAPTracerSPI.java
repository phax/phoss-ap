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

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.phoss.ap.api.CPhossAPVersion;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.api.trace.EAPSpanKind;
import com.helger.phoss.ap.api.trace.IAPSpan;
import com.helger.phoss.ap.api.trace.IAPTracerSPI;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;

/**
 * Registered via {@link java.util.ServiceLoader} (see
 * {@code META-INF/services/com.helger.phoss.ap.api.trace.IAPTracerSPI}). Resolves the OpenTelemetry
 * {@link Tracer} from {@link GlobalOpenTelemetry} on first use and starts a new active span per
 * call.
 * <p>
 * If the OTel SDK has not been initialised (for example because {@code otel.enabled} is
 * {@code false}), {@code GlobalOpenTelemetry.get ()} returns the official no-op
 * {@code OpenTelemetry} — every span operation through this adapter then becomes a cheap no-op at
 * the SDK level.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
@IsSPIImplementation
public final class OtelAPTracerSPI implements IAPTracerSPI
{
  private static volatile Tracer s_aTracer;

  @NonNull
  private static Tracer _tracer ()
  {
    Tracer aRet = s_aTracer;
    if (aRet == null)
    {
      aRet = GlobalOpenTelemetry.get ()
                                .getTracerProvider ()
                                .tracerBuilder (CPhossAPOtel.INSTRUMENTATION_SCOPE_NAME)
                                .setInstrumentationVersion (CPhossAPVersion.BUILD_VERSION)
                                .build ();
      s_aTracer = aRet;
    }
    return aRet;
  }

  @NonNull
  private static SpanKind _toOtelKind (@NonNull final EAPSpanKind eKind)
  {
    return switch (eKind)
    {
      case INTERNAL -> SpanKind.INTERNAL;
      case CLIENT -> SpanKind.CLIENT;
      case SERVER -> SpanKind.SERVER;
      case PRODUCER -> SpanKind.PRODUCER;
      case CONSUMER -> SpanKind.CONSUMER;
    };
  }

  @NonNull
  public IAPSpan startSpan (@NonNull final String sName, @NonNull final EAPSpanKind eKind)
  {
    final SpanBuilder aBuilder = _tracer ().spanBuilder (sName).setSpanKind (_toOtelKind (eKind));
    final Span aSpan = aBuilder.startSpan ();
    return new OtelAPSpan (aSpan);
  }
}
