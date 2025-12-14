# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build
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

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV JAVA_OPTS=""
EXPOSE 8080

COPY --from=build /build/target/*.jar /app/app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
