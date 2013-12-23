package org.javake.java;

import java.io.File;

public class JarProjectBuilder extends JavaProjectBuilder {
	
	protected String jarName() {
		return projectName() + versionSuffix() + ".jar";
	}
	
	protected int zipLevel() {
		return 0;
	}
	
	public void jar() {
		File zip = buildOuputDir().file(jarName());
		classDir().asZip(zip, zipLevel());
	}
	
	@Override
	public void doDefault() {
		super.doDefault();
		jar();
	}
	
	public static void main(String[] args) {
		new JarProjectBuilder().doDefault();
	}

}
