#!/usr/bin/env bash

set -u -e -o pipefail
for target in $(bazel query --noshow_progress --output=label "kind('ktlint_fix rule', //...)"); do
  bazel run "$target"
done

bazel build //...