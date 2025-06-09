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

## Catalog

Display the registered catalogs of applications:
```shell
jeka app: catalog
```

Display application registered in a given catalog:
```shell
jeka app:  name=demo
```


## Security

Trusted URL prefixes are stored in the `jeka.apps.url.trusted` property, located in the *~/.jeka/global.properties* file.  
You can adjust this property later to make it more or less restrictive.  
The check validates the start of the Git URL after removing the protocol and identifier part.

Example: `jeka.apps.url.trusted=github.com/djeang/` will trust urls formed as:

  - https://github.com/djeang/xxx...    
  - https://my-user-name@github.com/djeang/xxx...
  - git@github.com/djeang/xxx..
  - git@github.com:my-user-name/djeang/xxx..
  - ...

<!-- body-autogen-doc -->


