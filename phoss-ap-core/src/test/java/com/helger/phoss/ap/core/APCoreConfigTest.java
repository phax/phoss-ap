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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.codelist.EMlsSendingTrigger;

import java.time.Duration;

import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Test class for class {@link APCoreConfig}.
 *
 * @author Philip Helger
 */
public final class APCoreConfigTest
{
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
