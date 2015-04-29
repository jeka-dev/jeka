## Example : Let's see how Jerkar core build itself

Jerkar core build is not complex but do some specific stuff : apart making standard compilation, junit tests and jar packaging, it constructs a distribution archive gathering jars, sources, property, readme files and executable.
This is the build class :

```java
    public class CoreBuild extends JkJavaBuild {

	    public File distripZipFile; // The zip file that will contain the whole distrib

	    public File distribFolder;  // The folder that will contain the whole distrib

	    @Override
	    protected void init() {
	        distripZipFile = ouputDir("jerkar-distrib.zip");
            distribFolder = ouputDir("jerkar-distrib");
		    this.fatJar = true;
        }

	    // Interpolize resource files replacing ${version} by a timestamp (in the Manifest)
	    @Override
	    protected JkResourceProcessor resourceProcessor() {
		    return super.resourceProcessor().with("version", version().name() + " - built at - " + buildTimestamp());
	    }

	    // Include the making of the distribution into the application packaging.
	    @Override
	    public void pack() {
		    super.pack();
		    distrib();
	    }

        // Create a distribution of Jerkar core, including jars, sources and windows/linux launch scripts
	    private void distrib() {
		    final JkDir distrib = JkDir.of(distribFolder);
		    JkLog.startln("Creating distrib " + distripZipFile.getPath());
		    final JkJavaPacker packer = packer();
		    distrib.importDirContent(baseDir("src/main/dist"));
		    distrib.importFiles(packer.jarFile(), packer.fatJarFile());
		    distrib.sub("libs/required").importDirContent(baseDir("build/libs/compile"));
		    distrib.sub("libs/sources").importDirContent(baseDir("build/libs-sources"))
		                                  .importFiles(packer.jarSourceFile());
			distrib.zip().to(distripZipFile, Deflater.BEST_COMPRESSION);
			JkLog.done();
	    }
	}
```

Notice that we need only to specify what is not 'standard'
* group and project name are inferred from the project folder name ('org.jerkar.core' so group is 'org.jerkar' and project is 'core')
* version is not specified, so by default it is `1.0-SNAPSHOT`(unless you inject the version via the command line using `-forcedVersion=Xxxxx`)
* sources, resources and tests folder are located on the conventional folders (same as Maven).
* this build class relies on local dependencies (dependencies located conventionally inside the project) so we don't need to mention them

A dependency managed flavor of this build is [CoreDepManagedBuild.java](https://github.com/jerkar/jerkar/blob/master/org.jerkar.core/build/spec/org/jerkar/CoreDepManagedBuild.java)

To launch the build for creating distrib from the command line, simply type : 

    jerkar

This will interpole resources (replacing ${version} by a timestamp everywhere), compile, run unit tests, create jars and package the distrib in zip file. 
This command is equivalent to `jerkar doDefault` : when no method specified, Jerkar invoke the `doDefault` method. Build result is *output* folder : 

![image of project layout](https://github.com/jerkar/jerkar/blob/master/doc/project-layout.png)

---
To launch a SonarQube analysis along test coverage and producing javadoc: 

    jerkar clean compile unitTest sonar#verify javadoc jacoco# -verbose=true
    
This will compile, unit test with test coverage, launch a sonar analysis with sonar user settings and finally produce the javadoc. 
- `clean`, `compile`, `unitTest` and `javadoc` are build methods inherited by `CoreBuild`
- `sonar#verify` means that Jerkar will invoke a method called `verify`in the `sonar` plugin class
- `jacoco#` means that the `jacoco` plugin will be activated while the junit test will be running
- `-verbose=true`means that the log will display verbose information ('-' prefix is the way to pass parameter in Jerkar)


Notice that Jacoco test coverage and SonarQube analysis are triggered without mention in the build class ! 


    