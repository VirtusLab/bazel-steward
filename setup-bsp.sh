#!/usr/bin/env bash
cs launch -r m2Local org.jetbrains.bsp:bazel-bsp:2.5.1 -M org.jetbrains.bsp.bazel.install.Install -- --bazel-workspace . --directory ../bazel-steward-bsp
