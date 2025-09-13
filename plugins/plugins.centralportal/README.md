# Central Portal Plugin for JeKa

This plugin allows to publish project artefacts on Maven Central via [Central Portal](https://central.sonatype.com/)

## Usage

Execute:
```shell
jeka project: build centralportal: publish
```
The credentials and signing keys are expected to be found in the following environment variables:

- `JEKA_CENTRAL_PORTAL_USERNAME`: The username to connect to *Central Portal*.
- `JEKA_CENTRAL_PORTAL_PASSWORD`: The password to connect to *Central Portal*.
- `JEKA_CENTRAL_PORTAL_SIGN_KEY`: The armored GPG key to sign published artifacts.
- `JEKA_CENTRAL_PORTAL_SIGN_KEY_PASSPHRASE`: The passphrase for the below armored GPG key

The signing key is a string that can be extracted using following command line:
```shell
gpg --armor --export-secret-keys MY_KEY
```
The string key should contain *-----BEGIN PGP PRIVATE KEY BLOCK-----* header and *-----END PGP PRIVATE KEY BLOCK-----* footer.

## Configuration

Only Maven metadata for the published artifacts are required.

Example:
```properties
jeka.classpath=dev.jeka:centralportal-plugin

@maven.pub.metadata.projectName=Vincer-Dom
@maven.pub.metadata.projectDescription=Modern Dom manipulation library for Java
@maven.pub.metadata.projectUrl=https://github.com/djeang/vincer-dom
@maven.pub.metadata.projectScmUrl=https://github.com/djeang/vincer-dom.git
@maven.pub.metadata.licenses=Apache License V2.0:https://www.apache.org/licenses/LICENSE-2.0.html
@maven.pub.metadata.developers=djeang:djeangdev@yahoo.fr
```

Some extra properties can be specified:
```properties
@centralportal.automatic=false
@centralportal.timeout=90000
```

## Programmatic Usage

You can use directly `CentralportalKBean`, `JkCentralPortalBundle` and `JkCentralPortaPublisher` classes 
for lower-level access.

Source Code: [Visit here](src/dev/jeka/plugins/centralportal/CentralportalKBean.java)

## Generated Documentation

Plugin for publishing artifacts to Maven Central.

Run `jeka centralportal:publish` to publish artifacts.
Plugin for publishing artifacts to Maven Central.

Run `jeka centralportal:publish` to publish artifacts.


**This KBean post-initializes the following KBeans:**

|Post-initialised KBean   |Description  |
|-------|-------------|
|MavenKBean |Undocumented. |


**This KBean exposes the following fields:**

|Field  |Description  |
|-------|-------------|
|username [String] |The token user name to connect to Central Portal. |
|password [String] |The token password to connect to Central Portal. |
|signingKey [String] |The armored GPG key to sign published artifacts. |
|signingKeyPassphrase [String] |The passphrase of the armored GPG key. |
|automatic [boolean] |If true, the bundle will be automatically deployed to Maven Central without manual intervention. |
|timeout [int] |Wait time in seconds for successful publication. |


**This KBean exposes the following methods:**

|Method  |Description  |
|--------|-------------|
|publish |Publishes artifacts to Maven Central. |
