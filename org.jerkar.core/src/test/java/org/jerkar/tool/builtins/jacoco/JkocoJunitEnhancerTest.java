package org.jerkar.tool.builtins.jacoco;

import org.jerkar.api.java.junit.JkUnit;
import org.junit.Test;

import java.nio.file.Paths;

public class JkocoJunitEnhancerTest {

    @Test
    public void test() {
        JkocoJunitEnhancer.of(Paths.get("")).apply(JkUnit.of());
    }

}
