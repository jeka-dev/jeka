package org.jake;

import java.util.Set;

import org.jake.JakePlugins.JakePluginDescription;
import org.jake.java.build.JakeJavaBuildPlugin;
import org.junit.Assert;
import org.junit.Test;

public class JakePluginsTest {

	@Test
	public void testPluginsLoading() {
		final JakePlugins<DummyPlugin> plugins = JakePlugins.of(DummyPlugin.class);
		final Set<JakePluginDescription<DummyPlugin>> pluginSet = plugins.getAll();
		Assert.assertEquals(1, pluginSet.size());

		Assert.assertEquals(1, JakePlugins.of(JakeJavaBuildPlugin.class).getAll().size());

	}

}
