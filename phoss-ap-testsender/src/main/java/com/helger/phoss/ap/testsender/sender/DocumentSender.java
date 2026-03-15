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
package com.helger.phoss.ap.testsender.sender;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.helger.phoss.ap.testsender.config.TestSenderConfig;

/**
 * HTTP client that sends documents to the phoss-ap outbound API.
 */
@Component
public class DocumentSender
{
  private static final Logger LOGGER = LoggerFactory.getLogger (DocumentSender.class);

  private final HttpClient m_aHttpClient;
  private final String m_sBaseUrl;
  private final TestSenderConfig m_aConfig;

  public DocumentSender (@NonNull final TestSenderConfig aConfig)
  {
    m_aConfig = aConfig;
    m_sBaseUrl = aConfig.getTarget ().getBaseUrl ();
    m_aHttpClient = HttpClient.newBuilder ()
                              .connectTimeout (Duration.ofMillis (aConfig.getHttp ().getConnectTimeoutMs ()))
                              .build ();
  }

  @NonNull
  private static String _urlEncode (@NonNull final String s)
  {
    return URLEncoder.encode (s, StandardCharsets.UTF_8);
  }

  @NonNull
  private String buildSubmitUrl (final String sSenderID,
                                 final String sReceiverID,
                                 final String sDocTypeID,
                                 final String sProcessID,
                                 final String sC1CountryCode,
                                 final String sSbdhInstanceID,
                                 @Nullable final String sSbdhStandard,
                                 @Nullable final String sSbdhTypeVersion,
                                 @Nullable final String sSbdhType,
                                 @Nullable final String sPayloadMimeType)
  {
    final StringBuilder aSB = new StringBuilder (m_sBaseUrl);
    aSB.append ("/api/outbound/submit/");
    aSB.append (_urlEncode (sSenderID)).append ('/');
    aSB.append (_urlEncode (sReceiverID)).append ('/');
    aSB.append (_urlEncode (sDocTypeID)).append ('/');
    aSB.append (_urlEncode (sProcessID)).append ('/');
    aSB.append (_urlEncode (sC1CountryCode));

    // Query parameters
    aSB.append ("?sbdhInstanceID=").append (_urlEncode (sSbdhInstanceID));
    if (sSbdhStandard != null)
      aSB.append ("&sbdhStandard=").append (_urlEncode (sSbdhStandard));
    if (sSbdhTypeVersion != null)
      aSB.append ("&sbdhTypeVersion=").append (_urlEncode (sSbdhTypeVersion));
    if (sSbdhType != null)
      aSB.append ("&sbdhType=").append (_urlEncode (sSbdhType));
    if (sPayloadMimeType != null)
      aSB.append ("&payloadMimeType=").append (_urlEncode (sPayloadMimeType));

    return aSB.toString ();
  }

  /**
   * Send a raw XML document via POST /api/outbound/submit/{ids}.
   *
   * @param aXmlFile
   *        The XML file to be sent
   * @param sSbdhInstanceID
   *        The SBDH Instance ID to be used.
   * @return Sending result
   * @throws IOException
   *         in case of error
   */
  @NonNull
  public SendResult sendXml (@NonNull final Path aXmlFile, @NonNull final String sSbdhInstanceID) throws IOException
  {
    final TestSenderConfig.Peppol aPeppol = m_aConfig.getPeppol ();
    final String sUrl = buildSubmitUrl (aPeppol.getSenderID (),
                                        aPeppol.getReceiverID (),
                                        aPeppol.getDocTypeID (),
                                        aPeppol.getProcessID (),
                                        aPeppol.getC1CountryCode (),
                                        sSbdhInstanceID,
                                        null,
                                        null,
                                        null,
                                        null);

    final byte [] aBody = Files.readAllBytes (aXmlFile);
    return doSend ("xml", sSbdhInstanceID, sUrl, aBody, "application/xml");
  }

  /*
   * Send a PDF document via POST /api/outbound/submit/{ids} with PDF-specific
   * parameters.
   */
  @NonNull
  public SendResult sendPdf (@NonNull final Path aPdfFile, @NonNull final String sSbdhInstanceID) throws IOException
  {
    final TestSenderConfig.Peppol aPeppol = m_aConfig.getPeppol ();
    final TestSenderConfig.Pdf aPdf = m_aConfig.getPdf ();
    final String sUrl = buildSubmitUrl (aPeppol.getSenderID (),
                                        aPeppol.getReceiverID (),
                                        aPeppol.getDocTypeID (),
                                        aPeppol.getProcessID (),
                                        aPeppol.getC1CountryCode (),
                                        sSbdhInstanceID,
                                        aPdf.getSbdhStandard (),
                                        aPdf.getSbdhTypeVersion (),
                                        aPdf.getSbdhType (),
                                        "application/pdf");

    final byte [] aBody = Files.readAllBytes (aPdfFile);
    return doSend ("pdf", sSbdhInstanceID, sUrl, aBody, "application/pdf");
  }

