package org.jerkar.tool;

import java.util.Set;

import org.jerkar.tool.PluginDictionary.PluginDescription;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkPluginsTest {

    @Test
    public void testPluginsLoading() {
        final PluginDictionary plugins = new PluginDictionary();
        final Set<PluginDescription> pluginSet = plugins.getAll();

        // 6 or 8 depending of the classpath where tests are executed
        Assert.assertTrue(pluginSet.toString(), 6 == pluginSet.size() || 8 == pluginSet.size());
    }

    interface PluginBase {
    }

    @SuppressWarnings("unused")
    private static class PluginBaseMy implements PluginBase {

    }

}
