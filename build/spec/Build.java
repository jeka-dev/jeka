
import java.io.File;
import java.util.zip.Deflater;

import org.jake.JakeDoc;
import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.java.build.JakeBuildJava;
import org.jake.java.build.JakeJarPacker;
import org.jake.utils.JakeUtilsFile;

public class Build extends JakeBuildJava {


	// Just to run directly the whole build bypassing the bootstrap mechanism.
	// (Was needed in first place to build Jake with itself).
	public static void main(String[] args) {
		JakeOptions.forceVerbose(true);
		new Build().base();
	}

	@Override
	public void base() {
		super.base();
		pack();
	}

	// Include a time stamped version file as resource
	@Override
	protected void generateResources() {
		final File versionFile = new File(generatedResourceDir(),"org/jake/version.txt");
		JakeUtilsFile.writeString(versionFile, version(), false);
	}

	@Override
	public void pack() {
		super.pack();
		distrib();
	}

	// Create the whole distribution
	@JakeDoc("Make the whole distribution.")
	public void distrib() {
		final File distribDir = ouputDir("jake-distrib");
		final File distripZipFile = ouputDir("jake-distrib.zip");

		JakeLog.start("Creating distrib " + distripZipFile.getPath());
		JakeUtilsFile.copyDir(baseDir("src/main/dist"), distribDir, null, true);
		final JakeJarPacker jarPacker = jarPacker();
		JakeUtilsFile.copyFile(jarPacker.jarFile(), new File(distribDir,"jake.jar"));
		JakeUtilsFile.copyFile(jarPacker.jarSourceFile(), new File(distribDir,"jake-sources.jar"));
		JakeUtilsFile.zipDir(distripZipFile, Deflater.BEST_COMPRESSION, distribDir);
		JakeLog.done();
	}

}
