package org.jake.java.build;

import org.jake.JakePlugins;
import org.jake.java.testing.junit.JakeUnit;

public abstract class JakeUnitPlugin {

	public abstract void configure(JakeJavaBuild build);

	public abstract JakeUnit.Enhancer enhancer();

	public static JakeUnit enhanceWithAll(JakeUnit jakeUnit, JakePlugins<JakeUnitPlugin> plugins) {
		JakeUnit result = jakeUnit;
		for (final JakeUnitPlugin jakeUnitPlugin : plugins) {
			result = result.enhancedWith(jakeUnitPlugin.enhancer());
		}
		return result;
	}

}
