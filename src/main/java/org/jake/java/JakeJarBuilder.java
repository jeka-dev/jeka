package org.jake.java;

import java.io.File;

public class JakeJarBuilder extends JakeJavaBuilder {
	
	protected String jarName() {
		return projectName() + versionSuffix() + ".jar";
	}
	
	protected int zipLevel() {
		return 0;
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
		new JakeJarBuilder().doDefault();
	}

}
