package org.jerkar.plugins.jacoco;

import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.junit.JkUnit;
import org.junit.Test;

import java.io.File;

public class JkocoJunitEnhancerTest {

    @Test
    public void test() {
        JkocoJunitEnhancer.of(new File(".")).apply(JkUnit.of(JkClasspath.of()));
    }

}
