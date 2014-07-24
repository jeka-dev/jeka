package org.jake.java;

import java.io.File;
import java.util.zip.Deflater;

import org.jake.JakeLogger;
import org.jake.file.JakeZip;

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

	public void jar() {
		JakeLogger.startAndNextLine("Packaging as jar");
		final JakeZip base = JakeZip.of(classDir());
		JakeLogger.info("Creating file : " + jarFile().getPath());
		base.create(jarFile(), zipLevel());
		JakeLogger.info("Creating file : " + jarSourceFile().getPath());
		JakeZip.of(sourceDirs(), resourceDirs()).create(jarSourceFile(), zipLevel());
		JakeLogger.info("Creating file : " + jarTestFile().getPath());
		JakeZip.of(testClassDir()).create(jarTestFile(), zipLevel());
		JakeLogger.info("Creating file : " + jarTestSourceFile().getPath());
		JakeZip.of(testSourceDirs(), testResourceDirs()).create(jarTestSourceFile(), zipLevel());

		JakeLogger.done();
	}

	public void fatJar() {
		final File fatJarFile = buildOuputDir(jarName() + "-fat.jar");
		JakeLogger.info("Creating file : " + fatJarFile.getPath());
		JakeZip.of(classDir()).merge(dependencyPath().runtime()).create(fatJarFile, zipLevel());
	}

	@Override
	public void doDefault() {
		super.doDefault();
		jar();
	}

	public static void main(String[] args) {
		new JakeBuildJar().doDefault();
	}

}
