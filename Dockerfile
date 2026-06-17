# syntax=docker/dockerfile:1
# Single multi-stage build shared by every Spring Boot service. The build stage compiles ALL service
# bootJars in one pass and is byte-for-byte identical across images, so BuildKit builds it exactly
# once and reuses it for each service's runtime image — no triple build, no parallel-build OOM. The
# runtime stage selects the module's jar via the MODULE build arg (set per service in compose).
# Lets `docker compose up --build` bring the whole stack online from a clean checkout, no prior Gradle.

# --- build stage -------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Persist Gradle's home (downloaded distribution + dependency cache + build cache) in a BuildKit
# cache mount, NOT in an image layer — so a code change recompiles but never re-downloads Gradle or
# the dependencies. GRADLE_USER_HOME pins where that cache lives.
ENV GRADLE_USER_HOME=/root/.gradle

# Wrapper first: this layer (and the warmed Gradle distribution) is invalidated only when the wrapper
# itself changes, not on every source edit.
COPY gradlew ./
COPY gradle ./gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon --version

# Build config next, then the monorepo sources. Modules depend on the common-* libraries and
# build-logic convention plugins, so all are needed on the build context.
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY build-logic ./build-logic
COPY common-domain ./common-domain
COPY common-web ./common-web
COPY common-security ./common-security
COPY common-archtest ./common-archtest
COPY collection-service ./collection-service
COPY processing-service ./processing-service
COPY offer-service ./offer-service
COPY api-gateway ./api-gateway
COPY user-service ./user-service
COPY tracker-service ./tracker-service
COPY notification-service ./notification-service

# bootJar (not build) → only executable jars, no -plain.jar and no tests here (tests run in CI /
# `./gradlew build`). All service jars build in one Gradle invocation, reusing the cached Gradle home.
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon \
      :collection-service:bootJar :processing-service:bootJar :offer-service:bootJar \
      :api-gateway:bootJar :user-service:bootJar :tracker-service:bootJar \
      :notification-service:bootJar

# --- runtime stage -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
ARG MODULE
COPY --from=build /workspace/${MODULE}/build/libs/*.jar app.jar

# Run as an unprivileged system user.
RUN useradd -r appuser
USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
