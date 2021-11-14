package dev.jeka.core.tool;

import dev.jeka.core.tool.JkJekaVersionCompatibilityChecker.PluginAndJekaVersion;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

public class CompatibilityBreakingTest {

    @Test
    public void test() {
        InputStream is = CompatibilityBreakingTest.class.getResourceAsStream("compatibilitybreak.txt");
        JkJekaVersionCompatibilityChecker.CompatibilityBreak compatibilityBreak =
                JkJekaVersionCompatibilityChecker.CompatibilityBreak.of(is);
        PluginAndJekaVersion break0 = new PluginAndJekaVersion("1.2.1.RELEASE", "0.9.1.RELEASE");
        PluginAndJekaVersion break1 = new PluginAndJekaVersion("1.3.0.RELEASE", "0.9.5.M1");
        PluginAndJekaVersion result = compatibilityBreak.getBreakingJekaVersion(break0);
        Assert.assertEquals(break0, result);
        PluginAndJekaVersion pluginGreater = new PluginAndJekaVersion("1.4.0.RELEASE", break1.jekaVersion.getValue());
        result = compatibilityBreak.getBreakingJekaVersion(pluginGreater);
        Assert.assertNull(result);
        PluginAndJekaVersion pluginLower = new PluginAndJekaVersion("1.0.0.RELEASE", break1.jekaVersion.getValue());
        result = compatibilityBreak.getBreakingJekaVersion(pluginLower);
        Assert.assertEquals(break0, result);
    }
}
