package org.jerkar.api.file;

import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("javadoc")
public class JkPathMatcherTest {

    @Test
    public void testAccept() {
        this.testDoMatchOk("foo/bar.txt");
        this.testDoMatchOk("./foo/bar.txt");

        assertTrue(JkPathMatcher.accept("**/*.java").matches(Paths.get("foo/subfoo/bar.java")));
        assertTrue(JkPathMatcher.accept("**.java").matches(Paths.get("Bar.java")));
        assertTrue(JkPathMatcher.accept("**.java").matches(Paths.get("foo/Bar.java")));
        assertTrue(JkPathMatcher.accept("*.java").matches(Paths.get("Bar.java")));
        assertTrue(JkPathMatcher.accept("com/**", "com").matches(Paths.get("com")));
        assertTrue(!JkPathMatcher.accept("com/**/*").matches(Paths.get("com")));

        final String pathString = "foo/bar.txt";
        Path path = Paths.get(pathString);
        assertTrue(!JkPathMatcher.accept("**/*.tx").matches(path));
        assertTrue(!JkPathMatcher.accept("foo/br.txt").matches(path));
        assertTrue(!JkPathMatcher.accept("k*/bar.txt").matches(path));
        assertTrue(!JkPathMatcher.accept("*.java").matches(Paths.get("foo/Bar.java")));
    }

    @Test
    public void testRefuse() {

        assertTrue(JkPathMatcher.refuse("org/**", "com/**").matches(Paths.get("foo/subfoo/bar.java")));
        assertFalse(JkPathMatcher.refuse("org/**", "com/**").matches(Paths.get("com/subfoo/bar.java")));
        assertFalse(JkPathMatcher.refuse("org/**", "com/**").matches(Paths.get("org/subfoo/bar.java")));

        assertFalse(JkPathMatcher.refuse("**/_*").matches(Paths.get("foo/subfoo/_Bar.java")));
        assertFalse(JkPathMatcher.refuse("**/_*", "_*").matches(Paths.get("_Bar.java")));
    }

    @Test
    public void testAndRefuse() {
        assertTrue(JkPathMatcher.accept("**/*.java").andRefuse("**/_*")
                .matches(Paths.get("foo/subfoo/Bar.java")));

        Path path = Paths.get("C:/samples/sample-dependee/AClassicBuild.java");
        assertTrue(JkPathMatcher.accept("**/*.java").andRefuse("**/_*").matches(path));


    }

    private void testDoMatchOk(String pathString) {

        Path path = Paths.get(pathString).normalize();
        System.out.println("---------------------- " + pathString + ":" + path);

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.txt");
        assertTrue(matcher.matches(path));
        assertTrue(pathString, JkPathMatcher.accept("**/*.txt").matches(path));

        matcher = FileSystems.getDefault().getPathMatcher("glob:foo/bar.txt");
        assertTrue(pathString , matcher.matches(path));
        assertTrue(pathString, JkPathMatcher.accept("foo/bar.txt").matches(path));

        assertTrue(JkPathMatcher.accept("foo/b?r.txt").matches(path));
        assertTrue(JkPathMatcher.accept("f*/bar.txt").matches(path));
        assertTrue(JkPathMatcher.accept("*/bar.txt").matches(path));
        assertTrue(JkPathMatcher.accept("**").matches(path));
    }

}
