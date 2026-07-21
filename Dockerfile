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

VOLUME /tmp
VOLUME /var/phoss-ap/data

# Directory for external extension jars (custom DB drivers, SPI extensions).
# Any jar placed here is added to the classpath at runtime via LOADER_PATH below,
# without rebuilding the application jar. It is a plain directory (not a VOLUME) so it
# works both when bind-mounted from the host and when extensions are baked into a
# derived image via "COPY my-extension.jar /ext/". See phoss-ap-extension-demo.
RUN mkdir -p /ext
ENV LOADER_PATH=/ext

ADD https://github.com/phax/phoss-ap/releases/download/phoss-ap-parent-pom-${VERSION}/phoss-ap-webapp-${VERSION}.jar /app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/urandom", \
            "-XX:InitialRAMPercentage=10", \
            "-XX:MinRAMPercentage=50", \
            "-XX:MaxRAMPercentage=80", \
            "-jar", "/app.jar"]
