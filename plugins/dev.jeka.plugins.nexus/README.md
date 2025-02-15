# Nexus (repo) Plugin for JeKa

Plugin to auto-release when using [Nexus](https://www.sonatype.com/products/sonatype-nexus-repository) repo for publication.

It also contains utility classes to configure projects programmatically.

Resources:
  - Command-line documentation: `jeka nexus: --doc`.
  - Source Code: [Visit here](src/dev/jeka/plugins/nexus/NexusKBean.java)

## Initialization

This plugin resisters a post-publication action in *MavenKBean*, if present.
The action sends a *"close"* message to the repositories and waits until it is processed.

## Configuration

No configuration is required, nevertheless we can filter on specific profiles.

```properties
jeka.classpath=dev.jeka:nexus-plugin
@nexus=

# Optional properties
@nexus.profileNamesFilter=
@nexus.closeTimeout=600
```

## Programmatic Usage

You can use directly `JkNexusRepos` class for lower-level access.



