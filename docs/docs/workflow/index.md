---
layout: default
title: Workflow
nav_order: 3
has_children: true
---

# Workflow

Bazel Steward runs these steps:

1. Checkout the base branch
2. For each kind
  1. Resolve dependencies that are used (with versions)
  2. Resolve available versions for these dependencies
  3. Determine update suggestions (select versions to update)
  4. Use heuristics to determine the diff to apply to Bazel build definition
3. Prepare expected pull request suggestions according to the configuration
4. Apply pull request suggestions (open/close/update)