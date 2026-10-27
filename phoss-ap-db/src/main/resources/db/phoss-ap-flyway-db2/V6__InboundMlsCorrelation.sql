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
-- Correlated outbound transaction of an inbound MLS (issues #120 and #121)
-- The ID of the outbound business document transaction that a received MLS was correlated with.
-- It is persisted on the inbound MLS, so that a forwarding retry still knows the correlation and a
-- later MLS for the same outbound document does not lose it. Existing rows default to NULL,
-- meaning "not correlated".
-- The column order must stay identical to the archive table, because archival copies rows with
-- "INSERT INTO inbound_transaction_archive SELECT * FROM inbound_transaction".

ALTER TABLE inbound_transaction         ADD COLUMN mls_correlated_outbound_transaction_id VARCHAR(255);
CALL SYSPROC.ADMIN_CMD('REORG TABLE inbound_transaction');

ALTER TABLE inbound_transaction_archive ADD COLUMN mls_correlated_outbound_transaction_id VARCHAR(255);
CALL SYSPROC.ADMIN_CMD('REORG TABLE inbound_transaction_archive');
