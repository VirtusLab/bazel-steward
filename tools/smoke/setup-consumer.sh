#!/usr/bin/env bash
# Materializes a minimal consumer repo at GITHUB_WORKSPACE root (CI layout).
# The consumer has no .github docker action — see issue #459.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WORKSPACE="${GITHUB_WORKSPACE:?GITHUB_WORKSPACE is required}"

if [[ -d "${WORKSPACE}/action-src" ]]; then
  BAZEL_STEWARD_ROOT="${WORKSPACE}/action-src"
else
  BAZEL_STEWARD_ROOT="${BAZEL_STEWARD_ROOT:-${WORKSPACE}}"
fi

if [[ ! -f "${BAZEL_STEWARD_ROOT}/action.yaml" ]]; then
  echo "error: bazel-steward root not found at ${BAZEL_STEWARD_ROOT}" >&2
  exit 1
fi

cp "${SCRIPT_DIR}/consumer/WORKSPACE" "${WORKSPACE}/WORKSPACE"

echo "Consumer workspace: ${WORKSPACE}"
echo "Bazel Steward source: ${BAZEL_STEWARD_ROOT}"
