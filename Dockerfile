# syntax=docker/dockerfile:1

# --- build stage -------------------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /build
# Wrapper and POM first: the dependency download is cached as its own layer and survives source changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B --no-transfer-progress dependency:go-offline
COPY src/ src/
# Tests are neither compiled nor run inside the image build; they run in CI (./mvnw verify) and need Docker anyway.
# The jar is then split into layers so that unchanged dependency layers are reused between image builds.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B --no-transfer-progress -Dmaven.test.skip=true package \
    && cp target/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# --- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine
# The application runs as an unprivileged user.
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
# Least-changing layers first: a code change rebuilds only the application layer.
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
EXPOSE 8080
# wget, because the Temurin alpine JRE image has no curl.
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "application.jar"]
