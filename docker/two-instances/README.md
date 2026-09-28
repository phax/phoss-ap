# Two local phoss AP instances without Peppol certificates

This setup runs two phoss AP instances (A and B) that exchange real AS4 messages with each other.
It needs **no official Peppol test certificate** and **no SMP/SML**. It is meant for development
and testing only.

It uses two settings that only work with `peppol.stage=test` and are ignored on production:

| Property | Purpose |
|---|---|
| `peppol.dev.trusted-ca.path` | PEM file of your own CA. It replaces the Peppol test AP CA for the startup check, the inbound signature check and the outbound receiver check. Revocation checks are turned off. |
| `outbound.dev-fixed-endpoint.url` | AS4 URL of the other AP. The SMP lookup is skipped and every outbound document goes there. |
| `outbound.dev-fixed-endpoint.certificate-path` | PEM certificate of the other AP (used for encryption). |

Also, the AS4 trust store (`org.apache.wss4j.crypto.merlin.truststore.*`) must contain your own CA
instead of the Peppol test trust store.

## With Docker

From the repository root:

```bash
mvn clean install -DskipTests
docker/two-instances/generate-certs.sh
docker compose -f docker/two-instances/docker-compose.yml up -d --build
```

* AP A: http://localhost:8081 (Seat ID `PXX000001`)
* AP B: http://localhost:8082 (Seat ID `PXX000002`)

## Without Docker

Create two databases (for example `phoss-ap-a` and `phoss-ap-b`), run `generate-certs.sh`, and
start the jar twice. Each instance uses its own port, database, key store and fixed endpoint. For
instance A:

```bash
C=docker/two-instances/certs
SERVER_PORT=8081 \
GLOBAL_DATAPATH=/tmp/ap-a/ \
PHOSSAP_JDBC_URL=jdbc:postgresql://localhost:5432/phoss-ap-a \
PEPPOL_STAGE=test \
PEPPOL_OWNER_SEATID=PXX000001 \
PHASE4_ENDPOINT_ADDRESS=http://localhost:8081/as4 \
ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_FILE=$C/ap-a.p12 \
ORG_APACHE_WSS4J_CRYPTO_MERLIN_KEYSTORE_ALIAS=ap \
ORG_APACHE_WSS4J_CRYPTO_MERLIN_TRUSTSTORE_FILE=$C/dev-truststore.p12 \
PEPPOL_DEV_TRUSTED_CA_PATH=$C/dev-ca.pem \
OUTBOUND_DEV_FIXED_ENDPOINT_URL=http://localhost:8082/as4 \
OUTBOUND_DEV_FIXED_ENDPOINT_CERTIFICATE_PATH=$C/ap-b.pem \
FORWARDING_MODE=filesystem \
FORWARDING_FILESYSTEM_DIRECTORY=/tmp/ap-a/fwd \
java -jar phoss-ap-webapp/target/phoss-ap-webapp-*-SNAPSHOT.jar
```

For instance B, swap the values: port `8082`, database `phoss-ap-b`, `PXX000002`, `ap-b.p12`, and
the fixed endpoint `http://localhost:8081/as4` with `ap-a.pem`.

## Send a test invoice from A to B

```bash
enc() { python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1],safe=''))" "$1"; }
DT=$(enc 'busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1')
PR=$(enc 'cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0')
curl -X POST -H "X-Token: phoss-ap-development-token" -H "Content-Type: application/xml" \
  --data-binary @phoss-ap-testsender/src/main/resources/samples/invoice-ubl.xml \
  "http://localhost:8081/api/outbound/submit/$(enc iso6523-actorid-upis::9915:sender)/$(enc iso6523-actorid-upis::9915:receiver)/$DT/$PR/AT"
```

The response should contain `"overallSuccess":true`. B writes the received document to its
forwarding directory. With Docker, check it with
`docker compose -f docker/two-instances/docker-compose.yml exec ap-b ls /tmp/phoss-ap/fwd`.
To send from B to A, use port `8082` and swap sender and receiver.

## Moving to real Peppol certificates

Once you have a Peppol test certificate, remove `peppol.dev.trusted-ca.path` and
`outbound.dev-fixed-endpoint.*`. Then configure the real key store and use the Peppol test trust
store `truststore/2025/ap-test-truststore.p12` again.
