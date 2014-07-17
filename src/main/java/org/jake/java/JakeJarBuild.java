package org.jake.java;

import java.io.File;
import java.util.zip.Deflater;

import org.jake.JakeLogger;
import org.jake.file.JakeZip;

public class JakeJarBuild extends JakeJavaBuild {

	protected String jarName() {
		return projectName() + versionSuffix();
	}

	protected int zipLevel() {
		return Deflater.DEFAULT_COMPRESSION;
	}

	public void jar() {
		JakeLogger.start("Packaging as jar");
		final JakeZip base = JakeZip.of(classDir());
		base.create(buildOuputDir(jarName() + ".jar"), zipLevel());
		JakeZip.of(sourceDirs(), resourceDirs()).create(buildOuputDir(jarName() + "-sources.jar"), zipLevel());
		JakeZip.of(testClassDir()).create(buildOuputDir(jarName() + "-test.jar"), zipLevel());
		JakeZip.of(testSourceDirs(), testResourceDirs()).create(buildOuputDir(jarName() + "-test-sources.jar"), zipLevel());

		// Create a fat jar if runtime dependencies are defined on
		if (!dependencyPath().runtime().isEmpty()) {
			final File fatJarFile = buildOuputDir(jarName() + "-fat.jar");
			base.merge(dependencyPath().runtime()).create(fatJarFile, zipLevel());
		}

		JakeLogger.done();
	}

	@Override
	public void doDefault() {
		super.doDefault();
		jar();
	}

	public static void main(String[] args) {
		new JakeJarBuild().doDefault();
	}

}
