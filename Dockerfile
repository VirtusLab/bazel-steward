FROM ubuntu:22.10 AS builder
WORKDIR build
RUN apt-get update
RUN apt install -y build-essential
RUN apt-get install -y wget
RUN apt install -y openjdk-17-jdk openjdk-17-jre
RUN wget "https://github.com/bazelbuild/bazelisk/releases/download/v1.15.0/bazelisk-linux-amd64"
RUN chmod 733 bazelisk-linux-amd64
COPY . .
RUN ./bazelisk-linux-amd64 build //app:app_deploy.jar

FROM openjdk:17-alpine
COPY --from=builder build/bazel-bin/app/app_deploy.jar .
ENTRYPOINT ["java", "-jar", "app_deploy.jar", "/github/workspace"]
