package dev.jeka.core.tool;

import dev.jeka.core.tool.JkJekaVersionRanges.PluginAndJekaVersion;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompatibilityBreakingTest {

    @Test
    void test() {
        InputStream is = CompatibilityBreakingTest.class.getResourceAsStream("compatibilitybreak.txt");
        JkJekaVersionRanges.CompatibilityBreak compatibilityBreak =
                JkJekaVersionRanges.CompatibilityBreak.of(is);
        PluginAndJekaVersion break0 = new PluginAndJekaVersion("1.2.1.RELEASE", "0.9.1.RELEASE");
        PluginAndJekaVersion break1 = new PluginAndJekaVersion("1.3.0.RELEASE", "0.9.5.M1");
        PluginAndJekaVersion result = compatibilityBreak.getBreakingJekaVersion(break0);
        assertEquals(break0, result);
        PluginAndJekaVersion pluginGreater = new PluginAndJekaVersion("1.4.0.RELEASE", break1.jekaVersion.getValue());
        result = compatibilityBreak.getBreakingJekaVersion(pluginGreater);
        assertNull(result);
        PluginAndJekaVersion pluginLower = new PluginAndJekaVersion("1.0.0.RELEASE", break1.jekaVersion.getValue());
        result = compatibilityBreak.getBreakingJekaVersion(pluginLower);
        assertEquals(break0, result);
    }
}
