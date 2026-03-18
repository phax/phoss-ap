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
package com.helger.phoss.ap.core.notification;

import java.util.ServiceLoader;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.spi.IAPNotificationHandlerSPI;

/**
 * This class is responsible for managing the different notification handlers.
 *
 * @author Philip Helger
 */
public final class NotificationHandlerManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (NotificationHandlerManager.class);

  private static final ICommonsList <IAPNotificationHandlerSPI> s_aNotificationHandlers = new CommonsArrayList <> ();

  private NotificationHandlerManager ()
  {}

  /**
   * Initialization of all SPI handlers. May only be called once upon initialization.
   */
  public static void initSPI ()
  {
    if (s_aNotificationHandlers.isNotEmpty ())
      throw new InitializationException ("The Notification Handlers were already initialized");

    for (final IAPNotificationHandlerSPI aHandler : ServiceLoader.load (IAPNotificationHandlerSPI.class))
    {
      s_aNotificationHandlers.add (new SafeNotificationHandler (aHandler));
      LOGGER.info ("Loaded notification handler: " + aHandler.getClass ().getName ());
    }
  }

  /**
   * Manually register a new handler.
   *
   * @param aHandler
   *        The handler to be registered. May not be <code>null</code>.
   */
  public static void registerHandler (@NonNull final IAPNotificationHandlerSPI aHandler)
  {
    ValueEnforcer.notNull (aHandler, "Handler");
    s_aNotificationHandlers.add (new SafeNotificationHandler (aHandler));
  }

  /**
   * @return A copy of all registered handlers. Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IAPNotificationHandlerSPI> getAllNotificationHandlers ()
  {
    return s_aNotificationHandlers.getClone ();
  }
}
