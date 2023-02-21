<!--suppress HtmlDeprecatedAttribute -->
<p align="center">
  <img src="https://github.com/VirtusLab/bazel-steward/blob/main/docs/images/logo.png?raw=true" height="250px" alt="Basel Steward logo">
</p>

# Bazel Steward
[![Bazel Steward badge](https://img.shields.io/badge/Bazel_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,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)](https://github.com/VirtusLab/bazel-steward)

Bazel Steward is a bot that helps you keep your library dependencies, Bazel and Bazel rules up-to-date.

### How it works

Bazel Steward scans your repository looking for outside dependencies.
Then, it compares a version of the found dependency against a remote repository.
If a newer version is available on the remote repository, then it opens a pull request with change for that newer version.

Pull request created by bazel steward are managed by it until they are merged or closed, but there is an exception to that.
If you push any changes to a branch created by bazel steward, it will stop managing the associated pull request.
Otherwise, Bazel Steward will do the following things:
* When an even newer version of library appears, it will open a new pull request for newer version and close pull request for older versions
* When pull request is no longer mergeable, it will force push the branch with the same version
* When pull request is closed/merged, it will never create a new pull request for the same version

### Future plans
For know, Bazel Steward support maven dependencies addet though `maven_install` rule bazel rules added by `http_archive`.
We plan to extend the support over more rules that use other archives.

We are also planing to support more git hosting methods, but for now, we only support GitHub.

## Configuration
You can configure how to handle specific dependencies. Config is stored in a root of a repository.

Example config:
```yaml
maven:
  configs:
    -
      group: commons-io
      artifact: commons-io
      pin: "2."
      versioning: loose
      bumping: default
    -
      group: io.get-coursier
      artifact: interface
      versioning: semver
      bumping: latest
    -
      group: org.jetbrains.kotlinx
      artifact: kotlinx-coroutines-jdk8
      versioning: regex:^(?<major>\d*)(?:[.-](?<minor>(\d*)))?(?:[.-]?(?<patch>(\d*)))?(?:[-.]?(?<preRelease>(\d*)))(?<buildMetaData>)?
    -
      group: org.jetbrains.kotlinx
      versioning: loose
    -
      versioning: loose
```

When resolving which rule to use, Bazel Steward first tries to find exact match of group and artefact.
Then it tries to find exact group and no artifact. Lastly, it tries to find rule with no group and no artifact.

When the rule is found, it can configure for a dependency the following things:
* `versioning` (string) <br/>
Overrides what kind of versioning schema is used for the dependency.
Default: `semver`. Allowed values: `loose`, `semver`, `regex:...`.
* `pin` (string) <br/>
Filters versions that are allowed for the dependency.
It can be an exact version, prefix or regular expression
Bazel steward will try to automaticly determine what kind of input it is.
You can override this by prepending the value with `prefix:`, `exact:` or `regex:`.
* `bumping` (string) <br/>
Sets the strategy for bumping this dependency.
  1. `latest` - Bump to the latest version
  2. `default` - First bump to the latest patch, then to the latest minor, then finally to the latest major.
  3. `minor` - Bump to the latest minor


# Installation

You can download Bazel Steward jar and run it locally or use the GitHub Action bellow.

## GitHub Actions
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
* set `github-personal-token` to your personal token (recommended, your token will be used only for closing/reopening pull requests). 
Required permissions: read and write on pull requests.
* set `github-token` to your personal token (every operation will be made in your name)
* create GitHub app and use its token for `github-token` (the best option, requires a bit of setup)

Read more about triggering workflows using personal tokens and setting up GitHub app [here](https://github.com/peter-evans/create-pull-request/blob/main/docs/concepts-guidelines.md#triggering-further-workflow-runs). 

Read more about personal tokens [here](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token).

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

## A badge for your repos
A badge is available to show that Bazel Steward is helping your repos.

[![Bazel Steward badge](https://img.shields.io/badge/Bazel_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,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)](https://github.com/VirtusLab/bazel-steward)

```markdown
[![Bazel Steward badge](https://img.shields.io/badge/Bazel_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,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)](https://github.com/VirtusLab/bazel-steward)
```

# Contributing

Want to contribute? Look [here](CONTRIBUTING.md)!
