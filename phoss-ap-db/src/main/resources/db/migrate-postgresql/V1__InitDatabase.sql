--
-- Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

-- phoss-ap initial database schema for PostgreSQL

-- Outbound transactions
CREATE TABLE outbound_transaction (
  id                          TEXT        NOT NULL,
  transaction_type            TEXT        NOT NULL,
  sender_id                   TEXT        NOT NULL,
  receiver_id                 TEXT        NOT NULL,
  doc_type_id                 TEXT        NOT NULL,
  process_id                  TEXT        NOT NULL,
  sbdh_instance_id            TEXT        NOT NULL,
  source_type                 TEXT        NOT NULL,
  document_bytes              BYTEA       NOT NULL,
  document_size               BIGINT      NOT NULL,
  document_hash               CHAR(64)    NOT NULL,
  c1_country_code             CHAR(2)     NOT NULL,
  status                      TEXT        NOT NULL,
  attempt_count               INT         NOT NULL DEFAULT 0,
  created_dt                  TIMESTAMPTZ NOT NULL,
  completed_dt                TIMESTAMPTZ,
  reporting_status            TEXT        NOT NULL DEFAULT 'pending',
  next_retry_dt               TIMESTAMPTZ,
  error_details               TEXT,
  mls_to                      TEXT,
  mls_status                  TEXT,
  mls_received_dt             TIMESTAMPTZ,
  mls_id                      TEXT,
  mls_inbound_transaction_id  TEXT,
  CONSTRAINT pk_outbound_transaction PRIMARY KEY (id),
  CONSTRAINT uq_outbound_sbdh_instance_id UNIQUE (sbdh_instance_id)
);

CREATE INDEX idx_outbound_status_retry ON outbound_transaction (status, next_retry_dt);
CREATE INDEX idx_outbound_status ON outbound_transaction (status);

-- Outbound sending attempts
CREATE TABLE outbound_sending_attempt (
  id                       TEXT        NOT NULL,
  outbound_transaction_id  TEXT        NOT NULL,
  as4_message_id           TEXT        NOT NULL,
  as4_timestamp            TIMESTAMPTZ NOT NULL,
  receipt_message_id       TEXT,
  http_status_code         INT,
  attempt_dt               TIMESTAMPTZ NOT NULL,
  attempt_status           TEXT        NOT NULL,
  error_details            TEXT,
  CONSTRAINT pk_outbound_sending_attempt PRIMARY KEY (id),
  CONSTRAINT uq_outbound_as4_message_id UNIQUE (as4_message_id),
  CONSTRAINT fk_outbound_sending_attempt_tx FOREIGN KEY (outbound_transaction_id)
    REFERENCES outbound_transaction (id) ON DELETE CASCADE
);

CREATE INDEX idx_outbound_attempt_tx ON outbound_sending_attempt (outbound_transaction_id);

-- Inbound transactions
CREATE TABLE inbound_transaction (
  id                            TEXT        NOT NULL,
  incoming_id                   TEXT        NOT NULL,
  c2_seat_id                    TEXT        NOT NULL,
  c3_seat_id                    TEXT        NOT NULL,
  signing_cert_cn               TEXT        NOT NULL,
  sender_id                     TEXT        NOT NULL,
  receiver_id                   TEXT        NOT NULL,
  doc_type_id                   TEXT        NOT NULL,
  process_id                    TEXT        NOT NULL,
  document_bytes                BYTEA       NOT NULL,
  document_size                 BIGINT      NOT NULL,
  document_hash                 CHAR(64)    NOT NULL,
  as4_message_id                TEXT        NOT NULL,
  as4_timestamp                 TIMESTAMPTZ NOT NULL,
  sbdh_instance_id              TEXT        NOT NULL,
  c1_country_code               CHAR(2),
  c4_country_code               CHAR(2),
  is_duplicate_as4              BOOLEAN     NOT NULL DEFAULT FALSE,
  is_duplicate_sbdh             BOOLEAN     NOT NULL DEFAULT FALSE,
  status                        TEXT        NOT NULL,
  attempt_count                 INT         NOT NULL DEFAULT 0,
  received_dt                   TIMESTAMPTZ NOT NULL,
  completed_dt                  TIMESTAMPTZ,
  reporting_status              TEXT        NOT NULL DEFAULT 'pending',
  next_retry_dt                 TIMESTAMPTZ,
  error_details                 TEXT,
  mls_to                        TEXT,
  mls_type                      TEXT        NOT NULL,
  mls_response_code             TEXT,
  mls_outbound_transaction_id   TEXT,
  CONSTRAINT pk_inbound_transaction PRIMARY KEY (id),
  CONSTRAINT uq_inbound_as4_message_id UNIQUE (as4_message_id),
  CONSTRAINT uq_inbound_sbdh_instance_id UNIQUE (sbdh_instance_id)
);

CREATE INDEX idx_inbound_status_retry ON inbound_transaction (status, next_retry_dt);
CREATE INDEX idx_inbound_status ON inbound_transaction (status);

