package org.jerkar.api.depmanagement;

import org.jerkar.api.java.JkClassLoader;

public final class IvyClassloader {

	public static final JkClassLoader CLASSLOADER = classloader();

	private IvyClassloader() {
		// no instance
	}


	private static final JkClassLoader classloader() {
		if (JkClassLoader.current().isDefined("org.apache.ivy.Ivy")) {
			return JkClassLoader.current();
		}
		return JkClassLoader.current().sibling(
				IvyClassloader.class.getResource("ivy-2.4.0.jar"));
	}


}
