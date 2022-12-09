FROM l.gcr.io/google/bazel:latest AS builder
WORKDIR build
COPY . .
RUN bazel build //app:app_deploy.jar

FROM openjdk:17-alpine
COPY --from=builder build/bazel-bin/app/app_deploy.jar .
ENTRYPOINT ["java", "-jar", "app_deploy.jar", "."]
