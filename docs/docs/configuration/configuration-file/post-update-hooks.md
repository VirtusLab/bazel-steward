---
layout: default
title: Maven Config
parent: Configuration File
grand_parent: Configuration
nav_order: 4
---

# Maven Config
Settings specific to rules_jvm_external, i.e. maven dependencies resolution.


```yaml
maven:
  repository-name: maven
```

Available fields:
  * `repository-name` (string) <br/>
    Name of repository where maven dependencies appear, by default it is "maven", i.e. dependencies will be queried
    under `@maven//...`
  