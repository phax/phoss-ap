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
package com.helger.phoss.ap.core.status;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.xbill.DNS.ResolverConfig;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.string.StringHelper;
import com.helger.base.system.SystemProperties;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.peppol.commons.CPeppolCommonsVersion;
import com.helger.phase4.CAS4Version;
import com.helger.phoss.ap.api.CPhossAPVersion;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.basic.APBasicConfig;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.APCoreMetaManager;
import com.helger.phoss.ap.core.servlet.APServletInit;

/**
 * Provides the status data for the management status endpoint. The returned JSON object contains
 * non-sensitive configuration values, version information, and runtime metadata.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class APStatusProvider
{
  private APStatusProvider ()
  {}

  @Nullable
  private static String _formatDT (@Nullable final OffsetDateTime aDT)
  {
    return aDT != null ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format (aDT) : null;
  }

  /**
   * Build the default status data as a JSON object. All values are non-sensitive configuration
   * flags and version strings.
   *
   * @return A mutable JSON object with all status entries. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static IJsonObject getDefaultStatusData ()
  {
    final OffsetDateTime aNow = OffsetDateTime.now ();

    final IJsonObject aStatusData = new JsonObject ();

    // Runtime
    aStatusData.addIfNotNull ("startup.datetime", _formatDT (APServletInit.getStartupDateTime ()));
    aStatusData.add ("status.datetime", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format (aNow));
    aStatusData.add ("version.java", SystemProperties.getJavaVersion ());

    // Build information
    aStatusData.add ("build.version", CPhossAPVersion.BUILD_VERSION);
    aStatusData.add ("build.timestamp", CPhossAPVersion.BUILD_TIMESTAMP);

    // Library versions
    aStatusData.add ("version.peppol-commons", CPeppolCommonsVersion.BUILD_VERSION);
    aStatusData.add ("version.phase4", CAS4Version.BUILD_VERSION);

    // Peppol
    // Stage is checked for non-null on startup
    aStatusData.addIfNotNull ("peppol.stage", APCoreConfig.getPeppolStage ().getID ());
    aStatusData.addIfNotNull ("peppol.owner.seatid", APCoreConfig.getPeppolOwnerSeatID ());
    aStatusData.add ("peppol.owner.countrycode", APCoreConfig.getPeppolOwnerCountryCode ());
    aStatusData.add ("peppol.identifier.mode", APBasicConfig.getPeppolIdentifierMode ().getID ());

    // Sending and receiving
    aStatusData.add ("peppol.sending.enabled", APCoreConfig.isSendingEnabled ());
    aStatusData.add ("peppol.receiving.enabled", APCoreConfig.isReceivingEnabled ());

    // Forwarding
    aStatusData.add ("forwarding.mode", APCoreMetaManager.getForwardingMode ().getID ());

    // Storage
    aStatusData.add ("storage.mode", APBasicConfig.getStorageMode ().getID ());

    // MLS
    aStatusData.add ("mls.sending.enabled", APCoreConfig.isMlsSendingEnabled ());
    aStatusData.add ("mls.type", APCoreConfig.getMlsType ().getID ());

    // Verification
    aStatusData.add ("verification.inbound.enabled", APCoreConfig.isVerificationInboundEnabled ());
    aStatusData.add ("verification.outbound.enabled", APCoreConfig.isVerificationOutboundEnabled ());

    // Reporting
    aStatusData.add ("peppol.reporting.schedule.enabled", APCoreConfig.isPeppolReportingScheduled ());

    // Proxy
    final var aConfig = APConfigProvider.getConfig ();
    aStatusData.add ("proxy.http.configured", aConfig.getAsBoolean ("http.proxy.enabled", false));
    aStatusData.add ("proxy.http.username.configured",
                     StringHelper.isNotEmpty (aConfig.getAsString ("http.proxy.username")));

    // Duplicate detection
    aStatusData.add ("duplicate.detection.as4.mode", APCoreConfig.getDuplicateDetectionAS4Mode ().getID ());
    aStatusData.add ("duplicate.detection.sbdh.mode", APCoreConfig.getDuplicateDetectionSBDHMode ().getID ());

    // Sentry configuration
    aStatusData.add ("sentry.enabled", APConfigProvider.getConfig ().containsNonNullValue ("sentry.dsn"));

    // DNS configuration
    aStatusData.add ("dns.config.servers", ResolverConfig.getCurrentConfig ().servers ());

    return aStatusData;
  }

  /**
   * @return Minimal JSON object indicating that the status endpoint is disabled. Never
   *         <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static IJsonObject getStatusDisabledData ()
  {
    final IJsonObject aStatusData = new JsonObject ();
    aStatusData.add ("status.enabled", false);
    return aStatusData;
  }
}
