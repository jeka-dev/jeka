package org.jerkar.builtins.javabuild;

import org.jerkar.JkBuild;
import org.jerkar.JkBuildPlugin;
import org.jerkar.builtins.javabuild.testing.junit.JkUnit;
import org.jerkar.file.JkFileTreeSet;

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
	protected JkFileTreeSet alterSourceDirs(JkFileTreeSet original) {
		return original;
	}

	/**
	 * Override this method if the plugin need to alter the test source directory to use for compiling.
	 * 
	 * @see JkJavaBuild#testSourceDirs()
	 */
	protected JkFileTreeSet alterTestSourceDirs(JkFileTreeSet original) {
		return original;
	}

	/**
	 * Override this method if the plugin need to alter the resource directory to use for compiling.
	 * 
	 * @see JkJavaBuild#resourceDirs()
	 */
	protected JkFileTreeSet alterResourceDirs(JkFileTreeSet original) {
		return original;
	}

	/**
	 * Override this method if the plugin need to alter the test resource directory to use for compiling.
	 * 
	 * @see JkJavaBuild#testResourceDirs()
	 */
	protected JkFileTreeSet alterTestResourceDirs(JkFileTreeSet original) {
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

	static JkFileTreeSet applySourceDirs(Iterable<? extends JkBuildPlugin> plugins, JkFileTreeSet original) {
		JkFileTreeSet result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = ((JkJavaBuildPlugin) plugin).alterSourceDirs(result);
		}
		return result;
	}

	static JkFileTreeSet applyTestSourceDirs(Iterable<? extends JkBuildPlugin> plugins, JkFileTreeSet original) {
		JkFileTreeSet result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = ((JkJavaBuildPlugin) plugin).alterTestSourceDirs(result);
		}
		return result;
	}

	static JkFileTreeSet applyResourceDirs(Iterable<? extends JkBuildPlugin> plugins, JkFileTreeSet original) {
		JkFileTreeSet result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = ((JkJavaBuildPlugin) plugin).alterResourceDirs(result);
		}
		return result;
	}

	static JkFileTreeSet applyTestResourceDirs(Iterable<? extends JkBuildPlugin> plugins, JkFileTreeSet original) {
		JkFileTreeSet result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = ((JkJavaBuildPlugin) plugin).alterTestResourceDirs(result);
		}
		return result;
	}



}
