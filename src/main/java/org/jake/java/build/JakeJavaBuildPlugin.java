package org.jake.java.build;

import org.jake.java.testing.junit.JakeUnit;


public abstract class JakeJavaBuildPlugin {

	public abstract void configure(JakeJavaBuild build);

	public JakeUnit enhance(JakeUnit jakeUnit) {
		return jakeUnit;
	}

	public JakeJavaPacker enhance(JakeJavaPacker packer) {
		return packer;
	}

}
