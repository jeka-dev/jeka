package org.jake.java;

import java.io.File;

import org.jake.depmanagement.JakeDependencyResolver;

/**
 * Defines methods that a Build class must define in order to make this module reusable by
 * other projects.
 * 
 * @author Djeang
 */
public interface JakeJarModule {

	/**
	 * Returns the jar file containing the binaries. This jar will be the one upon dependent project
	 * will compile. At the end of the build this file must exist.
	 */
	File jarFile();

	/**
	 * Optional jar containing java sources. This method can return <code>null</code> or an non
	 * existing file.
	 */
	File jarSourceFile();

	/**
	 * Optional jar containing test binaries. Tests from dependent project may rely on this
	 * jar to compile and run. This method can return <code>null</code> or a non existing file.
	 */
	File jarTestFile();

	/**
	 * Optional jar containing java test sources. This method can return <code>null</code> or a non existing file.
	 */
	File jarTestSourceFile();

	/**
	 * Returns the dependency resolver for this module.
	 */
	JakeDependencyResolver deps();

}
