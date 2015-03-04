package org.jake.java.build;

import org.jake.JakeBuildPlugin;
import org.jake.java.testing.junit.JakeUnit;


public abstract class JakeJavaBuildPlugin extends JakeBuildPlugin {

	/**
	 * Override this method if the plugin need to alter the JakeUnit instance that run tests.
	 */
	public JakeUnit enhance(JakeUnit jakeUnit) {
		return jakeUnit;
	}

	/**
	 * Override this method if the plugin need to alter the packer instance that package the project
	 * into jar files.
	 */
	public JakeJavaPacker enhance(JakeJavaPacker packer) {
		return packer;
	}

	static JakeJavaPacker apply(Iterable<JakeJavaBuildPlugin> plugins, JakeJavaPacker original) {
		JakeJavaPacker result = original;
		for (final JakeJavaBuildPlugin plugin : plugins) {
			result = plugin.enhance(result);
		}
		return result;
	}

	static JakeUnit apply(Iterable<JakeJavaBuildPlugin> plugins, JakeUnit original) {
		JakeUnit result = original;
		for (final JakeJavaBuildPlugin plugin : plugins) {
			result = plugin.enhance(result);
		}
		return result;
	}

}
