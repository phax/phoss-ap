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
package com.helger.phoss.ap.core;

import java.util.ServiceLoader;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.exception.InitializationException;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.codelist.EForwardingMode;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.spi.IDocumentForwarder;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.INotificationHandlerSPI;
import com.helger.phoss.ap.api.spi.IOutboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IPeppolReceiverCheckSPI;
import com.helger.phoss.ap.core.notification.NotificationHandlerManager;
import com.helger.phoss.ap.forwarding.http.HttpDocumentForwarder;
import com.helger.phoss.ap.forwarding.s3.S3DocumentForwarder;
import com.helger.phoss.ap.forwarding.sftp.SftpDocumentForwarder;

public final class APCoreMetaManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APCoreMetaManager.class);

  private static IDocumentForwarder s_aForwarder;
  private static final ICommonsList <IInboundDocumentVerifierSPI> s_aInboundVerifiers = new CommonsArrayList <> ();
  private static final ICommonsList <IOutboundDocumentVerifierSPI> s_aOutboundVerifiers = new CommonsArrayList <> ();
  private static final ICommonsList <IPeppolReceiverCheckSPI> s_aReceiverChecks = new CommonsArrayList <> ();

  private APCoreMetaManager ()
  {}

  public static void init ()
  {
    LOGGER.info ("Initializing APMetaManager");

    // Create forwarder based on configuration
    {
      final String sForwardingMode = APConfigProvider.getConfig ()
                                                     .getAsString (APConfigurationProperties.FORWARDING_MODE);
      final EForwardingMode eForwardingMode = EForwardingMode.getFromIDOrNull (sForwardingMode);
      if (eForwardingMode == null)
        throw new InitializationException ("The configured Forwarding Mode '" + sForwardingMode + "' is invalid");

      final IDocumentForwarder aForwarder = switch (eForwardingMode)
      {
        case HTTP_POST_SYNC, HTTP_POST_ASYNC -> new HttpDocumentForwarder (eForwardingMode);
        case S3_LINK -> new S3DocumentForwarder ();
        case SFTP -> new SftpDocumentForwarder ();
      };
      aForwarder.initFromConfiguration (APConfigProvider.getConfig ());
      s_aForwarder = aForwarder;
      LOGGER.info ("Loaded document forwarder: " + aForwarder.toString ());
    }

    for (final IInboundDocumentVerifierSPI aVerifier : ServiceLoader.load (IInboundDocumentVerifierSPI.class))
    {
      s_aInboundVerifiers.add (aVerifier);
      LOGGER.info ("Loaded inbound document verifier: " + aVerifier.getClass ().getName ());
    }

    for (final IOutboundDocumentVerifierSPI aVerifier : ServiceLoader.load (IOutboundDocumentVerifierSPI.class))
    {
      s_aOutboundVerifiers.add (aVerifier);
      LOGGER.info ("Loaded outbound document verifier: " + aVerifier.getClass ().getName ());
    }

    for (final IPeppolReceiverCheckSPI aCheck : ServiceLoader.load (IPeppolReceiverCheckSPI.class))
    {
      s_aReceiverChecks.add (aCheck);
      LOGGER.info ("Loaded receiver check: " + aCheck.getClass ().getName ());
    }

    NotificationHandlerManager.initSPI ();

    LOGGER.info ("APMetaManager initialized successfully");
  }

  public static void shutdown ()
  {
    LOGGER.info ("Shutting down APMetaManager");
  }

  @NonNull
  public static IDocumentForwarder getForwarder ()
  {
    return s_aForwarder;
  }

  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IInboundDocumentVerifierSPI> getAllInboundVerifiers ()
  {
    return s_aInboundVerifiers.getClone ();
  }

  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IOutboundDocumentVerifierSPI> getAllOutboundVerifiers ()
  {
    return s_aOutboundVerifiers.getClone ();
  }

  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IPeppolReceiverCheckSPI> getAllPeppolReceiverChecks ()
  {
    return s_aReceiverChecks.getClone ();
  }

  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <INotificationHandlerSPI> getAllNotificationHandlers ()
  {
    return NotificationHandlerManager.getAllNotificationHandlers ();
  }
}
