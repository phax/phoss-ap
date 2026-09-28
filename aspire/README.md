# Two phoss AP instances with .NET Aspire

`PhossAP.AppHost` is a .NET Aspire 9 AppHost (.NET 9). It starts:

* one PostgreSQL container with the databases `phoss-ap-a` and `phoss-ap-b`
* phoss AP **A** on http://localhost:8081 (Seat ID `PXX000001`)
* phoss AP **B** on http://localhost:8082 (Seat ID `PXX000002`)

A sends every document to B and B sends every document to A. No official Peppol test certificate
and no SMP/SML are needed. This uses the test-stage-only settings `peppol.dev.trusted-ca.path` and
`outbound.dev-fixed-endpoint.*`, described in
[../docker/two-instances/README.md](../docker/two-instances/README.md). For local development only.

## Prerequisites

* .NET 9 SDK
* JDK 21 or later. `java` and `keytool` must be on the `PATH`, or `JAVA_HOME` must be set
* Maven
* Docker (or Podman) for the PostgreSQL container

## Run

From the repository root:

```bash
mvn clean install -DskipTests
dotnet run --project aspire/PhossAP.AppHost
```

On the first start, the AppHost uses `keytool` to create a CA and one AP certificate per instance
in `aspire/.dev/certs`. Delete that folder to create new certificates.

The dashboard opens at http://localhost:15180. Both APs appear there with their console logs,
health status and environment. Their OpenTelemetry traces, metrics and logs also go there, through
`otel.enabled=true`.

Received documents are written to `aspire/.dev/ap-a/fwd` and `aspire/.dev/ap-b/fwd`.

## Send a test invoice from A to B

```bash
enc() { python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1],safe=''))" "$1"; }
DT=$(enc 'busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1')
PR=$(enc 'cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0')
curl -X POST -H "X-Token: phoss-ap-development-token" -H "Content-Type: application/xml" \
  --data-binary @phoss-ap-testsender/src/main/resources/samples/invoice-ubl.xml \
  "http://localhost:8081/api/outbound/submit/$(enc iso6523-actorid-upis::9915:sender)/$(enc iso6523-actorid-upis::9915:receiver)/$DT/$PR/AT"
```

The response should contain `"overallSuccess":true`. To send from B to A, use port `8082` and swap
sender and receiver. The Swagger UI of each instance is another way to send documents.
