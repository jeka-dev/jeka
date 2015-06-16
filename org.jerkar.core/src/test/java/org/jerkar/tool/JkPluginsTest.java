package org.jerkar.tool;

import java.util.Set;

import org.jerkar.tool.PluginDictionnary;
import org.jerkar.tool.PluginDictionnary.JkPluginDescription;
import org.junit.Assert;
import org.junit.Test;

public class JkPluginsTest {

	@Test
	public void testPluginsLoading() {
		final PluginDictionnary<PluginBase> plugins = PluginDictionnary.of(PluginBase.class);
		final Set<JkPluginDescription<PluginBase>> pluginSet = plugins.getAll();
		Assert.assertEquals(1, pluginSet.size());
	}

	interface PluginBase {}

	@SuppressWarnings("unused")
	private static class PluginBaseMy implements PluginBase {

	}

}