  /*
   * Send a prebuilt SBD via POST /api/outbound/submit-sbd.
   */
  @NonNull
  public SendResult sendPrebuiltSbd (@NonNull final Path aSbdFile, @NonNull final String sSbdhInstanceID)
                                                                                                          throws IOException
  {
    final String sUrl = m_sBaseUrl + "/api/outbound/submit-sbd";
    String sBody = Files.readString (aSbdFile, StandardCharsets.UTF_8);
    // Inject custom SBDH ID
    sBody = sBody.replace ("92f7e6a5-c392-4e66-b786-fd2b7c535eb2", sSbdhInstanceID);

    return doSend ("sbd", sSbdhInstanceID, sUrl, sBody.getBytes (StandardCharsets.UTF_8), "application/xml");
  }

  /*
   * Poll the transaction status until a terminal state is reached or timeout.
   * @return The final status string, or {@code null} if not found or timed out.
   */
  @Nullable
  public String pollStatus (@NonNull final String sSbdhInstanceID, final long nTimeoutMs, final long nIntervalMs)
  {
    final String sUrl = m_sBaseUrl + "/api/outbound/status/" + _urlEncode (sSbdhInstanceID);
    final long nDeadline = System.currentTimeMillis () + nTimeoutMs;

    while (System.currentTimeMillis () < nDeadline)
    {
      try
      {
        final HttpRequest aReq = HttpRequest.newBuilder ()
                                            .uri (URI.create (sUrl))
                                            .timeout (Duration.ofMillis (m_aConfig.getHttp ().getReadTimeoutMs ()))
                                            .header ("X-Token", m_aConfig.getTarget ().getToken ())
                                            .GET ()
                                            .build ();
        final HttpResponse <String> aResp = m_aHttpClient.send (aReq, HttpResponse.BodyHandlers.ofString ());
        if (aResp.statusCode () == 200)
        {
          final String sBody = aResp.body ();
          // Check for terminal states in the JSON response
          if (sBody.contains ("\"sent\"") || sBody.contains ("\"permanently_failed\""))
          {
            return sBody;
          }
          // else wait
        }
        else
          if (aResp.statusCode () == 404)
          {
            // Not yet created, keep waiting
          }

        Thread.sleep (nIntervalMs);
      }
      catch (final InterruptedException ex)
      {
        Thread.currentThread ().interrupt ();
        return null;
      }
      catch (final Exception ex)
      {
        LOGGER.warn ("Error polling status for '" + sSbdhInstanceID + "': " + ex.getMessage ());
        try
        {
          Thread.sleep (nIntervalMs);
        }
        catch (final InterruptedException ie)
        {
          Thread.currentThread ().interrupt ();
          return null;
        }
      }
    }

    LOGGER.warn ("Timeout polling status for '" + sSbdhInstanceID + "'");
    return null;
  }

  @NonNull
  private SendResult doSend (final String sDocType,
                             final String sSbdhInstanceID,
                             final String sUrl,
                             final byte [] aBody,
                             final String sContentType)
  {
    final long nStart = System.nanoTime ();
    try
    {
      final HttpRequest aReq = HttpRequest.newBuilder ()
                                          .uri (URI.create (sUrl))
                                          .timeout (Duration.ofMillis (m_aConfig.getHttp ().getReadTimeoutMs ()))
                                          .header ("Content-Type", sContentType)
                                          .header ("X-Token", m_aConfig.getTarget ().getToken ())
                                          .POST (HttpRequest.BodyPublishers.ofByteArray (aBody))
                                          .build ();

      final HttpResponse <String> aResp = m_aHttpClient.send (aReq, HttpResponse.BodyHandlers.ofString ());
      final long nDurationMs = (System.nanoTime () - nStart) / 1_000_000;

      if (aResp.statusCode () == 200)
      {
        return SendResult.success (sDocType, sSbdhInstanceID, aResp.statusCode (), nDurationMs, aResp.body ());
      }

      return SendResult.failure (sDocType,
                                 sSbdhInstanceID,
                                 aResp.statusCode (),
                                 nDurationMs,
                                 aResp.body (),
                                 "HTTP " + aResp.statusCode ());
    }
    catch (final Exception ex)
    {
      final long nDurationMs = (System.nanoTime () - nStart) / 1_000_000;
      return SendResult.failure (sDocType, sSbdhInstanceID, 0, nDurationMs, null, ex.getMessage ());
    }
  }
}
