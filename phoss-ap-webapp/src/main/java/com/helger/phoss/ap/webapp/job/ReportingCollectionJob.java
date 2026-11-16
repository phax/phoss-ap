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
package com.helger.phoss.ap.webapp.job;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.config.IConfig;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.PeppolReportingJsonHelper;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.backend.sql.PeppolReportingBackendSqlSPI;
import com.helger.phoss.ap.core.APCoreConfig;

/** Collects the previous month's records independently of the report submission scheduler. */
public final class ReportingCollectionJob implements AutoCloseable
{
  static final String PREFIX = "peppol.reporting.collection.";
  private static final Logger LOGGER = LoggerFactory.getLogger (ReportingCollectionJob.class);

  private record Source (String id, URI base, String token) {}

  private final List <Source> m_aSources = new ArrayList <> ();
  private final ReportingCollectionStore m_aStore;
  private final Duration m_aLease;
  private final HttpClient m_aHttpClient;
  private final ScheduledExecutorService m_aExecutor;

  public ReportingCollectionJob (final IConfig aConfig) throws Exception
  {
    final HashSet <URI> aAddresses = new HashSet <> ();
    for (final String sPart : aConfig.getAsString (PREFIX + "sources", "").split (","))
    {
      final String sID = sPart.trim ();
      if (sID.isEmpty ())
        continue;
      if (!sID.matches ("[a-z0-9_-]{1,64}") || m_aSources.stream ().anyMatch (x -> x.id ().equals (sID)))
        throw new IllegalArgumentException ("Invalid or duplicate reporting source ID");
      final String sSourcePrefix = PREFIX + "source." + sID + ".";
      final URI aBase = URI.create (aConfig.getAsString (sSourcePrefix + "base-url", "").replaceAll ("/+$", ""));
      final String sToken = aConfig.getAsString (sSourcePrefix + "token", "");
      if (!"https".equalsIgnoreCase (aBase.getScheme ()) || aBase.getHost () == null ||
          aBase.getUserInfo () != null || aBase.getQuery () != null || aBase.getFragment () != null ||
          sToken.isBlank () || !aAddresses.add (aBase))
        throw new IllegalArgumentException ("Invalid reporting source configuration: " + sID);
      m_aSources.add (new Source (sID, aBase, sToken));
    }
    if (m_aSources.isEmpty ())
      throw new IllegalArgumentException ("Reporting collection requires at least one source");
    final int nLeaseHours = aConfig.getAsInt (PREFIX + "lease-hours", 12);
    if (nLeaseHours < 1)
      throw new IllegalArgumentException ("Reporting collection lease must be positive");
    m_aLease = Duration.ofHours (nLeaseHours);
    if (!(PeppolReportingBackend.getBackendService () instanceof PeppolReportingBackendSqlSPI) ||
        PeppolReportingBackend.withBackendDo (aConfig, aBackend -> {}).isFailure ())
      throw new IllegalArgumentException ("Reporting collection requires an initialized SQL reporting backend");
    m_aStore = new ReportingCollectionStore (aConfig);
    m_aStore.initialize ();
    m_aHttpClient = HttpClient.newBuilder ().connectTimeout (Duration.ofSeconds (30))
                              .followRedirects (HttpClient.Redirect.NEVER).build ();
    m_aExecutor = Executors.newSingleThreadScheduledExecutor (aRunnable -> {
      final Thread aThread = new Thread (aRunnable, "peppol-reporting-collection");
      aThread.setDaemon (true);
      return aThread;
    });
  }

  public void start ()
  {
    m_aExecutor.scheduleWithFixedDelay (this::collect, 0, 1, TimeUnit.HOURS);
  }

  static boolean isCollectionWindow (final ZonedDateTime aNow, final int nDay, final int nHour, final int nMinute)
  {
    if (nDay < 1 || nDay > 15 || nHour < 0 || nHour > 23 || nMinute < 0 || nMinute > 59)
      throw new IllegalArgumentException ("Invalid report submission schedule");
    // Do not freeze a source before the previous UTC month has actually ended.
    return YearMonth.from (aNow).minusMonths (1).isBefore (YearMonth.from (aNow.withZoneSameInstant (ZoneOffset.UTC))) &&
           aNow.isBefore (aNow.withDayOfMonth (nDay).withHour (nHour).withMinute (nMinute).withSecond (0).withNano (0));
  }

