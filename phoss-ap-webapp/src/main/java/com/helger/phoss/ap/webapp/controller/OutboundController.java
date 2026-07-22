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
package com.helger.phoss.ap.webapp.controller;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;

import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.ddd.DocumentDetails;
import com.helger.json.JsonValue;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.api.dto.OutboundS3SubmitRequest;
import com.helger.phoss.ap.api.dto.OutboundTransactionResponse;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.core.APCoreConfig;
import com.helger.phoss.ap.core.ddd.DDDHelper;
import com.helger.phoss.ap.core.outbound.OutboundOrchestrator;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.phoss.ap.webapp.config.OpenApiConfig;
import com.helger.xml.serialize.read.DOMReader;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * REST controller for outbound transaction operations including submitting raw documents and
 * pre-built SBDs for Peppol AS4 sending, querying transaction status, and listing all transactions
 * currently in transmission.
 *
 * @author Philip Helger
 */
@RestController
@RequestMapping ("/api/outbound")
@Tag (name = "Outbound", description = "Outbound document submission and transmission status")
@SecurityRequirement (name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class OutboundController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (OutboundController.class);

  /** Maximum length of each custom field (issue #64). */
  private static final int MAX_CUSTOM_FIELD_LENGTH = 255;

  /**
   * Validate that none of the optional custom fields exceeds the maximum length.
   *
   * @param sCustom1
   *        First custom field. May be <code>null</code>.
   * @param sCustom2
   *        Second custom field. May be <code>null</code>.
   * @param sCustom3
   *        Third custom field. May be <code>null</code>.
   * @return A 400 Bad Request response describing the first offending field, or <code>null</code>
   *         if all fields are valid.
   */
  @Nullable
  private static ResponseEntity <String> _validateCustomFields (@Nullable final String sCustom1,
                                                                @Nullable final String sCustom2,
                                                                @Nullable final String sCustom3)
  {
    final String [] aValues = { sCustom1, sCustom2, sCustom3 };
    for (int i = 0; i < aValues.length; ++i)
      if (aValues[i] != null && aValues[i].length () > MAX_CUSTOM_FIELD_LENGTH)
        return ResponseEntity.badRequest ()
                             .body (JsonValue.create ("The 'custom" +
                                                      (i + 1) +
                                                      "' field exceeds the maximum length of " +
                                                      MAX_CUSTOM_FIELD_LENGTH +
                                                      " characters").getAsJsonString ());
    return null;
  }

  /**
   * Submit a raw (payload-only) document for outbound sending via the Peppol network. The document
   * payload is read from the HTTP request body. Peppol identifiers are parsed from the URL path
   * variables.
   *
   * @param sSenderID
   *        The sender participant identifier.
   * @param sReceiverID
   *        The receiver participant identifier.
   * @param sDocTypeID
   *        The document type identifier.
   * @param sProcessID
   *        The process identifier.
   * @param sC1CountryCode
   *        The C1 country code.
   * @param aServletRequest
   *        The HTTP servlet request containing the document payload.
   * @param sSbdhInstanceID
   *        Optional SBDH Instance ID. A random one is generated if not provided.
   * @param sMlsTo
   *        Optional MLS "To" address.
   * @param sSbdhStandard
   *        Optional SBDH standard identifier.
   * @param sSbdhTypeVersion
   *        Optional SBDH type version.
   * @param sSbdhType
   *        Optional SBDH type.
   * @param sPayloadMimeType
   *        Optional payload MIME type.
   * @param sCustom1
   *        Optional custom field 1 (max 255 characters).
   * @param sCustom2
   *        Optional custom field 2 (max 255 characters).
   * @param sCustom3
   *        Optional custom field 3 (max 255 characters).
   * @return The {@link Phase4PeppolSendingReport} as JSON on success, or an error response.
   * @throws Exception
   *         On unexpected errors.
   */
  @PostMapping (value = "/submit/{senderID}/{receiverID}/{docTypeID}/{processID}/{c1CountryCode}",
                produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation (summary = "Submit a raw document for outbound sending",
              description = "Submits a raw business document (e.g., UBL Invoice XML) for outbound transmission. " +
                            "The AP creates the SBDH envelope from the path/query parameters. " +
                            "Returns a Phase4PeppolSendingReport as JSON.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Document accepted and sent successfully"),
                   @ApiResponse (responseCode = "400",
                                 description = "Invalid Peppol identifier (sender, receiver, document type or process)"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content),
                   @ApiResponse (responseCode = "404",
                                 description = "Sending is disabled in the configuration",
                                 content = @Content),
                   @ApiResponse (responseCode = "422", description = "Sending failed — see the report body for details") })
  public ResponseEntity <String> submitRawDocument (@Parameter (description = "Peppol Participant ID of the sender (C1)",
                                                                required = true,
                                                                example = "iso6523-actorid-upis::0088:senderbackend") @PathVariable ("senderID") final String sSenderID,
                                                    @Parameter (description = "Peppol Participant ID of the receiver (C4)",
                                                                required = true,
                                                                example = "iso6523-actorid-upis::0088:receiverbackend") @PathVariable ("receiverID") final String sReceiverID,
                                                    @Parameter (description = "Peppol Document Type Identifier",
                                                                required = true,
                                                                example = "busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:peppol:pint:billing-1@jp:peppol-1::2.1") @PathVariable ("docTypeID") final String sDocTypeID,
                                                    @Parameter (description = "Peppol Process Identifier",
                                                                required = true,
                                                                example = "cenbii-procid-ubl::urn:peppol:pint:billing-1@jp-1") @PathVariable ("processID") final String sProcessID,
                                                    @Parameter (description = "ISO 3166-1 alpha-2 country code of the sender (C1)",
                                                                required = true,
                                                                example = "AT") @PathVariable ("c1CountryCode") final String sC1CountryCode,
                                                    @Parameter (hidden = true) @NonNull final HttpServletRequest aServletRequest,
                                                    @Parameter (description = "Custom SBDH Instance Identifier. A random UUID-based identifier is generated when omitted.") @RequestParam (value = "sbdhInstanceID",
                                                                                                                                                                                            required = false) final String sSbdhInstanceID,
                                                    @Parameter (description = "Alternative Peppol Participant ID to receive MLS responses") @RequestParam (value = "mlsTo",
                                                                                                                                                            required = false) final String sMlsTo,
                                                    @Parameter (description = "SBDH Standard override for non-XML payloads (e.g., urn:peppol:doctype:pdf+xml). Auto-derived from the document type when omitted.") @RequestParam (value = "sbdhStandard",
                                                                                                                                                                                                                                  required = false) final String sSbdhStandard,
                                                    @Parameter (description = "SBDH TypeVersion override (e.g., 0). Auto-derived from the document type when omitted.") @RequestParam (value = "sbdhTypeVersion",
                                                                                                                                                                                       required = false) final String sSbdhTypeVersion,
                                                    @Parameter (description = "SBDH Type override (e.g., factur-x). Auto-derived from the document type when omitted.") @RequestParam (value = "sbdhType",
                                                                                                                                                                                       required = false) final String sSbdhType,
                                                    @Parameter (description = "MIME type for binary payloads (e.g., application/pdf). When set, the payload is wrapped in <BinaryContent>; otherwise treated as XML.") @RequestParam (value = "payloadMimeType",
                                                                                                                                                                                                                                      required = false) final String sPayloadMimeType,
                                                    @Parameter (description = "Optional custom field 1 (max 255 characters). Stored with the transaction and returned by the status APIs.") @RequestParam (value = "custom1",
                                                                                                                                                                                                          required = false) final String sCustom1,
                                                    @Parameter (description = "Optional custom field 2 (max 255 characters). Stored with the transaction and returned by the status APIs.") @RequestParam (value = "custom2",
                                                                                                                                                                                                          required = false) final String sCustom2,
                                                    @Parameter (description = "Optional custom field 3 (max 255 characters). Stored with the transaction and returned by the status APIs.") @RequestParam (value = "custom3",
                                                                                                                                                                                                          required = false) final String sCustom3) throws Exception
  {
    if (!APCoreConfig.isSendingEnabled ())
    {
      LOGGER.info ("Peppol AP sending is disabled");
      return ResponseEntity.notFound ().build ();
    }

    final ResponseEntity <String> aCustomErr = _validateCustomFields (sCustom1, sCustom2, sCustom3);
    if (aCustomErr != null)
      return aCustomErr;

    final String sEffectiveSbdhInstanceID = StringHelper.isNotEmpty (sSbdhInstanceID) ? sSbdhInstanceID
                                                                                      : PeppolSBDHData.createRandomSBDHInstanceIdentifier ();

    // Parse the identifiers
    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();

    // Start configuring here
    IParticipantIdentifier aSenderID = aIF.parseParticipantIdentifier (sSenderID);
    if (aSenderID == null)
    {
      // Fallback to default scheme
      aSenderID = aIF.createParticipantIdentifierWithDefaultScheme (sSenderID);
    }
    if (aSenderID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the sending participant ID '" + sSenderID + "'")
                                           .getAsJsonString ());
    }

    IParticipantIdentifier aReceiverID = aIF.parseParticipantIdentifier (sReceiverID);
    if (aReceiverID == null)
    {
      // Fallback to default scheme
      aReceiverID = aIF.createParticipantIdentifierWithDefaultScheme (sReceiverID);
    }
    if (aReceiverID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the receiving participant ID '" +
                                                    sReceiverID +
                                                    "'").getAsJsonString ());
    }

    IDocumentTypeIdentifier aDocTypeID = aIF.parseDocumentTypeIdentifier (sDocTypeID);
    if (aDocTypeID == null)
    {
      // Fallback to default scheme
      aDocTypeID = aIF.createDocumentTypeIdentifierWithDefaultScheme (sDocTypeID);
    }
    if (aDocTypeID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the document type ID '" + sDocTypeID + "'")
                                           .getAsJsonString ());
    }

    IProcessIdentifier aProcessID = aIF.parseProcessIdentifier (sProcessID);
    if (aProcessID == null)
    {
      // Fallback to default scheme
      aProcessID = aIF.createProcessIdentifierWithDefaultScheme (sProcessID);
    }
    if (aProcessID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the process ID '" + sProcessID + "'")
                                           .getAsJsonString ());
    }

    // Read the InputStream only once
    try (final InputStream aIS = aServletRequest.getInputStream ())
    {
      // Store in DB
      final IOutboundTransaction aTx = OutboundOrchestrator.submitRawDocument ("[SubmitRaw] ",
                                                                               aSenderID,
                                                                               aReceiverID,
                                                                               aDocTypeID,
                                                                               aProcessID,
                                                                               sEffectiveSbdhInstanceID,
                                                                               sC1CountryCode,
                                                                               aIS,
                                                                               sMlsTo,
                                                                               sSbdhStandard,
                                                                               sSbdhTypeVersion,
                                                                               sSbdhType,
                                                                               sPayloadMimeType,
                                                                               sCustom1,
                                                                               sCustom2,
                                                                               sCustom3);
      if (aTx == null)
      {
        return ResponseEntity.unprocessableContent ()
                             .body (JsonValue.create ("Failed to submit outbound transaction").getAsJsonString ());
      }

      // Perform actual sending
      final Phase4PeppolSendingReport aSendingReport = OutboundOrchestrator.processPendingOutbound ("[SubmitRaw] ",
                                                                                                    aTx);
      if (!aSendingReport.isOverallSuccess ())
      {
        return ResponseEntity.unprocessableContent ().body (aSendingReport.getAsJsonString ());
      }

      // Sending success
      return ResponseEntity.ok (aSendingReport.getAsJsonString ());
    }
  }

  /**
   * Submit a pre-built Standard Business Document (SBD) for outbound sending via the Peppol
   * network. The complete SBD is read from the HTTP request body.
   *
   * @param aServletRequest
   *        The HTTP servlet request containing the SBD payload.
   * @param sMlsTo
   *        Optional MLS "To" address.
   * @param sCustom1
   *        Optional custom field 1 (max 255 characters).
   * @param sCustom2
   *        Optional custom field 2 (max 255 characters).
   * @param sCustom3
   *        Optional custom field 3 (max 255 characters).
   * @return The {@link Phase4PeppolSendingReport} as JSON on success, or an error response.
   * @throws Exception
   *         On unexpected errors.
   */
  @PostMapping (value = "/submit-sbd", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation (summary = "Submit a pre-built SBD for outbound sending",
              description = "Submits a complete Standard Business Document (with SBDH already present). All Peppol metadata " +
                            "(sender, receiver, document type, process, C1 country code) is extracted from the SBDH.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "SBD accepted and sent successfully"),
                   @ApiResponse (responseCode = "400", description = "Failed to submit the outbound SBD transaction"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content),
                   @ApiResponse (responseCode = "404",
                                 description = "Sending is disabled in the configuration",
                                 content = @Content),
                   @ApiResponse (responseCode = "422", description = "Sending failed — see the report body for details") })
  public ResponseEntity <String> submitPrebuiltSBD (@Parameter (hidden = true) @NonNull final HttpServletRequest aServletRequest,
                                                    @Parameter (description = "Alternative Peppol Participant ID to receive MLS responses") @RequestParam (value = "mlsTo",
                                                                                                                                                            required = false) final String sMlsTo,
                                                    @Parameter (description = "Optional custom field 1 (max 255 characters). Stored with the transaction and returned by the status APIs.") @RequestParam (value = "custom1",
                                                                                                                                                                                                          required = false) final String sCustom1,
                                                    @Parameter (description = "Optional custom field 2 (max 255 characters). Stored with the transaction and returned by the status APIs.") @RequestParam (value = "custom2",
                                                                                                                                                                                                          required = false) final String sCustom2,
                                                    @Parameter (description = "Optional custom field 3 (max 255 characters). Stored with the transaction and returned by the status APIs.") @RequestParam (value = "custom3",
                                                                                                                                                                                                          required = false) final String sCustom3) throws Exception
  {
    if (!APCoreConfig.isSendingEnabled ())
    {
      LOGGER.info ("Peppol AP sending is disabled");
      return ResponseEntity.notFound ().build ();
    }

    final ResponseEntity <String> aCustomErr = _validateCustomFields (sCustom1, sCustom2, sCustom3);
    if (aCustomErr != null)
      return aCustomErr;

    // Read the InputStream only once
    try (final InputStream aIS = aServletRequest.getInputStream ())
    {
      // Store in DB
      final IOutboundTransaction aTx = OutboundOrchestrator.submitPrebuiltSBD ("[SubmitPrebuiltSBD] ",
                                                                               aIS,
                                                                               sMlsTo,
                                                                               sCustom1,
                                                                               sCustom2,
                                                                               sCustom3);
      if (aTx == null)
      {
        return ResponseEntity.badRequest ()
                             .body (JsonValue.create ("Failed to submit outbound SBD transaction").getAsJsonString ());
      }

      // Perform actual sending
      final Phase4PeppolSendingReport aSendingReport = OutboundOrchestrator.processPendingOutbound ("[SubmitPrebuiltSBD] ",
                                                                                                    aTx);
      if (!aSendingReport.isOverallSuccess ())
      {
        return ResponseEntity.unprocessableContent ().body (aSendingReport.getAsJsonString ());
      }

      // Sending success
      return ResponseEntity.ok (aSendingReport.getAsJsonString ());
    }
  }

  /**
   * Submit a raw document for outbound sending with automatic document type and process detection
   * via the DDD (Document Details Determinator) library. The XML payload is analyzed to determine
   * the Peppol document type and process identifiers automatically.
   *
   * @param sSenderID
   *        The sender participant identifier.
   * @param sReceiverID
   *        The receiver participant identifier.
   * @param sC1CountryCode
   *        The C1 country code.
   * @param aServletRequest
   *        The HTTP servlet request containing the XML document payload.
   * @param sSbdhInstanceID
   *        Optional SBDH Instance ID. A random one is generated if not provided.
   * @param sMlsTo
   *        Optional MLS "To" address.
   * @param sCustom1
   *        Optional custom field 1 (max 255 characters).
   * @param sCustom2
   *        Optional custom field 2 (max 255 characters).
   * @param sCustom3
   *        Optional custom field 3 (max 255 characters).
   * @return The {@link Phase4PeppolSendingReport} as JSON on success, or an error response.
   * @throws Exception
   *         On unexpected errors.
   * @since v0.2.0
   */
  @PostMapping (value = "/submit-auto/{senderID}/{receiverID}/{c1CountryCode}",
                produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation (summary = "Submit a document with auto-detected document type",
              description = "Submits a raw business document for outbound sending using the DDD (Document Details Determinator) " +
                            "library to determine the document type and process identifiers from the XML automatically. " +
                            "Does not support binary payloads or SBDH overrides. Since v0.2.0.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Document accepted and sent successfully"),
                   @ApiResponse (responseCode = "400",
                                 description = "Empty body, invalid XML, unrecognised document type, or invalid participant ID"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content),
                   @ApiResponse (responseCode = "404",
                                 description = "Sending is disabled in the configuration",
                                 content = @Content),
                   @ApiResponse (responseCode = "422", description = "Sending failed — see the report body for details") })
  public ResponseEntity <String> submitAutoDetect (@Parameter (description = "Peppol Participant ID of the sender (C1)",
                                                               required = true,
                                                               example = "iso6523-actorid-upis::0088:senderbackend") @PathVariable ("senderID") final String sSenderID,
                                                   @Parameter (description = "Peppol Participant ID of the receiver (C4)",
                                                               required = true,
                                                               example = "iso6523-actorid-upis::0088:receiverbackend") @PathVariable ("receiverID") final String sReceiverID,
                                                   @Parameter (description = "ISO 3166-1 alpha-2 country code of the sender (C1)",
                                                               required = true,
                                                               example = "AT") @PathVariable ("c1CountryCode") final String sC1CountryCode,
                                                   @Parameter (hidden = true) @NonNull final HttpServletRequest aServletRequest,
                                                   @Parameter (description = "Custom SBDH Instance Identifier. A random UUID-based identifier is generated when omitted.") @RequestParam (value = "sbdhInstanceID",
                                                                                                                                                                                           required = false) final String sSbdhInstanceID,
                                                   @Parameter (description = "Alternative Peppol Participant ID to receive MLS responses") @RequestParam (value = "mlsTo",
                                                                                                                                                           required = false) final String sMlsTo,
                                                   @Parameter (description = "Optional custom field 1 (max 255 characters). Stored with the transaction and returned by the status APIs.") @RequestParam (value = "custom1",
                                                                                                                                                                                                         required = false) final String sCustom1,
                                                   @Parameter (description = "Optional custom field 2 (max 255 characters). Stored with the transaction and returned by the status APIs.") @RequestParam (value = "custom2",
                                                                                                                                                                                                         required = false) final String sCustom2,
                                                   @Parameter (description = "Optional custom field 3 (max 255 characters). Stored with the transaction and returned by the status APIs.") @RequestParam (value = "custom3",
                                                                                                                                                                                                         required = false) final String sCustom3) throws Exception
  {
    if (!APCoreConfig.isSendingEnabled ())
    {
      LOGGER.info ("Peppol AP sending is disabled");
      return ResponseEntity.notFound ().build ();
    }

    final ResponseEntity <String> aCustomErr = _validateCustomFields (sCustom1, sCustom2, sCustom3);
    if (aCustomErr != null)
      return aCustomErr;

    final String sLogPrefix = "[SubmitAutoDetect] ";
    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();

    // Parse sender and receiver
    IParticipantIdentifier aSenderID = aIF.parseParticipantIdentifier (sSenderID);
    if (aSenderID == null)
      aSenderID = aIF.createParticipantIdentifierWithDefaultScheme (sSenderID);
    if (aSenderID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the sending participant ID '" + sSenderID + "'")
                                           .getAsJsonString ());
    }

    IParticipantIdentifier aReceiverID = aIF.parseParticipantIdentifier (sReceiverID);
    if (aReceiverID == null)
      aReceiverID = aIF.createParticipantIdentifierWithDefaultScheme (sReceiverID);
    if (aReceiverID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the receiving participant ID '" +
                                                    sReceiverID +
                                                    "'").getAsJsonString ());
    }

    // Read the full payload into memory (needed for both DDD parsing and storage)
    final byte [] aPayloadBytes;
    try (final InputStream aIS = aServletRequest.getInputStream ())
    {
      aPayloadBytes = StreamHelper.getAllBytes (aIS);
    }
    if (aPayloadBytes == null || aPayloadBytes.length == 0)
      return ResponseEntity.badRequest ().body (JsonValue.create ("The request body is empty").getAsJsonString ());

    // Parse XML
    final Document aDoc = DOMReader.readXMLDOM (aPayloadBytes);
    if (aDoc == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("The request body is not valid XML").getAsJsonString ());
    }

    // Auto-detect document type and process via DDD
    final DocumentDetails aDD = DDDHelper.findDocumentDetails (aDoc.getDocumentElement ());
    if (aDD == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Unable to determine the document type from the provided XML")
                                           .getAsJsonString ());
    }

    final IDocumentTypeIdentifier aDocTypeID = aDD.getDocumentTypeID ();
    final IProcessIdentifier aProcessID = aDD.getProcessID ();

    if (aDocTypeID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("DDD could not determine the document type identifier")
                                           .getAsJsonString ());
    }
    if (aProcessID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("DDD could not determine the process identifier")
                                           .getAsJsonString ());
    }

    LOGGER.info (sLogPrefix +
                 "DDD detected docTypeID='" +
                 aDocTypeID.getURIEncoded () +
                 "', processID='" +
                 aProcessID.getURIEncoded () +
                 "'");

    final String sEffectiveSbdhInstanceID = StringHelper.isNotEmpty (sSbdhInstanceID) ? sSbdhInstanceID
                                                                                      : PeppolSBDHData.createRandomSBDHInstanceIdentifier ();

    // Submit via the standard outbound pipeline
    try (final InputStream aPayloadIS = new java.io.ByteArrayInputStream (aPayloadBytes))
    {
      final IOutboundTransaction aTx = OutboundOrchestrator.submitRawDocument (sLogPrefix,
                                                                               aSenderID,
                                                                               aReceiverID,
                                                                               aDocTypeID,
                                                                               aProcessID,
                                                                               sEffectiveSbdhInstanceID,
                                                                               sC1CountryCode,
                                                                               aPayloadIS,
                                                                               sMlsTo,
                                                                               null,
                                                                               null,
                                                                               null,
                                                                               null,
                                                                               sCustom1,
                                                                               sCustom2,
                                                                               sCustom3);
      if (aTx == null)
        return ResponseEntity.badRequest ()
                             .body (JsonValue.create ("Failed to submit outbound transaction").getAsJsonString ());

      // Perform actual sending
      final Phase4PeppolSendingReport aSendingReport = OutboundOrchestrator.processPendingOutbound (sLogPrefix, aTx);
      if (!aSendingReport.isOverallSuccess ())
        return ResponseEntity.unprocessableContent ().body (aSendingReport.getAsJsonString ());

      return ResponseEntity.ok (aSendingReport.getAsJsonString ());
    }
  }

  /**
   * Submit a document for outbound sending by referencing an S3 object. The document payload is
   * fetched from the specified S3 bucket/key rather than being inlined in the HTTP request body.
   * This allows sender backends to upload large documents to S3 and then trigger sending via the
   * AP.
   *
   * @param aRequest
   *        The JSON request body containing Peppol identifiers and S3 reference. May not be
   *        <code>null</code>.
   * @return The {@link Phase4PeppolSendingReport} as JSON on success, or an error response.
   */
  @PostMapping (value = "/submit-s3",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation (summary = "Submit a document referenced from S3",
              description = "Submits a document for outbound sending by referencing an S3 object instead of inlining the payload. " +
                            "The Sender Backend uploads the document to S3 first, then calls this endpoint. " +
                            "Requires 'outbound.s3.enabled=true'. Since v0.1.1.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Document fetched, accepted and sent successfully"),
                   @ApiResponse (responseCode = "400",
                                 description = "Outbound S3 disabled, missing required fields, invalid identifiers, or S3 fetch failed"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content),
                   @ApiResponse (responseCode = "404",
                                 description = "Sending is disabled in the configuration",
                                 content = @Content),
                   @ApiResponse (responseCode = "422", description = "Sending failed — see the report body for details") })
  public ResponseEntity <String> submitFromS3 (@RequestBody final OutboundS3SubmitRequest aRequest)
  {
    if (!APCoreConfig.isSendingEnabled ())
    {
      LOGGER.info ("Peppol AP sending is disabled");
      return ResponseEntity.notFound ().build ();
    }

    if (!APCoreConfig.isOutboundS3Enabled ())
    {
      LOGGER.info ("Outbound S3 submission is disabled");
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Outbound S3 submission is not enabled").getAsJsonString ());
    }

    // Validate required fields
    if (StringHelper.isEmpty (aRequest.getSenderID ()) ||
      StringHelper.isEmpty (aRequest.getReceiverID ()) ||
      StringHelper.isEmpty (aRequest.getDocTypeID ()) ||
      StringHelper.isEmpty (aRequest.getProcessID ()) ||
      StringHelper.isEmpty (aRequest.getC1CountryCode ()) ||
      StringHelper.isEmpty (aRequest.getS3Key ()))
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Missing required fields: senderID, receiverID, docTypeID, processID, c1CountryCode, s3Key")
                                           .getAsJsonString ());
    }

    final ResponseEntity <String> aCustomErr = _validateCustomFields (aRequest.getCustom1 (),
                                                                      aRequest.getCustom2 (),
                                                                      aRequest.getCustom3 ());
    if (aCustomErr != null)
      return aCustomErr;

    final String sEffectiveSbdhInstanceID = StringHelper.isNotEmpty (aRequest.getSbdhInstanceID ()) ? aRequest.getSbdhInstanceID ()
                                                                                                    : PeppolSBDHData.createRandomSBDHInstanceIdentifier ();

    // Parse the identifiers
    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();

    IParticipantIdentifier aSenderID = aIF.parseParticipantIdentifier (aRequest.getSenderID ());
    if (aSenderID == null)
      aSenderID = aIF.createParticipantIdentifierWithDefaultScheme (aRequest.getSenderID ());
    if (aSenderID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the sending participant ID '" +
                                                    aRequest.getSenderID () +
                                                    "'").getAsJsonString ());
    }

    IParticipantIdentifier aReceiverID = aIF.parseParticipantIdentifier (aRequest.getReceiverID ());
    if (aReceiverID == null)
      aReceiverID = aIF.createParticipantIdentifierWithDefaultScheme (aRequest.getReceiverID ());
    if (aReceiverID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the receiving participant ID '" +
                                                    aRequest.getReceiverID () +
                                                    "'").getAsJsonString ());
    }

    IDocumentTypeIdentifier aDocTypeID = aIF.parseDocumentTypeIdentifier (aRequest.getDocTypeID ());
    if (aDocTypeID == null)
      aDocTypeID = aIF.createDocumentTypeIdentifierWithDefaultScheme (aRequest.getDocTypeID ());
    if (aDocTypeID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the document type ID '" +
                                                    aRequest.getDocTypeID () +
                                                    "'").getAsJsonString ());
    }

    IProcessIdentifier aProcessID = aIF.parseProcessIdentifier (aRequest.getProcessID ());
    if (aProcessID == null)
      aProcessID = aIF.createProcessIdentifierWithDefaultScheme (aRequest.getProcessID ());
    if (aProcessID == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to parse the process ID '" + aRequest.getProcessID () + "'")
                                           .getAsJsonString ());
    }

    // Determine the S3 region - use from configuration
    final String sS3Region = APCoreConfig.getOutboundS3Region ();
    if (StringHelper.isEmpty (sS3Region))
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("No outbound S3 region configured (outbound.s3.region)")
                                           .getAsJsonString ());
    }
    final Region aRegion = Region.of (sS3Region);
    if (aRegion == null)
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("The outbound S3 region configuration '" +
                                                    sS3Region +
                                                    "' is invalid!").getAsJsonString ());
    }

    // Determine the S3 bucket - use from request, fallback to configured default
    final String sS3Bucket = StringHelper.isNotEmpty (aRequest.getS3Bucket ()) ? aRequest.getS3Bucket ()
                                                                               : APCoreConfig.getOutboundS3Bucket ();
    if (StringHelper.isEmpty (sS3Bucket))
    {
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("No S3 bucket specified in request and no default configured")
                                           .getAsJsonString ());
    }

    // Build S3 client for the sender's bucket

    final S3ClientBuilder aS3Builder = S3Client.builder ().region (aRegion);
    final String sOutboundEndpoint = APCoreConfig.getOutboundS3Endpoint ();
    if (StringHelper.isNotEmpty (sOutboundEndpoint))
      aS3Builder.endpointOverride (URI.create (sOutboundEndpoint));
    if (APCoreConfig.isOutboundS3PathStyleAccess ())
      aS3Builder.forcePathStyle (Boolean.TRUE);
    final String sAccessKeyID = APCoreConfig.getOutboundS3AccessKeyID ();
    final String sSecretAccessKey = APCoreConfig.getOutboundS3SecretAccessKey ();
    if (StringHelper.isNotEmpty (sAccessKeyID) && StringHelper.isNotEmpty (sSecretAccessKey))
    {
      aS3Builder.credentialsProvider (StaticCredentialsProvider.create (AwsBasicCredentials.create (sAccessKeyID,
                                                                                                    sSecretAccessKey)));
    }

    try (final S3Client aS3Client = aS3Builder.build ();
         final InputStream aIS = aS3Client.getObject (GetObjectRequest.builder ()
                                                                      .bucket (sS3Bucket)
                                                                      .key (aRequest.getS3Key ())
                                                                      .build ()))
    {
      // Store in DB
      final IOutboundTransaction aTx = OutboundOrchestrator.submitRawDocument ("[SubmitS3] ",
                                                                               aSenderID,
                                                                               aReceiverID,
                                                                               aDocTypeID,
                                                                               aProcessID,
                                                                               sEffectiveSbdhInstanceID,
                                                                               aRequest.getC1CountryCode (),
                                                                               aIS,
                                                                               aRequest.getMlsTo (),
                                                                               aRequest.getSbdhStandard (),
                                                                               aRequest.getSbdhTypeVersion (),
                                                                               aRequest.getSbdhType (),
                                                                               aRequest.getPayloadMimeType (),
                                                                               aRequest.getCustom1 (),
                                                                               aRequest.getCustom2 (),
                                                                               aRequest.getCustom3 ());
      if (aTx == null)
      {
        return ResponseEntity.unprocessableContent ()
                             .body (JsonValue.create ("Failed to submit outbound transaction from S3")
                                             .getAsJsonString ());
      }

      // Perform actual sending
      final Phase4PeppolSendingReport aSendingReport = OutboundOrchestrator.processPendingOutbound ("[SubmitS3] ", aTx);
      if (!aSendingReport.isOverallSuccess ())
      {
        return ResponseEntity.unprocessableContent ().body (aSendingReport.getAsJsonString ());
      }

      return ResponseEntity.ok (aSendingReport.getAsJsonString ());
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to fetch document from S3 bucket '" + sS3Bucket + "' key '" + aRequest.getS3Key () + "'",
                    ex);
      return ResponseEntity.badRequest ()
                           .body (JsonValue.create ("Failed to fetch document from S3: " + ex.getMessage ())
                                           .getAsJsonString ());
    }
  }

  /**
   * Get the current status of an outbound transaction by its SBDH Instance ID.
   *
   * @param sSbdhInstanceID
   *        The SBDH Instance ID to look up.
   * @param bIncludeArchive
   *        When <code>true</code>, the archive table is searched if the transaction is no longer
   *        present in the active table. Default is <code>false</code> (active table only). Added in
   *        0.9.0.
   * @return The transaction details, or 404 if not found.
   */
  @GetMapping ("/status/{sbdhInstanceID}")
  @Operation (summary = "Query outbound transaction status",
              description = "Returns the current status of a specific outbound transaction. By default only the active " +
                            "outbound_transaction table is searched. Pass includeArchive=true to also consider archived transactions.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "Transaction found"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content),
                   @ApiResponse (responseCode = "404",
                                 description = "No outbound transaction with the given SBDH Instance ID",
                                 content = @Content) })
  public ResponseEntity <OutboundTransactionResponse> getStatus (@Parameter (description = "Peppol SBDH Instance Identifier of the outbound message",
                                                                             required = true,
                                                                             example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable ("sbdhInstanceID") final String sSbdhInstanceID,
                                                                 @Parameter (description = "When true, the archive table is consulted if the transaction is not in the active table. Since 0.9.0.") @RequestParam (name = "includeArchive",
                                                                                                                                                                                                                    defaultValue = "false") final boolean bIncludeArchive)
  {
    LOGGER.info ("Checking for status of transmission with ID '" +
                 sSbdhInstanceID +
                 "'" +
                 (bIncludeArchive ? " (including archive)" : ""));

    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final IOutboundTransaction aTx = bIncludeArchive ? aTxMgr.getBySbdhInstanceIDIncludingArchive (sSbdhInstanceID)
                                                     : aTxMgr.getBySbdhInstanceID (sSbdhInstanceID);
    if (aTx == null)
    {
      LOGGER.info ("No such transaction");
      return ResponseEntity.notFound ().build ();
    }

    return ResponseEntity.ok (OutboundTransactionResponse.fromDomain (aTx));
  }

  /**
   * Get all outbound transactions that are currently in transmission (not yet completed or
   * permanently failed).
   *
   * @return A list of in-transmission outbound transactions.
   */
  @GetMapping ("/in-transmission")
  @Operation (summary = "List outbound transactions in transmission",
              description = "Returns all outbound transactions that are not yet in a final state — includes status pending, " +
                            "sending, failed (awaiting retry). Excludes rejected, sent, permanently_failed.")
  @ApiResponses ({ @ApiResponse (responseCode = "200", description = "List of outbound transactions"),
                   @ApiResponse (responseCode = "401",
                                 description = "Missing or invalid API token",
                                 content = @Content) })
  public ResponseEntity <List <OutboundTransactionResponse>> getInTransmission ()
  {
    LOGGER.info ("Checking for all outbound transmissions in progress");

    final IOutboundTransactionManager aTxMgr = APJdbcMetaManager.getOutboundTransactionMgr ();
    final var aTxs = aTxMgr.getAllInTransmission ();
    final ICommonsList <OutboundTransactionResponse> aResult = aTxs.getAllMapped (OutboundTransactionResponse::fromDomain);
    return ResponseEntity.ok (aResult);
  }
}
