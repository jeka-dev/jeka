package dev.jeka.core.tool.builtins.jacoco;

import dev.jeka.core.api.java.junit.JkTestProcessor;
import org.junit.Test;

import java.nio.file.Paths;

public class JkocoJunitEnhancerTest {

    @Test
    public void test() {
        JkocoJunitEnhancer.of(Paths.get("")).apply(JkTestProcessor.of());
    }

}
