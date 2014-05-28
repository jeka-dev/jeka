package org.jake.java;

import java.util.zip.Deflater;

import org.jake.Notifier;
import org.jake.Zip;

public class JakeJarBuild extends JakeJavaBuild {
	
	protected String jarName() {
		return projectName() + versionSuffix();
	}
	
	protected int zipLevel() {
		return Deflater.DEFAULT_COMPRESSION;
	}
	
	public void jar() {
		Notifier.start("Packaging");
		Zip.of(classDir()).create(buildOuputDir().file(jarName() + ".jar"), zipLevel());
		Zip.of(sourceDirs(), resourceDirs()).create(buildOuputDir().file(jarName() + "-sources.jar"), zipLevel());
		Zip.of(testClassDir()).create(buildOuputDir().file(jarName() + "-test.jar"), zipLevel());
		Zip.of(testSourceDirs(), testResourceDirs()).create(buildOuputDir().file(jarName() + "-test-sources.jar"), zipLevel());
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
