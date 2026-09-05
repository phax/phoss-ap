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
package com.helger.phoss.ap.core.mls;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.state.ESuccess;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.core.outbound.MlsSmpFallback;

/**
 * The result of creating the MLS of an inbound transaction, and everything the subsequent sending
 * of that MLS needs. It exists so that the creation - which persists the outbound transaction and
 * therefore yields the MLS transaction ID - can be answered to an API caller before the AS4
 * transmission is even started.
 * <p>
 * Three outcomes are distinguished:
 * </p>
 * <ul>
 * <li>created - {@link #isSuccess()} and {@link #hasMlsTx()} are both <code>true</code>: the MLS
 * document exists, the outbound transaction is persisted and it is waiting to be sent.</li>
 * <li>suppressed - {@link #isSuccess()} is <code>true</code> but {@link #hasMlsTx()} is
 * <code>false</code>: nothing goes on the wire. This is the case for the global kill switch
 * {@code mls.sending.enabled=false} - which records nothing at all - and for a successful response
 * code with {@link com.helger.peppol.sbdh.EPeppolMLSType#FAILURE_ONLY}, where the response code is
 * recorded on the inbound transaction nevertheless.</li>
 * <li>failed - {@link #isFailure()} is <code>true</code>: the MLS could not be built, serialized or
 * persisted. The details are in the log.</li>
 * </ul>
 *
 * @param success
 *        Whether the creation succeeded. May not be <code>null</code>.
 * @param responseCode
 *        The MLS response code of the created MLS. <code>null</code> only if the creation failed
 *        before the response code was known.
 * @param mlsTx
 *        The created outbound MLS transaction. <code>null</code> if the MLS was suppressed or the
 *        creation failed.
 * @param smpFallback
 *        The MLS specific SMP lookup fallback to use for the sending, according to MLS SPOG section
 *        5.4. <code>null</code> if and only if {@code mlsTx} is <code>null</code>.
 * @author Philip Helger
 * @since 0.13.0
 */
public record MlsCreationResult (@NonNull ESuccess success,
                                 @Nullable EPeppolMLSResponseCode responseCode,
                                 @Nullable IOutboundTransaction mlsTx,
                                 @Nullable MlsSmpFallback smpFallback)
{
  /**
   * @return <code>true</code> if the creation succeeded - which includes a deliberately suppressed
   *         MLS.
   */
  public boolean isSuccess ()
  {
    return success.isSuccess ();
  }

  /**
   * @return <code>true</code> if the creation failed.
   */
  public boolean isFailure ()
  {
    return success.isFailure ();
  }

  /**
   * @return <code>true</code> if an outbound MLS transaction was created and is waiting to be sent.
   */
  public boolean hasMlsTx ()
  {
    return mlsTx != null;
  }

  /**
   * @return The ID of the created outbound MLS transaction, or <code>null</code> if no MLS
   *         transaction was created.
   */
  @Nullable
  public String getMlsTxID ()
  {
    return mlsTx == null ? null : mlsTx.getID ();
  }

  /**
   * Create the result of an MLS that was created and is waiting to be sent.
   *
   * @param eResponseCode
   *        The MLS response code. May not be <code>null</code>.
   * @param aMlsTx
   *        The created outbound MLS transaction. May not be <code>null</code>.
   * @param aSmpFallback
   *        The MLS specific SMP lookup fallback. May not be <code>null</code>.
   * @return A new {@link MlsCreationResult}. Never <code>null</code>.
   */
  @NonNull
  public static MlsCreationResult created (@NonNull final EPeppolMLSResponseCode eResponseCode,
                                           @NonNull final IOutboundTransaction aMlsTx,
                                           @NonNull final MlsSmpFallback aSmpFallback)
  {
    return new MlsCreationResult (ESuccess.SUCCESS, eResponseCode, aMlsTx, aSmpFallback);
  }

  /**
   * Create the result of an MLS that is deliberately not put on the wire.
   *
   * @param eSuccess
   *        Whether recording the response code on the inbound transaction succeeded, or
   *        {@link ESuccess#SUCCESS} if nothing had to be recorded. May not be <code>null</code>.
   * @param eResponseCode
   *        The MLS response code that was recorded. May not be <code>null</code>.
   * @return A new {@link MlsCreationResult}. Never <code>null</code>.
   */
  @NonNull
  public static MlsCreationResult suppressed (@NonNull final ESuccess eSuccess,
                                              @NonNull final EPeppolMLSResponseCode eResponseCode)
  {
    return new MlsCreationResult (eSuccess, eResponseCode, null, null);
  }

  /**
   * Create the result of a failed MLS creation.
   *
   * @param eResponseCode
   *        The intended MLS response code. May be <code>null</code>.
   * @return A new {@link MlsCreationResult}. Never <code>null</code>.
   */
  @NonNull
  public static MlsCreationResult failure (@Nullable final EPeppolMLSResponseCode eResponseCode)
  {
    return new MlsCreationResult (ESuccess.FAILURE, eResponseCode, null, null);
  }
}
