# Action smoke test (issue #459)

Verifies that `use-release: false` works when Bazel Steward is consumed from another repository via `uses: VirtusLab/bazel-steward@…`.

The `action-smoke-test` job in `.github/workflows/tests.yml`:

1. Checks out bazel-steward into `action-src/`
2. Runs `setup-consumer.sh`, which places a minimal `WORKSPACE` at the workspace root (simulating an external consumer with no `.github` docker action)
3. Invokes `./action-src` with `use-release: false` (same-repo checkout; `uses` cannot reference `${{ github.sha }}`)

The nested build step still resolves `VirtusLab/bazel-steward/.github@…` from GitHub, which is what issue #459 fixes — it must not resolve `./.github` against the consumer workspace.

This only runs in GitHub Actions on push/PR to `main`.
