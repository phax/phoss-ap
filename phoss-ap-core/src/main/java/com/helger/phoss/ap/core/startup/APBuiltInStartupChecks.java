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
package com.helger.phoss.ap.core.startup;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.spi.IAPStartupCheckSPI;

/**
 * The startup checks that are part of phoss AP core.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public final class APBuiltInStartupChecks
{
  private APBuiltInStartupChecks ()
  {}

  /**
   * @return All built-in startup checks in the order they are run. The order matters within a
   *         phase: e.g. the key store check must come before the certificate check. Never
   *         <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IAPStartupCheckSPI> getAll ()
  {
    return new CommonsArrayList <> (new StartupCheckOutboundTlsTrustStore (),
                                    new StartupCheckPeppolStage (),
                                    new StartupCheckPeppolSeatID (),
                                    new StartupCheckPeppolOwnerCountry (),
                                    new StartupCheckPeppolReceiverCheck (),
                                    new StartupCheckPhase4EndpointAddress (),
                                    new StartupCheckApiToken (),
                                    new StartupCheckDatabase (),
                                    new StartupCheckStorage (),
                                    new StartupCheckForwarding (),
                                    new StartupCheckRetryScheduler (),
                                    new StartupCheckArchivalScheduler (),
                                    new StartupCheckCleanupScheduler (),
                                    new StartupCheckAS4KeyStore (),
                                    new StartupCheckPeppolAPCertificate (),
                                    new StartupCheckPeppolSeatIDCertificate (),
                                    new StartupCheckDocumentVerifiers ());
  }
}
