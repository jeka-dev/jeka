# Nexus (repo) Plugin for JeKa

## Initialization

This plugin resisters a post-publication action in *MavenKBean*, if present.
The action sends a *"close"* message to the repositories and waits until it is processed.

## Configuration

No configuration is required, nevertheless we can filter on specific profiles.

```properties
jeka.classpath=dev.jeka:nexus-plugin
@nexus=on

# Optional properties
@nexus.profileNamesFilter=
@nexus.closeTimeout=600
```

## Programmatic Usage

You can use directly `JkNexusRepos` class for lower-level access.

Source Code: [Visit here](src/dev/jeka/plugins/nexus/NexusKBean.java)


_____________________________
**Auto-Generated Documentation**
_____________________________


**This KBean post-initializes the following KBeans:**

|Post-initialised KBean   |Description  |
|-------|-------------|
|MavenKBean |Wraps Maven publish repo with Nexus autoclose trigger. |


**This KBean exposes the following fields:**

|Field  |Description  |
|-------|-------------|
|profileNamesFilter [String] |Comma separated filters for taking in account only repositories with specified profile names. |
|closeTimeout [int] |Timeout in seconds, before the 'close' operation times out. |


**This KBean exposes the following methods:**

|Method  |Description  |
|--------|-------------|
|closeAndRelease |Closes and releases the nexus repositories used by project KBean to publish artifacts. |






