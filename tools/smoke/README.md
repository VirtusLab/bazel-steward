# Action smoke test (issue #459)

Verifies that `use-release: false` works when Bazel Steward is consumed from another repository.

The `action-smoke-test` job in `.github/workflows/tests.yml`:

1. Checks out bazel-steward into `action-src/`
2. Runs `setup-consumer.sh`, which copies a minimal Bazel + git consumer repo to the workspace root (no `.github/` action)
3. Invokes `./action-src` with `use-release: false` and `push-to-remote: false`

The consumer fixture is based on e2e `bazel/bazelOnly`: `.bazelversion` plus `.bazel-steward.yaml` disabling maven/rules so only the bazel-version kind runs. Steward runs the full local path (branches/commits, no push) instead of `--analyze-only`.

This only runs in GitHub Actions on push/PR to `main`.
