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
package com.helger.phoss.ap.core.inbound;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phoss.ap.api.mgr.IDocumentForwarder;
import com.helger.phoss.ap.api.model.ForwardingResult;
import com.helger.phoss.ap.api.model.IForwardableDocument;

/**
 * A document forwarder whose results are controlled by the test, per transaction ID. A transaction
 * without a queued result is forwarded successfully. All instances share the same state, because
 * the instance used by the AP is created via {@link MockControlledForwarderProviderSPI}.
 *
 * @author Philip Helger
 */
public final class MockControlledForwarder implements IDocumentForwarder
{
  public static final String ID = "test-controlled";

  private static final Map <String, Deque <ForwardingResult>> RESULTS = new ConcurrentHashMap <> ();
  private static final Map <String, AtomicInteger> CALLS = new ConcurrentHashMap <> ();

  /**
   * Queue the result of the next forwarding of the provided transaction.
   *
   * @param sTxID
   *        The transaction ID. May not be <code>null</code>.
   * @param aResult
   *        The result to be returned. May not be <code>null</code>.
   */
  public static void queueResult (@NonNull final String sTxID, @NonNull final ForwardingResult aResult)
  {
    ValueEnforcer.notNull (sTxID, "TxID");
    ValueEnforcer.notNull (aResult, "Result");
    RESULTS.computeIfAbsent (sTxID, k -> new ArrayDeque <> ()).add (aResult);
  }

  /**
   * @param sTxID
   *        The transaction ID. May not be <code>null</code>.
   * @return How often the provided transaction was forwarded so far.
   */
  public static int getCallCount (@NonNull final String sTxID)
  {
    final AtomicInteger aCount = CALLS.get (sTxID);
    return aCount == null ? 0 : aCount.get ();
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return ID;
  }

  @NonNull
  public ESuccess initFromConfiguration (@NonNull final IConfigWithFallback aConfig, @NonNull final String sKeyPrefix)
  {
    return ESuccess.SUCCESS;
  }

  @NonNull
  public ForwardingResult forwardDocument (@NonNull final IForwardableDocument aDocument)
  {
    final String sTxID = aDocument.id ();
    CALLS.computeIfAbsent (sTxID, k -> new AtomicInteger ()).incrementAndGet ();

    final Deque <ForwardingResult> aQueue = RESULTS.get (sTxID);
    final ForwardingResult aResult = aQueue == null ? null : aQueue.poll ();
    // No C4 country code, so that no Peppol Reporting item is created
    return aResult != null ? aResult : ForwardingResult.success ();
  }

  public boolean isWithDeliveryConfirmation ()
  {
    return true;
  }
}
