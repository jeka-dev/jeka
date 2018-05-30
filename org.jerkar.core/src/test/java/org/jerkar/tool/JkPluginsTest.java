package org.jerkar.tool;

import java.util.Set;

import org.jerkar.tool.PluginDictionary.JkPluginDescription;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkPluginsTest {

    @Test
    public void testPluginsLoading() {
        final PluginDictionary plugins = new PluginDictionary();
        final Set<JkPluginDescription> pluginSet = plugins.getAll();
        Assert.assertEquals(1, pluginSet.size());
    }

    interface PluginBase {
    }

    @SuppressWarnings("unused")
    private static class PluginBaseMy implements PluginBase {

    }

}
