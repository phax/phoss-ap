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
import com.helger.phoss.ap.api.spi.IAPLifecycleEventSPI;

/**
 * Manager for the positive-event lifecycle handlers. The counterpart of
 * {@link NotificationHandlerManager}, but for {@link IAPLifecycleEventSPI}.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
public final class LifecycleEventManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (LifecycleEventManager.class);

  private static final ICommonsList <IAPLifecycleEventSPI> s_aLifecycleHandlers = new CommonsArrayList <> ();

  private LifecycleEventManager ()
  {}

  /**
   * Initialization of all SPI handlers. May only be called once upon initialization.
   */
  public static void initSPI ()
  {
    if (s_aLifecycleHandlers.isNotEmpty ())
      throw new InitializationException ("The Lifecycle Event Handlers were already initialized");

    for (final IAPLifecycleEventSPI aHandler : ServiceLoader.load (IAPLifecycleEventSPI.class))
    {
      s_aLifecycleHandlers.add (new SafeLifecycleEventHandler (aHandler));
      LOGGER.info ("Loaded lifecycle event handler: " + aHandler.getClass ().getName ());
    }
  }

  /**
   * Manually register a new handler.
   *
   * @param aHandler
   *        The handler to be registered. May not be <code>null</code>.
   */
  public static void registerHandler (@NonNull final IAPLifecycleEventSPI aHandler)
  {
    ValueEnforcer.notNull (aHandler, "Handler");
    s_aLifecycleHandlers.add (new SafeLifecycleEventHandler (aHandler));
  }

  /**
   * @return A copy of all registered handlers. Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IAPLifecycleEventSPI> getAllLifecycleHandlers ()
  {
    return s_aLifecycleHandlers.getClone ();
  }
}
