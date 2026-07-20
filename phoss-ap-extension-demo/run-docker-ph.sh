#!/bin/sh

# Example script that runs the Docker image built in this submodule via "assemble.sh".
# It selects the same image name assemble.sh produces: the "-arm64" variant on Mac
# (Apple Silicon), the plain variant elsewhere. Override by setting IMAGE_NAME.
if [ "$(uname -s)" = "Darwin" ]; then
  IMAGE_NAME="${IMAGE_NAME:-phelger/phoss-ap-with-demo-ext-arm64}"
else
  IMAGE_NAME="${IMAGE_NAME:-phelger/phoss-ap-with-demo-ext}"
fi

docker stop phoss-ap-with-demo-ext
docker rm phoss-ap-with-demo-ext
docker run -d \
  --name phoss-ap-with-demo-ext \
  -p 8080:8080 \
  -e _LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_BOOT_CONTEXT_CONFIG=TRACE \
  -e _LOADER_DEBUG=true \
  -e PHOSSAP_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/phoss-ap \
  -e PHOSSAP_JDBC_USER=peppol \
  -e PHOSSAP_JDBC_PASSWORD=peppol \
  -e PEPPOL_OWNER_SEATID=POP000306 \
  -e ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_FILE=/config/keystore.p12 \
  -e ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_PASSWORD=peppol \
  -e ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_ALIAS=private_key_for_pkcs12_certificate \
  -e ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_PRIVATE_PASSWORD=peppol \
  -v ./generated/data:/var/phoss-ap/data \
  -v /Users/philip/dev/git/phoss-ap/phoss-ap-webapp/src/main/resources/test-ap-2025-g3.p12:/config/keystore.p12 \
  "${IMAGE_NAME}"
