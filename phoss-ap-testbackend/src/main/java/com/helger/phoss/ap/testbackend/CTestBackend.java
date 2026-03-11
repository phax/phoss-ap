package com.helger.phoss.ap.testbackend;

import com.helger.annotation.concurrent.Immutable;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

/**
 * Constants for the test backend
 *
 * @author Philip Helger
 */
@Immutable
public final class CTestBackend
{
  public static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;

  private CTestBackend ()
  {}
}
