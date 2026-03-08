# Bundle KBean

<!-- header-autogen-doc -->

## Creates a customized self-contained app

Thanks to *jpackage* and *jlink* tools, this KBean creates self-contained Java applications or installers tailored
for the host system.

To create such a bundle, execute: `jeka bundle: pack`.

The application or installer will be created in _[project dir]/jeka-output_ dir.

If you want to create this bundle along other artifacts while executing `jeka project: pack`,
you need to specify the following property in `jeka.properties`:
```properties
@bundle.projectPack=true
```

## Configuration Options

### Properties

- **`customJre`** - If `true`, creates a custom JRE containing only the Java modules used by the application, using *jlink*. This reduces bundle size.

- **`projectPack`** - If `true`, the bundled application is created automatically when executing `jeka project: pack`.

- **`includeRuntimeLibs`** - If `true` (default), runtime dependencies are included in the packaged application. Set to `false` when using fat JARs.

### JPackage Options

Configure *jpackage* using the `@bundle.jpackage.options` prefix in `jeka.properties`:

```properties
# Common options for all platforms
@bundle.jpackage.options.all=--name=MyApp --vendor="My Company" --description="My Application"

# Windows-specific options
@bundle.jpackage.options.windows=--icon=media/icon.ico --type=msi

# Linux-specific options
@bundle.jpackage.options.linux=--icon=media/icon.png --type=deb

# macOS-specific options
@bundle.jpackage.options.mac=--icon=media/icon.icns --type=dmg
```

**Suggested options:**
- `--name` - Application name
- `--vendor` - Vendor name
- `--description` - Application description
- `--icon` - Application icon (platform-specific format)
- `--type` - Package type (`app-image`, `exe`, `msi`, `deb`, `rpm`, `pkg`, `dmg`)
- `--add-modules` - Additional JPMS modules to include (or use `jdeps:compute` to auto-detect)
- `--java-options` - JVM options for the application
- `--runtime-image` - Path to custom JRE

### JLink Options

When using `customJre=true`, configure *jlink* using the `@bundle.jlink.options` prefix:

```properties
# Common jlink options
@bundle.jlink.options.all=--compress=2 --bind-services

# Platform-specific options
@bundle.jlink.options.windows=--launcher=myapp
```

**Suggested options:**
- `--add-modules` - Modules to include in custom JRE
- `--bind-services` - Include service provider modules
- `--compress` - Compression level (0=none, 1=constant strings, 2=ZIP)
- `--disable-plugin` - Disable specific plugins
- `--limit-module` - Limit universe of observable modules

## Advanced Usage

### Automatic Module Detection

Use `jdeps:compute` to automatically detect required JPMS modules:

```properties
@bundle.jpackage.options.all=--add-modules=jdeps:compute
```

This analyzes the main JAR and its dependencies to determine which modules are needed.

### Programmatic Configuration

In your KBean code, you can configure bundle options programmatically:

```java
@JkInject
BundleKBean bundleKBean;

@Override
protected void init() {
    bundleKBean.customJre = true;
    bundleKBean.includeRuntimeLibs = false;
    bundleKBean.addJpackageOptions("--vendor", "My Company");
    bundleKBean.addJlinkOptions("--compress", "2");
}
```

## Available Methods

- **`bundle: pack`** - Creates the bundled application or installer
- **`bundle: computeModuleDeps`** - Prints JPMS module dependencies detected by *jdeps*
- **`bundle: jpackageHelp`** - Displays *jpackage* help
- **`bundle: jlinkHelp`** - Displays *jlink* help

## Summary

<!-- body-autogen-doc -->


