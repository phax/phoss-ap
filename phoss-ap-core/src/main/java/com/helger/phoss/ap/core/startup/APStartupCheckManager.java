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

import java.util.Comparator;
import java.util.ServiceLoader;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.ap.api.spi.IAPStartupCheckSPI;

/**
 * Manager for the startup checks. The built-in checks come first, in the order of
 * {@link APBuiltInStartupChecks#getAll()}, followed by the checks loaded via
 * {@link ServiceLoader}, ordered by their name - the {@link ServiceLoader} order is not part of
 * its contract. Manually registered checks come last, in registration order - also if they were
 * registered before {@link #initSPI()} was called.
 *
 * @author Philip Helger
 * @since 0.13.3
 */
public final class APStartupCheckManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APStartupCheckManager.class);

  private static final ICommonsList <IAPStartupCheckSPI> s_aChecks = new CommonsArrayList <> ();
  private static final ICommonsList <IAPStartupCheckSPI> s_aManualChecks = new CommonsArrayList <> ();
  private static boolean s_bInitialized = false;

  private APStartupCheckManager ()
  {}

  /**
   * Register the built-in checks and load all SPI checks. May only be called once upon
   * initialization.
   */
  public static void initSPI ()
  {
    if (s_bInitialized)
      throw new InitializationException ("The Startup Checks were already initialized");
    s_bInitialized = true;

    for (final IAPStartupCheckSPI aCheck : APBuiltInStartupChecks.getAll ())
      s_aChecks.add (aCheck);

    final ICommonsList <IAPStartupCheckSPI> aSPIChecks = new CommonsArrayList <> ();
    for (final IAPStartupCheckSPI aCheck : ServiceLoader.load (IAPStartupCheckSPI.class))
      aSPIChecks.add (aCheck);
    aSPIChecks.sort (Comparator.comparing (IAPStartupCheckSPI::getCheckName));
    for (final IAPStartupCheckSPI aCheck : aSPIChecks)
    {
      s_aChecks.add (aCheck);
      LOGGER.info ("Loaded startup check '" + aCheck.getCheckName () + "': " + aCheck.getClass ().getName ());
    }
  }

  /**
   * Manually register a new check. It is run after the built-in and the SPI checks and after all
   * checks that were manually registered before. May be called before {@link #initSPI()}.
   *
   * @param aCheck
   *        The check to be registered. May not be <code>null</code>.
   */
  public static void registerCheck (@NonNull final IAPStartupCheckSPI aCheck)
  {
    ValueEnforcer.notNull (aCheck, "Check");
    s_aManualChecks.add (aCheck);
  }

  /**
   * @return A copy of all registered checks in the order they are run. Never <code>null</code> but
   *         maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IAPStartupCheckSPI> getAllChecks ()
  {
    final ICommonsList <IAPStartupCheckSPI> ret = s_aChecks.getClone ();
    ret.addAll (s_aManualChecks);
    return ret;
  }

  /**
   * Remove all registered checks, so that {@link #initSPI()} can be called again.
   */
  @VisibleForTesting
  static void reset ()
  {
    s_aChecks.clear ();
    s_aManualChecks.clear ();
    s_bInitialized = false;
  }
}
