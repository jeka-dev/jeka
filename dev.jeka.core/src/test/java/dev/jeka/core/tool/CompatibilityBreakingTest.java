package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkVersion;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

public class CompatibilityBreakingTest {

    @Test
    public void test() {
        InputStream is = CompatibilityBreakingTest.class.getResourceAsStream("compatibilitybreak.txt");
        JkPlugin.CompatibilityBreak compatibilityBreak = JkPlugin.CompatibilityBreak.of(is);
        JkVersion pluginDeclared0 = JkVersion.of("1.2.1.RELEASE");
        JkVersion pluginDeclared1 = JkVersion.of("1.3.0.RELEASE");
        JkVersion jekaDeclared0 = JkVersion.of("0.9.1.RELEASE");
        JkVersion jekaDeclared1 = JkVersion.of("0.9.5.M1");
        String result = compatibilityBreak.getBreakingJekaVersion(pluginDeclared0, jekaDeclared0);
        Assert.assertEquals(jekaDeclared0.getValue(), result);
        JkVersion pluginGreater = JkVersion.of("1.4.0.RELEASE");
        result = compatibilityBreak.getBreakingJekaVersion(pluginGreater, jekaDeclared1);
        Assert.assertNull(result);
        JkVersion pluginLower = JkVersion.of("1.0.0.RELEASE");
        result = compatibilityBreak.getBreakingJekaVersion(pluginLower, jekaDeclared1);
        Assert.assertEquals(jekaDeclared0.getValue(), result);
    }
}
