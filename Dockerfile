# syntax=docker/dockerfile:1
# Single multi-stage build shared by every Spring Boot service. The build stage compiles ALL service
# bootJars in one pass and is byte-for-byte identical across images, so BuildKit builds it exactly
# once and reuses it for each service's runtime image — no triple build, no parallel-build OOM. The
# runtime stage selects the module's jar via the MODULE build arg (set per service in compose).
# Lets `docker compose up --build` bring the whole stack online from a clean checkout, no prior Gradle.

# --- build stage -------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Build config first (better layer caching), then the monorepo sources. Modules depend on the
# common-* libraries and build-logic convention plugins, so all are needed on the build context.
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY build-logic ./build-logic
COPY common-domain ./common-domain
COPY common-web ./common-web
COPY common-security ./common-security
COPY common-archtest ./common-archtest
COPY collection-service ./collection-service
COPY processing-service ./processing-service
COPY offer-service ./offer-service

# bootJar (not build) → only executable jars, no -plain.jar and no tests here (tests run in CI /
# `./gradlew build`). All three jars build in one Gradle invocation.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && \
    ./gradlew --no-daemon \
      :collection-service:bootJar :processing-service:bootJar :offer-service:bootJar

# --- runtime stage -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
ARG MODULE
COPY --from=build /workspace/${MODULE}/build/libs/*.jar app.jar

# Run as an unprivileged system user.
RUN useradd -r appuser
USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
