package org.jerkar.internal.ivy;

import org.jerkar.JkClassLoader;

public final class JkInternalIvy {

	public static final JkClassLoader CLASSLOADER = JkClassLoader.current().sibling(
			JkInternalIvy.class.getResource("ivy-2.4.0.jar"));

	public JkInternalIvy() {
		// not instance
	}

}
