package org.jake;

import java.util.Set;

import org.jake.JakePlugins.JakePlugin;
import org.jake.java.build.JakeJavaBuildPlugin;
import org.jake.java.build.JakeUnitPlugin;
import org.junit.Assert;
import org.junit.Test;

public class JakePluginsTest {

	@Test
	public void testPluginsLoading() {
		final JakePlugins<DummyPlugin> plugins = JakePlugins.of(DummyPlugin.class);
		final Set<JakePlugin<DummyPlugin>> pluginSet = plugins.plugins();
		Assert.assertEquals(1, pluginSet.size());

		Assert.assertEquals(0, JakePlugins.of(JakeJavaBuildPlugin.class).plugins().size());
		Assert.assertEquals(1, JakePlugins.of(JakeUnitPlugin.class).plugins().size());
	}

}
