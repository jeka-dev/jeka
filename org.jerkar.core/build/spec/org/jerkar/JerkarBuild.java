package org.jerkar;

import org.jerkar.java.build.JkJavaBuild;

public abstract class JerkarBuild extends JkJavaBuild {

	@Override
	public String sourceJavaVersion() {
		return JkJavaCompiler.V6;
	}

	@Override
	public void pack() {
		super.pack();
		this.javadocMaker().process();
	}

}
