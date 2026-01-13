# phoss-ap

phoss Peppol Access Point - when looking for the SMP, see [phoss SMP](https://github.com/phax/phoss-smp) 

## **WORK IN PROGRESS**

This repository will soon contain a complete Peppol Open Source AP based on [phase4](https://github.com/phax/phase4).

It is a Spring Boot application that handles all the relevant Peppol specifications:
* Peppol AS4 specification - using [phase4](https://github.com/phax/phase4)
* Peppol SMP specification - using [peppol-smp-client](https://github.com/phax/peppol-commons)
* Peppol Network Reporting specification - using [peppol-reporting](https://github.com/phax/peppol-reporting)
* Peppol MLS specification - using [peppol-mls](https://github.com/phax/peppol-commons)

Additionally planned features:
* It will support calling the external validation service - contact me for more details if you are interested 
* It will contain support for proper retry.

phoss AP is part of my Peppol solution stack. See https://github.com/phax/peppol for other components and libraries in that area.

## Development environment

* Java 21 or later
* Maven 3.x for building

## Infrastructure

As the backend system a PostgreSQL DB will be required.
The system can be deployed both locally as well as in a Cloud environment.
Docker images will be made available publicly.

## Documentation

Will be provided in the Wiki: https://github.com/phax/phoss-ap/wiki

## Misc

If you like the project, a star on GitHub is always appreciated.

If you need commercial support, please reach out to me by email (*firstname@lastname.com*).

If you'd like to sponsor this project, please reach out to me by email (*firstname@lastname.com*).

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
