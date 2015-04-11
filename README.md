Jerkar
======

Built system ala Ant, Maven or Gradle but using Java code only to describe builds.
It leads in a powerfull tool yet quite simple to learn and use (launch and debug build 'script' as a regular java class).

Notably Jerkar provides :
* Powerfull dependency management (back-ended by Ivy so compatible with Maven repository)
* Multi-project support
* Powerfull fluent API to manipulate files, perform  compilations/tests, package archives and all build related stuff
* Choice between free form builds (ala Ant) or structured build definitions (ala Maven).   
* Hierarchical log output tracing execution time for each intermediate steps

The documentation is at its very early stage but the code is yet pretty close to completion for a first release. 
I mainly need help for further testing, write documentation and polish the API... and giving some feedback of course.

Example : Let's see how Jerkar build itself
--
The build contains directives for injecting timestamp in the Manifest file and for creating a full distribution containing jars, sources, configuration file and Windows executable.

The build class is as follow :

    public class CoreBuild extends JkJavaBuild {

	    public File distripZipFile; // The zip file that will contain the whole distrib

	    public File distribFolder;  // The folder that will contain the whole distrib

	    @Override
	    protected void init() {
	        distripZipFile = ouputDir("jake-distrib.zip");
            distribFolder = ouputDir("jake-distrib");
		    this.fatJar = true;
        }

	    // Just to run directly the whole build bypassing the Jake bootstrap mechanism.
	    // Was necessary in first place to build Jake with itself.
	    public static void main(String[] args) {
		    new CoreBuild().base();
	    }

	    // Interpolize resource files replacing ${version} by a timestamp
	    @Override
	    protected JkResourceProcessor resourceProcessor() {
		    return super.resourceProcessor().with("version", version().name() + " - built at - " +                   buildTimestamp());
	    }

	    // Include the making of the distribution into the application packaging.
	    @Override
	    public void pack() {
		    super.pack();
		    distrib();
	    }

        // Create a distribution of Jerkar core, including jars, sources and windows/linux launch scripts
	    private void distrib() {
		    final JkDir distribDir = JkDir.of(distribFolder);
		    JkLog.startln("Creating distrib " + distripZipFile.getPath());
		    final JkJavaPacker packer = packer();
		    distribDir.copyInDirContent(baseDir("src/main/dist"));
		    distribDir.importFiles(packer.jarFile(), packer.fatJarFile());
		    distribDir.sub("libs/required").copyInDirContent(baseDir("build/libs/compile"));
		    distribDir.sub("libs/sources").copyInDirContent(baseDir("build/libs-sources")).importFiles(packer.jarSourceFile());
		distribDir.zip().to(distripZipFile, Deflater.BEST_COMPRESSION);
		JkLog.done();
	    }
	}

To launch the build for creating distrib from the command line, simply type : 

    jerkar

This will interpole resources, compile, unit tests, create jar and package the full distrib in zip file. When no method specified, Jerkar invoke the `doDefault` method.

To launch a SonarQube analisys along test coverage : 

    jerkar doDefault jacoco# sonar#verify
    
This will compile, unit test with test coverage and launch a sonar analysis with sonar user settings. 
`jacoco#` means that the Jacoco plugin will be activated while the junit test will be running and `sonar#verify` means that Jerkar will invoke a method called `verify`in the sonar plugin class.
    
    
    
    