#!/bin/sh

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
