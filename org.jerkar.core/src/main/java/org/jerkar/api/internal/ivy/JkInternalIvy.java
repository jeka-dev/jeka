package org.jerkar.api.internal.ivy;

import org.jerkar.api.java.JkClassLoader;

public final class JkInternalIvy {

	public static final JkClassLoader CLASSLOADER = JkClassLoader.current().sibling(
			JkInternalIvy.class.getResource("ivy-2.4.0.jar")).copyCurrentOptions();

	public JkInternalIvy() {
		// not instance
	}

}
