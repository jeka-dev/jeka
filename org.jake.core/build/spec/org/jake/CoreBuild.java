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

	public File distripZipFile() {
		return ouputDir("jake-distrib.zip");
	}

	// Just to run directly the whole build bypassing the Jake bootstrap mechanism.
	// Was necessary in first place to build Jake with itself.
	public static void main(String[] args) {
		new CoreBuild().base();
	}

	// Include a time stamped version file as resource.
	@Override
	protected JakeResourceProcessor resourceProcessor() {
		return JakeResourceProcessor.of(resourceDirs(), "version", version().name());
	}

	@Override
	protected JakeJavaPacker createPacker() {
		return super.createPacker().withFatJar(true).withFullName(true);
	}

	// Include the making of the distribution into the application packaging.
	@Override
	public void pack() {
		super.pack();
		distrib2();
	}

	private void distrib2() {
		final JakeDir distribDir = baseDir().sub("build/output/jake-distrib");
		final File distripZipFile = ouputDir("jake-distrib.zip");
		JakeLog.startln("Creating distrib " + distripZipFile().getPath());
		final JakeJavaPacker packer = packer();
		distribDir.copyDirContent(distFolder());
		distribDir.copyFiles(packer.jarFile(), packer.fatJarFile());
		distribDir.sub("libs/required").copyDirContent(baseDir("build/libs/compile"));
		distribDir.sub("libs/sources").copyDirContent(baseDir("build/libs-sources")).copyFiles(packer.jarSourceFile());
		distribDir.zip().to(distripZipFile, Deflater.BEST_COMPRESSION);
		JakeLog.done();
	}

	public File distFolder() {
		return baseDir("src/main/dist");
	}

}
