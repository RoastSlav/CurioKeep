# syntax=docker/dockerfile:1.7

FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /build

# copy only dependency descriptors first for caching
COPY pom.xml .
COPY frontend/package*.json frontend/

# cache Maven repo between builds
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests dependency:go-offline

# now copy the full source
COPY . .

# cache Maven + node_modules between builds
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/build/frontend/node_modules \
    mvn -DskipTests -Pfrontend clean package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# A fixed uid keeps volume ownership predictable; /app/data and /app/logs are the only places the application writes.
RUN addgroup -S -g 10001 curiokeep && adduser -S -u 10001 -G curiokeep curiokeep \
    && mkdir -p /app/data /app/logs && chown -R curiokeep:curiokeep /app

ENV JAVA_OPTS=""
EXPOSE 8080

COPY --from=build --chown=curiokeep:curiokeep /build/target/*.jar /app/app.jar
USER curiokeep

# The setup status endpoint is public and reads the database, so it fails when either is unreachable.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/api/setup/status || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
