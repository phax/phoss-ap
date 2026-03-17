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
package com.helger.phoss.ap.testsender.report;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phoss.ap.testsender.sender.BulkSendResult;
import com.helger.phoss.ap.testsender.sender.SendResult;

/**
 * Reports send results to console and optionally to a JSON file.
 */
@Component
public class ResultReporter
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ResultReporter.class);

  /**
   * Report a single send result to the console log.
   *
   * @param aResult
   *        the send result to report. May not be {@code null}.
   */
  public void reportSingle (@NonNull final SendResult aResult)
  {
    LOGGER.info ("--- Single Send Result ---");
    LOGGER.info ("Type:     " + aResult.getDocumentType ());
    LOGGER.info ("SBDH ID:  " + aResult.getSbdhInstanceID ());
    LOGGER.info ("Success:  " + aResult.isSuccess ());
    LOGGER.info ("HTTP:     " + aResult.getHttpStatus ());
    LOGGER.info ("Duration: " + aResult.getDurationMs () + " ms");
    if (aResult.isFailure ())
      LOGGER.info ("Error:    " + aResult.getErrorMessage ());
  }

  /**
   * Report an aggregated bulk send result to the console log.
   *
   * @param aResult
   *        the bulk send result to report. May not be {@code null}.
   */
  public void reportBulk (@NonNull final BulkSendResult aResult)
  {
    LOGGER.info ("--- Bulk Send Result ---");
    LOGGER.info ("Total:       " + aResult.getTotalCount ());
    LOGGER.info ("Succeeded:   " + aResult.getSuccessCount ());
    LOGGER.info ("Failed:      " + aResult.getFailureCount ());
    LOGGER.info ("Duration:    " + aResult.getOverallDurationMs () + " ms");
    LOGGER.info ("Throughput:  " + aResult.getThroughputPerSecond () + "/s");
    LOGGER.info ("Latency min: " + aResult.getMinDurationMs () + " ms");
    LOGGER.info ("Latency avg: " + aResult.getAvgDurationMs () + " ms");
    LOGGER.info ("Latency p95: " + aResult.getP95DurationMs () + " ms");
    LOGGER.info ("Latency max: " + aResult.getMaxDurationMs () + " ms");

    final Map <String, Long> aByType = aResult.getCountByDocumentType ();
    if (aByType.size () > 1)
    {
      LOGGER.info ("By type:");
      aByType.forEach ( (sType, nCount) -> LOGGER.info ("  " + sType + ": " + nCount));
    }

    final Map <String, Long> aErrors = aResult.getErrorBreakdown ();
    if (!aErrors.isEmpty ())
    {
      LOGGER.info ("Error breakdown:");
      aErrors.forEach ( (sErr, nCount) -> LOGGER.info ("  " + nCount + " x " + sErr));
    }
  }

  /**
   * Write the bulk send result to a JSON file. Does nothing if
   * {@code sOutputFile} is {@code null} or blank.
   *
   * @param aResult
   *        the bulk send result to serialize. May not be {@code null}.
   * @param sOutputFile
   *        the output file path, or {@code null} to skip writing.
   */
  public void writeJsonFile (@NonNull final BulkSendResult aResult, @Nullable final String sOutputFile)
  {
    if (sOutputFile == null || sOutputFile.isBlank ())
      return;

    final IJsonObject aJson = new JsonObject ();
    aJson.add ("totalCount", aResult.getTotalCount ());
    aJson.add ("successCount", aResult.getSuccessCount ());
    aJson.add ("failureCount", aResult.getFailureCount ());
    aJson.add ("overallDurationMs", aResult.getOverallDurationMs ());
    aJson.add ("throughputPerSecond", aResult.getThroughputPerSecond ());
    aJson.add ("latencyMinMs", aResult.getMinDurationMs ());
    aJson.add ("latencyAvgMs", aResult.getAvgDurationMs ());
    aJson.add ("latencyP95Ms", aResult.getP95DurationMs ());
    aJson.add ("latencyMaxMs", aResult.getMaxDurationMs ());

    final IJsonObject aByType = new JsonObject ();
    aResult.getCountByDocumentType ().forEach (aByType::add);
    aJson.add ("countByType", aByType);

    final IJsonObject aErrors = new JsonObject ();
    aResult.getErrorBreakdown ().forEach (aErrors::add);
    aJson.add ("errorBreakdown", aErrors);

    final JsonArray aDetails = new JsonArray ();
    for (final SendResult r : aResult.getAllResults ())
    {
      final IJsonObject aDetail = new JsonObject ();
      aDetail.add ("type", r.getDocumentType ());
      aDetail.add ("sbdhInstanceID", r.getSbdhInstanceID ());
      aDetail.add ("success", r.isSuccess ());
      aDetail.add ("httpStatus", r.getHttpStatus ());
      aDetail.add ("durationMs", r.getDurationMs ());
      if (r.getErrorMessage () != null)
        aDetail.add ("error", r.getErrorMessage ());
      aDetails.add (aDetail);
    }
    aJson.add ("results", aDetails);

    try (final Writer aWriter = Files.newBufferedWriter (Path.of (sOutputFile), StandardCharsets.UTF_8))
    {
      new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeToWriter (aJson, aWriter);
      LOGGER.info ("Results written to " + sOutputFile);
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to write results to '" + sOutputFile + "'", ex);
    }
  }
}
