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

# Dockerfile for phoss-ap
# Downloads the pre-built JAR from a GitHub release - no build tools needed.

FROM eclipse-temurin:21-alpine

ARG VERSION

LABEL maintainer="Philip Helger <philip@helger.com>"
LABEL org.opencontainers.image.title="phoss-ap"
LABEL org.opencontainers.image.description="Open-source Peppol Access Point based on phase4"
LABEL org.opencontainers.image.url="https://github.com/phax/phoss-ap"
LABEL org.opencontainers.image.version="${VERSION}"

# Directory for external extension jars (custom DB drivers, SPI extensions).
# Any jar placed here is added to the classpath at runtime via LOADER_PATH below,
# without rebuilding the application jar. It is a plain directory (not a VOLUME) so it
# works both when bind-mounted from the host and when extensions are baked into a
# derived image via "COPY my-extension.jar /ext/". See phoss-ap-extension-demo.
#
# Both directories are group owned by GID 0 with the group permissions mirroring the
# owner permissions, so the image also runs as an arbitrary non-root UID - Kubernetes
# with a "restricted" PodSecurity profile (runAsNonRoot / runAsUser) and OpenShift both
# assign a random UID but keep GID 0. This must happen before the VOLUME instructions,
# because changes to a volume path made after its VOLUME line are discarded.
RUN mkdir -p /ext /var/phoss-ap/data && \
    chgrp -R 0 /ext /var/phoss-ap/data && \
    chmod -R g=u /ext /var/phoss-ap/data

VOLUME /tmp
VOLUME /var/phoss-ap/data

ENV LOADER_PATH=/ext

# Block the compromised eMudhra intermediate CA certificates (16 certificates, including the
# ones cross-signed by Sectigo AAA), see https://github.com/phax/phoss-ap/issues/115
# The hashes are appended to the JDK "blocked.certs" file. This is idempotent, so it stays
# harmless once the base image ships a JDK that already blocks them.
# The file must remain world-readable for arbitrary non-root UIDs.
COPY docker/emudhra-blocked-certs.txt /tmp/emudhra-blocked-certs.txt
RUN BLOCKED="$JAVA_HOME/lib/security/blocked.certs" && \
    grep -q 42C4ACCD12C4F59597925E1A08A7A90102608543809308D50EC427E118BA1493 "$BLOCKED" || \
      { echo >> "$BLOCKED" && cat /tmp/emudhra-blocked-certs.txt >> "$BLOCKED"; } && \
    rm /tmp/emudhra-blocked-certs.txt

# Store all runtime data below the /var/phoss-ap/data volume declared above. Without
# this, the "global.datapath=generated/" default of the bundled application.properties
# is resolved relative to the working directory "/", so documents and AS4 dumps end up
# in the ephemeral /generated - unwritable for a non-root UID, and lost on container
# removal even for root.
ENV GLOBAL_DATAPATH=/var/phoss-ap/data/

# "--chmod" requires BuildKit (default since Docker 23; the release build uses buildx).
# Without it, files added from a remote URL get mode 0600 root:root, which a non-root
# UID cannot read - see https://github.com/phax/phoss-ap/issues/79
ADD --chmod=0444 https://github.com/phax/phoss-ap/releases/download/phoss-ap-parent-pom-${VERSION}/phoss-ap-webapp-${VERSION}.jar /app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Disable the HotSpot Base64 intrinsic as mitigation for CVE-2026-114938 ("Java2Shell")
ENTRYPOINT ["java", \
            "-XX:-UseBASE64Intrinsics", \
            "-Djava.security.egd=file:/dev/urandom", \
            "-XX:InitialRAMPercentage=10", \
            "-XX:MinRAMPercentage=50", \
            "-XX:MaxRAMPercentage=80", \
            "-jar", "/app.jar"]
