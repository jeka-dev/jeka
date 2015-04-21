package org.jerkar;

import java.io.File;
import java.util.zip.Deflater;

import org.jerkar.java.JkResourceProcessor;
import org.jerkar.java.build.JkJavaPacker;

/**
 * Build class for Jerkar itself.
 * This build does not rely on any dependence manager.
 */
public class CoreBuild extends JerkarBuild {

	public File distripZipFile;

	public File distribFolder;

	@Override
	protected void init() {
		distripZipFile = ouputDir("jerkar-distrib.zip");
		distribFolder = ouputDir("jerkar-distrib");
		this.fatJar = true;
	}

	// Just to run directly the whole build bypassing the Jerkar bootstrap mechanism.
	// Was necessary in first place to build Jerkar with itself.
	public static void main(String[] args) {
		new CoreBuild().doDefault();
	}

	// Include a time stamped version file as resource.
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

	private void distrib() {
		final JkDir distribDir = JkDir.of(distribFolder);
		JkLog.startln("Creating distrib " + distripZipFile.getPath());
		final JkJavaPacker packer = packer();
		distribDir.importDirContent(baseDir("src/main/dist"));

		// Simpler to put both Jerkar and Jerkar-fat jar at the root (in order to find the Jerker HOME)
		distribDir.importFiles(packer.jarFile(), packer.fatJarFile());
		distribDir.sub("libs/required").importDirContent(baseDir("build/libs/compile"));
		distribDir.sub("libs-sources").importDirContent(baseDir("build/libs-sources"))
		.importFiles(packer.jarSourceFile());
		distribDir.sub("libs-javadoc").importFiles(this.javadocMaker().zipFile());
		distribDir.zip().to(distripZipFile, Deflater.BEST_COMPRESSION);
		JkLog.done();
	}


}
