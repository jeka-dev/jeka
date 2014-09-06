package org.jake.java;

import java.io.File;
import java.util.zip.Deflater;

import org.jake.JakeDoc;
import org.jake.JakeLog;
import org.jake.file.JakeDirSet;
import org.jake.file.JakeZip;
import org.jake.file.utils.JakeUtilsFile;

public class JakeBuildJar extends JakeBuildJava implements JakeJarModule {

	protected String jarName() {
		return projectName() + versionSuffix();
	}

	protected int zipLevel() {
		return Deflater.DEFAULT_COMPRESSION;
	}

	@Override
	public File jarFile() {
		return ouputDir(jarName() + ".jar");
	}

	@Override
	public File jarSourceFile() {
		return ouputDir(jarName() + "-sources.jar");
	}

	@Override
	public File jarTestFile() {
		return ouputDir(jarName() + "-test.jar");
	}

	@Override
	public File jarTestSourceFile() {
		return ouputDir(jarName() + "-test-sources.jar");
	}

	public File fatJarFile() {
		return ouputDir(jarName() + "-fat.jar");
	}

	@JakeDoc({	"Create many jar files containing respectively binaries, sources, test binaries and test sources.",
	"The jar containing the binary is the one that will be used as a depe,dence for other project."})
	public void pack() {
		JakeLog.startAndNextLine("Packaging module");
		makeZip(JakeDirSet.of(classDir()), jarFile());
		makeZip(sourceDirs().and(resourceDirs()), jarSourceFile());
		if (!skipTests) {
			makeZip(JakeDirSet.of(testClassDir()), jarTestFile());
		}
		makeZip(testSourceDirs().and(testResourceDirs()), jarTestSourceFile());
		afterPackJars();
		JakeLog.done();
	}

	protected final void makeZip(JakeDirSet dirSet, File dest) {
		JakeLog.start("Creating file : " + dest.getPath());
		dirSet.zip(dest, zipLevel());
		JakeLog.done();
	}

	/**
	 * Override this method if you want to add some extra distribution files.
	 */
	protected void afterPackJars() {
		// Do nothing by default.
	}

	@JakeDoc("Create jar file containing the binaries for itself all its dependencies.")
	public void fatJar() {
		JakeLog.info("Creating file : " + fatJarFile().getPath());
		JakeZip.of(classDir()).merge(deps().runtime()).create(fatJarFile(), zipLevel());
	}

	@JakeDoc("Do clean, compile, unit test and then make jar files.")
	@Override
	public void base() {
		super.base();
		pack();
	}

	@JakeDoc("Create MD5 check sum for both regular and fat jar files.")
	public void checksum() {
		final File file = ouputDir(jarName() + ".md5");
		JakeLog.info("Creating file : " + file);
		JakeUtilsFile.writeString(file, JakeUtilsFile.md5Checksum(jarFile()), false);
		if (fatJarFile().exists()) {
			final File fatSum = ouputDir(jarName() + "-fat" + ".md5");
			JakeLog.info("Creating file : " + fatSum);
			JakeUtilsFile.writeString(fatSum, JakeUtilsFile.md5Checksum(fatJarFile()), false);
		}
	}

	public static void main(String[] args) {
		new JakeBuildJar().base();
	}

}
