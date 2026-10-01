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
  performed_dt        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  details             TEXT,
  CONSTRAINT pk_transaction_audit_log PRIMARY KEY (id)
);

CREATE INDEX idx_tx_audit_sbdh ON transaction_audit_log (sbdh_instance_id, performed_dt);
CREATE INDEX idx_tx_audit_tx_id ON transaction_audit_log (transaction_id);

CREATE OR REPLACE FUNCTION trg_fn_transaction_status_audit()
RETURNS TRIGGER AS $$
BEGIN
  IF (OLD.status IS DISTINCT FROM NEW.status OR OLD.attempt_count IS DISTINCT FROM NEW.attempt_count) THEN
    INSERT INTO transaction_audit_log (
      transaction_id, sbdh_instance_id, direction, event_type, action,
      from_status, to_status, attempt_count, performed_by, performed_dt, details
    ) VALUES (
      NEW.id,
      NEW.sbdh_instance_id,
      TG_ARGV[0],
      'STATUS_CHANGE',
      'STATUS_UPDATE',
      OLD.status,
      NEW.status,
      NEW.attempt_count,
      'SYSTEM',
      CURRENT_TIMESTAMP,
      NEW.error_details
    );
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_inbound_status_audit
AFTER UPDATE ON inbound_transaction
FOR EACH ROW
EXECUTE FUNCTION trg_fn_transaction_status_audit('INBOUND');

CREATE TRIGGER trg_outbound_status_audit
AFTER UPDATE ON outbound_transaction
FOR EACH ROW
EXECUTE FUNCTION trg_fn_transaction_status_audit('OUTBOUND');
