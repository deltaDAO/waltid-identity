# syntax=docker/dockerfile:1.7

# NOTE: Prefer Gradle Ktor/Jib tasks (publishImageToLocalRegistry/publishImage) for builds.
# This Dockerfile is for manual/custom Docker workflows.
FROM gradle:latest AS pre-cache
COPY . /home/src
COPY gradle.properties settings.gradle.kts /home/app/
COPY build-logic /home/app/build-logic
COPY gradle /home/app/gradle
RUN cd /home/src && \
    find . -name "build.gradle.kts" -exec sh -c 'mkdir -p /home/app/$(dirname "$1"); cp "$1" /home/app/$1' _ {} \;

# Stage 1: Cache Gradle dependencies
FROM gradle:latest AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY --from=pre-cache /home/app/ /home/gradle/app/
WORKDIR /home/gradle/app/waltid-services/waltid-wallet-api
RUN --mount=type=cache,target=/home/gradle/cache_home \
    gradle dependencies --no-daemon --build-cache

# Stage 2: Build Application
FROM gradle:latest AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src/waltid-services/waltid-wallet-api
# Allocate enough heap for shadowJar on this large monorepo.
# --no-daemon avoids a second JVM process; --max-workers limits parallel tasks
# so total memory stays within Colima's 8 GiB VM.
ENV GRADLE_OPTS="-Xmx4g -Xms512m -XX:+HeapDumpOnOutOfMemoryError"
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle buildFatJar --no-daemon --build-cache --max-workers 2

# Stage 3: Create the Runtime Image
FROM amazoncorretto:22 AS runtime
WORKDIR /waltid-wallet-api

# Create an unprivileged user and group
RUN yum install -y shadow-utils \
 && groupadd -r app \
 && useradd -r -g app -u 10001 app \
 && yum clean all

COPY --chown=app:app --from=build /home/gradle/src/waltid-services/waltid-wallet-api/build/libs/*.jar /waltid-wallet-api/wallet-api.jar

USER app
EXPOSE 7001
ENTRYPOINT ["java","-jar","/waltid-wallet-api/wallet-api.jar"]
