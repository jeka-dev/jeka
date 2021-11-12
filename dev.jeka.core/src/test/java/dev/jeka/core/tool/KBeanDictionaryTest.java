package dev.jeka.core.tool;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

@SuppressWarnings("javadoc")
public class KBeanDictionaryTest {

    @Test
    public void testPluginsLoading() {
        final KBeanDictionary dictionary = new KBeanDictionary();
        final Set<KBeanDictionary.KBeanDescription> kBeans = dictionary.getAll();

        Assert.assertTrue(kBeans.toString(), kBeans.size() > 6);
    }

    interface PluginBase {
    }

    @SuppressWarnings("unused")
    private static class PluginBaseMy implements PluginBase {

    }

}
