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
  id                  BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
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
  details             CLOB(64K),
  CONSTRAINT pk_transaction_audit_log PRIMARY KEY (id)
);

CREATE INDEX idx_tx_audit_sbdh ON transaction_audit_log (sbdh_instance_id, performed_dt);
CREATE INDEX idx_tx_audit_tx_id ON transaction_audit_log (transaction_id);

-- Trigger for inbound transaction status updates
CREATE TRIGGER trg_inbound_status_audit
AFTER UPDATE ON inbound_transaction
REFERENCING OLD AS o NEW AS n
FOR EACH ROW
WHEN (o.status IS DISTINCT FROM n.status OR o.attempt_count IS DISTINCT FROM n.attempt_count)
INSERT INTO transaction_audit_log (
  transaction_id, sbdh_instance_id, direction, event_type, action,
  from_status, to_status, attempt_count, performed_by, performed_dt, details
) VALUES (
  n.id, n.sbdh_instance_id, 'INBOUND', 'STATUS_CHANGE', 'STATUS_UPDATE',
  o.status, n.status, n.attempt_count, 'SYSTEM', CURRENT_TIMESTAMP, n.error_details
);

-- Trigger for outbound transaction status updates
CREATE TRIGGER trg_outbound_status_audit
AFTER UPDATE ON outbound_transaction
REFERENCING OLD AS o NEW AS n
FOR EACH ROW
WHEN (o.status IS DISTINCT FROM n.status OR o.attempt_count IS DISTINCT FROM n.attempt_count)
INSERT INTO transaction_audit_log (
  transaction_id, sbdh_instance_id, direction, event_type, action,
  from_status, to_status, attempt_count, performed_by, performed_dt, details
) VALUES (
  n.id, n.sbdh_instance_id, 'OUTBOUND', 'STATUS_CHANGE', 'STATUS_UPDATE',
  o.status, n.status, n.attempt_count, 'SYSTEM', CURRENT_TIMESTAMP, n.error_details
);
