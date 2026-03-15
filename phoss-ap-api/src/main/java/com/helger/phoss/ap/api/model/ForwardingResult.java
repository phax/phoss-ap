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
package com.helger.phoss.ap.api.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.state.ISuccessIndicator;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;

/**
 * Immutable result of a document forwarding operation. Carries structured error
 * information on failure so that callers can record meaningful error codes and
 * details.
 *
 * @author Philip Helger
 */
@Immutable
public final class ForwardingResult implements ISuccessIndicator
{
  private static final ForwardingResult SUCCESS = new ForwardingResult (true, null, null, null);

  private final boolean m_bSuccess;
  private final String m_sCountryCodeC4;
  private final String m_sErrorCode;
  private final String m_sErrorDetails;

  private ForwardingResult (final boolean bSuccess,
                            @Nullable final String sCountryCodeC4,
                            @Nullable final String sErrorCode,
                            @Nullable final String sErrorDetails)
  {
    m_bSuccess = bSuccess;
    m_sCountryCodeC4 = sCountryCodeC4;
    m_sErrorCode = sErrorCode;
    m_sErrorDetails = sErrorDetails;
  }

  public boolean isSuccess ()
  {
    return m_bSuccess;
  }

  /**
   * @return <code>true</code> if the result contains the country code of C4.
   *         This can only happen in the success case.
   */
  public boolean hasCountryCodeC4 ()
  {
    return StringHelper.isNotEmpty (m_sCountryCodeC4);
  }

  /**
   * @return The C4 country code from the forwarding response, or
   *         <code>null</code> if not available.
   */
  @Nullable
  public String getCountryCodeC4 ()
  {
    return m_sCountryCodeC4;
  }

  /**
   * @return The machine-readable error code classifying the failure, or
   *         <code>null</code> on success.
   */
  @Nullable
  public String getErrorCode ()
  {
    return m_sErrorCode;
  }

  /**
   * @return The human-readable error description, or <code>null</code> on
   *         success.
   */
  @Nullable
  public String getErrorDetails ()
  {
    return m_sErrorDetails;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Success", m_bSuccess)
                                       .appendIfNotNull ("CountryCodeC4", m_sCountryCodeC4)
                                       .appendIfNotNull ("ErrorCode", m_sErrorCode)
                                       .appendIfNotNull ("ErrorDetails", m_sErrorDetails)
                                       .getToString ();
  }

  /**
   * @return A successful forwarding result without a C4 country code.
   */
  @NonNull
  public static ForwardingResult success ()
  {
    return SUCCESS;
  }

  /**
   * Create a successful forwarding result with an optional C4 country code.
   *
   * @param sCountryCodeC4
   *        The C4 country code from the forwarding response. May be
   *        <code>null</code>.
   * @return A new success result. Never <code>null</code>.
   */
  @NonNull
  public static ForwardingResult success (@Nullable final String sCountryCodeC4)
  {
    return new ForwardingResult (true, sCountryCodeC4, null, null);
  }

  /**
   * Create a failure result with error details.
   *
   * @param sErrorCode
   *        Machine-readable error code. May be <code>null</code>.
   * @param sErrorDetails
   *        Human-readable error description. May be <code>null</code>.
   * @return A new failure result. Never <code>null</code>.
   */
  @NonNull
  public static ForwardingResult failure (@Nullable final String sErrorCode, @Nullable final String sErrorDetails)
  {
    return new ForwardingResult (false, null, sErrorCode, sErrorDetails);
  }
}
