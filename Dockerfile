# Multi-stage Dockerfile for phoss-ap
# Builds the application from source inside Docker - no local build tools needed.

# --- Stage 1: Build ---
FROM eclipse-temurin:21-alpine AS builder

RUN apk add --no-cache maven

WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests -q

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-alpine

LABEL maintainer="Philip Helger <philip@helger.com>"
LABEL org.opencontainers.image.title="phoss-ap"
LABEL org.opencontainers.image.description="Open-source Peppol Access Point based on phase4"
LABEL org.opencontainers.image.url="https://github.com/phax/phoss-ap"

VOLUME /tmp
VOLUME /var/phoss-ap/data

COPY --from=builder /build/phoss-ap-webapp/target/*.jar /app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/urandom", \
            "-XX:InitialRAMPercentage=10", \
            "-XX:MinRAMPercentage=50", \
            "-XX:MaxRAMPercentage=80", \
            "-jar", "/app.jar"]
