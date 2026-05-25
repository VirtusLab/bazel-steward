# Action smoke test (issue #459)

Verifies that `use-release: false` works when Bazel Steward is consumed from another repository via `uses: VirtusLab/bazel-steward@…`.

The `action-smoke-test` job in `.github/workflows/tests.yml`:

1. Checks out bazel-steward into `action-src/`
2. Runs `setup-consumer.sh`, which places a minimal `WORKSPACE` at the workspace root (simulating an external consumer that does not invoke the deprecated `.github/` docker action)
3. Invokes `./action-src` with `use-release: false` (same-repo checkout; `uses` cannot reference `${{ github.sha }}`)

The build step uses `$GITHUB_ACTION_PATH` (the installed action directory), not a path under the caller workspace — that is what issue #459 fixes.

This only runs in GitHub Actions on push/PR to `main`.
