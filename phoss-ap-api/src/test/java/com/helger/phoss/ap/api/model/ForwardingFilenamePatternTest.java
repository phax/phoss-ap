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
package com.helger.phoss.ap.api.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.phoss.ap.api.codelist.EForwardableKind;

/**
 * Test class for class {@link ForwardingFilenamePattern}.
 *
 * @author Philip Helger
 */
public final class ForwardingFilenamePatternTest
{
  private static final String CONFIG_KEY = "forwarding.sftp.filename-pattern";
  private static final OffsetDateTime TIMESTAMP = OffsetDateTime.parse ("2026-08-30T10:15:30+02:00");
  private static final String SENDER_ID = "iso6523-actorid-upis::9915:sender";
  private static final String RECEIVER_ID = "iso6523-actorid-upis::0151:35747532810";
  private static final String DOCTYPE_ID = "busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1";
  private static final String PROCESS_ID = "cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0";

  @NonNull
  private static IForwardableDocument _doc (@NonNull final String sSbdhInstanceID)
  {
    return new ForwardableDocument ("tx-1",
                                    EForwardableKind.INBOUND_DOCUMENT,
                                    sSbdhInstanceID,
                                    "/var/phoss-ap/inbound/tx-1.xml",
                                    4711,
                                    DOCTYPE_ID,
                                    PROCESS_ID,
                                    SENDER_ID,
                                    RECEIVER_ID,
                                    TIMESTAMP,
                                    "incoming-1",
                                    null,
                                    null,
                                    null,
                                    null,
                                    null);
  }

  @NonNull
  private static IForwardableDocument _doc ()
  {
    return _doc ("sbdh-1");
  }

  @NonNull
  private static String _resolveFilename (@NonNull final String sPattern)
  {
    return new ForwardingFilenamePattern (sPattern, false, ForwardingFilenamePattern.MAX_LENGTH_FILENAME)
                                                                                                         .getResolvedBaseName (_doc ());
  }

  @Test
  public void testDatetimeAndIncomingID ()
  {
    assertEquals ("20260830101530_incoming-1", _resolveFilename ("{datetime}_{incoming-id}"));
  }

  @Test
  public void testDollarSyntaxIsNotSupported ()
  {
    // "${name}" is the variable syntax of the configuration itself and is rejected at startup
    assertEquals (ESuccess.FAILURE,
                  ForwardingFilenamePattern.checkPattern (CONFIG_KEY, "${datetime}_${incoming-id}", false));
    assertEquals (ESuccess.FAILURE,
                  ForwardingFilenamePattern.checkPattern (CONFIG_KEY, "{datetime}-${sbdh-instance-id}", false));
    // The '$' itself is an ordinary character of the literal part
    assertEquals ("_20260830101530", _resolveFilename ("${datetime}"));
  }

  @Test
  public void testParticipantPlaceholders ()
  {
    // The full ID contains the identifier scheme; ':' is replaced platform independently
    assertEquals ("iso6523-actorid-upis__0151_35747532810", _resolveFilename ("{receiver-id}"));
    assertEquals ("iso6523-actorid-upis__9915_sender", _resolveFilename ("{sender-id}"));
    // The value keeps the ICD - the same number in a different ICD is a different participant
    assertEquals ("0151_35747532810", _resolveFilename ("{receiver-value}"));
    assertEquals ("9915_sender", _resolveFilename ("{sender-value}"));
  }

  @Test
  public void testUnknownPlaceholderIsEmptyAtRuntime ()
  {
    // An unknown placeholder is rejected by checkPattern, so it may never end up in a name verbatim
    assertEquals ("20260830101530_", _resolveFilename ("{datetime}_{reciever-id}"));
  }

  @Test
  public void testUnbalancedBracesAreLiteral ()
  {
    // The first brace is no placeholder start, because of the nested one
    assertEquals ("_datetime_incoming-1", _resolveFilename ("{datetime_{incoming-id}"));
  }

  @Test
  public void testNoPathTraversalFromDocumentValues ()
  {
    // The SBDH Instance Identifier is provided by the sender
    final String sBaseName = new ForwardingFilenamePattern ("{sbdh-instance-id}",
                                                            false,
                                                            ForwardingFilenamePattern.MAX_LENGTH_FILENAME).getResolvedBaseName (_doc ("../../etc/passwd"));
    // The leading dots are removed as well, so that this is no hidden file
    assertEquals ("_.._etc_passwd", sBaseName);
    assertFalse (sBaseName.contains ("/"));
    assertFalse (sBaseName.contains ("\\"));
  }

  @Test
  public void testNoPathTraversalInAnObjectKeyEither ()
  {
    // The literal '/' is kept, but the one from the document value is not
    final String sKey = new ForwardingFilenamePattern ("in/{sbdh-instance-id}",
                                                       true,
                                                       ForwardingFilenamePattern.MAX_LENGTH_OBJECT_KEY).getResolvedBaseName (_doc ("../../etc/passwd"));
    assertEquals ("in/.._.._etc_passwd", sKey);
  }

