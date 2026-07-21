#!/bin/sh
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


export PHOSSAP_JDBC_URL=jdbc:postgresql://localhost:5432/phoss-ap
export PHOSSAP_JDBC_USER=peppol
export PHOSSAP_JDBC_PASSWORD=peppol
export PEPPOL_OWNER_SEATID=POP000306
export ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_FILE=/Users/philip/dev/git/phoss-ap/phoss-ap-webapp/src/main/resources/test-ap-2025-g3.p12
export ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_PASSWORD=peppol
export ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_ALIAS=private_key_for_pkcs12_certificate
export ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_PRIVATE_PASSWORD=peppol

export LOADER_PATH=../phoss-ap-extension-demo/target/

java -jar target/phoss-ap-webapp-*-SNAPSHOT.jar 
