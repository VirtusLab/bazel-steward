FROM ubuntu:24.04 AS builder
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      build-essential \
      ca-certificates \
      curl \
      openjdk-21-jdk-headless \
 && rm -rf /var/lib/apt/lists/*
RUN curl -fsSLo /usr/local/bin/bazelisk \
      "https://github.com/bazelbuild/bazelisk/releases/download/v1.25.0/bazelisk-linux-amd64" \
 && chmod +x /usr/local/bin/bazelisk
COPY . .
RUN bazelisk build //app:app_deploy.jar

FROM alpine:3.20
COPY --from=builder bazel-bin/app/app_deploy.jar /opt/bazel-steward.jar
ENTRYPOINT ["cp", "/opt/bazel-steward.jar", "."]
