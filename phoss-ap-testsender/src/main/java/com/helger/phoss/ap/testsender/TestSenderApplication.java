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
package com.helger.phoss.ap.testsender;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import com.helger.phoss.ap.testsender.config.TestSenderConfig;
import com.helger.phoss.ap.testsender.report.ResultReporter;
import com.helger.phoss.ap.testsender.runner.BulkRunner;
import com.helger.phoss.ap.testsender.runner.SingleDocumentRunner;
import com.helger.phoss.ap.testsender.scenario.ITestScenario;
import com.helger.phoss.ap.testsender.scenario.PdfScenario;
import com.helger.phoss.ap.testsender.scenario.PrebuiltSbdScenario;
import com.helger.phoss.ap.testsender.scenario.XmlScenario;
import com.helger.phoss.ap.testsender.sender.BulkSendResult;
import com.helger.phoss.ap.testsender.sender.SendResult;

@SpringBootApplication
public class TestSenderApplication implements CommandLineRunner
{
  private static final Logger LOGGER = LoggerFactory.getLogger (TestSenderApplication.class);

  private final TestSenderConfig m_aConfig;
  private final SingleDocumentRunner m_aSingleRunner;
  private final BulkRunner m_aBulkRunner;
  private final ResultReporter m_aReporter;

  public TestSenderApplication (final TestSenderConfig aConfig,
                                final SingleDocumentRunner aSingleRunner,
                                final BulkRunner aBulkRunner,
                                final ResultReporter aReporter)
  {
    m_aConfig = aConfig;
    m_aSingleRunner = aSingleRunner;
    m_aBulkRunner = aBulkRunner;
    m_aReporter = aReporter;
  }

  public static void main (final String [] args)
  {
    SpringApplication.run (TestSenderApplication.class, args);
  }

  @Override
  public void run (final String... args) throws Exception
  {
    LOGGER.info ("phoss-ap Test Sender starting");
    LOGGER.info ("Target: " + m_aConfig.getTarget ().getBaseUrl ());

    // Build the list of active scenarios based on configuration
    final List <ITestScenario> aScenarios = _buildScenarios ();
    if (aScenarios.isEmpty ())
    {
      LOGGER.error ("No test scenarios configured. Set at least one of: " +
                    "testsender.samples.xml, testsender.samples.sbd, testsender.samples.pdf");
      return;
    }

    LOGGER.info ("Active scenarios: " + aScenarios.stream ().map (ITestScenario::getName).toList ());

    if (m_aConfig.getBulk ().isEnabled ())
    {
      // Bulk mode
      final BulkSendResult aBulkResult = m_aBulkRunner.run (aScenarios);
      m_aReporter.reportBulk (aBulkResult);
      m_aReporter.writeJsonFile (aBulkResult, m_aConfig.getOutput ().getFile ());
    }
    else
    {
      // Single document mode - send one of each configured type
      for (final ITestScenario aScenario : aScenarios)
      {
        final SendResult aResult = m_aSingleRunner.run (aScenario);
        m_aReporter.reportSingle (aResult);
      }
    }

    LOGGER.info ("phoss-ap Test Sender finished");
  }

  private List <ITestScenario> _buildScenarios () throws IOException
  {
    final List <ITestScenario> aScenarios = new ArrayList <> ();
    final TestSenderConfig.Samples aSamples = m_aConfig.getSamples ();

    final String sDocTypes = m_aConfig.getBulk ().isEnabled () ? m_aConfig.getBulk ().getDocumentTypes ()
                                                               : "xml,sbd,pdf";

    if (aSamples.getXml () != null && sDocTypes.contains ("xml"))
    {
      final Path aPath = _resolvePath (aSamples.getXml ());
      if (aPath != null)
      {
        aScenarios.add (new XmlScenario (aPath));
        LOGGER.info ("Loaded XML sample: " + aPath);
      }
    }

    if (aSamples.getSbd () != null && sDocTypes.contains ("sbd"))
    {
      final Path aPath = _resolvePath (aSamples.getSbd ());
      if (aPath != null)
      {
        aScenarios.add (new PrebuiltSbdScenario (aPath));
        LOGGER.info ("Loaded SBD sample: " + aPath);
      }
    }

    if (aSamples.getPdf () != null && sDocTypes.contains ("pdf"))
    {
      final Path aPath = _resolvePath (aSamples.getPdf ());
      if (aPath != null)
      {
        aScenarios.add (new PdfScenario (aPath));
        LOGGER.info ("Loaded PDF sample: " + aPath);
      }
    }

    // Apply mix ratio in bulk mode
    if (m_aConfig.getBulk ().isEnabled () && aScenarios.size () > 1)
    {
      final List <ITestScenario> aWeighted = _applyMixRatio (aScenarios);
      if (!aWeighted.isEmpty ())
        return aWeighted;
    }

    return aScenarios;
  }

  private List <ITestScenario> _applyMixRatio (final List <ITestScenario> aScenarios)
  {
    final String [] aParts = m_aConfig.getBulk ().getMixRatio ().split (",");
    if (aParts.length != aScenarios.size ())
    {
      LOGGER.warn ("Mix ratio '" +
                   m_aConfig.getBulk ().getMixRatio () +
                   "' has " +
                   aParts.length +
                   " parts but " +
                   aScenarios.size () +
                   " scenarios configured. Using equal distribution.");
      return aScenarios;
    }

    // Parse ratios
    final int [] aRatios = new int [aParts.length];
    int nTotal = 0;
    for (int i = 0; i < aParts.length; i++)
    {
      aRatios[i] = Integer.parseInt (aParts[i].trim ());
      nTotal += aRatios[i];
    }

    // Build weighted list
    final int nCount = m_aConfig.getBulk ().getCount ();
    final List <ITestScenario> aWeighted = new ArrayList <> (nCount);
    for (int i = 0; i < aScenarios.size (); i++)
    {
      final int nShare = (int) Math.round ((double) aRatios[i] / nTotal * nCount);
      for (int j = 0; j < nShare && aWeighted.size () < nCount; j++)
        aWeighted.add (aScenarios.get (i));
    }

    // Fill remainder with the last scenario
    while (aWeighted.size () < nCount)
      aWeighted.add (aScenarios.getLast ());

    return aWeighted;
  }

  /**
   * Resolve a resource path that can be either a classpath resource or an absolute/relative file
   * path. For classpath resources, the content is copied to a temporary file.
   */
  private static Path _resolvePath (final String sPath) throws IOException
  {
    if (sPath.startsWith ("classpath:"))
    {
      final Resource aResource = new DefaultResourceLoader ().getResource (sPath);
      if (!aResource.exists ())
      {
        LOGGER.warn ("Classpath resource not found: " + sPath);
        return null;
      }
      // Copy classpath resource to a temp file so it can be read as a Path
      final String sFilename = aResource.getFilename ();
      final String sSuffix = sFilename != null && sFilename.contains (".") ? sFilename.substring (sFilename
                                                                                                           .lastIndexOf ('.'))
                                                                           : ".tmp";
      final Path aTempFile = Files.createTempFile ("testsender-", sSuffix);
      aTempFile.toFile ().deleteOnExit ();
      try (final InputStream aIS = aResource.getInputStream ())
      {
        Files.copy (aIS, aTempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      return aTempFile;
    }

    // File path
    final Path aPath = Path.of (sPath);
    if (!Files.exists (aPath))
    {
      LOGGER.warn ("File not found: " + sPath);
      return null;
    }
    return aPath;
  }
}
