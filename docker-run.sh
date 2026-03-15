#!/bin/bash
#
# Run the phoss-ap Docker image.
# Assumes PostgreSQL is accessible at localhost:5432.
#

set -e

docker run -d \
  --name phoss-ap \
  -p 8080:8080 \
  -e PHOSSAP_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/phoss-ap \
  -e PHOSSAP_JDBC_USER=peppol \
  -e PHOSSAP_JDBC_PASSWORD=peppol \
  phelger/phoss-ap

echo "phoss-ap started on http://localhost:8080"
echo "Health check: http://localhost:8080/actuator/health"
