#!/usr/bin/env bash

set -u -e -o pipefail
for target in $(bazel test //... --test_tag_filters=lint | grep FAILED | awk '{print $1}' | sed s/_lint_test/_lint_fix/); do
  bazel run "$target"
done

bazel build //...