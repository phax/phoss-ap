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

import static org.junit.Assert.assertEquals;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringImplode;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.ap.api.model.VerificationOutcome;
import com.helger.phoss.ap.api.spi.IDocumentVerifier;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;

/**
 * Test class for the order of the document verifiers defined by
 * {@link APCoreMetaManager#VERIFIER_ORDER}. See
 * <a href="https://github.com/phax/phoss-ap/issues/130">issue 130</a>.
 *
 * @author Philip Helger
 */
public final class APCoreMetaManagerVerifierOrderTest
{
  /** A verifier that only carries an ID and a priority - the verification itself is irrelevant */
  private static final class MockInboundVerifier implements IInboundDocumentVerifierSPI
  {
    private final String m_sID;
    private final int m_nPriority;

    MockInboundVerifier (@NonNull @Nonempty final String sID, final int nPriority)
    {
      m_sID = sID;
      m_nPriority = nPriority;
    }

    @NonNull
    @Nonempty
    public String getID ()
    {
      return m_sID;
    }

    @Override
    public int getPriority ()
    {
      return m_nPriority;
    }

    @NonNull
    public VerificationOutcome verifyInboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                                      @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                      @NonNull final IProcessIdentifier aProcessID)
    {
      return VerificationOutcome.passed ();
    }
  }

  @NonNull
  private static String _getOrderedIDs (@NonNull final ICommonsList <? extends IDocumentVerifier> aVerifiers)
  {
    aVerifiers.sort (APCoreMetaManager.VERIFIER_ORDER);
    return StringImplode.getImplodedMapped (",", aVerifiers, IDocumentVerifier::getID);
  }

  @Test
  public void testLowerPriorityFirst ()
  {
    assertEquals ("virusscan,phorm",
                  _getOrderedIDs (new CommonsArrayList <> (new MockInboundVerifier ("phorm", 100),
                                                           new MockInboundVerifier ("virusscan", 0))));
    assertEquals ("first,second,third",
                  _getOrderedIDs (new CommonsArrayList <> (new MockInboundVerifier ("third", 5),
                                                           new MockInboundVerifier ("first", -5),
                                                           new MockInboundVerifier ("second", 0))));
  }

  @Test
  public void testSamePriorityOrderedByID ()
  {
    assertEquals ("a,b,c",
                  _getOrderedIDs (new CommonsArrayList <> (new MockInboundVerifier ("c", 0),
                                                           new MockInboundVerifier ("a", 0),
                                                           new MockInboundVerifier ("b", 0))));
  }

  @Test
  public void testDefaultPriority ()
  {
    final IInboundDocumentVerifierSPI aVerifier = new IInboundDocumentVerifierSPI ()
    {
      @NonNull
      @Nonempty
      public String getID ()
      {
        return "default";
      }

      @NonNull
      public VerificationOutcome verifyInboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                                        @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                                        @NonNull final IProcessIdentifier aProcessID)
      {
        return VerificationOutcome.passed ();
      }
    };
    assertEquals (IDocumentVerifier.DEFAULT_PRIORITY, aVerifier.getPriority ());
  }
}
