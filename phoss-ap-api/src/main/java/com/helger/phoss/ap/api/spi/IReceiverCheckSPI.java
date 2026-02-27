/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import com.helger.annotation.style.IsSPIInterface;

/**
 * SPI interface for verifying that this AP services a given receiver
 * participant. Implementations are loaded via {@link java.util.ServiceLoader}.
 * If the receiver is not serviced, the inbound AS4 message is rejected before
 * database storage.
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface IReceiverCheckSPI
{
  /**
   * Check whether the given receiver participant is serviced by this AP for the
   * specified document type and process.
   *
   * @param sReceiverID
   *        The Peppol Participant ID of the receiver. Never <code>null</code>.
   * @param sDocTypeID
   *        The Peppol Document Type Identifier. Never <code>null</code>.
   * @param sProcessID
   *        The Peppol Process Identifier. Never <code>null</code>.
   * @return <code>true</code> if the receiver is serviced, <code>false</code>
   *         otherwise.
   */
  boolean isReceiverServiced (@NonNull String sReceiverID,
                              @NonNull String sDocTypeID,
                              @NonNull String sProcessID);
}