-- Inbound forwarding attempts
CREATE TABLE inbound_forwarding_attempt (
  id                      TEXT        NOT NULL,
  inbound_transaction_id  TEXT        NOT NULL,
  attempt_dt              TIMESTAMPTZ NOT NULL,
  attempt_status          TEXT        NOT NULL,
  error_code              TEXT,
  error_details           TEXT,
  CONSTRAINT pk_inbound_forwarding_attempt PRIMARY KEY (id),
  CONSTRAINT fk_inbound_forwarding_attempt_tx FOREIGN KEY (inbound_transaction_id)
    REFERENCES inbound_transaction (id) ON DELETE CASCADE
);

CREATE INDEX idx_inbound_attempt_tx ON inbound_forwarding_attempt (inbound_transaction_id);

-- Archive tables (identical structure, no unique constraints)
CREATE TABLE outbound_transaction_archive (
  id                          TEXT        NOT NULL,
  transaction_type            TEXT        NOT NULL,
  sender_id                   TEXT        NOT NULL,
  receiver_id                 TEXT        NOT NULL,
  doc_type_id                 TEXT        NOT NULL,
  process_id                  TEXT        NOT NULL,
  sbdh_instance_id            TEXT        NOT NULL,
  source_type               TEXT        NOT NULL,
  document_bytes              BYTEA       NOT NULL,
  document_size               BIGINT      NOT NULL,
  document_hash               CHAR(64)    NOT NULL,
  c1_country_code             CHAR(2)     NOT NULL,
  status                      TEXT        NOT NULL,
  attempt_count               INT         NOT NULL DEFAULT 0,
  created_dt                  TIMESTAMPTZ NOT NULL,
  completed_dt                TIMESTAMPTZ,
  reporting_status            TEXT        NOT NULL DEFAULT 'pending',
  next_retry_dt               TIMESTAMPTZ,
  error_details               TEXT,
  mls_to                      TEXT,
  mls_status                  TEXT,
  mls_received_dt             TIMESTAMPTZ,
  mls_id                      TEXT,
  mls_inbound_transaction_id  TEXT,
  CONSTRAINT pk_outbound_transaction_archive PRIMARY KEY (id)
);

CREATE TABLE outbound_sending_attempt_archive (
  id                       TEXT        NOT NULL,
  outbound_transaction_id  TEXT        NOT NULL,
  as4_message_id           TEXT        NOT NULL,
  as4_timestamp            TIMESTAMPTZ NOT NULL,
  receipt_message_id       TEXT,
  http_status_code         INT,
  attempt_dt               TIMESTAMPTZ NOT NULL,
  attempt_status           TEXT        NOT NULL,
  error_details            TEXT,
  CONSTRAINT pk_outbound_sending_attempt_archive PRIMARY KEY (id)
);

CREATE TABLE inbound_transaction_archive (
  id                            TEXT        NOT NULL,
  incoming_id                   TEXT        NOT NULL,
  c2_seat_id                    TEXT        NOT NULL,
  c3_seat_id                    TEXT        NOT NULL,
  signing_cert_cn               TEXT        NOT NULL,
  sender_id                     TEXT        NOT NULL,
  receiver_id                   TEXT        NOT NULL,
  doc_type_id                   TEXT        NOT NULL,
  process_id                    TEXT        NOT NULL,
  document_bytes                BYTEA       NOT NULL,
  document_size                 BIGINT      NOT NULL,
  document_hash                 CHAR(64)    NOT NULL,
  as4_message_id                TEXT        NOT NULL,
  as4_timestamp                 TIMESTAMPTZ NOT NULL,
  sbdh_instance_id              TEXT        NOT NULL,
  c1_country_code               CHAR(2),
  c4_country_code               CHAR(2),
  is_duplicate_as4              BOOLEAN     NOT NULL DEFAULT FALSE,
  is_duplicate_sbdh             BOOLEAN     NOT NULL DEFAULT FALSE,
  status                        TEXT        NOT NULL,
  attempt_count                 INT         NOT NULL DEFAULT 0,
  received_dt                   TIMESTAMPTZ NOT NULL,
  completed_dt                  TIMESTAMPTZ,
  reporting_status              TEXT        NOT NULL DEFAULT 'pending',
  next_retry_dt                 TIMESTAMPTZ,
  error_details                 TEXT,
  mls_to                        TEXT,
  mls_type                      TEXT        NOT NULL,
  mls_response_code             TEXT,
  mls_outbound_transaction_id   TEXT,
  CONSTRAINT pk_inbound_transaction_archive PRIMARY KEY (id)
);

CREATE TABLE inbound_forwarding_attempt_archive (
  id                      TEXT        NOT NULL,
  inbound_transaction_id  TEXT        NOT NULL,
  attempt_dt              TIMESTAMPTZ NOT NULL,
  attempt_status          TEXT        NOT NULL,
  error_code              TEXT,
  error_details           TEXT,
  CONSTRAINT pk_inbound_forwarding_attempt_archive PRIMARY KEY (id)
);
