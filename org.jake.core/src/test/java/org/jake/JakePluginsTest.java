package org.jake;

import java.util.Set;

import org.jake.PluginDictionnary.JakePluginDescription;
import org.junit.Assert;
import org.junit.Test;

public class JakePluginsTest {

	@Test
	public void testPluginsLoading() {
		final PluginDictionnary<PluginBase> plugins = PluginDictionnary.of(PluginBase.class);
		final Set<JakePluginDescription<PluginBase>> pluginSet = plugins.getAll();
		Assert.assertEquals(1, pluginSet.size());
	}

	interface PluginBase {}

	@SuppressWarnings("unused")
	private static class PluginBaseMy implements PluginBase {

	}

}
