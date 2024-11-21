# Developing JeKa Extensions
JeKa extensions encompass Java classes that rely on JeKa code and can include any number of KBeans. They primarily serve the purpose of integrating external technologies. However, they can also be comprised of Helper classes, or a specific KBean for a build template.

## Organising Your Code
Keeping all code in *jeka-src* simplifies both setup and testing due to shared classpaths. Here's how to structure your packages:

- `_dev` package: This houses all elements that are not required in the plugin package (such as build, tests classes and resources).
- `org.example` package: Rename this to fit your needs. It will contain the extension's production code.
- `_dev.samples` package: This can store test code for your extension development.

For adding dependencies, utilize the `@JkDep` annotation. Any annotations declared under the `_dev` package will not be included in the transitive dependencies of your final extension.

## Versioning Your Extension

### Setting a Compatible Version Range
You can define a version range your extension is compatible with JeKa. While you might know the minimum JeKa version at the time of your extension's creation, predicting future JeKa versions that could break compatibility is impossible. Store this information in a specific external location mentioned in the build class.

### Version Numbering for Your Extension
We suggest the versioning scheme `xx.xx.xx-y` for your extension, where `xx.xx.xx` corresponds to the minimum JeKa version and `y` signifies the iteration. # Developing JeKa Extensions
JeKa extensions encompass Java classes that rely on JeKa code and can include any number of KBeans. They primarily serve the purpose of integrating external technologies. However, they can also be comprised of Helper classes, or a specific KBean for a build template.

## Organising Your Code
Keeping all code in *jeka-src* simplifies both setup and testing due to shared classpaths. Here's how to structure your packages:

- `_dev` package: This houses all elements that are not required in the plugin package (such as build, tests classes and resources).
- `org.example` package: Rename this to fit your needs. It will contain the extension's production code.
- `_dev.samples` package: This can store test code for your extension development.

For adding dependencies, utilize the `@JkDep` annotation. Any annotations declared under the `_dev` package will not be included in the transitive dependencies of your final extension.

## Versioning Your Extension

### Setting a Compatible Version Range
You can define a version range your extension is compatible with JeKa. While you might know the minimum JeKa version at the time of your extension's creation, predicting future JeKa versions that could break compatibility is impossible. Store this information in a specific external location mentioned in the build class.

### Version Numbering for Your Extension
We strongly suggest using the versioning scheme xx.xx.xx-y for your extension. In this scheme, xx.xx.xx represents the minimum required JeKa version, while y indicates the iteration of your extension.

## Examples
- A plugin for integrating *OpenAPI* tech: https://github.com/jeka-dev/openapi-plugin
- An extension offering templates for building Springboot-ReactJS with quality metrics: https://github.com/jeka-dev/demo-build-templates
