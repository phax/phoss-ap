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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;

import org.junit.Test;

import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.codelist.EMlsSendingTrigger;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for class {@link APCoreConfig}.
 *
 * @author Philip Helger
 */
public final class APCoreConfigTest
{
  private static void _restore (final String sKey, final String sOldValue)
  {
    if (sOldValue == null)
      System.clearProperty (sKey);
    else
      System.setProperty (sKey, sOldValue);
  }

  @Test
  public void testOutboundDevLoopbackRequiresTestStageAndFlag ()
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.getConfig ();
    final String sOldStage = System.getProperty (APConfigurationProperties.PEPPOL_STAGE);
    final String sOldFlag = System.getProperty (APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED);

    try
    {
      System.clearProperty (APConfigurationProperties.PEPPOL_STAGE);
      System.clearProperty (APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED);
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertFalse (APCoreConfig.isOutboundDevLoopbackEnabled ());

      System.setProperty (APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED, "true");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertFalse (APCoreConfig.isOutboundDevLoopbackEnabled ());

      System.setProperty (APConfigurationProperties.PEPPOL_STAGE, "production");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertFalse (APCoreConfig.isOutboundDevLoopbackEnabled ());

      System.setProperty (APConfigurationProperties.PEPPOL_STAGE, "test");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertTrue (APCoreConfig.isOutboundDevLoopbackEnabled ());
    }
    finally
    {
      if (sOldStage == null)
        System.clearProperty (APConfigurationProperties.PEPPOL_STAGE);
      else
        System.setProperty (APConfigurationProperties.PEPPOL_STAGE, sOldStage);

      if (sOldFlag == null)
        System.clearProperty (APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED);
      else
        System.setProperty (APConfigurationProperties.OUTBOUND_DEV_LOOPBACK_ENABLED, sOldFlag);

      APConfigProvider.setConfig (aOldConfig);
    }
  }

  @Test
  public void testDevCertificateSettingsRequireTestStage ()
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.getConfig ();
    final String sOldStage = System.getProperty (APConfigurationProperties.PEPPOL_STAGE);
    final String sOldCA = System.getProperty (APConfigurationProperties.PEPPOL_DEV_TRUSTED_CA_PATH);
    final String sOldURL = System.getProperty (APConfigurationProperties.OUTBOUND_DEV_FIXED_ENDPOINT_URL);

    try
    {
      System.setProperty (APConfigurationProperties.PEPPOL_DEV_TRUSTED_CA_PATH, "dev-ca.pem");
      System.setProperty (APConfigurationProperties.OUTBOUND_DEV_FIXED_ENDPOINT_URL, "http://localhost:8082/as4");

      System.clearProperty (APConfigurationProperties.PEPPOL_STAGE);
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertNull (APCoreConfig.getPeppolDevTrustedCAPath ());
      assertNull (APCoreConfig.getOutboundDevFixedEndpointUrl ());

      System.setProperty (APConfigurationProperties.PEPPOL_STAGE, "production");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertNull (APCoreConfig.getPeppolDevTrustedCAPath ());
      assertNull (APCoreConfig.getOutboundDevFixedEndpointUrl ());

      System.setProperty (APConfigurationProperties.PEPPOL_STAGE, "test");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals ("dev-ca.pem", APCoreConfig.getPeppolDevTrustedCAPath ());
      assertEquals ("http://localhost:8082/as4", APCoreConfig.getOutboundDevFixedEndpointUrl ());
    }
    finally
    {
      _restore (APConfigurationProperties.PEPPOL_STAGE, sOldStage);
      _restore (APConfigurationProperties.PEPPOL_DEV_TRUSTED_CA_PATH, sOldCA);
      _restore (APConfigurationProperties.OUTBOUND_DEV_FIXED_ENDPOINT_URL, sOldURL);
      APConfigProvider.setConfig (aOldConfig);
    }
  }

  @Test
  public void testPeppolReportingExcludedParticipantIDs ()
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.getConfig ();
    final String sOldValue = System.getProperty (APConfigurationProperties.PEPPOL_REPORTING_EXCLUDE_PARTICIPANT_IDS);

    try
    {
      // Nothing configured
      System.clearProperty (APConfigurationProperties.PEPPOL_REPORTING_EXCLUDE_PARTICIPANT_IDS);
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertTrue (APCoreConfig.getPeppolReportingExcludedParticipantIDs ().isEmpty ());

      // Surrounding whitespaces and empty parts are ignored
      System.setProperty (APConfigurationProperties.PEPPOL_REPORTING_EXCLUDE_PARTICIPANT_IDS,
                          " iso6523-actorid-upis::9915:test , ,0088:1234567890128,");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (new CommonsLinkedHashSet <> ("iso6523-actorid-upis::9915:test", "0088:1234567890128"),
                    APCoreConfig.getPeppolReportingExcludedParticipantIDs ());
    }
    finally
    {
      if (sOldValue == null)
        System.clearProperty (APConfigurationProperties.PEPPOL_REPORTING_EXCLUDE_PARTICIPANT_IDS);
      else
        System.setProperty (APConfigurationProperties.PEPPOL_REPORTING_EXCLUDE_PARTICIPANT_IDS, sOldValue);

      APConfigProvider.setConfig (aOldConfig);
    }
  }

  @Test
  public void testCircuitBreakerDeferMaxDuration ()
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.getConfig ();
    final String sOldValue = System.getProperty (APConfigurationProperties.CIRCUIT_BREAKER_DEFER_MAX_DURATION);

    try
    {
      // Nothing configured
      System.clearProperty (APConfigurationProperties.CIRCUIT_BREAKER_DEFER_MAX_DURATION);
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (APConfigurationProperties.CIRCUIT_BREAKER_DEFER_MAX_DURATION_DEFAULT,
                    APCoreConfig.getCircuitBreakerDeferMaxDuration ());

      // A valid duration
      System.setProperty (APConfigurationProperties.CIRCUIT_BREAKER_DEFER_MAX_DURATION, "2h 30m");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (Duration.ofHours (2).plusMinutes (30), APCoreConfig.getCircuitBreakerDeferMaxDuration ());

      // An invalid duration falls back to the default
      System.setProperty (APConfigurationProperties.CIRCUIT_BREAKER_DEFER_MAX_DURATION, "not a duration");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (APConfigurationProperties.CIRCUIT_BREAKER_DEFER_MAX_DURATION_DEFAULT,
                    APCoreConfig.getCircuitBreakerDeferMaxDuration ());
    }
    finally
    {
      if (sOldValue == null)
        System.clearProperty (APConfigurationProperties.CIRCUIT_BREAKER_DEFER_MAX_DURATION);
      else
        System.setProperty (APConfigurationProperties.CIRCUIT_BREAKER_DEFER_MAX_DURATION, sOldValue);

      APConfigProvider.setConfig (aOldConfig);
    }
  }

  @Test
  public void testCircuitBreakerTimeBasedThresholding ()
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.getConfig ();
    final String sOldExecutions = System.getProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_EXECUTIONS);
    final String sOldPeriod = System.getProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_PERIOD);
    final String sOldRate = System.getProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_RATE);

    try
    {
      // Nothing configured - the current behaviour stays
      System.clearProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_EXECUTIONS);
      System.clearProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_PERIOD);
      System.clearProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_RATE);
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (0, APCoreConfig.getCircuitBreakerFailureExecutions ());
      assertNull (APCoreConfig.getCircuitBreakerFailurePeriod ());
      assertEquals (0, APCoreConfig.getCircuitBreakerFailureRate ());

      // Valid values
      System.setProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_EXECUTIONS, "20");
      System.setProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_PERIOD, "5m");
      System.setProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_RATE, "50");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (20, APCoreConfig.getCircuitBreakerFailureExecutions ());
      assertEquals (Duration.ofMinutes (5), APCoreConfig.getCircuitBreakerFailurePeriod ());
      assertEquals (50, APCoreConfig.getCircuitBreakerFailureRate ());

      // An unparsable period is treated as "not configured"
      System.setProperty (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_PERIOD, "whenever");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertNull (APCoreConfig.getCircuitBreakerFailurePeriod ());
    }
    finally
    {
      _restore (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_EXECUTIONS, sOldExecutions);
      _restore (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_PERIOD, sOldPeriod);
      _restore (APConfigurationProperties.CIRCUIT_BREAKER_FAILURE_RATE, sOldRate);
      APConfigProvider.setConfig (aOldConfig);
    }
  }

  @Test
  public void testPeppolSmpTimeouts ()
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.getConfig ();
    final String sOldConnect = System.getProperty (APConfigurationProperties.PEPPOL_SMP_TIMEOUT_CONNECT);
    final String sOldResponse = System.getProperty (APConfigurationProperties.PEPPOL_SMP_TIMEOUT_RESPONSE);

    try
    {
      // Nothing configured - the defaults equal the previously hardcoded values
      System.clearProperty (APConfigurationProperties.PEPPOL_SMP_TIMEOUT_CONNECT);
      System.clearProperty (APConfigurationProperties.PEPPOL_SMP_TIMEOUT_RESPONSE);
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (Duration.ofSeconds (5), APCoreConfig.getPeppolSmpTimeoutConnect ());
      assertEquals (Duration.ofSeconds (10), APCoreConfig.getPeppolSmpTimeoutResponse ());

      // Valid values
      System.setProperty (APConfigurationProperties.PEPPOL_SMP_TIMEOUT_CONNECT, "2s");
      System.setProperty (APConfigurationProperties.PEPPOL_SMP_TIMEOUT_RESPONSE, "30s");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (Duration.ofSeconds (2), APCoreConfig.getPeppolSmpTimeoutConnect ());
      assertEquals (Duration.ofSeconds (30), APCoreConfig.getPeppolSmpTimeoutResponse ());

      // An invalid value falls back to the default
      System.setProperty (APConfigurationProperties.PEPPOL_SMP_TIMEOUT_RESPONSE, "soon");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (APConfigurationProperties.PEPPOL_SMP_TIMEOUT_RESPONSE_DEFAULT,
                    APCoreConfig.getPeppolSmpTimeoutResponse ());
    }
    finally
    {
      _restore (APConfigurationProperties.PEPPOL_SMP_TIMEOUT_CONNECT, sOldConnect);
      _restore (APConfigurationProperties.PEPPOL_SMP_TIMEOUT_RESPONSE, sOldResponse);
      APConfigProvider.setConfig (aOldConfig);
    }
  }

  public void testMlsSendingTrigger ()
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.getConfig ();
    final String sOldValue = System.getProperty (APConfigurationProperties.MLS_SENDING_TRIGGER);

    try
    {
      // Nothing configured - the default reproduces the pre-0.13.0 behaviour
      System.clearProperty (APConfigurationProperties.MLS_SENDING_TRIGGER);
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (EMlsSendingTrigger.AUTO, APCoreConfig.getMlsSendingTrigger ());

      System.setProperty (APConfigurationProperties.MLS_SENDING_TRIGGER, "api");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (EMlsSendingTrigger.API, APCoreConfig.getMlsSendingTrigger ());

      // An unsupported value falls back to the default
      System.setProperty (APConfigurationProperties.MLS_SENDING_TRIGGER, "whatever");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (EMlsSendingTrigger.AUTO, APCoreConfig.getMlsSendingTrigger ());
    }
    finally
    {
      if (sOldValue == null)
        System.clearProperty (APConfigurationProperties.MLS_SENDING_TRIGGER);
      else
        System.setProperty (APConfigurationProperties.MLS_SENDING_TRIGGER, sOldValue);

      APConfigProvider.setConfig (aOldConfig);
    }
  }

  @Test
  public void testMlsSendingApiTimeout ()
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.getConfig ();
    final String sOldValue = System.getProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT);

    try
    {
      System.clearProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT);
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (APConfigurationProperties.MLS_SENDING_API_TIMEOUT_DEFAULT, APCoreConfig.getMlsSendingApiTimeout ());

      System.setProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT, "90s");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (Duration.ofSeconds (90), APCoreConfig.getMlsSendingApiTimeout ());

      // A non-positive value would answer every document immediately
      System.setProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT, "0s");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (APConfigurationProperties.MLS_SENDING_API_TIMEOUT_DEFAULT, APCoreConfig.getMlsSendingApiTimeout ());
    }
    finally
    {
      if (sOldValue == null)
        System.clearProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT);
      else
        System.setProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT, sOldValue);

      APConfigProvider.setConfig (aOldConfig);
    }
  }

  @Test
  public void testMlsSendingApiTimeoutResponseCode ()
  {
    final IConfigWithFallback aOldConfig = APConfigProvider.getConfig ();
    final String sOldValue = System.getProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT_CODE);

    try
    {
      System.clearProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT_CODE);
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (EPeppolMLSResponseCode.ACKNOWLEDGING, APCoreConfig.getMlsSendingApiTimeoutResponseCode ());

      System.setProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT_CODE, "AP");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (EPeppolMLSResponseCode.ACCEPTANCE, APCoreConfig.getMlsSendingApiTimeoutResponseCode ());

      // A rejection is a statement only the Receiver Backend can make
      System.setProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT_CODE, "RE");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (EPeppolMLSResponseCode.ACKNOWLEDGING, APCoreConfig.getMlsSendingApiTimeoutResponseCode ());

      System.setProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT_CODE, "whatever");
      APConfigProvider.setConfig (new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ()));
      assertEquals (EPeppolMLSResponseCode.ACKNOWLEDGING, APCoreConfig.getMlsSendingApiTimeoutResponseCode ());
    }
    finally
    {
      if (sOldValue == null)
        System.clearProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT_CODE);
      else
        System.setProperty (APConfigurationProperties.MLS_SENDING_API_TIMEOUT_CODE, sOldValue);

      APConfigProvider.setConfig (aOldConfig);
    }
  }
}
