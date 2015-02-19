package org.jake.java.build;

import org.jake.java.testing.junit.JakeUnit;


public abstract class JakeJavaBuildPlugin {

	public abstract void configure(JakeJavaBuild build);

	public JakeUnit enhance(JakeUnit jakeUnit) {
		return jakeUnit;
	}

	public JakeJavaPacker enhance(JakeJavaPacker packer) {
		return packer;
	}

	public static JakeJavaPacker apply(Iterable<JakeJavaBuildPlugin> plugins, JakeJavaPacker original) {
		JakeJavaPacker result = original;
		for (final JakeJavaBuildPlugin plugin : plugins) {
			result = plugin.enhance(result);
		}
		return result;
	}

	public static JakeUnit apply(Iterable<JakeJavaBuildPlugin> plugins, JakeUnit original) {
		JakeUnit result = original;
		for (final JakeJavaBuildPlugin plugin : plugins) {
			result = plugin.enhance(result);
		}
		return result;
	}

}
