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
package com.helger.phoss.ap.api;

import org.jspecify.annotations.NonNull;

import com.helger.base.state.ESuccess;

/**
 * Base interface for archiving transactions.
 *
 * @author Philip Helger
 */
public interface IArchivalManager
{
  /**
   * Archive the provided outbound transaction. This includes the main
   * transaction as well as all attempts.
   *
   * @param sID
   *        The outbound transaction to archive. May not be <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  ESuccess archiveOutboundTransaction (@NonNull String sID);

  /**
   * Archive the provided inbound transaction. This includes the main
   * transaction as well as all attempts.
   *
   * @param sID
   *        The inbound transaction to archive. May not be <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  ESuccess archiveInboundTransaction (@NonNull String sID);
}
