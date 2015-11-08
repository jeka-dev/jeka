package org.jerkar.plugins.jacoco;

import java.io.File;

import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.plugins.jacoco.JkocoJunitEnhancer;
import org.junit.Test;

public class JkocoJunitEnhancerTest {

    @Test
    public void test() {
	JkocoJunitEnhancer.of(new File(".")).enhance(JkUnit.of(JkClasspath.of()));
    }

}
