#!/usr/bin/env bash
# Run ktlint via Bazel (same rules_kotlin targets as CI) on staged Kotlin files.
set -euo pipefail

mode="${1:?usage: $0 <fix|check>}"
shift

if [ "$#" -eq 0 ]; then
  exit 0
fi

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

targets_file="$(mktemp)"
trap 'rm -f "$targets_file"' EXIT

nearest_build_package() {
  local path="$1"
  local dir
  dir="$(dirname "$path")"
  while [ "$dir" != "." ] && [ "$dir" != "/" ]; do
    if [ -f "$dir/BUILD.bazel" ] || [ -f "$dir/BUILD" ]; then
      printf '//%s' "${dir#./}"
      return 0
    fi
    dir="$(dirname "$dir")"
  done
  return 1
}

for file in "$@"; do
  pkg="$(nearest_build_package "$file")" || continue
  base="$(basename "$file")"
  suffix="_lint_fix"
  if [ "$mode" = "check" ]; then
    suffix="_lint_test"
  fi
  # Match the lint target whose srcs include this file (e.g. e2e_*Test.kt_lint_fix),
  # not every *_lint_* target in the package.
  bazel query "filter('${suffix}\$', attr('srcs', '${base}', ${pkg}:all))" 2>/dev/null >>"$targets_file" || true
done

if [ ! -s "$targets_file" ]; then
  exit 0
fi

if [ "$mode" = "fix" ]; then
  while IFS= read -r target; do
    [ -n "$target" ] && bazel run "$target"
  done < <(sort -u "$targets_file")
else
  # shellcheck disable=SC2046
  bazel test $(sort -u "$targets_file")
fi
