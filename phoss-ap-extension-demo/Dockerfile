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

# Example Dockerfile that bakes the phoss-ap-extension-demo SPI extension into a
# phoss-ap image. It layers on top of an existing phoss-ap image and drops the thin
# extension jar into /ext, which the base image already puts on the classpath via
# LOADER_PATH=/ext (see the top-level Dockerfile).
#
# Build (from this module directory, after "mvn package"):
#   docker build -t phoss-ap-with-demo-ext .
#
# Or override the base image:
#   docker build --build-arg PHOSS_AP_IMAGE=phelger/phoss-ap:0.10.4 -t phoss-ap-with-demo-ext .
#
# The assemble.sh script in this directory performs both the Maven build and this
# Docker build in one step.

ARG PHOSS_AP_IMAGE=phelger/phoss-ap:latest
FROM ${PHOSS_AP_IMAGE}

LABEL org.opencontainers.image.title="phoss-ap-with-demo-ext"
LABEL org.opencontainers.image.description="phoss-ap with the SPI extension demo baked in"

# Put the /ext extension directory on the runtime classpath. The base phoss-ap image
# already sets this, but declaring it here keeps this image self-contained even when
# layered on an older base that predates the extension mechanism.
ENV LOADER_PATH=/ext

# Copy the thin extension jar (built by "mvn package") into the classpath extension
# directory. The wildcard matches the versioned artifact name; the *-sources /
# *-javadoc jars are intentionally excluded by the explicit suffix pattern below.
COPY target/phoss-ap-extension-demo-*.jar /ext/phoss-ap-extension-demo.jar
