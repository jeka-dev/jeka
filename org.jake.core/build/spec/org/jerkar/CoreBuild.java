package org.jerkar;

import java.io.File;
import java.util.zip.Deflater;

import org.jerkar.JkDir;
import org.jerkar.JkLog;
import org.jerkar.java.JkResourceProcessor;
import org.jerkar.java.build.JkJavaBuild;
import org.jerkar.java.build.JkJavaPacker;

/**
 * Build class for Jake itself.
 * This build does not rely on any dependence manager.
 */
public class CoreBuild extends JkJavaBuild {

	public File distripZipFile;

	public File distribFolder;

	@Override
	protected void init() {
		distripZipFile = ouputDir("jake-distrib.zip");
		distribFolder = ouputDir("jake-distrib");
		this.fatJar = true;
	}

	// Just to run directly the whole build bypassing the Jake bootstrap mechanism.
	// Was necessary in first place to build Jake with itself.
	public static void main(String[] args) {
		new CoreBuild().doDefault();
	}

	// Include a time stamped version file as resource.
	@Override
	protected JkResourceProcessor resourceProcessor() {
		return super.resourceProcessor().with("version", version().name() + " - built at - " + buildTimestamp());
	}

	@Override
	protected JkJavaPacker createPacker() {
		return super.createPacker();
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
		distribDir.copyInDirContent(baseDir("src/main/dist"));
		distribDir.importFiles(packer.jarFile(), packer.fatJarFile());
		distribDir.sub("libs/required").copyInDirContent(baseDir("build/libs/compile"));
		distribDir.sub("libs/sources").copyInDirContent(baseDir("build/libs-sources")).importFiles(packer.jarSourceFile());
		distribDir.zip().to(distripZipFile, Deflater.BEST_COMPRESSION);
		JkLog.done();
	}


}
