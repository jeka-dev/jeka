package org.jake.java;

import java.io.File;
import java.util.zip.Deflater;

import org.jake.JakeDoc;
import org.jake.JakeLog;
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
		return buildOuputDir(jarName() + ".jar");
	}

	@Override
	public File jarSourceFile() {
		return buildOuputDir(jarName() + "-sources.jar");
	}

	@Override
	public File jarTestFile() {
		return buildOuputDir(jarName() + "-test.jar");
	}

	@Override
	public File jarTestSourceFile() {
		return buildOuputDir(jarName() + "-test-sources.jar");
	}


	public File fatJarFile() {
		return buildOuputDir(jarName() + "-fat.jar");
	}

	@JakeDoc({	"Create jar file containing the binaries, along others containing sources, test binaries and test sources.",
		"The jar containing the binary is the one that will be used as a depe,dence for other project.",
	"This method has to be invoked after compile, tests and recourses processed."})
	public void jar() {
		JakeLog.startAndNextLine("Packaging as jar");
		final JakeZip base = JakeZip.of(classDir());
		JakeLog.info("Creating file : " + jarFile().getPath());
		base.create(jarFile(), zipLevel());
		JakeLog.info("Creating file : " + jarSourceFile().getPath());
		JakeZip.of(sourceDirs(), resourceDirs()).create(jarSourceFile(), zipLevel());
		if (!skipTests) {
			JakeLog.info("Creating file : " + jarTestFile().getPath());
			JakeZip.of(testClassDir()).create(jarTestFile(), zipLevel());
		}
		JakeLog.info("Creating file : " + jarTestSourceFile().getPath());
		JakeZip.of(testSourceDirs(), testResourceDirs()).create(jarTestSourceFile(), zipLevel());

		JakeLog.done();
	}

	@JakeDoc("Create jar file containing the binaries for itself and all its compile and runtime dependencies.")
	public void fatJar() {
		JakeLog.info("Creating file : " + fatJarFile().getPath());
		JakeZip.of(classDir()).merge(dependencyResolver().runtime()).create(fatJarFile(), zipLevel());
	}

	@JakeDoc("Do clean, compile, test, process resources and then make jar.")
	@Override
	public void base() {
		super.base();
		jar();
	}

	@JakeDoc("Create MD5 check sum for both regular and fat jar files.")
	public void checksum() {
		final File file = buildOuputDir(jarName() + ".md5");
		JakeLog.info("Creating file : " + file);
		JakeUtilsFile.writeString(file, JakeUtilsFile.createChecksum(jarFile()), false);
		if (fatJarFile().exists()) {
			final File fatSum = buildOuputDir(jarName() + "-fat" + ".md5");
			JakeLog.info("Creating file : " + fatSum);
			JakeUtilsFile.writeString(fatSum, JakeUtilsFile.createChecksum(fatJarFile()), false);
		}
	}

	public static void main(String[] args) {
		new JakeBuildJar().base();
	}

}
