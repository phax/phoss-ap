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

-- Cleanup of archived transactions (issue #20)
-- Indexes on completed_dt to support the cleanup scheduler's range query.

CREATE INDEX idx_outbnd_archive_completed_dt ON outbound_transaction_archive (completed_dt);
CREATE INDEX idx_inbnd_archive_completed_dt  ON inbound_transaction_archive  (completed_dt);
