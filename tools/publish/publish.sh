#!/usr/bin/env bash
set -euo pipefail

if [ "${1:-}" == "--local" ]; then
  bazel run --stamp \
    --define "maven_repo=file://$HOME/.m2/repository" \
    //app:maven.publish
else
  : "$PGP_PASSPHRASE"
  : "$RELEASE_VERSION"
  : "$SONATYPE_USERNAME"
  : "$SONATYPE_PASSWORD"

  GROUP_ID="org.virtuslab"
  ARTIFACT_ID="bazel-steward"
  VERSION="$RELEASE_VERSION"

  if [[ $(uname -s) == "Darwin" ]]; then
    sed -i "" "s/0\.0\.0/$VERSION/g" app/BUILD.bazel
  else
    sed -i "s/0\.0\.0/$VERSION/g" app/BUILD.bazel
  fi

  bazel build --stamp \
    //app:maven-maven-artifact \
    //app:maven-maven-source \
    //app:maven-docs \
    //app:maven-pom

  ARTIFACT_JAR="bazel-bin/app/maven-project.jar"
  SOURCE_JAR="bazel-bin/app/maven-project-src.jar"
  DOCS_JAR="bazel-bin/app/maven-docs.jar"
  POM_FILE="bazel-bin/app/maven-pom.xml"

  for f in "$ARTIFACT_JAR" "$SOURCE_JAR" "$DOCS_JAR" "$POM_FILE"; do
    if [ ! -f "$f" ]; then
      echo "Error: Expected artifact not found: $f"
      echo "Contents of bazel-bin/app/:"
      ls -la bazel-bin/app/ || true
      exit 1
    fi
  done

  BUNDLE_DIR=$(mktemp -d)
  trap 'rm -rf "$BUNDLE_DIR"' EXIT

  ARTIFACT_DIR="${BUNDLE_DIR}/${GROUP_ID//./\/}/${ARTIFACT_ID}/${VERSION}"
  mkdir -p "$ARTIFACT_DIR"

  cp "$ARTIFACT_JAR" "${ARTIFACT_DIR}/${ARTIFACT_ID}-${VERSION}.jar"
  cp "$SOURCE_JAR"   "${ARTIFACT_DIR}/${ARTIFACT_ID}-${VERSION}-sources.jar"
  cp "$DOCS_JAR"     "${ARTIFACT_DIR}/${ARTIFACT_ID}-${VERSION}-javadoc.jar"
  cp "$POM_FILE"     "${ARTIFACT_DIR}/${ARTIFACT_ID}-${VERSION}.pom"

  for file in "${ARTIFACT_DIR}/${ARTIFACT_ID}-${VERSION}"{.jar,-sources.jar,-javadoc.jar,.pom}; do
    gpg --batch --yes --passphrase "$PGP_PASSPHRASE" --pinentry-mode loopback -ab "$file"
    md5sum "$file" | cut -d ' ' -f 1 > "${file}.md5"
    sha1sum "$file" | cut -d ' ' -f 1 > "${file}.sha1"
  done

  BUNDLE_ZIP="${BUNDLE_DIR}/central-bundle.zip"
  (cd "$BUNDLE_DIR" && zip -r "$BUNDLE_ZIP" "${GROUP_ID%%.*}")

  AUTH_TOKEN=$(printf '%s:%s' "$SONATYPE_USERNAME" "$SONATYPE_PASSWORD" | base64 -w0)

  echo "Uploading ${GROUP_ID}:${ARTIFACT_ID}:${VERSION} to Maven Central..."

  DEPLOYMENT_ID=$(curl --fail --silent --show-error \
    --request POST \
    --header "Authorization: Bearer ${AUTH_TOKEN}" \
    --form "bundle=@${BUNDLE_ZIP};filename=central-bundle.zip;type=application/octet-stream" \
    "https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC&name=${GROUP_ID}:${ARTIFACT_ID}:${VERSION}")

  echo "Deployment ID: ${DEPLOYMENT_ID}"

  for _ in $(seq 1 60); do
    sleep 10

    STATUS_JSON=$(curl --fail --silent --show-error \
      --request POST \
      --header "Authorization: Bearer ${AUTH_TOKEN}" \
      "https://central.sonatype.com/api/v1/publisher/status?id=${DEPLOYMENT_ID}")

    STATE=$(echo "$STATUS_JSON" | python3 -c "import sys, json; print(json.load(sys.stdin)['deploymentState'])")
    echo "Deployment state: ${STATE}"

    case "$STATE" in
      PUBLISHED)
        echo "Successfully published to Maven Central!"
        exit 0
        ;;
      FAILED)
        echo "Deployment failed!"
        echo "$STATUS_JSON" | python3 -m json.tool
        exit 1
        ;;
      PENDING|VALIDATING|VALIDATED|PUBLISHING)
        continue
        ;;
      *)
        echo "Unknown deployment state: ${STATE}"
        ;;
    esac
  done

  echo "Timed out waiting for deployment to complete (last state: ${STATE})"
  exit 1
fi
