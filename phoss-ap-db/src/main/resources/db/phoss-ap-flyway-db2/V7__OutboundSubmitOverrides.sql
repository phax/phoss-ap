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

-- Per-transmission overrides of an outbound transaction (issue #134 and PR #135)
-- "mls_type" is the MLS_TYPE requested for this transaction. NULL means that the configured
-- "mls.type" applies at sending time. For a pre-built SBD it holds the MLS_TYPE of the SBDH.
-- "requested_as4_message_id" is the AS4 message ID requested by the submitter. It is only used for
-- the first sending attempt; NULL means a random one is created.
-- Existing rows default to NULL. Mirrored into the archive table.
-- The column order must stay identical to the archive table, because archival copies rows with
-- "INSERT INTO outbound_transaction_archive SELECT * FROM outbound_transaction".
-- The indices support the check that a requested AS4 message ID was not used before.

-- DB2 puts a table into REORG-pending state after ALTER TABLE ... ADD COLUMN,
-- so a REORG is required before the table is fully usable (and before exceeding
-- DB2's limit of 3 pending ALTERs per table).
ALTER TABLE outbound_transaction         ADD COLUMN mls_type VARCHAR(50);
ALTER TABLE outbound_transaction         ADD COLUMN requested_as4_message_id VARCHAR(255);
CALL SYSPROC.ADMIN_CMD('REORG TABLE outbound_transaction');

ALTER TABLE outbound_transaction_archive ADD COLUMN mls_type VARCHAR(50);
ALTER TABLE outbound_transaction_archive ADD COLUMN requested_as4_message_id VARCHAR(255);
CALL SYSPROC.ADMIN_CMD('REORG TABLE outbound_transaction_archive');

CREATE INDEX idx_outbound_req_as4_msg_id    ON outbound_transaction (requested_as4_message_id);
CREATE INDEX idx_outbnd_arch_req_as4_msg_id ON outbound_transaction_archive (requested_as4_message_id);
CREATE INDEX idx_outbnd_att_arch_as4_msg_id ON outbound_sending_attempt_archive (as4_message_id);
