#!/usr/bin/env bash
# Run ktlint --format via Bazel *_lint_fix targets (same as rules_kotlin in CI).
set -euo pipefail

readonly LINT_FIX_SUFFIX="_lint_fix"

find_bazel_package_for_file() {
  local file_path="$1"
  local dir
  dir="$(dirname "$file_path")"
  while [ "$dir" != "." ] && [ "$dir" != "/" ]; do
    if [ -f "$dir/BUILD.bazel" ] || [ -f "$dir/BUILD" ]; then
      printf '//%s' "${dir#./}"
      return 0
    fi
    dir="$(dirname "$dir")"
  done
  return 1
}

query_lint_fix_targets_for_file() {
  local file_path="$1"
  local bazel_package
  local source_basename

  bazel_package="$(find_bazel_package_for_file "$file_path")" || return 0
  source_basename="$(basename "$file_path")"

  # One target per source when macros define per-file srcs (e.g. e2e_*Test.kt_lint_fix).
  bazel query "filter('${LINT_FIX_SUFFIX}\$', attr('srcs', '${source_basename}', ${bazel_package}:all))" \
    2>/dev/null || true
}

collect_lint_fix_targets() {
  local output_file="$1"
  shift

  local file_path
  for file_path in "$@"; do
    query_lint_fix_targets_for_file "$file_path" >>"$output_file"
  done
}

run_bazel_lint_fix() {
  local targets_file="$1"
  local target

  while IFS= read -r target; do
    [ -n "$target" ] && bazel run "$target"
  done < <(sort -u "$targets_file")
}

main() {
  if [ "$#" -eq 0 ]; then
    exit 0
  fi

  cd "$(git rev-parse --show-toplevel)"

  local targets_file
  targets_file="$(mktemp)"
  trap "rm -f '${targets_file}'" EXIT

  collect_lint_fix_targets "$targets_file" "$@"

  if [ ! -s "$targets_file" ]; then
    exit 0
  fi

  run_bazel_lint_fix "$targets_file"
}

main "$@"
