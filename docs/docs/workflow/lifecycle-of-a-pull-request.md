---
layout: default
title: Lifecycle of a pull request
parent: Workflow
nav_order: 1
---

# Lifecycle of a Pull Request

Pull requests created by Bazel Steward are managed by it until they are merged or closed, but there is an exception to that.
If you push any changes to a branch created by Bazel Steward, it will stop managing the associated pull request.
Otherwise, Bazel Steward will do the following things:
* When an even newer version of the library appears, it will open a new pull request for the newer version and close pull requests for older versions
* When a pull request is no longer mergeable (has conflicts), it will force push the branch with the same version
* When a pull request is closed/merged, it will never create a new pull request for the same version