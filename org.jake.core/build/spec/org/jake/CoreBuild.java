package org.jake;

import java.io.File;
import java.util.zip.Deflater;

import org.jake.java.JakeResourceProcessor;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.build.JakeJavaPacker;

/**
 * Build class for Jake itself.
 * This build does not rely on any dependence manager.
 */
public class CoreBuild extends JakeJavaBuild {

	public File distripZipFile;

	public File distribFolder;

	@Override
	protected void init() {
		distripZipFile = ouputDir("jake-distrib.zip");
		distribFolder = ouputDir("jake-distrib");
	}

	// Just to run directly the whole build bypassing the Jake bootstrap mechanism.
	// Was necessary in first place to build Jake with itself.
	public static void main(String[] args) {
		new CoreBuild().base();
	}

	// Include a time stamped version file as resource.
	@Override
	protected JakeResourceProcessor resourceProcessor() {
		return JakeResourceProcessor.of(resourceDirs(), "version", version().name() + " - built at - " + buildTimestamp());
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

	private void distrib() {
		final JakeDir distribDir = JakeDir.of(distribFolder);
		JakeLog.startln("Creating distrib " + distripZipFile.getPath());
		final JakeJavaPacker packer = packer();
		distribDir.copyDirContent(baseDir("src/main/dist"));
		distribDir.copyFiles(packer.jarFile(), packer.fatJarFile());
		distribDir.sub("libs/required").copyDirContent(baseDir("build/libs/compile"));
		distribDir.sub("libs/sources").copyDirContent(baseDir("build/libs-sources")).copyFiles(packer.jarSourceFile());
		distribDir.zip().to(distripZipFile, Deflater.BEST_COMPRESSION);
		JakeLog.done();
	}


}
