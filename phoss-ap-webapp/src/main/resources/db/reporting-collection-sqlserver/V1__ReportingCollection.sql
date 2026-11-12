--
-- Copyright (C) 2026 Philip Helger (www.helger.com)
-- philip[at]helger[dot]com
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--         http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE TABLE peppol_reporting_collection (
  source_id varchar(128) NOT NULL,
  report_period varchar(7) NOT NULL,
  state varchar(16) NOT NULL,
  lease_owner varchar(36),
  lease_until datetime2(3),
  record_count integer NOT NULL,
  revision integer NOT NULL,
  PRIMARY KEY (source_id, report_period)
);

CREATE TABLE peppol_reporting_collection_queue (
  source_id varchar(128) NOT NULL,
  report_period varchar(7) NOT NULL,
  item_index integer NOT NULL,
  payload_json nvarchar(max) NOT NULL,
  PRIMARY KEY (source_id, report_period, item_index),
  FOREIGN KEY (source_id, report_period) REFERENCES peppol_reporting_collection (source_id, report_period)
);
