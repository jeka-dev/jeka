# Maven KBean

<!-- header-autogen-doc -->


[`MavenKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/tooling/maven/MavenKBean.java) provides the ability to publish artifacts to a 
Maven repository. The artifacts are those produced by the [`project`](kbeans-project.md) or [`base` KBeans](kbeans-base.md). 
It also provides convenient means to migrate from Maven projects.

**Key Features:**

- Publish artifacts produced by projects to local or remote repositories.
- Display information about publication, especially transitive dependencies published along with the artifacts.
- Property or programmatic configuration for published POM metadata and dependencies.
- Property or programmatic configuration for the publication repository.

## Publish

A publication needs a module ID and a version. Declare them on the [`project`](kbeans-project.md) or [`base`](kbeans-base.md) KBean:
```properties title="jeka.properties"
@project.moduleId=org.example:my-lib
@project.version=1.0.0
```
Instead of hardcoding the version, you can infer it from Git with `@project.gitVersioning.enable=true`. 
To publish with other values than those of the project, use `@maven.pub.moduleId` and `@maven.pub.version`.

Check what will be published, without publishing:
```shell
jeka maven: info
```

The JAR, sources, javadoc and POM are published. The JAR must exist, so run `pack` in the same command.

### Publish Locally

```shell
jeka project: pack maven: publishLocal      # in the JeKa local repository (~/.jeka/maven-publish-dir)
jeka project: pack maven: publishLocalM2    # in the Maven local repository, as 'mvn install'
```

A version already present in the target repository is not overwritten: the command fails, unless it is a *SNAPSHOT*.

### Publish to a Remote Repository

Set the repository in *jeka.properties* or *global.properties*. Values can reference environment variables:
```properties title="jeka.properties"
jeka.repos.publish=https://my.company/repo
jeka.repos.publish.username=${MY_REPO_USER}
jeka.repos.publish.password=${MY_REPO_PASSWORD}
```
```shell
jeka project: pack maven: publish
```
Without `jeka.repos.publish`, `publish` publishes to the JeKa local repository. 
See [Repositories](properties.md#repositories) for HTTP headers, aliases and multiple repositories.

### Publish to Maven Central

Use the [Central Portal plugin](https://github.com/jeka-dev/jeka/tree/master/plugins/plugins.centralportal), which signs and uploads the artifacts. 
Maven Central requires metadata, provided as follows:
```properties title="jeka.properties"
jeka.classpath=dev.jeka:centralportal-plugin

@maven.pub.metadata.projectName=My Library
@maven.pub.metadata.projectDescription=A library doing something useful
@maven.pub.metadata.projectUrl=https://github.com/my-org/my-lib
@maven.pub.metadata.projectScmUrl=https://github.com/my-org/my-lib.git
@maven.pub.metadata.licenses=Apache License V2.0=https://www.apache.org/licenses/LICENSE-2.0.html
@maven.pub.metadata.developers=jdoe:john.doe@example.org
```
```shell
jeka project: pack centralportal: publish
```
Credentials and signing key are read from environment variables. See the plugin documentation.

### Publish Additional Artifacts

```properties title="jeka.properties"
# Create an extra shaded JAR, and publish it along an extra zip file
@project.pack.shadeJarClassifier=all
@maven.pub.extraArtifacts=all, doc.zip
```
Files are expected in *jeka-output*, named after the main JAR (e.g. `org.example.my-lib-all.jar`, `org.example.my-lib-doc.zip`).

### Publish a BOM

In a [multi-module project](multi-modules.md), the parent can publish a BOM listing the modules that contain a `project` KBean:
```properties title="jeka.properties of the parent directory"
_jeka.child-bases=core, plugins/*
_@project=off

@maven=on
@maven.pub.version=2.0.0

_@maven.pub.moduleId=org.example:bom
_@maven.pub.parentBom=true
```
```shell
jeka project: pack maven: publish
```
Properties prefixed with `_` apply to the parent only. Each child must declare its own `@project.moduleId`.

### Customize the Publication in Java

```java title="jeka-src/Custom.java"
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

class Custom extends KBean {

    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {
        mavenKBean.getPublication()
                .customizeDependencies(deps -> deps   // published dependencies
                        .withTransitivity("com.google.code.gson:gson", JkTransitivity.COMPILE)
                        .minus("org.postgresql:postgresql"))
                .pomMetadata
                        .setProjectName("My library")
                        .addLicense("Apache License V2.0", "https://www.apache.org/licenses/LICENSE-2.0.html");
    }
}
```
See also the [publication API](api-dependency-management.md#publication).

## Run Maven

Run any Maven command in the current directory, using the Maven wrapper (`mvnw`) if present, and the JDK running JeKa:
```shell
jeka maven: wrap args="clean verify -Pmy-profile"
```

Build with Maven and copy the content of the *target* directory to *jeka-output*, so the application can be run or installed by JeKa:
```shell
jeka maven: wrapPackage
jeka maven: wrapPackage appModule=app args="-Pnative"
```
`wrapPackage` runs `clean package` without tests. In multi-module Maven projects, `appModule` is the module containing the application.
See [Source-Runnable Applications](../tutorials/source-runnable-apps.md#maven-projects).

## Migrate from Maven

From a directory containing a *pom.xml* file, display its dependencies as Java code and as *jeka.project.deps* content:
```shell
jeka maven: migrateDeps
```
See [Migrate a Maven Project](../tutorials/migrate-from-maven.md) for all the steps.

## Summary

<!-- body-autogen-doc -->