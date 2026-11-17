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

-- Per-transmission overrides of an outbound transaction (issue #134 and PR #135), and the
-- microsecond precision of all date time columns (MySQL only)
-- "mls_type" is the MLS_TYPE requested for this transaction. NULL means that the configured
-- "mls.type" applies at sending time. For a pre-built SBD it holds the MLS_TYPE of the SBDH.
-- "requested_as4_message_id" is the AS4 message ID requested by the submitter. It is only used for
-- the first sending attempt; NULL means a random one is created.
-- Existing rows default to NULL. Mirrored into the archive table.
-- The column order must stay identical to the archive table, because archival copies rows with
-- "INSERT INTO outbound_transaction_archive SELECT * FROM outbound_transaction".
-- The indices support the check that a requested AS4 message ID was not used before.

ALTER TABLE `outbound_transaction`         ADD COLUMN `mls_type` VARCHAR(50);
ALTER TABLE `outbound_transaction`         ADD COLUMN `requested_as4_message_id` VARCHAR(255);

ALTER TABLE `outbound_transaction_archive` ADD COLUMN `mls_type` VARCHAR(50);
ALTER TABLE `outbound_transaction_archive` ADD COLUMN `requested_as4_message_id` VARCHAR(255);

CREATE INDEX idx_outbound_req_as4_msg_id    ON `outbound_transaction` (`requested_as4_message_id`);
CREATE INDEX idx_outbnd_arch_req_as4_msg_id ON `outbound_transaction_archive` (`requested_as4_message_id`);
CREATE INDEX idx_outbnd_att_arch_as4_msg_id ON `outbound_sending_attempt_archive` (`as4_message_id`);

-- MySQL only: DATETIME has a precision of whole seconds by default and rounds the fractional part.
-- Sending and forwarding attempts made within the same second could therefore not be ordered, and
-- all other databases store microseconds. Each table is altered with a single statement, because
-- MySQL rebuilds the table for this change - this may take a while for large archive tables.
ALTER TABLE `outbound_transaction`
  MODIFY COLUMN `created_dt` DATETIME(6) NOT NULL,
  MODIFY COLUMN `completed_dt` DATETIME(6),
  MODIFY COLUMN `next_retry_dt` DATETIME(6),
  MODIFY COLUMN `mls_received_dt` DATETIME(6);
ALTER TABLE `outbound_transaction_archive`
  MODIFY COLUMN `created_dt` DATETIME(6) NOT NULL,
  MODIFY COLUMN `completed_dt` DATETIME(6),
  MODIFY COLUMN `next_retry_dt` DATETIME(6),
  MODIFY COLUMN `mls_received_dt` DATETIME(6);
ALTER TABLE `outbound_sending_attempt`
  MODIFY COLUMN `as4_timestamp` DATETIME(6) NOT NULL,
  MODIFY COLUMN `attempt_dt` DATETIME(6) NOT NULL;
ALTER TABLE `outbound_sending_attempt_archive`
  MODIFY COLUMN `as4_timestamp` DATETIME(6) NOT NULL,
  MODIFY COLUMN `attempt_dt` DATETIME(6) NOT NULL;
ALTER TABLE `inbound_transaction`
  MODIFY COLUMN `as4_timestamp` DATETIME(6) NOT NULL,
  MODIFY COLUMN `received_dt` DATETIME(6) NOT NULL,
  MODIFY COLUMN `completed_dt` DATETIME(6),
  MODIFY COLUMN `next_retry_dt` DATETIME(6);
ALTER TABLE `inbound_transaction_archive`
  MODIFY COLUMN `as4_timestamp` DATETIME(6) NOT NULL,
  MODIFY COLUMN `received_dt` DATETIME(6) NOT NULL,
  MODIFY COLUMN `completed_dt` DATETIME(6),
  MODIFY COLUMN `next_retry_dt` DATETIME(6);
ALTER TABLE `inbound_forwarding_attempt`
  MODIFY COLUMN `attempt_dt` DATETIME(6) NOT NULL;
ALTER TABLE `inbound_forwarding_attempt_archive`
  MODIFY COLUMN `attempt_dt` DATETIME(6) NOT NULL;
ALTER TABLE `as4_duplicate_item`
  MODIFY COLUMN `created_dt` DATETIME(6) NOT NULL;
