package org.jake.java;

import java.io.File;
import java.util.zip.ZipOutputStream;

import org.jake.DirView;
import org.jake.Notifier;

public class JakeJarBuild extends JakeJavaBuild {
	
	protected String jarName() {
		return projectName() + versionSuffix() + ".jar";
	}
	
	protected int zipLevel() {
		return ZipOutputStream.DEFLATED;
	}
	
	public void jar() {
		Notifier.start("Creating jar file(s)");
		File zip = buildOuputDir().file(jarName());
		DirView.of(classDir()).asZip(zip, zipLevel());
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
