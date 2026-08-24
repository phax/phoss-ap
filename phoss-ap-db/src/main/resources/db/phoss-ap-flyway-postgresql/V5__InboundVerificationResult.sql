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

-- Inbound verification verdict (issue #81)
-- The verdict of the inbound document verification, persisted separately from the transaction
-- status, so that it survives the forwarding state machine and the clearing of "error_details"
-- performed by "updateStatusCompleted". Existing rows default to NULL, meaning "not verified".
-- The column order must stay identical to the archive table, because archival copies rows with
-- "INSERT INTO inbound_transaction_archive SELECT * FROM inbound_transaction".

ALTER TABLE inbound_transaction         ADD COLUMN verification_result  VARCHAR(20);
ALTER TABLE inbound_transaction         ADD COLUMN verification_details TEXT;

ALTER TABLE inbound_transaction_archive ADD COLUMN verification_result  VARCHAR(20);
ALTER TABLE inbound_transaction_archive ADD COLUMN verification_details TEXT;
