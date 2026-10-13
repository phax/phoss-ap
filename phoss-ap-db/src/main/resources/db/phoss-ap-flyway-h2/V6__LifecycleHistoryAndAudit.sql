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

-- Transaction lifecycle history and operator audit log

CREATE TABLE transaction_audit_log (
  id                  BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
  transaction_id      VARCHAR(64),
  sbdh_instance_id    VARCHAR(255)  NOT NULL,
  direction           VARCHAR(10)   NOT NULL,
  event_type          VARCHAR(32)   NOT NULL,
  action              VARCHAR(64)   NOT NULL,
  from_status         VARCHAR(50),
  to_status           VARCHAR(50),
  attempt_count       INT,
  performed_by        VARCHAR(256)  NOT NULL DEFAULT 'SYSTEM',
  performed_dt        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  details             TEXT,
  CONSTRAINT pk_transaction_audit_log PRIMARY KEY (id)
);

CREATE INDEX idx_tx_audit_sbdh ON transaction_audit_log (sbdh_instance_id, performed_dt);
CREATE INDEX idx_tx_audit_tx_id ON transaction_audit_log (transaction_id);

-- Trigger for inbound transaction status updates
CREATE TRIGGER trg_inbound_status_audit
AFTER UPDATE ON inbound_transaction
FOR EACH ROW
CALL "com.helger.phoss.ap.db.trigger.H2StatusAuditTrigger";

-- Trigger for outbound transaction status updates
CREATE TRIGGER trg_outbound_status_audit
AFTER UPDATE ON outbound_transaction
FOR EACH ROW
CALL "com.helger.phoss.ap.db.trigger.H2StatusAuditTrigger";
