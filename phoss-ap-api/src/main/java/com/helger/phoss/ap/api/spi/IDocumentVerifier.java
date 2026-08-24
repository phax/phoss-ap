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
import com.helger.base.lang.clazz.ClassHelper;

/**
 * Base interface of the two document verifier SPIs, carrying what both directions have in common.
 * <p>
 * It exists so that a verifier implementing {@link IInboundDocumentVerifierSPI} <em>and</em>
 * {@link IOutboundDocumentVerifierSPI} - like the bundled phorm verifier - inherits a single
 * {@link #getVerifierName()} instead of two unrelated defaults, which Java would reject. This is
 * deliberately <b>not</b> a merge of the two SPIs: the inbound and the outbound verification have
 * different semantics (fail modes, deferral and MLS responses apply to inbound only), so the
 * verification methods stay separate.
 * </p>
 * <p>
 * It deliberately carries no <code>SPI</code> suffix and no
 * {@link com.helger.annotation.style.IsSPIInterface} annotation: it is never loaded via
 * {@link java.util.ServiceLoader} itself, only its two sub-interfaces are.
 * </p>
 *
 * @author Philip Helger
 * @since 0.12.0
 */
public interface IDocumentVerifier
{
  /**
   * @return The name of this verifier, as used in log messages, in the transaction error details,
   *         in the MLS response and in the rejection reported back to an outbound submitter.
   *         Neither <code>null</code> nor empty. By default this is the local class name of the
   *         implementation.
   */
  @NonNull
  @Nonempty
  default String getVerifierName ()
  {
    return ClassHelper.getClassLocalName (this);
  }
}
