# Bazel-Steward

Bazel-Steward is a bot that helps you keep your library dependencies up-to-date.

## Using with GitHub
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
          with:
            github-personal-token: '' # used for triggering workflows, read below
```

After every run, it creates a new branch and a pull request with a bumped version for every library it detects to be outdated.
You can merge the PR, close it, or push your own changes onto the branch.
Once a PR is opened, it will never open another one for the same version, regardless of what you do with the PR.

### Triggering workflows for pull requests

By design, workflows don't trigger on pull requests when it is created by GitHub Actions.
To trigger workflows automatically, do one of the following:
* set `github-personal-token` to your personal token (recommended, your token will be used only for closing/reopening pull requests)
* set `github-token` to your personal token (every operation will be made in your name)
* create GitHub app and use its token for `github-token` (the best option, requires a bit of setup)

Read more about triggering workflows using personal tokens and setting up GitHub app [here](https://github.com/peter-evans/create-pull-request/blob/main/docs/concepts-guidelines.md#triggering-further-workflow-runs). 

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

    # An optional token for closing and reopening pull requests
    github-personal-token: ''
```

## Contributing

Want to contribute? Look [here](CONTRIBUTING.md)!