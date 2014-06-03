package org.jake.java;

import java.io.File;
import java.util.zip.Deflater;

import org.jake.Notifier;
import org.jake.file.Zip;

public class JakeJarBuild extends JakeJavaBuild {
	
	protected String jarName() {
		return projectName() + versionSuffix();
	}
	
	protected int zipLevel() {
		return Deflater.DEFAULT_COMPRESSION;
	}
	
	public void jar() {
		Notifier.start("Packaging");
		Zip base = Zip.of(classDir());
		base.create(buildOuputDir(jarName() + ".jar"), zipLevel());
		Zip.of(sourceDirs(), resourceDirs()).create(buildOuputDir(jarName() + "-sources.jar"), zipLevel());
		Zip.of(testClassDir()).create(buildOuputDir(jarName() + "-test.jar"), zipLevel());
		Zip.of(testSourceDirs(), testResourceDirs()).create(buildOuputDir(jarName() + "-test-sources.jar"), zipLevel());
		
		// Create a fat jar if runtime dependencies are defined on
		if (!dependenciesPath().runtimeDependencies().isEmpty()) {
			final File fatJarFile = buildOuputDir(jarName() + "-fat.jar");
			base.merge(dependenciesPath().runtimeDependencies()).create(fatJarFile, zipLevel());
		}
		
		Notifier.done();
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
