package org.jerkar.builtins.javabuild.build;

import org.jerkar.JkBuild;
import org.jerkar.JkBuildPlugin;
import org.jerkar.JkDirSet;
import org.jerkar.builtins.javabuild.testing.junit.JkUnit;

/**
 * Class to extend to create plugin for {@link JkJavaBuild}.
 * 
 * @author Jerome Angibaud
 */
public abstract class JkJavaBuildPlugin extends JkBuildPlugin {

	@Override
	public Class<? extends JkBuild> baseBuildClass() {
		return JkJavaBuild.class;
	}

	/**
	 * Override this method if the plugin need to alter the JkUnit instance that run tests.
	 * 
	 * @see JkJavaBuild#unitTester()
	 */
	protected JkUnit alterUnitTester(JkUnit jkUnit) {
		return jkUnit;
	}

	/**
	 * Override this method if the plugin need to alter the packer instance that package the project
	 * into jar files.
	 * 
	 * @see JkJavaBuild#packer()
	 */
	protected JkJavaPacker alterPacker(JkJavaPacker packer) {
		return packer;
	}

	/**
	 * Override this method if the plugin need to alter the source directory to use for compiling.
	 * 
	 * @see JkJavaBuild#sourceDirs()
	 */
	protected JkDirSet alterSourceDirs(JkDirSet original) {
		return original;
	}

	/**
	 * Override this method if the plugin need to alter the test source directory to use for compiling.
	 * 
	 * @see JkJavaBuild#testSourceDirs()
	 */
	protected JkDirSet alterTestSourceDirs(JkDirSet original) {
		return original;
	}

	/**
	 * Override this method if the plugin need to alter the resource directory to use for compiling.
	 * 
	 * @see JkJavaBuild#resourceDirs()
	 */
	protected JkDirSet alterResourceDirs(JkDirSet original) {
		return original;
	}

	/**
	 * Override this method if the plugin need to alter the test resource directory to use for compiling.
	 * 
	 * @see JkJavaBuild#testResourceDirs()
	 */
	protected JkDirSet alterTestResourceDirs(JkDirSet original) {
		return original;
	}

	static JkJavaPacker applyPacker(Iterable<? extends JkBuildPlugin> plugins, JkJavaPacker original) {
		JkJavaPacker result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = ((JkJavaBuildPlugin) plugin).alterPacker(result);
		}
		return result;
	}

	static JkUnit applyUnitTester(Iterable<? extends JkBuildPlugin> plugins, JkUnit original) {
		JkUnit result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = ((JkJavaBuildPlugin) plugin).alterUnitTester(result);
		}
		return result;
	}

	static JkDirSet applySourceDirs(Iterable<? extends JkBuildPlugin> plugins, JkDirSet original) {
		JkDirSet result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = ((JkJavaBuildPlugin) plugin).alterSourceDirs(result);
		}
		return result;
	}

	static JkDirSet applyTestSourceDirs(Iterable<? extends JkBuildPlugin> plugins, JkDirSet original) {
		JkDirSet result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = ((JkJavaBuildPlugin) plugin).alterTestSourceDirs(result);
		}
		return result;
	}

	static JkDirSet applyResourceDirs(Iterable<? extends JkBuildPlugin> plugins, JkDirSet original) {
		JkDirSet result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = ((JkJavaBuildPlugin) plugin).alterResourceDirs(result);
		}
		return result;
	}

	static JkDirSet applyTestResourceDirs(Iterable<? extends JkBuildPlugin> plugins, JkDirSet original) {
		JkDirSet result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = ((JkJavaBuildPlugin) plugin).alterTestResourceDirs(result);
		}
		return result;
	}



}