  @Test
  public void testUnusableResultFallsBackToTheDefaultLayout ()
  {
    // An SBDH Instance Identifier that consists of illegal characters only
    final String sBaseName = new ForwardingFilenamePattern ("{sbdh-instance-id}",
                                                            false,
                                                            ForwardingFilenamePattern.MAX_LENGTH_FILENAME).getResolvedBaseName (_doc ("..."));
    assertEquals ("20260830101530_incoming-1", sBaseName);
  }

  @Test
  public void testTooLongNameIsTruncated ()
  {
    // The document type ID alone is longer than the maximum filename length
    final String sBaseName = _resolveFilename ("{doctype-id}_{process-id}");
    assertEquals (ForwardingFilenamePattern.MAX_LENGTH_FILENAME, sBaseName.length ());
    assertTrue (sBaseName.startsWith ("busdox-docid-qns__urn_oasis_names"));

    // The same pattern fits into an object key
    final String sKey = new ForwardingFilenamePattern ("{doctype-id}_{process-id}",
                                                       true,
                                                       ForwardingFilenamePattern.MAX_LENGTH_OBJECT_KEY).getResolvedBaseName (_doc ());
    assertEquals (225, sKey.length ());
  }

  @Test
  public void testCheckPatternSuccess ()
  {
    assertEquals (ESuccess.SUCCESS,
                  ForwardingFilenamePattern.checkPattern (CONFIG_KEY,
                                                          "{receiver-value}_{datetime}_{incoming-id}",
                                                          false));
    // No unique part - a warning is logged, but the pattern is accepted
    assertEquals (ESuccess.SUCCESS, ForwardingFilenamePattern.checkPattern (CONFIG_KEY, "{receiver-value}", false));
    // A path separator is allowed in an object key only
    assertEquals (ESuccess.SUCCESS,
                  ForwardingFilenamePattern.checkPattern (CONFIG_KEY, "{receiver-value}/{sbdh-instance-id}", true));
  }

  @Test
  public void testCheckPatternFailure ()
  {
    // Typo in a placeholder name
    assertEquals (ESuccess.FAILURE,
                  ForwardingFilenamePattern.checkPattern (CONFIG_KEY, "{datetime}_{reciever-id}", false));
    // Empty pattern
    assertEquals (ESuccess.FAILURE, ForwardingFilenamePattern.checkPattern (CONFIG_KEY, "", false));
    assertEquals (ESuccess.FAILURE, ForwardingFilenamePattern.checkPattern (CONFIG_KEY, null, false));
    // Unbalanced braces
    assertEquals (ESuccess.FAILURE,
                  ForwardingFilenamePattern.checkPattern (CONFIG_KEY, "{datetime_{incoming-id}", false));
    // Subdirectories are not supported in a filename
    assertEquals (ESuccess.FAILURE,
                  ForwardingFilenamePattern.checkPattern (CONFIG_KEY, "{receiver-value}/{datetime}", false));
    assertEquals (ESuccess.FAILURE, ForwardingFilenamePattern.checkPattern (CONFIG_KEY, "..\\{datetime}", false));
  }

  @Test
  public void testAllPlaceholdersAreResolvable ()
  {
    // Every placeholder must have a unique ID and must provide a value
    final ICommonsSet <String> aIDs = new CommonsHashSet <> ();
    for (final EForwardingFilenamePlaceholder ePlaceholder : EForwardingFilenamePlaceholder.values ())
    {
      assertTrue ("Duplicate ID " + ePlaceholder.getID (), aIDs.add (ePlaceholder.getID ()));
      assertSame (ePlaceholder, EForwardingFilenamePlaceholder.getFromIDOrNull (ePlaceholder.getID ()));
      assertTrue ("No value for " + ePlaceholder.getID (), StringHelper.isNotEmpty (ePlaceholder.getValue (_doc ())));
    }
    assertEquals (9, aIDs.size ());
  }

  @Test
  public void testOnlyTheIDsAreUniquePerDocument ()
  {
    // These two are what makes a resolved name unique - the warning at startup depends on it
    assertTrue (EForwardingFilenamePlaceholder.INCOMING_ID.isUniquePerDocument ());
    assertTrue (EForwardingFilenamePlaceholder.SBDH_INSTANCE_ID.isUniquePerDocument ());
    assertFalse (EForwardingFilenamePlaceholder.DATETIME.isUniquePerDocument ());
    assertFalse (EForwardingFilenamePlaceholder.RECEIVER_ID.isUniquePerDocument ());
  }

  @Test
  public void testPlaceholderValuesAreUnsanitized ()
  {
    // The sanitization happens in ForwardingFilenamePattern, not in the value provider
    assertEquals (RECEIVER_ID, EForwardingFilenamePlaceholder.RECEIVER_ID.getValue (_doc ()));
    assertEquals ("0151:35747532810", EForwardingFilenamePlaceholder.RECEIVER_VALUE.getValue (_doc ()));
    assertEquals ("9915:sender", EForwardingFilenamePlaceholder.SENDER_VALUE.getValue (_doc ()));
    assertEquals ("20260830101530", EForwardingFilenamePlaceholder.DATETIME.getValue (_doc ()));
  }
}
