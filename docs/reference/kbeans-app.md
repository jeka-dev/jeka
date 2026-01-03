# App KBean

<!-- header-autogen-doc -->

## Install Applications

```shell
jeka app: install repo=<repo-url>
```

Install an application in native mode
```shell
jeka app: install repo=<repo-url> native:
```

Example:
```shell
jeka app: install repo=https://github.com/djeang/kill8 name=kill8 native:
```

## List Installed Applications

```shell
jeka app: list
```
This displays the the name of the applications, along their repo, version/status, and runtime type.
```
App Name   │ Repo                                      │ Version  │ Status     │ Runtime │ 
calculator │ https://github.com/djeang/Calculator-jeka │ latest   │ up-to-date │ jvm     │ 
kill8      │ https://github.com/djeang/kill8           │ <master> │ up-to-date │ native  │ 
```

## Update Applications

```shell
jeka app: update name=kill8
```
Updates the application to the highest semantic version tag.
If a 'latest' tag exists, uses that tag.
If no tags exist, uses the latest commit.

## Uninstall Applications

```shell
jeka app: uninstall name=kill8
```

## Catalogs

Display the registered catalogs of applications:
```shell
jeka app: catalog
```

Display application registered in a given catalog:
```shell
jeka app: name=demo 
```

To add a catalog, add the following lines to your `~/.jeka/global.properties` file.
```properties
catalog.xxx.repo=[http location or git repo url]
catalog.xxx.desc=[Description of the catalog]
```
where `xxxx` stands for the name of your catalog.

`catalog.xxx.repo` can value a url pointing at the catalog file, or the url of the *Github* repo containing the catalog file.

Both examples are valid:
```properties
# Inside the `demo' repo of the 'jeka-dev' Github organization. 
# The jeka-catalog.properties file is expected at the repo root.
catalog.demo.repo=jeka-dev/demo

# Inside the `jeka-catalog' repo of the 'djeang' Github organization.
# The jeka-catalog.properties file is expected at the repo root.
catalog.djeang.repo=djeang

# Direct pointing at the url.
catalog.foo.repo=https://raw.githubusercontent.com/jeka-dev/jeka-catalog/refs/heads/main/jeka-catalog.properties
```

Catalog file example:
```properties
app.cowsay.repo=https://github.com/jeka-dev/demo-cowsay
app.cowsay.desc=Java port or the Cowsay famous CLI.
app.cowsay.type=CLI

app.demo-springboot-angular.repo=https://github.com/jeka-dev/demo-project-springboot-angular
app.demo-springboot-angular.desc=Manage a list of users. Written in Springboot and Angular
app.demo-springboot-angular.type=SERVER-UI
```


## Security

Trusted URL prefixes are stored in the `jeka.apps.url.trusted` property, located in the *~/.jeka/global.properties* file.  
You can adjust this property later to make it more or less restrictive.  
The check validates the start of the Git URL after removing the protocol and identifier part.

Example: `jeka.app.url.trusted=github.com/djeang/` will trust urls formed as:

  - https://github.com/djeang/xxx...    
  - https://my-user-name@github.com/djeang/xxx...
  - git@github.com/djeang/xxx..
  - git@github.com:my-user-name/djeang/xxx..
  - ...

## Summary

<!-- body-autogen-doc -->


