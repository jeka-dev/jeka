package org.jake.java;

import java.io.File;
import java.util.zip.ZipOutputStream;

public class JakeJarBuild extends JakeJavaBuild {
	
	protected String jarName() {
		return projectName() + versionSuffix() + ".jar";
	}
	
	protected int zipLevel() {
		return ZipOutputStream.DEFLATED;
	}
	
	public void jar() {
		logger().info("Creating jar file(s)");
		File zip = buildOuputDir().file(jarName());
		classDir().asZip(zip, zipLevel());
		logger().info(zip.getPath() + " created");
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
