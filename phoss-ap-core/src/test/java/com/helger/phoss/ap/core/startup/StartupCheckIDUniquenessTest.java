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

import static org.junit.Assert.assertEquals;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.model.VerificationOutcome;
import com.helger.phoss.ap.api.spi.IDocumentForwarderProviderSPI;
import com.helger.phoss.ap.api.spi.IDocumentVerifier;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.api.spi.IOutboundDocumentVerifierSPI;

/**
 * Test class for the ID uniqueness checks of {@link StartupCheckDocumentVerifiers} and
 * {@link StartupCheckForwarding}. See
 * <a href="https://github.com/phax/phoss-ap/issues/80">issue 80</a>.
 *
 * @author Philip Helger
 */
public final class StartupCheckIDUniquenessTest
{
  /** A verifier that only carries an ID - the verification itself is irrelevant here */
  private static class MockInboundVerifier implements IInboundDocumentVerifierSPI
  {
    private final String m_sID;

    MockInboundVerifier (@NonNull @Nonempty final String sID)
    {
      m_sID = sID;
    }

    @NonNull
    @Nonempty
    public String getID ()
    {
      return m_sID;
    }

    @NonNull
    public VerificationOutcome verifyInboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                                      @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                      @NonNull final IProcessIdentifier aProcessID)
    {
      return VerificationOutcome.passed ();
    }
  }

  /** A second, unrelated implementation class using the same kind of ID */
  private static final class OtherInboundVerifier extends MockInboundVerifier
  {
    OtherInboundVerifier (@NonNull @Nonempty final String sID)
    {
      super (sID);
    }
  }

  /** A verifier that serves the inbound and the outbound direction, like the bundled one */
  private static final class MockBothDirectionsVerifier extends MockInboundVerifier implements
                                                        IOutboundDocumentVerifierSPI
  {
    MockBothDirectionsVerifier (@NonNull @Nonempty final String sID)
    {
      super (sID);
    }

    @NonNull
    public VerificationOutcome verifyOutboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                                       @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                       @NonNull final IProcessIdentifier aProcessID)
    {
      return VerificationOutcome.passed ();
    }
  }

  private static class MockForwarderProvider implements IDocumentForwarderProviderSPI
  {
    private final String m_sID;

    MockForwarderProvider (@NonNull @Nonempty final String sID)
    {
      m_sID = sID;
    }

    @NonNull
    @Nonempty
    public String getID ()
    {
      return m_sID;
    }

    @NonNull
    public IDocumentForwarder createDocumentForwarder ()
    {
      throw new UnsupportedOperationException ();
    }
  }

  private static final class OtherForwarderProvider extends MockForwarderProvider
  {
    OtherForwarderProvider (@NonNull @Nonempty final String sID)
    {
      super (sID);
    }
  }

  private static int _verifierErrors (@NonNull final Iterable <? extends IDocumentVerifier> aVerifiers)
  {
    final ErrorList aErrors = new ErrorList ();
    StartupCheckDocumentVerifiers.checkVerifierIDsAreUnique (aVerifiers, aErrors);
    return aErrors.size ();
  }

  private static int _providerErrors (@NonNull final Iterable <? extends IDocumentForwarderProviderSPI> aProviders)
  {
    final ErrorList aErrors = new ErrorList ();
    StartupCheckForwarding.checkForwarderProviderIDsAreUnique (aProviders, aErrors);
    return aErrors.size ();
  }

  @Test
  public void testUniqueVerifierIDsAreAccepted ()
  {
    assertEquals (0, _verifierErrors (new CommonsArrayList <> (new MockInboundVerifier ("a"),
                                                               new OtherInboundVerifier ("b"))));

    // No verifier at all is fine as well
    assertEquals (0, _verifierErrors (new CommonsArrayList <> ()));
  }

  @Test
  public void testSameVerifierClassInBothDirectionsIsNoDuplicate ()
  {
    // A verifier implementing both SPIs is loaded once per SPI, so the very same ID shows up twice
    // - that is not a duplicate
    assertEquals (0, _verifierErrors (new CommonsArrayList <> (new MockBothDirectionsVerifier ("phorm"),
                                                               new MockBothDirectionsVerifier ("phorm"))));
  }

  @Test
  public void testDuplicateVerifierIDIsRejected ()
  {
    assertEquals (1, _verifierErrors (new CommonsArrayList <> (new MockInboundVerifier ("dup"),
                                                               new OtherInboundVerifier ("dup"))));
  }

  @Test
  public void testEmptyVerifierIDIsRejected ()
  {
    assertEquals (1, _verifierErrors (new CommonsArrayList <> (new MockInboundVerifier (""))));
  }

  @Test
  public void testUniqueForwarderProviderIDsAreAccepted ()
  {
    assertEquals (0, _providerErrors (new CommonsArrayList <> (new MockForwarderProvider ("a"),
                                                               new OtherForwarderProvider ("b"))));
    assertEquals (0, _providerErrors (new CommonsArrayList <> ()));
  }

  @Test
  public void testDuplicateForwarderProviderIDIsRejected ()
  {
    assertEquals (1, _providerErrors (new CommonsArrayList <> (new MockForwarderProvider ("dup"),
                                                               new OtherForwarderProvider ("dup"))));
  }

  @Test
  public void testEmptyForwarderProviderIDIsRejected ()
  {
    assertEquals (1, _providerErrors (new CommonsArrayList <> (new MockForwarderProvider (""))));
  }
}
