# Bazel-Steward

Bazel-Steward is a bot that helps you keep your library dependencies up-to-date.

## Using with Github
Create file at  `.github/workflows/` with this content:
```yaml
name: Update dependencies

on:
  schedule:
    - cron: '30 5 * * 6' # runs every saturday at 5:30 am

  jobs:
    bazel-steward:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v2
          with:
            fetch-depth: 0
        - uses: VirtusLab/bazel-steward@v0.1.0 # or latest
```

After every run, it creates a new branch and a pull request with a bumped version for every library it detects to be outdated.
You can merge the PR, close it, or push your own changes onto the branch.
Once a PR is opened, it will never open another one for the same version, regardless of what you do with the PR.

### Detailed usage
```yaml
- uses: VirtusLab/bazel-steward@latest
  with:
    # The path to bazel steward configuration
    # Default: ".github/bazel-steward.yaml"
    configuration-path: ''
    
    # A token for the GitHub repository
    # Default: ${{ github.token }}
    github-token: ''
    
    # Additional arguments to bazel steward jar
    additional-args: ''
```

## Contributing

Want to contribute? Look [here](CONTRIBUTING.md)!