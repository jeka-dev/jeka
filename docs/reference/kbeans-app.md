# App KBean

Manages applications by installing them from their source repositories.  
Includes security and version update management.

## Summary

Provides a way to install, update, or remove applications from the user PATH.

nApplications are installed from a Git repository and built by the client before installation.
Applications can be installed as executable JARs or native apps.

|Field  |Description  |Type  |
|-------|-------------|------|
|remote|Specifies the URL of the remote Git repository used to install the app.t can be written as `https://.../my-app#[tag-name]` to fetch a specific tag.|class java.lang.String|
|name|Specifies the app name.|class java.lang.String|


|Mathod  |Description  |
|--------|-------------|
|examples|Display some example on the console that you can play with.|
|install|Build and install the app to make it available in PATH. 
Use 'remote=[Git URL]' to set the source repository.
Use 'native:' argument to install as a native app.|
|list|Lists installed Jeka commands in the user's PATH.|
|trustUrl|Add permanently the url to trusted list.
The urls starting with the specified prefix will be automatically trusted.
Use 'name=my.host/my.path/' to specify the prefix.|
|uninstall|Uninstalls an app from the user's PATH.
Use `name=[app-name]` to specify the app.|
|update|Update an app from the given PATH.
Use `name=[app-name]` to specify the app.|


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



