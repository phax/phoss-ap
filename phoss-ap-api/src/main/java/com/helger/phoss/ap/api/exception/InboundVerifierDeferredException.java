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
package com.helger.phoss.ap.api.exception;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;

/**
 * Exception thrown by an {@link com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI} when
 * verification cannot be completed due to verifier service unavailability in
 * <code>deferred</code> fail mode, signaling to hold/defer processing without sending an immediate
 * permanent MLS rejection.
 *
 * @author Philip Helger
 */
public class InboundVerifierDeferredException extends RuntimeException
{
  private final String m_sVerifierName;

  /**
   * Constructor.
   *
   * @param sVerifierName
   *        The class or display name of the verifier that was unavailable. May not be
   *        <code>null</code> nor empty.
   * @param sErrorMessage
   *        The error message detailing the service unavailability. May not be <code>null</code> nor
   *        empty.
   */
  public InboundVerifierDeferredException (@NonNull @Nonempty final String sVerifierName,
                                           @NonNull @Nonempty final String sErrorMessage)
  {
    super (sErrorMessage);
    m_sVerifierName = ValueEnforcer.notEmpty (sVerifierName, "VerifierName");
  }

  /**
   * @return The name of the verifier that was unavailable. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getVerifierName ()
  {
    return m_sVerifierName;
  }
}
