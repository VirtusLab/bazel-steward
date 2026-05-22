#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?Usage: $0 <version> [max-wait-seconds]}"
MAX_WAIT="${2:-0}"
GROUP_ID="org.virtuslab"
ARTIFACT_ID="bazel-steward"
COORDS="${GROUP_ID}:${ARTIFACT_ID}:${VERSION}"

GROUP_PATH=$(echo "$GROUP_ID" | tr '.' '/')
BASE_URL="https://repo1.maven.org/maven2/${GROUP_PATH}/${ARTIFACT_ID}/${VERSION}"
POM_URL="${BASE_URL}/${ARTIFACT_ID}-${VERSION}.pom"

if [ "$MAX_WAIT" -gt 0 ]; then
  echo "Waiting up to ${MAX_WAIT}s for ${COORDS} to appear on Maven Central..."
  ELAPSED=0
  INTERVAL=15
  while [ "$ELAPSED" -lt "$MAX_WAIT" ]; do
    if curl --fail --silent --head "$POM_URL" > /dev/null 2>&1; then
      echo "Artifact available after ${ELAPSED}s."
      break
    fi
    echo "  Not yet available (${ELAPSED}s elapsed), retrying in ${INTERVAL}s..."
    sleep "$INTERVAL"
    ELAPSED=$((ELAPSED + INTERVAL))
    INTERVAL=$((INTERVAL < 60 ? INTERVAL * 2 : 60))
  done

  if ! curl --fail --silent --head "$POM_URL" > /dev/null 2>&1; then
    echo "Artifact not available after ${MAX_WAIT}s. Giving up."
    exit 1
  fi
fi

echo "Smoke-testing ${COORDS} from Maven Central..."

JAR_NAME="${ARTIFACT_ID}-${VERSION}.jar"
POM_NAME="${ARTIFACT_ID}-${VERSION}.pom"
SOURCES_NAME="${ARTIFACT_ID}-${VERSION}-sources.jar"

WORKDIR=$(mktemp -d)
trap 'rm -rf "$WORKDIR"' EXIT

FAILED=0
for artifact in "$POM_NAME" "$JAR_NAME" "$SOURCES_NAME"; do
  URL="${BASE_URL}/${artifact}"
  printf "  %-50s " "${artifact}"
  if curl --fail --silent --head "$URL" > "$WORKDIR/headers" 2>&1; then
    SIZE=$(grep -i content-length "$WORKDIR/headers" | tail -1 | tr -dc '0-9')
    echo "OK (${SIZE:-?} bytes)"
  else
    echo "MISSING"
    FAILED=1
  fi
done

echo ""

echo "Checking POM metadata..."
curl --fail --silent "${BASE_URL}/${POM_NAME}" > "$WORKDIR/pom.xml"
for field in groupId artifactId version name url; do
  VALUE=$(sed -n "s/.*<${field}>\([^<]*\)<\/${field}>.*/\1/p" "$WORKDIR/pom.xml" | head -1)
  printf "  %-15s %s\n" "${field}:" "${VALUE:-NOT FOUND}"
done

echo ""

echo "Checking JAR contents..."
curl --fail --silent --output "$WORKDIR/$JAR_NAME" "${BASE_URL}/${JAR_NAME}"
if java -jar "$WORKDIR/$JAR_NAME" --version 2>/dev/null; then
  :
elif jar tf "$WORKDIR/$JAR_NAME" | grep -q "org/virtuslab/bazelsteward/app/Main"; then
  echo "  Main class present: org.virtuslab.bazelsteward.app.Main"
else
  echo "  WARNING: Main class not found in JAR"
  FAILED=1
fi

echo ""
if [ "$FAILED" -eq 0 ]; then
  echo "Smoke test PASSED for ${COORDS}."
else
  echo "Smoke test FAILED for ${COORDS}."
  exit 1
fi
