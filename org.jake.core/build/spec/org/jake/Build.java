package org.jake;

import java.io.File;
import java.util.zip.Deflater;

import org.jake.java.JakeResourceProcessor;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.build.JakeJavaPacker;
import org.jake.utils.JakeUtilsFile;

/**
 * Build class for Jake itself.
 * This build does not rely on any dependence manager.
 */
public class Build extends JakeJavaBuild {

	public File distripZipFile() {
		return ouputDir("jake-distrib.zip");
	}

	// Just to run directly the whole build bypassing the Jake bootstrap mechanism.
	// Was necessary in first place to build Jake with itself.
	public static void main(String[] args) {
		new Build().base();
	}

	// Include a time stamped version file as resource.
	@Override
	protected JakeResourceProcessor resourceProcessor() {
		return JakeResourceProcessor.of(resourceDirs(), "version", version().name());
	}

	@Override
	protected JakeJavaPacker createPacker() {
		return super.createPacker().withFatJar(true);
	}


	// Include the making of the distribution into the application packaging.
	@Override
	public void pack() {
		super.pack();
		distrib();
	}

	// Create the whole distribution : creates distrib directory and zip containing all
	private void distrib() {
		final File distribDir = ouputDir("jake-distrib");
		//final File distripZipFile = ouputDir("jake-distrib.zip");

		JakeLog.start("Creating distrib " + distripZipFile().getPath());
		JakeUtilsFile.copyDir(baseDir("src/main/dist"), distribDir, null, true);
		final JakeJavaPacker jarPacker = packer();
		JakeUtilsFile.copyFile(jarPacker.jarFile(), new File(distribDir,"jake.jar"));
		JakeUtilsFile.copyFile(jarPacker.jarSourceFile(), new File(distribDir,"jake-sources.jar"));
		JakeDir.of(this.baseDir("build/libs/compile")).include("**/*.jar").copyTo(new File(distribDir, "libs/required"));
		JakeDir.of(this.baseDir("build/libs-sources")).copyTo(new File(distribDir, "libs/sources"));
		JakeUtilsFile.zipDir(distripZipFile(), Deflater.BEST_COMPRESSION, distribDir);
		JakeLog.done();
	}

}
