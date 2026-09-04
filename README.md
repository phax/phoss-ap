# phoss-ap

<!-- ph-badge-start -->
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.helger.phoss.ap/phoss-ap-parent-pom/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.helger.phoss.ap/phoss-ap-parent-pom/)
[![javadoc](https://javadoc.io/badge2/com.helger.phoss.ap/phoss-ap-api/javadoc.svg)](https://javadoc.io/doc/com.helger.phoss.ap/phoss-ap-api)

> If this project saved you some time or made your day a little easier, a star would mean a lot — it helps others find it too.
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
* Optional document validation via external [verification service](https://github.com/phax/phorm)
* Duplicate detection on AS4 Message ID and SBDH Instance Identifier
* MLS (Message Level Status) support with SLA monitoring
* Peppol Reporting with scheduled submission
* Archival of completed transactions
* Sentry integration for error tracking
* OpenTelemetry based execution telemetry measurements

phoss AP is part of my Peppol solution stack. See https://github.com/phax/peppol for other components and libraries in that area.

## Build

Prerequisites:
* Java 21 or later
* Maven 3.x

```bash
mvn clean verify
```

## Quick Start

1. Set up a PostgreSQL or MySQL database (see `docker-compose.yml` for an example)
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

By default, phoss-ap can easily connect to either a MySQL/MariaDB or a PostgreSQL database.

### DB2 (Experimental)
Experimental support for IBM DB2 LUW is also included. Because the IBM `jcc` driver is proprietary, it is **not bundled** — it is supplied at runtime via the extension mechanism (requires 0.10.4 or later). To use DB2:
1. Drop the `com.ibm.db2:jcc` driver and the `org.flywaydb:flyway-database-db2` dialect jars into the `/ext` extension directory (`LOADER_PATH`). See the [Runtime Extensions](https://github.com/phax/phoss-ap/wiki/Runtime-Extensions) wiki page.
2. Configure `phossap.jdbc.database-type=db2` in your `application.properties`.
3. Due to DB2's strictness, ensure your driver version matches your database. Boolean values are mapped to `SMALLINT` (`0`/`1`), and unbounded text is mapped to `CLOB` or `VARCHAR(32000)`.

The Peppol Reporting SQL backend also supports DB2 (since `peppol-reporting` 4.2.0), so the reporting database may run on DB2 as well — it uses the same `jcc` driver supplied via `/ext`.

See `docker-compose.db2.yml` for an example of how to spin up a local DB2 instance for testing.

## Documentation

The full documentation is in the **[Wiki](https://github.com/phax/phoss-ap/wiki)**:

* [News and noteworthy](https://github.com/phax/phoss-ap/wiki/News-and-noteworthy)
* [Running phoss AP](https://github.com/phax/phoss-ap/wiki/Running-phoss-AP)
* [Architecture Overview](https://github.com/phax/phoss-ap/wiki/Architecture-Overview)
  * [Sending Process](https://github.com/phax/phoss-ap/wiki/Sending-Process)
  * [Receiving Process](https://github.com/phax/phoss-ap/wiki/Receiving-Process)
  * [Forwarding Process](https://github.com/phax/phoss-ap/wiki/Forwarding-Process)
  * [Retry and Resilience Patterns](https://github.com/phax/phoss-ap/wiki/Retry-and-Resilience-Patterns)
  * [Message Level Status](https://github.com/phax/phoss-ap/wiki/Message-Level-Status)
* [API Specification](https://github.com/phax/phoss-ap/wiki/API-Specification)
* [Configuration Properties](https://github.com/phax/phoss-ap/wiki/Configuration-Properties)
* [Code Lists](https://github.com/phax/phoss-ap/wiki/Code-Lists)
* [Database Design Notes](https://github.com/phax/phoss-ap/wiki/Database-Design-Notes)
* [Maven Module Structure](https://github.com/phax/phoss-ap/wiki/Maven-Module-Structure)
* [Runtime Extensions](https://github.com/phax/phoss-ap/wiki/Runtime-Extensions)
* [OpenTelemetry Integration](https://github.com/phax/phoss-ap/wiki/OpenTelemetry-Integration)
* [Security Considerations](https://github.com/phax/phoss-ap/wiki/Security-Considerations)
* [Peppol Specifics](https://github.com/phax/phoss-ap/wiki/Peppol-Specifics)
* [Testing Without Peppol Network](https://github.com/phax/phoss-ap/wiki/Testing-Without-Peppol-Network)
* [Known Users](https://github.com/phax/phoss-ap/wiki/Known-Users)
* [Migrating from phase4-peppol-standalone](https://github.com/phax/phoss-ap/wiki/Migrating-from-phase4-peppol-standalone)
* [Contributing](https://github.com/phax/phoss-ap/wiki/Contributing)

## Misc

If you like the project, a star on GitHub is always appreciated.

If you need commercial support or if you'd like to sponsor this project, please reach out to me by email (philip@helger.com).

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
