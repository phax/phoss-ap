# phoss-ap

<!-- ph-badge-start -->
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.helger.phoss.ap/phoss-ap-parent-pom/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.helger.phoss.ap/phoss-ap-parent-pom/)
[![javadoc](https://javadoc.io/badge2/com.helger.phoss.ap/phoss-ap-api/javadoc.svg)](https://javadoc.io/doc/com.helger.phoss.ap/phoss-ap-api)
<!-- ph-badge-end -->

phoss Peppol Access Point - when looking for the SMP, see [phoss SMP](https://github.com/phax/phoss-smp)

A complete open-source Peppol Access Point based on [phase4](https://github.com/phax/phase4).

It is a Spring Boot application that handles all the relevant Peppol specifications:
* Peppol AS4 specification - using [phase4](https://github.com/phax/phase4)
* Peppol SMP specification - using [peppol-smp-client](https://github.com/phax/peppol-commons)
* Peppol Network Reporting specification - using [peppol-reporting](https://github.com/phax/peppol-reporting)
* Peppol MLS specification - using [peppol-mls](https://github.com/phax/peppol-commons)

Features include:
* Inbound document reception with configurable forwarding (HTTP, S3, SFTP)
* Outbound document sending with automatic SMP lookup and SBDH creation
* Retry with exponential backoff and circuit breaker for both sending and forwarding
* Optional document validation via external verification service
* Duplicate detection on AS4 Message ID and SBDH Instance Identifier
* MLS (Message Level Status) support with SLA monitoring
* Peppol Reporting with scheduled submission
* Archival of completed transactions
* Sentry integration for error tracking

phoss AP is part of my Peppol solution stack. See https://github.com/phax/peppol for other components and libraries in that area.

## Build

Prerequisites:
* Java 21 or later
* Maven 3.x

```bash
mvn clean verify
```

## Quick Start

1. Set up a PostgreSQL database (see `docker-compose.yml` for an example)
2. Copy `phoss-ap-webapp/src/main/resources/application.properties` and adjust for your environment
3. Run the application:

```bash
java -jar phoss-ap-webapp/target/phoss-ap-webapp-*.jar
```

Or use Docker:

```bash
docker compose up -d
```

See the [Running phoss AP](https://github.com/phax/phoss-ap/wiki/Running-phoss-AP) wiki page for detailed setup instructions.

## Infrastructure

As the backend system a PostgreSQL DB is required.
The system can be deployed both locally as well as in a Cloud environment.
Docker images are available publicly.

## Documentation

Provided in the Wiki: https://github.com/phax/phoss-ap/wiki

## Misc

If you like the project, a star on GitHub is always appreciated.

If you need commercial support, please reach out to me by email (philip@helger.com).

If you'd like to sponsor this project, please reach out to me by email (philip@helger.com).

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
