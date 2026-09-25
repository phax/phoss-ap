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
package com.helger.phoss.ap.api.dto;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST API response DTO containing the complete chronological timeline and replay indicator for a transaction.
 *
 * @author Philip Helger
 */
@Schema (description = "Full lifecycle history and operator audit timeline for a transaction.")
public class TransactionTimelineResponse
{
  @Schema (description = "Peppol SBDH Instance Identifier", example = "550e8400-e29b-41d4-a716-446655440000")
  private String sbdhInstanceID;

  @Schema (description = "Direction: INBOUND or OUTBOUND", example = "INBOUND")
  private String direction;

  @Schema (description = "Whether this transaction has ever been manually replayed or re-verified by an operator")
  private boolean isReplayed;

  @Schema (description = "Total number of times a replay or manual operational action was triggered")
  private int replayCount;

  @Schema (description = "Chronological list of all lifecycle status updates and operator actions")
  private List <TimelineEventItem> events = new ArrayList <> ();

  public TransactionTimelineResponse ()
  {}

  public TransactionTimelineResponse (@NonNull final String sSbdhInstanceID,
                                      @NonNull final String sDirection,
                                      @NonNull final List <TimelineEventItem> aEvents)
  {
    this.sbdhInstanceID = sSbdhInstanceID;
    this.direction = sDirection;
    this.events = aEvents;
    int nReplays = 0;
    for (final TimelineEventItem aEvt : aEvents)
    {
      final String sAction = aEvt.getAction ();
      if (sAction != null && (sAction.contains ("REPLAY") || sAction.contains ("REVERIFY")))
        nReplays++;
    }
    this.replayCount = nReplays;
    this.isReplayed = nReplays > 0;
  }

  @Nullable
  public String getSbdhInstanceID ()
  {
    return sbdhInstanceID;
  }

  public void setSbdhInstanceID (@Nullable final String s)
  {
    this.sbdhInstanceID = s;
  }

  @Nullable
  public String getDirection ()
  {
    return direction;
  }

  public void setDirection (@Nullable final String s)
  {
    this.direction = s;
  }

  public boolean isReplayed ()
  {
    return isReplayed;
  }

  public boolean getIsReplayed ()
  {
    return isReplayed;
  }

  public void setReplayed (final boolean b)
  {
    this.isReplayed = b;
  }

  public int getReplayCount ()
  {
    return replayCount;
  }

  public void setReplayCount (final int n)
  {
    this.replayCount = n;
  }

  @NonNull
  public List <TimelineEventItem> getEvents ()
  {
    return events;
  }

  public void setEvents (@NonNull final List <TimelineEventItem> aList)
  {
    this.events = aList;
  }

  @NonNull
  @Schema (hidden = true)
  public IJsonObject toJson ()
  {
    final IJsonObject ret = new JsonObject ();
    if (sbdhInstanceID != null)
      ret.add ("sbdhInstanceID", sbdhInstanceID);
    if (direction != null)
      ret.add ("direction", direction);
    ret.add ("isReplayed", isReplayed);
    ret.add ("replayCount", replayCount);

    final IJsonArray aArr = new JsonArray ();
    for (final TimelineEventItem aEvt : events)
      aArr.add (aEvt.toJson ());
    ret.add ("events", aArr);

    return ret;
  }
}
