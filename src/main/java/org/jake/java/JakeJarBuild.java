package org.jake.java;

import java.io.File;
import java.util.zip.ZipOutputStream;

import org.jake.Notifier;
import org.jake.Zip;

public class JakeJarBuild extends JakeJavaBuild {
	
	protected String jarName() {
		return projectName() + versionSuffix();
	}
	
	protected int zipLevel() {
		return ZipOutputStream.DEFLATED;
	}
	
	public void jar() {
		Notifier.start("Creating jar file(s)");
		File zip = buildOuputDir().file(jarName() + ".jar");
		Zip.of(classDir()).create(zip, zipLevel());
		sourceDirs().and(resourceDirs()).zip(buildOuputDir().file(jarName() + "-sources.jar"), zipLevel());
		Zip.of(testClassDir()).create(buildOuputDir().file(jarName() + "-test.jar"), zipLevel());
		testSourceDirs().and(testResourceDirs()).zip(buildOuputDir().file(jarName() + "-test-sources.jar"), zipLevel());
		Notifier.done(zip.getPath() + " created");
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
