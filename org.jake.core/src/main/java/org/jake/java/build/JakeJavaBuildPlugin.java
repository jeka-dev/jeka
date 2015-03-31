package org.jake.java.build;

import org.jake.JakeBuild;
import org.jake.JakeBuildPlugin;
import org.jake.JakeDirSet;
import org.jake.java.testing.junit.JakeUnit;

/**
 * Class to extend to create plugin for {@link JakeJavaBuild}.
 * 
 * @author Jerome Angibaud
 */
public abstract class JakeJavaBuildPlugin extends JakeBuildPlugin {

	@Override
	public Class<? extends JakeBuild> baseBuildClass() {
		return JakeJavaBuild.class;
	}

	/**
	 * Override this method if the plugin need to alter the JakeUnit instance that run tests.
	 * 
	 * @see JakeJavaBuild#unitTester()
	 */
	protected JakeUnit alterUnitTester(JakeUnit jakeUnit) {
		return jakeUnit;
	}

	/**
	 * Override this method if the plugin need to alter the packer instance that package the project
	 * into jar files.
	 * 
	 * @see JakeJavaBuild#packer()
	 */
	protected JakeJavaPacker alterPacker(JakeJavaPacker packer) {
		return packer;
	}

	/**
	 * Override this method if the plugin need to alter the source directory to use for compiling.
	 * 
	 * @see JakeJavaBuild#sourceDirs()
	 */
	protected JakeDirSet alterSourceDirs(JakeDirSet original) {
		return original;
	}

	/**
	 * Override this method if the plugin need to alter the test source directory to use for compiling.
	 * 
	 * @see JakeJavaBuild#testSourceDirs()
	 */
	protected JakeDirSet alterTestSourceDirs(JakeDirSet original) {
		return original;
	}

	/**
	 * Override this method if the plugin need to alter the resource directory to use for compiling.
	 * 
	 * @see JakeJavaBuild#resourceDirs()
	 */
	protected JakeDirSet alterResourceDirs(JakeDirSet original) {
		return original;
	}

	/**
	 * Override this method if the plugin need to alter the test resource directory to use for compiling.
	 * 
	 * @see JakeJavaBuild#testResourceDirs()
	 */
	protected JakeDirSet alterTestResourceDirs(JakeDirSet original) {
		return original;
	}




	static JakeJavaPacker applyPacker(Iterable<? extends JakeBuildPlugin> plugins, JakeJavaPacker original) {
		JakeJavaPacker result = original;
		for (final JakeBuildPlugin plugin : plugins) {
			result = ((JakeJavaBuildPlugin) plugin).alterPacker(result);
		}
		return result;
	}

	static JakeUnit applyUnitTester(Iterable<? extends JakeBuildPlugin> plugins, JakeUnit original) {
		JakeUnit result = original;
		for (final JakeBuildPlugin plugin : plugins) {
			result = ((JakeJavaBuildPlugin) plugin).alterUnitTester(result);
		}
		return result;
	}

	static JakeDirSet applySourceDirs(Iterable<? extends JakeBuildPlugin> plugins, JakeDirSet original) {
		JakeDirSet result = original;
		for (final JakeBuildPlugin plugin : plugins) {
			result = ((JakeJavaBuildPlugin) plugin).alterSourceDirs(result);
		}
		return result;
	}

	static JakeDirSet applyTestSourceDirs(Iterable<? extends JakeBuildPlugin> plugins, JakeDirSet original) {
		JakeDirSet result = original;
		for (final JakeBuildPlugin plugin : plugins) {
			result = ((JakeJavaBuildPlugin) plugin).alterTestSourceDirs(result);
		}
		return result;
	}

	static JakeDirSet applyResourceDirs(Iterable<? extends JakeBuildPlugin> plugins, JakeDirSet original) {
		JakeDirSet result = original;
		for (final JakeBuildPlugin plugin : plugins) {
			result = ((JakeJavaBuildPlugin) plugin).alterResourceDirs(result);
		}
		return result;
	}

	static JakeDirSet applyTestResourceDirs(Iterable<? extends JakeBuildPlugin> plugins, JakeDirSet original) {
		JakeDirSet result = original;
		for (final JakeBuildPlugin plugin : plugins) {
			result = ((JakeJavaBuildPlugin) plugin).alterTestResourceDirs(result);
		}
		return result;
	}

}
