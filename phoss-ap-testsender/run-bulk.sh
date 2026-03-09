#!/bin/bash
#
# Copyright (C) 2026 Philip Helger (www.helger.com)
# philip[at]helger[dot]com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Bulk send documents to the local phoss-ap instance for concurrency/race condition testing
# Defaults: 100 XML documents, 10 threads, no ramp-up
# Override via CLI args, e.g.:
#   ./run-bulk.sh --testsender.bulk.count=500 --testsender.bulk.threads=20
#   ./run-bulk.sh --testsender.bulk.document-types=xml,pdf --testsender.bulk.mix-ratio=70,30
java -jar target/phoss-ap-testsender-0.1.0-SNAPSHOT.jar \
  --testsender.bulk.enabled=true \
  "$@"
