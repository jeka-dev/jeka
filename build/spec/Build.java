
import java.io.File;

import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeBuildJar;
import org.jake.utils.JakeUtilsTime;

public class Build extends JakeBuildJar {

	// Just to run directly the whole build bypassing the bootstrap mechanism.
	// (Was needed in first place to build Jake with itself).
	public static void main(String[] args) {
		JakeOptions.forceVerbose(true);
		new Build().base();
	}

	// Include a time stamped version file as resource
	@Override
	protected void generateResources() {
		final File versionFile = new File(generatedResourceDir(),"org/jake/version.txt");
		JakeUtilsFile.writeString(versionFile, JakeUtilsTime.timestampSec(), false);
	}

	// Create the whole distribution
	@Override
	protected void afterPackJars() {
		final File distribDir = ouputDir("jake-distrib");
		final File distripZipFile = ouputDir("jake-distrib.zip");

		JakeLog.start("Creating distrib " + distripZipFile.getPath());
		JakeUtilsFile.copyDir(baseDir("src/main/dist"), distribDir, null, true);
		final File jarFile = ouputDir(jarName()+".jar");
		JakeUtilsFile.copyFileToDir(jarFile, distribDir);
		JakeUtilsFile.copyFileToDir(ouputDir(jarName() + "-sources.jar"), distribDir);
		JakeUtilsFile.zipDir(distripZipFile, zipLevel(), distribDir);
		JakeLog.done();
	}

}
