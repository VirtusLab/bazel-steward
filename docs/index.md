---
layout: home
title: "Bazel Steward: keep your Bazel dependencies up to date"
nav_exclude: true
---

# **Bazel Steward**

## What it is
A bot that helps you keep your library dependencies, Bazel and Bazel rules up-to-date. 
It runs on your CI such as GitHub Actions.

## How it works

Bazel Steward scans your repository, looking for outside dependencies.

Afterwards, it compares the version of each found dependency against the latest version in its upstream repository.

If a newer version is available in the upstream, Bazel Steward opens a pull request in your repository, with a proposed change for that newer version.