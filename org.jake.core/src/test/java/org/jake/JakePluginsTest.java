package org.jake;

import java.util.Set;

import org.jake.PluginDictionnary.JakePluginDescription;
import org.junit.Assert;
import org.junit.Test;

public class JakePluginsTest {

	@Test
	public void testPluginsLoading() {
		final PluginDictionnary<PluginBaseMy> plugins = PluginDictionnary.of(PluginBaseMy.class);
		final Set<JakePluginDescription<PluginBaseMy>> pluginSet = plugins.getAll();
		Assert.assertEquals(1, pluginSet.size());
	}

	interface PluginBase {}

	private static class PluginBaseMy implements PluginBase {

	}

}
