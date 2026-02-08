# syntax=docker/dockerfile:1.7

# ---------- Stage 1: build ----------
# Pre-built gradle image with JDK 21 — no need to ship gradlew or worry about
# Windows CRLF issues in the gradle wrapper script.
FROM gradle:8.14-jdk21-alpine AS builder

WORKDIR /app

# Copy build descriptors first so dependency resolution caches between source-only
# changes. The `|| true` lets the layer cache even if `dependencies` itself
# can't fully resolve without source — we just want a warm Gradle cache here.
COPY --chown=gradle:gradle settings.gradle.kts build.gradle.kts ./
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true

# Now copy sources and build the application distribution. `installDist` produces
# `build/install/powerlifting-assistant-server/` with `bin/` and `lib/` — we copy
# that whole tree into the runtime image. This is faster to start and easier to
# debug than a single fat-jar.
COPY --chown=gradle:gradle src ./src
RUN gradle --no-daemon installDist -x test

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine

# Drop privileges. The base image doesn't ship a non-root user, so we make one.
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app

COPY --from=builder --chown=app:app \
    /app/build/install/powerlifting-assistant-server /app

# Default JVM tuning. Override via `-e JAVA_OPTS=...` if needed.
# - UseContainerSupport + MaxRAMPercentage tells the JVM to read the cgroup
#   memory limit (i.e. respect `mem_limit` in compose) instead of host RAM.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# Application config defaults — override via env vars or docker-compose.
ENV PORT=8080

USER app

EXPOSE 8080

# Healthcheck hits the public /health endpoint. wget is preinstalled on alpine.
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:${PORT}/health || exit 1

ENTRYPOINT ["/app/bin/powerlifting-assistant-server"]
