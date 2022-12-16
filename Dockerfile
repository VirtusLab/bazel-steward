FROM ubuntu:22.04 AS builder
RUN apt-get update
RUN apt install -y build-essential
RUN apt-get install -y wget
RUN apt install -y openjdk-17-jdk openjdk-17-jre
RUN wget "https://github.com/bazelbuild/bazelisk/releases/download/v1.15.0/bazelisk-linux-amd64"
RUN chmod 733 bazelisk-linux-amd64
COPY . .
RUN ./bazelisk-linux-amd64 build //app:app_deploy.jar

FROM alpine:3.16
COPY --from=builder bazel-bin/app/app_deploy.jar /opt/bazel-steward.jar
ENTRYPOINT ["cp", "/opt/bazel-steward.jar", "."]
