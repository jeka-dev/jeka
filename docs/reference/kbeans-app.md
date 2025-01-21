# App KBean

Manages applications by installing them from their source repositories.  
Includes security and version update management.

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



