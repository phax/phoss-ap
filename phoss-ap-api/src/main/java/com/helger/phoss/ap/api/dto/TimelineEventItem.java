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

import java.time.format.DateTimeFormatter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.phoss.ap.api.model.ITransactionAuditItem;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing an individual event (database status update or operator action) in a transaction timeline.
 *
 * @author Philip Helger
 */
@Schema (description = "A single timeline event for a transaction (status transition or operator intervention).")
public class TimelineEventItem
{
  @Schema (description = "Unique audit record ID")
  private long id;

  @Schema (description = "Internal transaction ID")
  private String transactionID;

  @Schema (description = "SBDH Instance Identifier", example = "550e8400-e29b-41d4-a716-446655440000")
  private String sbdhInstanceID;

  @Schema (description = "Direction: INBOUND or OUTBOUND", example = "INBOUND")
  private String direction;

  @Schema (description = "Event category: STATUS_CHANGE or OPERATOR_ACTION", example = "OPERATOR_ACTION")
  private String eventType;

  @Schema (description = "Action name", example = "REPLAY_INBOUND")
  private String action;

  @Schema (description = "Status before this event", example = "forwarding_failed")
  private String fromStatus;

  @Schema (description = "Status after this event", example = "pending")
  private String toStatus;

  @Schema (description = "Retry/attempt count at event time")
  private Integer attemptCount;

  @Schema (description = "Operator ID, username, email, or SYSTEM", example = "admin@example.org")
  private String performedBy;

  @Schema (description = "Event timestamp in ISO-8601 UTC", example = "2026-09-21T03:30:00Z")
  private String performedDT;

  @Schema (description = "Additional context or reason")
  private String details;

  public TimelineEventItem ()
  {}

  public TimelineEventItem (@NonNull final ITransactionAuditItem aItem)
  {
    this.id = aItem.getID ();
    this.transactionID = aItem.getTransactionID ();
    this.sbdhInstanceID = aItem.getSbdhInstanceID ();
    this.direction = aItem.getDirection ();
    this.eventType = aItem.getEventType ();
    this.action = aItem.getAction ();
    this.fromStatus = aItem.getFromStatus ();
    this.toStatus = aItem.getToStatus ();
    this.attemptCount = aItem.getAttemptCount ();
    this.performedBy = aItem.getPerformedBy ();
    this.performedDT = aItem.getPerformedDT () != null ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format (aItem.getPerformedDT ()) : null;
    this.details = aItem.getDetails ();
  }

  public long getId ()
  {
    return id;
  }

  public void setId (final long n)
  {
    this.id = n;
  }

  @Nullable
  public String getTransactionID ()
  {
    return transactionID;
  }

  public void setTransactionID (@Nullable final String s)
  {
    this.transactionID = s;
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

  @Nullable
  public String getEventType ()
  {
    return eventType;
  }

  public void setEventType (@Nullable final String s)
  {
    this.eventType = s;
  }

  @Nullable
  public String getAction ()
  {
    return action;
  }

  public void setAction (@Nullable final String s)
  {
    this.action = s;
  }

  @Nullable
  public String getFromStatus ()
  {
    return fromStatus;
  }

  public void setFromStatus (@Nullable final String s)
  {
    this.fromStatus = s;
  }

  @Nullable
  public String getToStatus ()
  {
    return toStatus;
  }

  public void setToStatus (@Nullable final String s)
  {
    this.toStatus = s;
  }

  @Nullable
  public Integer getAttemptCount ()
  {
    return attemptCount;
  }

  public void setAttemptCount (@Nullable final Integer n)
  {
    this.attemptCount = n;
  }

  @Nullable
  public String getPerformedBy ()
  {
    return performedBy;
  }

  public void setPerformedBy (@Nullable final String s)
  {
    this.performedBy = s;
  }

  @Nullable
  public String getPerformedDT ()
  {
    return performedDT;
  }

  public void setPerformedDT (@Nullable final String s)
  {
    this.performedDT = s;
  }

  @Nullable
  public String getDetails ()
  {
    return details;
  }

  public void setDetails (@Nullable final String s)
  {
    this.details = s;
  }

  @NonNull
  @Schema (hidden = true)
  public IJsonObject toJson ()
  {
    final IJsonObject ret = new JsonObject ();
    ret.add ("id", id);
    if (transactionID != null)
      ret.add ("transactionID", transactionID);
    if (sbdhInstanceID != null)
      ret.add ("sbdhInstanceID", sbdhInstanceID);
    if (direction != null)
      ret.add ("direction", direction);
    if (eventType != null)
      ret.add ("eventType", eventType);
    if (action != null)
      ret.add ("action", action);
    if (fromStatus != null)
      ret.add ("fromStatus", fromStatus);
    if (toStatus != null)
      ret.add ("toStatus", toStatus);
    if (attemptCount != null)
      ret.add ("attemptCount", attemptCount.intValue ());
    if (performedBy != null)
      ret.add ("performedBy", performedBy);
    if (performedDT != null)
      ret.add ("performedDT", performedDT);
    if (details != null)
      ret.add ("details", details);
    return ret;
  }
}
