#!/usr/bin/env bash
set -euo pipefail

if [ "$1" == "--local" ]; then
  bazel run --stamp \
    --define "maven_repo=file://$HOME/.m2/repository" \
    //app:maven.publish
else
  : "$PGP_SECRET"
  : "$PGP_PASSPHRASE"
  : "$RELEASE_VERSION"

  if [[ $(uname -s) == "Darwin" ]]; then
    sed -i "" "s/0\.0\.0/$RELEASE_VERSION/g" app/BUILD.bazel
  else
    sed -i "s/0\.0\.0/$RELEASE_VERSION/g" app/BUILD.bazel
  fi

  bazel run --stamp \
    --define "maven_repo=https://oss.sonatype.org/service/local" \
    --define "maven_user=$SONATYPE_USERNAME" \
    --define "maven_password=$SONATYPE_PASSWORD" \
    //app:maven.publish
fi

