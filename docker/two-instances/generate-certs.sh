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

#
# Creates a self-made CA and two AP certificates signed by it, so that two phoss AP instances can
# exchange messages locally WITHOUT official Peppol test certificates.
# For local development only - these certificates are never accepted in the Peppol network.
#
# Requires: openssl and keytool (part of every JDK)
#
# Output in ./certs:
#   dev-ca.pem          CA certificate  -> peppol.dev.trusted-ca.path
#   dev-truststore.p12  CA as trust store -> org.apache.wss4j.crypto.merlin.truststore.file
#   ap-a.p12 / ap-b.p12 AP key stores (alias "ap", password "peppol")
#   ap-a.pem / ap-b.pem AP certificates -> outbound.dev-fixed-endpoint.certificate-path of the other AP
#

set -e

cd "$(dirname "$0")"
mkdir -p certs
cd certs

PW=peppol
DAYS=730

# CA
openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days $DAYS \
  -keyout dev-ca.key -out dev-ca.pem \
  -subj "/C=XX/O=phoss AP local development/CN=phoss AP DEV CA" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,keyCertSign,cRLSign"

# One AP certificate per instance - the CN is the Peppol Seat ID
for AP in a:PXX000001 b:PXX000002; do
  NAME=${AP%%:*}
  SEAT=${AP##*:}
  openssl req -newkey rsa:2048 -nodes -sha256 \
    -keyout ap-$NAME.key -out ap-$NAME.csr \
    -subj "/C=XX/O=phoss AP local development/OU=PEPPOL TEST AP/CN=$SEAT"
  openssl x509 -req -sha256 -days $DAYS -in ap-$NAME.csr \
    -CA dev-ca.pem -CAkey dev-ca.key -CAcreateserial -out ap-$NAME.pem \
    -extfile <(printf "basicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment,dataEncipherment,keyAgreement\n")
  openssl pkcs12 -export -name ap -passout pass:$PW \
    -inkey ap-$NAME.key -in ap-$NAME.pem -certfile dev-ca.pem -out ap-$NAME.p12
  rm ap-$NAME.csr
done

# Trust store containing only the CA
rm -f dev-truststore.p12
keytool -importcert -noprompt -alias dev-ca -file dev-ca.pem \
  -keystore dev-truststore.p12 -storetype PKCS12 -storepass $PW

# Readable for the non-root user inside the container
chmod 644 ./*.p12 ./*.pem

echo "Created the following files in $(pwd):"
ls -1
