#!/usr/bin/env bash
set -euo pipefail

# Publish the bazel-steward jar to Maven Central.
#
# OSSRH (oss.sonatype.org) was sunset on 2025-06-30 and replaced by the
# Central Publisher Portal. Sonatype provides an OSSRH-compatible staging
# API at https://ossrh-staging-api.central.sonatype.com so that existing
# Nexus 2 tooling (like bazel_sonatype) continues to work. After upload,
# the deployment must be released via the Portal UI or Portal API.
#
# See: https://central.sonatype.org/pages/ossrh-eol

readonly CENTRAL_PORTAL_API="https://ossrh-staging-api.central.sonatype.com/service/local"
readonly BUILD_FILE="app/BUILD.bazel"

if [ "${1:-}" == "--local" ]; then
  bazel run --stamp \
    --define "maven_repo=file://$HOME/.m2/repository" \
    //app:maven.publish
  exit 0
fi

: "${PGP_SECRET:?PGP_SECRET is required}"
: "${PGP_PASSPHRASE:?PGP_PASSPHRASE is required}"
: "${RELEASE_VERSION:?RELEASE_VERSION is required}"
: "${SONATYPE_USERNAME:?SONATYPE_USERNAME is required (Portal user token name)}"
: "${SONATYPE_PASSWORD:?SONATYPE_PASSWORD is required (Portal user token pass)}"

restore_build_file() {
  if [ -f "${BUILD_FILE}.bak" ]; then
    mv "${BUILD_FILE}.bak" "${BUILD_FILE}"
  fi
}
trap restore_build_file EXIT

cp "${BUILD_FILE}" "${BUILD_FILE}.bak"
if [[ "$(uname -s)" == "Darwin" ]]; then
  sed -i "" "s/0\.0\.0/${RELEASE_VERSION}/g" "${BUILD_FILE}"
else
  sed -i "s/0\.0\.0/${RELEASE_VERSION}/g" "${BUILD_FILE}"
fi

bazel run --stamp \
  --define "maven_repo=${CENTRAL_PORTAL_API}" \
  --define "maven_user=${SONATYPE_USERNAME}" \
  --define "maven_password=${SONATYPE_PASSWORD}" \
  //app:maven.publish

echo
echo "Upload complete. Open https://central.sonatype.com to validate and"
echo "release the staged deployment, or call the Portal release API."
