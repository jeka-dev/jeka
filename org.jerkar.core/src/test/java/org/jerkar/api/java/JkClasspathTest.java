package org.jerkar.api.java;

import org.junit.Test;

import java.nio.file.Paths;

/**
 * Created by angibaudj on 17-10-17.
 */
public class JkClasspathTest {

    @Test
    public void current() throws Exception {
        JkClasspath.current().asPath();
    }

    public void ofWithWildCard() {
        JkClasspath.of(Paths.get("toto/*")).asPath();
    }

}