FROM ubuntu:24.04 AS builder

ARG BAZELISK_VERSION=v1.22.1

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        wget \
        openjdk-17-jdk-headless \
 && rm -rf /var/lib/apt/lists/*

RUN wget -qO /usr/local/bin/bazelisk \
        "https://github.com/bazelbuild/bazelisk/releases/download/${BAZELISK_VERSION}/bazelisk-linux-amd64" \
 && chmod 0755 /usr/local/bin/bazelisk

WORKDIR /src
COPY . .
RUN /usr/local/bin/bazelisk build //app:app_deploy.jar

FROM alpine:3.20
COPY --from=builder /src/bazel-bin/app/app_deploy.jar /opt/bazel-steward.jar
ENTRYPOINT ["cp", "/opt/bazel-steward.jar", "."]
