package org.jerkar.api.depmanagement;

import org.jerkar.api.java.JkClassLoader;

public final class IvyClassloader {

	public static final JkClassLoader CLASSLOADER = JkClassLoader.current().sibling(
			IvyClassloader.class.getResource("ivy-2.4.0.jar"));

	public IvyClassloader() {
		// not instance
	}

}
