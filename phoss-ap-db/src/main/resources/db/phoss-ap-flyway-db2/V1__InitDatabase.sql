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

-- phoss-ap initial database schema for DB2

-- Outbound transactions
CREATE TABLE outbound_transaction (
  id                          VARCHAR(36)   NOT NULL,
  transaction_type            VARCHAR(255)  NOT NULL,
  sender_id                   VARCHAR(255)  NOT NULL,
  receiver_id                 VARCHAR(255)  NOT NULL,
  doc_type_id                 VARCHAR(500)  NOT NULL,
  process_id                  VARCHAR(500)  NOT NULL,
  sbdh_instance_id            VARCHAR(255)  NOT NULL,
  source_type                 VARCHAR(255)  NOT NULL,
  document_path               VARCHAR(2000) NOT NULL,
  document_size               BIGINT        NOT NULL,
  document_hash               CHAR(64)      NOT NULL,
  c1_country_code             CHAR(2)       NOT NULL,
  status                      VARCHAR(50)   NOT NULL,
  attempt_count               INT           NOT NULL DEFAULT 0,
  created_dt                  TIMESTAMP     NOT NULL,
  completed_dt                TIMESTAMP,
  reporting_status            VARCHAR(50)   NOT NULL,
  next_retry_dt               TIMESTAMP,
  error_details               CLOB,
  mls_to                      VARCHAR(255),
  mls_status                  VARCHAR(255),
  mls_received_dt             TIMESTAMP,
  mls_id                      VARCHAR(255),
  mls_inbound_transaction_id  VARCHAR(255),
  sbdh_standard               VARCHAR(255),
  sbdh_type_version           VARCHAR(255),
  sbdh_type                   VARCHAR(255),
  payload_mime_type           VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE INDEX idx_outbound_status_retry ON outbound_transaction (status, next_retry_dt);
CREATE INDEX idx_outbound_status ON outbound_transaction (status);
CREATE INDEX idx_outbound_sbdh_instance_id ON outbound_transaction (sbdh_instance_id);

-- Outbound sending attempts
CREATE TABLE outbound_sending_attempt (
  id                       VARCHAR(36)   NOT NULL,
  outbound_transaction_id  VARCHAR(36)   NOT NULL,
  as4_message_id           VARCHAR(255)  NOT NULL,
  as4_timestamp            TIMESTAMP     NOT NULL,
  receipt_message_id       VARCHAR(255),
  http_status_code         INT,
  attempt_dt               TIMESTAMP     NOT NULL,
  attempt_status           VARCHAR(50)   NOT NULL,
  error_details            CLOB,
  sending_report           CLOB,
  PRIMARY KEY (id),
  UNIQUE (as4_message_id),
  CONSTRAINT fk_outbnd_send_attempt_tx FOREIGN KEY (outbound_transaction_id)
    REFERENCES outbound_transaction (id) ON DELETE CASCADE
);

CREATE INDEX idx_outbound_attempt_tx ON outbound_sending_attempt (outbound_transaction_id);

-- Inbound transactions
CREATE TABLE inbound_transaction (
  id                            VARCHAR(36)   NOT NULL,
  incoming_id                   VARCHAR(255)  NOT NULL,
  c2_seat_id                    VARCHAR(255)  NOT NULL,
  c3_seat_id                    VARCHAR(255)  NOT NULL,
  signing_cert_cn               VARCHAR(255)  NOT NULL,
  sender_id                     VARCHAR(255)  NOT NULL,
  receiver_id                   VARCHAR(255)  NOT NULL,
  doc_type_id                   VARCHAR(500)  NOT NULL,
  process_id                    VARCHAR(500)  NOT NULL,
  document_path                 VARCHAR(2000) NOT NULL,
  document_size                 BIGINT        NOT NULL,
  document_hash                 CHAR(64)      NOT NULL,
  as4_message_id                VARCHAR(255)  NOT NULL,
  as4_timestamp                 TIMESTAMP     NOT NULL,
  sbdh_instance_id              VARCHAR(255)  NOT NULL,
  c1_country_code               CHAR(2),
  c4_country_code               CHAR(2),
  is_duplicate_as4              SMALLINT      NOT NULL DEFAULT 0,
  is_duplicate_sbdh             SMALLINT      NOT NULL DEFAULT 0,
  status                        VARCHAR(50)   NOT NULL,
  attempt_count                 INT           NOT NULL DEFAULT 0,
  received_dt                   TIMESTAMP     NOT NULL,
  completed_dt                  TIMESTAMP,
  reporting_status              VARCHAR(50)   NOT NULL DEFAULT 'pending',
  next_retry_dt                 TIMESTAMP,
  error_details                 CLOB,
  mls_to                        VARCHAR(255),
  mls_type                      VARCHAR(50)   NOT NULL,
  mls_response_code             VARCHAR(255),
  mls_outbound_transaction_id   VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE INDEX idx_inbound_status_retry ON inbound_transaction (status, next_retry_dt);
CREATE INDEX idx_inbound_status ON inbound_transaction (status);
CREATE INDEX idx_inbound_as4_message_id ON inbound_transaction (as4_message_id);
CREATE INDEX idx_inbound_sbdh_instance_id ON inbound_transaction (sbdh_instance_id);

-- Inbound forwarding attempts
CREATE TABLE inbound_forwarding_attempt (
  id                      VARCHAR(36)   NOT NULL,
  inbound_transaction_id  VARCHAR(36)   NOT NULL,
  attempt_dt              TIMESTAMP     NOT NULL,
  attempt_status          VARCHAR(50)   NOT NULL,
  error_code              VARCHAR(255),
  error_details           CLOB,
  PRIMARY KEY (id),
  CONSTRAINT fk_inbnd_fwd_attempt_tx FOREIGN KEY (inbound_transaction_id)
    REFERENCES inbound_transaction (id) ON DELETE CASCADE
);

CREATE INDEX idx_inbound_attempt_tx ON inbound_forwarding_attempt (inbound_transaction_id);

-- Archive tables (identical structure, no foreign key constraints)
CREATE TABLE outbound_transaction_archive (
  id                          VARCHAR(36)   NOT NULL,
  transaction_type            VARCHAR(255)  NOT NULL,
  sender_id                   VARCHAR(255)  NOT NULL,
  receiver_id                 VARCHAR(255)  NOT NULL,
  doc_type_id                 VARCHAR(500)  NOT NULL,
  process_id                  VARCHAR(500)  NOT NULL,
  sbdh_instance_id            VARCHAR(255)  NOT NULL,
  source_type                 VARCHAR(255)  NOT NULL,
  document_path               VARCHAR(2000) NOT NULL,
  document_size               BIGINT        NOT NULL,
  document_hash               CHAR(64)      NOT NULL,
  c1_country_code             CHAR(2)       NOT NULL,
  status                      VARCHAR(50)   NOT NULL,
  attempt_count               INT           NOT NULL DEFAULT 0,
  created_dt                  TIMESTAMP     NOT NULL,
  completed_dt                TIMESTAMP,
  reporting_status            VARCHAR(50)   NOT NULL DEFAULT 'pending',
  next_retry_dt               TIMESTAMP,
  error_details               CLOB,
  mls_to                      VARCHAR(255),
  mls_status                  VARCHAR(255),
  mls_received_dt             TIMESTAMP,
  mls_id                      VARCHAR(255),
  mls_inbound_transaction_id  VARCHAR(255),
  sbdh_standard               VARCHAR(255),
  sbdh_type_version           VARCHAR(255),
  sbdh_type                   VARCHAR(255),
  payload_mime_type           VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE outbound_sending_attempt_archive (
  id                       VARCHAR(36)   NOT NULL,
  outbound_transaction_id  VARCHAR(36)   NOT NULL,
  as4_message_id           VARCHAR(255)  NOT NULL,
  as4_timestamp            TIMESTAMP     NOT NULL,
  receipt_message_id       VARCHAR(255),
  http_status_code         INT,
  attempt_dt               TIMESTAMP     NOT NULL,
  attempt_status           VARCHAR(50)   NOT NULL,
  error_details            CLOB,
  sending_report           CLOB,
  PRIMARY KEY (id)
);

CREATE TABLE inbound_transaction_archive (
  id                            VARCHAR(36)   NOT NULL,
  incoming_id                   VARCHAR(255)  NOT NULL,
  c2_seat_id                    VARCHAR(255)  NOT NULL,
  c3_seat_id                    VARCHAR(255)  NOT NULL,
  signing_cert_cn               VARCHAR(255)  NOT NULL,
  sender_id                     VARCHAR(255)  NOT NULL,
  receiver_id                   VARCHAR(255)  NOT NULL,
  doc_type_id                   VARCHAR(500)  NOT NULL,
  process_id                    VARCHAR(500)  NOT NULL,
  document_path                 VARCHAR(2000) NOT NULL,
  document_size                 BIGINT        NOT NULL,
  document_hash                 CHAR(64)      NOT NULL,
  as4_message_id                VARCHAR(255)  NOT NULL,
  as4_timestamp                 TIMESTAMP     NOT NULL,
  sbdh_instance_id              VARCHAR(255)  NOT NULL,
  c1_country_code               CHAR(2),
  c4_country_code               CHAR(2),
  is_duplicate_as4              SMALLINT      NOT NULL DEFAULT 0,
  is_duplicate_sbdh             SMALLINT      NOT NULL DEFAULT 0,
  status                        VARCHAR(50)   NOT NULL,
  attempt_count                 INT           NOT NULL DEFAULT 0,
  received_dt                   TIMESTAMP     NOT NULL,
  completed_dt                  TIMESTAMP,
  reporting_status              VARCHAR(50)   NOT NULL DEFAULT 'pending',
  next_retry_dt                 TIMESTAMP,
  error_details                 CLOB,
  mls_to                        VARCHAR(255),
  mls_type                      VARCHAR(50)   NOT NULL,
  mls_response_code             VARCHAR(255),
  mls_outbound_transaction_id   VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE inbound_forwarding_attempt_archive (
  id                      VARCHAR(36)   NOT NULL,
  inbound_transaction_id  VARCHAR(36)   NOT NULL,
  attempt_dt              TIMESTAMP     NOT NULL,
  attempt_status          VARCHAR(50)   NOT NULL,
  error_code              VARCHAR(255),
  error_details           CLOB,
  PRIMARY KEY (id)
);
