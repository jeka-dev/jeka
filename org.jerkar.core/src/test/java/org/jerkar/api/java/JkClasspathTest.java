package org.jerkar.api.java;

import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Created by angibaudj on 17-10-17.
 */
public class JkClasspathTest {

    @Test
    public void current() throws Exception {
        JkClasspath.current().asPath();
    }

    public void ofWithWildCard() {
        JkClasspath.ofPath(Paths.get("toto/*")).asPath();
    }

}