package org.jerkar.api.depmanagement;

import org.jerkar.api.java.JkClassLoader;

public final class InternalIvy {

	public static final JkClassLoader CLASSLOADER = JkClassLoader.current().sibling(
			InternalIvy.class.getResource("ivy-2.4.0.jar"));

	public InternalIvy() {
		// not instance
	}

}
