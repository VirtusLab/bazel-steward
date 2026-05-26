#!/usr/bin/env bash
# Run ktlint --format via Bazel *_lint_fix targets (same as rules_kotlin in CI).
set -euo pipefail

readonly LINT_FIX_SUFFIX="_lint_fix"

find_bazel_package_for_file() {
  local file_path="$1"
  local repo_root="$2"
  local dir

  case "$file_path" in
    "$repo_root"/*) file_path="${file_path#"$repo_root"/}" ;;
  esac

  dir="$(dirname "$file_path")"
  while [ "$dir" != "." ] && [ "$dir" != "/" ] && [ "$dir" != "$repo_root" ]; do
    if [ -f "$dir/BUILD.bazel" ] || [ -f "$dir/BUILD" ]; then
      printf '//%s' "$dir"
      return 0
    fi
    dir="$(dirname "$dir")"
  done
  return 1
}

escape_regex() {
  printf '%s' "$1" | sed 's/[][\\.^$*+?(){}|]/\\&/g'
}

build_union_query() {
  local repo_root="$1"
  shift

  local first=1
  local file_path package basename basename_re
  for file_path in "$@"; do
    if ! package="$(find_bazel_package_for_file "$file_path" "$repo_root")"; then
      continue
    fi
    basename="$(basename "$file_path")"
    basename_re="$(escape_regex "$basename")"
    if [ "$first" -eq 1 ]; then
      first=0
    else
      printf ' union '
    fi
    printf "filter('%s\$', attr('srcs', '%s', %s:all))" \
      "$LINT_FIX_SUFFIX" "$basename_re" "$package"
  done
}

main() {
  if [ "$#" -eq 0 ]; then
    exit 0
  fi

  local repo_root
  repo_root="$(git rev-parse --show-toplevel)"
  cd "$repo_root"

  local query
  query="$(build_union_query "$repo_root" "$@")"

  if [ -z "$query" ]; then
    exit 0
  fi

  local targets
  targets="$(bazel query "$query")"

  if [ -z "$targets" ]; then
    exit 0
  fi

  local target
  while IFS= read -r target; do
    if [ -n "$target" ]; then
      bazel run "$target"
    fi
  done < <(printf '%s\n' "$targets" | sort -u)
}

main "$@"
