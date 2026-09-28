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
package com.helger.phoss.ap.core.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.exception.InitializationException;
import com.helger.base.string.StringHelper;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.security.certificate.CertificateHelper;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.security.revocation.ERevocationCheckMode;

/**
 * Determines the {@link TrustedCAChecker} used for all Peppol AP certificate checks (own
 * certificate, inbound signing certificate and outbound receiver certificate). By default this is
 * the official Peppol CA of the configured stage. For local development on the test stage a custom
 * CA can be configured, so that multiple phoss AP instances can exchange messages without official
 * Peppol test certificates.
 *
 * @author Philip Helger
 * @since 0.13.1
 */
@ThreadSafe
public final class APTrustedCAHelper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APTrustedCAHelper.class);

  private static volatile TrustedCAChecker s_aCustomAPCAChecker;

  private APTrustedCAHelper ()
  {}

  /**
   * Read a single PEM encoded X.509 certificate from a file.
   *
   * @param sPath
   *        The file path. May not be <code>null</code>.
   * @return The certificate and never <code>null</code>.
   * @throws InitializationException
   *         if the file cannot be read or does not contain a certificate
   */
  @NonNull
  public static X509Certificate readCertificate (@NonNull final String sPath)
  {
    try
    {
      final String sPEM = Files.readString (Path.of (sPath), StandardCharsets.US_ASCII);
      final X509Certificate aCert = CertificateHelper.convertStringToCertficate (sPEM);
      if (aCert == null)
        throw new InitializationException ("The file '" + sPath + "' does not contain a PEM encoded certificate");
      return aCert;
    }
    catch (final IOException | CertificateException ex)
    {
      throw new InitializationException ("Failed to read the certificate from '" + sPath + "'", ex);
    }
  }

  /**
   * Initialize the custom CA checker, if one is configured. Must be called once during startup.
   *
   * @return <code>true</code> if a custom CA is used, <code>false</code> if the official Peppol CA
   *         is used.
   * @throws InitializationException
   *         if the configured CA certificate cannot be read
   */
  public static boolean init ()
  {
    final String sCAPath = APCoreConfig.getPeppolDevTrustedCAPath ();
    if (StringHelper.isEmpty (sCAPath))
    {
      s_aCustomAPCAChecker = null;
      return false;
    }

    final X509Certificate aCACert = readCertificate (sCAPath);
    // A self-made CA has neither CRL nor OCSP
    s_aCustomAPCAChecker = TrustedCAChecker.builder ()
                                           .checkMode (ERevocationCheckMode.NONE)
                                           .addTrustedCACertificate (aCACert)
                                           .build ();
    LOGGER.warn ("Using the custom AP CA '" +
                 aCACert.getSubjectX500Principal ().getName () +
                 "' instead of the Peppol test CA - for local development only! Official Peppol certificates are NOT accepted.");
    return true;
  }

  /**
   * @param ePeppolStage
   *        The Peppol stage to use. May not be <code>null</code>.
   * @return The CA checker for Peppol AP certificates. Never <code>null</code>.
   */
  @NonNull
  public static TrustedCAChecker getAPCAChecker (@NonNull final EPeppolNetwork ePeppolStage)
  {
    final TrustedCAChecker aCustom = s_aCustomAPCAChecker;
    if (aCustom != null && ePeppolStage.isTest ())
      return aCustom;
    return ePeppolStage.isProduction () ? PeppolTrustedCA.peppolProductionAP () : PeppolTrustedCA.peppolTestAP ();
  }

  /**
   * @return <code>true</code> if a custom AP CA is in use.
   */
  public static boolean isCustomAPCA ()
  {
    return s_aCustomAPCAChecker != null;
  }
}
