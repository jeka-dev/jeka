package dev.jeka.core.tool;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

@SuppressWarnings("javadoc")
public class BeanDictionaryTest {

    @Test
    public void testPluginsLoading() {
        final BeanDictionary dictionary = new BeanDictionary();
        final Set<BeanDictionary.KBeanDescription> kBeans = dictionary.getAll();

        Assert.assertTrue(kBeans.toString(), kBeans.size() > 6);
    }

    interface PluginBase {
    }

    @SuppressWarnings("unused")
    private static class PluginBaseMy implements PluginBase {

    }

}
