package org.jake;

import java.util.Set;

import org.jake.PluginDictionnary.JakePluginDescription;
import org.jake.java.build.JakeJavaBuildPlugin;
import org.junit.Assert;
import org.junit.Test;

public class JakePluginsTest {

	@Test
	public void testPluginsLoading() {
		final PluginDictionnary<DummyPlugin> plugins = PluginDictionnary.of(DummyPlugin.class);
		final Set<JakePluginDescription<DummyPlugin>> pluginSet = plugins.getAll();
		Assert.assertEquals(1, pluginSet.size());

		Assert.assertEquals(1, PluginDictionnary.of(JakeJavaBuildPlugin.class).getAll().size());

	}

}
