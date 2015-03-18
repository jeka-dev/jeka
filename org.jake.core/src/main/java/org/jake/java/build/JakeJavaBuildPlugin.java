package org.jake.java.build;

import org.jake.JakeBuild;
import org.jake.JakeBuildPlugin;
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
	 */
	protected JakeUnit enhance(JakeUnit jakeUnit) {
		return jakeUnit;
	}

	/**
	 * Override this method if the plugin need to alter the packer instance that package the project
	 * into jar files.
	 */
	protected JakeJavaPacker enhance(JakeJavaPacker packer) {
		return packer;
	}

	static JakeJavaPacker apply(Iterable<? extends JakeBuildPlugin> plugins, JakeJavaPacker original) {
		JakeJavaPacker result = original;
		for (final JakeBuildPlugin plugin : plugins) {
			result = ((JakeJavaBuildPlugin) plugin).enhance(result);
		}
		return result;
	}

	static JakeUnit apply(Iterable<? extends JakeBuildPlugin> plugins, JakeUnit original) {
		JakeUnit result = original;
		for (final JakeBuildPlugin plugin : plugins) {
			result = ((JakeJavaBuildPlugin) plugin).enhance(result);
		}
		return result;
	}

}
