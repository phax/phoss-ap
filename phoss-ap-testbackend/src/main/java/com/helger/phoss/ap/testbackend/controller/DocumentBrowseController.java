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
package com.helger.phoss.ap.testbackend.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helger.phoss.ap.testbackend.model.ReceivedDocument;
import com.helger.phoss.ap.testbackend.store.DocumentStore;

/**
 * Browse, inspect and download received documents across all forwarding
 * channels (HTTP, SFTP, S3).
 *
 * @author Philip Helger
 */
@RestController
@RequestMapping ("/api/documents")
public class DocumentBrowseController
{
  @Autowired
  private DocumentStore m_aDocumentStore;

  @GetMapping
  public ResponseEntity <List <Map <String, Object>>> listAll (@RequestParam (value = "channel",
                                                                              required = false) final String sChannel)
  {
    final List <ReceivedDocument> aDocs;
    if (sChannel != null)
      aDocs = m_aDocumentStore.getAllByChannel (sChannel);
    else
      aDocs = m_aDocumentStore.getAll ();

    final List <Map <String, Object>> aResult = aDocs.stream ().map (this::_toMap).toList ();
    return ResponseEntity.ok (aResult);
  }

  @GetMapping ("/count")
  public ResponseEntity <Map <String, Integer>> count ()
  {
    return ResponseEntity.ok (Map.of ("total", Integer.valueOf (m_aDocumentStore.getCount ())));
  }

  @GetMapping ("/{id}")
  public ResponseEntity <Map <String, Object>> getByID (@PathVariable final String id)
  {
    final ReceivedDocument aDoc = m_aDocumentStore.getByID (id);
    if (aDoc == null)
      return ResponseEntity.notFound ().build ();

    return ResponseEntity.ok (_toMap (aDoc));
  }

  @GetMapping (value = "/{id}/content", produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity <byte []> getContent (@PathVariable final String id) throws IOException
  {
    final byte [] aContent = m_aDocumentStore.readContent (id);
    if (aContent == null)
      return ResponseEntity.notFound ().build ();

    return ResponseEntity.ok (aContent);
  }

  @DeleteMapping
  public ResponseEntity <Map <String, String>> clearAll ()
  {
    m_aDocumentStore.clear ();
    return ResponseEntity.ok (Map.of ("status", "cleared"));
  }

  private Map <String, Object> _toMap (final ReceivedDocument aDoc)
  {
    final Map <String, Object> aMap = new LinkedHashMap <> ();
    aMap.put ("id", aDoc.getID ());
    aMap.put ("channel", aDoc.getChannel ());
    aMap.put ("filename", aDoc.getFilename ());
    aMap.put ("sizeBytes", Long.valueOf (aDoc.getSizeBytes ()));
    aMap.put ("receivedDT", aDoc.getReceivedDT ().toString ());
    aMap.put ("storagePath", aDoc.getStoragePath ());
    aMap.put ("callbackSent", Boolean.valueOf (aDoc.isCallbackSent ()));
    return aMap;
  }
}
