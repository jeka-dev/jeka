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
        Assert.assertEquals(4, pluginSet.size());
    }

    interface PluginBase {
    }

    @SuppressWarnings("unused")
    private static class PluginBaseMy implements PluginBase {

    }

}
