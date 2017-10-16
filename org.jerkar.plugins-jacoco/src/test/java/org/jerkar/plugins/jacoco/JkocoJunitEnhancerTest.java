package org.jerkar.plugins.jacoco;

import java.io.File;
import java.nio.file.Paths;

import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.junit.JkUnit;
import org.junit.Test;

public class JkocoJunitEnhancerTest {

    @Test
    public void test() {
        JkocoJunitEnhancer.of(Paths.get("")).apply(JkUnit.of(JkClasspath.of()));
    }

}