  private void collect ()
  {
    try
    {
      final ZonedDateTime aNow = ZonedDateTime.now ();
      if (!isCollectionWindow (aNow, APCoreConfig.getPeppolReportingScheduleDayOfMonth (),
                                APCoreConfig.getPeppolReportingScheduleHour (),
                                APCoreConfig.getPeppolReportingScheduleMinute ()))
        return;
      final YearMonth aPeriod = YearMonth.from (aNow).minusMonths (1);
      for (final Source aSource : m_aSources)
      {
        if (!isCollectionWindow (ZonedDateTime.now (), APCoreConfig.getPeppolReportingScheduleDayOfMonth (),
                                                       APCoreConfig.getPeppolReportingScheduleHour (),
                                                       APCoreConfig.getPeppolReportingScheduleMinute ()))
          break;
        ReportingCollectionStore.Claim aClaim = null;
        try
        {
          aClaim = m_aStore.claim (aSource.id (), aPeriod, Instant.now (), m_aLease);
          if (aClaim == null)
            continue;
          if (!aClaim.fetched ())
          {
            final URI aURI = URI.create (aSource.base () + "/api/reporting/records/" + aPeriod.getYear () + "/" + aPeriod.getMonthValue ());
            final HttpRequest aRequest = HttpRequest.newBuilder (aURI).timeout (Duration.ofMinutes (2))
                                                    .header ("X-Token", aSource.token ())
                                                    .header ("Accept", "application/json").GET ().build ();
            final HttpResponse <String> aResponse = m_aHttpClient.send (aRequest, HttpResponse.BodyHandlers.ofString ());
            if (aResponse.statusCode () != 200)
              throw new IllegalStateException ("Reporting source request failed");
            m_aStore.stage (aClaim, parseRecords (aResponse.body (), aPeriod));
          }
          m_aStore.transfer (aClaim);
          LOGGER.info ("Collected reporting source {} for {}", aSource.id (), aPeriod);
        }
        catch (final Exception ex)
        {
          // Exception messages may contain remote endpoints. Log only the configured source ID and error type.
          LOGGER.warn ("Reporting collection failed for source {} and period {} ({})",
                       aSource.id (), aPeriod, ex.getClass ().getSimpleName ());
          if (aClaim != null)
          {
            try
            {
              m_aStore.fail (aClaim);
            }
            catch (final Exception aReleaseError)
            {
              LOGGER.warn ("Reporting source {} lease will expire after release failure", aSource.id ());
            }
          }
          if (ex instanceof InterruptedException)
          {
            Thread.currentThread ().interrupt ();
            return;
          }
        }
      }
    }
    catch (final Exception ex)
    {
      LOGGER.warn ("Reporting collection iteration failed ({})", ex.getClass ().getSimpleName ());
    }
  }

  static List <PeppolReportingItem> parseRecords (final String sBody, final YearMonth aPeriod)
  {
    final IJsonArray aArray = JsonReader.builder ().source (sBody).readAsArray ();
    if (aArray == null)
      throw new IllegalArgumentException ("Reporting source response must be a complete JSON array");
    final List <PeppolReportingItem> ret = new ArrayList <> ();
    for (final IJsonObject aObject : aArray.iteratorObjects ())
    {
      final PeppolReportingItem aItem = PeppolReportingJsonHelper.toDomain (aObject);
      if (!aPeriod.equals (YearMonth.from (aItem.getExchangeDTUTC ().withOffsetSameInstant (ZoneOffset.UTC))))
        throw new IllegalArgumentException ("Reporting record is outside the requested UTC month");
      ret.add (aItem);
    }
    if (ret.size () != aArray.size ())
      throw new IllegalArgumentException ("Reporting response contains non-object records");
    return ret;
  }

  @Override
  public void close ()
  {
    m_aExecutor.shutdownNow ();
    m_aHttpClient.shutdownNow ();
  }
}
