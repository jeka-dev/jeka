# Setup KBean

<!-- header-autogen-doc -->

## Creates a customized sef-contained app

Thanks to *jpackage* and *jlink* tools, this KBean creates self-contained Java applications or installer tailored 
for the host system.

To create such a bundle, execute: ```jeka bundle: pack```.

The application or installer will be created in _[project dir]/jeka-output_ dir.

If you want to create this bundle along other artifacts while executing ```jeka project: pack```, 
you need to specify the following property in *jeka.properties*.
```properties
@bundle.projectPack=true
```

<!-- body-autogen-doc -->


