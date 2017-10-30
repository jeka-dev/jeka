package org.jerkar.api.java;

import org.junit.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Created by angibaudj on 29-10-17.
 */
public class JkClasspathTest {

    @Test
    public void allPathMatching() throws Exception {
        JkClasspath classpath = JkClasspath.current();
        List<String> stringList = new ArrayList<>();
        stringList.add("**/TestSuite.class");
        Set<Path> classes = classpath.allPathMatching(stringList);
        assertTrue(classes.size() > 0);
    }

}