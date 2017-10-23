package org.jerkar.api.file;

import org.junit.Ignore;
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
    public void testDoMatchOk() {
        this.testDoMatchOk("foo/bar.txt");
        this.testDoMatchOk("./foo/bar.txt");

        assertTrue(JkPathMatcher.in("**/*.java").matches(Paths.get("foo/subfoo/bar.java")));
        assertTrue(JkPathMatcher.in("com/**", "com").matches(Paths.get("com")));
        assertTrue(!JkPathMatcher.in("com/**/*").matches(Paths.get("com")));
    }

    @Test
    public void testNotIn() {
        assertTrue(JkPathMatcher.notIn("org/**", "com/**").matches(Paths.get("foo/subfoo/bar.java")));
        assertFalse(JkPathMatcher.notIn("org/**", "com/**").matches(Paths.get("com/subfoo/bar.java")));
        assertFalse(JkPathMatcher.notIn("org/**", "com/**").matches(Paths.get("org/subfoo/bar.java")));
    }

    @Test
    public void testDoMatchNotOk() {
        final String pathString = "foo/bar.txt";
        Path path = Paths.get(pathString);
        assertTrue(!JkPathMatcher.in("**/*.tx").matches(path));
        assertTrue(!JkPathMatcher.in("foo/br.txt").matches(path));
        assertTrue(!JkPathMatcher.in("k*/bar.txt").matches(path));
    }

    private void testDoMatchOk(String pathString) {

        Path path = Paths.get(pathString).normalize();
        System.out.println("---------------------- " + pathString + ":" + path);

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.txt");
        assertTrue(matcher.matches(path));
        assertTrue(pathString, JkPathMatcher.in("**/*.txt").matches(path));

        matcher = FileSystems.getDefault().getPathMatcher("glob:foo/bar.txt");
        assertTrue(pathString , matcher.matches(path));
        assertTrue(pathString, JkPathMatcher.in("foo/bar.txt").matches(path));

        assertTrue(JkPathMatcher.in("foo/b?r.txt").matches(path));
        assertTrue(JkPathMatcher.in("f*/bar.txt").matches(path));
        assertTrue(JkPathMatcher.in("*/bar.txt").matches(path));
        assertTrue(JkPathMatcher.in("**").matches(path));
    }

}
