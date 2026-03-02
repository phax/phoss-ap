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

import java.io.File;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.IsSPIInterface;
import com.helger.base.state.ESuccess;

/**
 * SPI interface for optional document verification. Implementations are loaded via
 * {@link java.util.ServiceLoader}. Multiple verifiers may be registered and are evaluated in order
 * — all must pass for the document to be accepted.
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface IOutboundDocumentVerifierSPI
{
  /**
   * Verify a document's content against the given document type and process identifiers.
   *
   * @param aDocument
   *        The path where the document resides. Must only be opened for reading. Never
   *        <code>null</code>.
   * @param sDocTypeID
   *        The Peppol Document Type Identifier. Never <code>null</code>.
   * @param sProcessID
   *        The Peppol Process Identifier. Never <code>null</code>.
   * @return {@link ESuccess#SUCCESS} if the document is valid, {@link ESuccess#FAILURE} if
   *         verification failed.
   */
  @NonNull
  ESuccess verifyDocument (@NonNull File aDocument, @NonNull String sDocTypeID, @NonNull String sProcessID);
}
