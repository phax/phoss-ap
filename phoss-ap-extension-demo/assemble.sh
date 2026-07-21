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
# Build the phoss-ap-extension-demo SPI extension and assemble a phoss-ap Docker
# image with it baked into /ext.
#
# Usage:
#   ./assemble.sh                 build the jar + the Docker image "phoss-ap-with-demo-ext"
#   ./assemble.sh --jar-only      only build the thin extension jar (no Docker)
#   PHOSS_AP_IMAGE=phelger/phoss-ap:0.10.4 ./assemble.sh
#                                 base the image on a specific phoss-ap tag
#

set -euo pipefail

# Resolve directories independent of the caller's working directory.
MODULE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${MODULE_DIR}/.." && pwd)"

# Default image names by OS: the arm64 variants on macOS (Apple Silicon), the amd64
# variants elsewhere. Override IMAGE_NAME / PHOSS_AP_IMAGE explicitly to bypass.
if [ "$(uname -s)" = "Darwin" ]; then
  IMAGE_NAME="${IMAGE_NAME:-phelger/phoss-ap-with-demo-ext-arm64}"
  PHOSS_AP_IMAGE="${PHOSS_AP_IMAGE:-phelger/phoss-ap-arm64:latest}"
else
  IMAGE_NAME="${IMAGE_NAME:-phelger/phoss-ap-with-demo-ext}"
  PHOSS_AP_IMAGE="${PHOSS_AP_IMAGE:-phelger/phoss-ap:latest}"
fi
JAR_ONLY=false
[ "${1:-}" = "--jar-only" ] && JAR_ONLY=true

# 1. Build the thin extension jar. Run from the repo root and use "-am" so the
#    phoss-ap-api dependency is (re)built if it is not yet in the local repository.
echo "==> Building phoss-ap-extension-demo jar"
mvn -f "${REPO_ROOT}/pom.xml" -pl phoss-ap-extension-demo -am -DskipTests clean package

JAR="$(ls "${MODULE_DIR}"/target/phoss-ap-extension-demo-*.jar | grep -v -e '-sources' -e '-javadoc' | head -n 1)"
echo "==> Built extension jar: ${JAR}"

if [ "${JAR_ONLY}" = true ]; then
  echo "==> --jar-only given; drop the jar above into your phoss-ap /ext directory and restart."
  exit 0
fi

# 2. Assemble the Docker image with the extension baked into /ext.
echo "==> Building Docker image '${IMAGE_NAME}' on top of '${PHOSS_AP_IMAGE}'"
docker build \
  --build-arg PHOSS_AP_IMAGE="${PHOSS_AP_IMAGE}" \
  -t "${IMAGE_NAME}" \
  "${MODULE_DIR}"

echo "==> Done. Run it with e.g.:"
echo "    docker run --rm -p 8080:8080 \\"
echo "      -e PHOSSAP_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/phoss-ap \\"
echo "      -e PHOSSAP_JDBC_USER=peppol -e PHOSSAP_JDBC_PASSWORD=peppol \\"
echo "      ${IMAGE_NAME}"
echo "    Then watch the log for lines starting with '[extension-demo]'."
