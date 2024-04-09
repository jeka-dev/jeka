# Nexus (repo) Plugin for JeKa

Plugin to auto-release when using [Nexus](https://www.sonatype.com/products/sonatype-nexus-repository) repo for publication.

This plugin contains a [KBean](src/dev/jeka/plugins/jacoco/JacocoKBean.java) to auto-configure *MavenKBean*.

It also contains utilities class to configure projects programmatically.

## Configure using Kean

```properties
jeka.inject.classpath=dev.jeka:nexus-plugin


# Optional settings. Execute `jeka jacoco#help` to see available options.
@nexus=
@jacoco.jacocoVersion=0.8.7
```

## Use Programmatically

You can use directly `JkNexusRepos` class for lower-level access.



