package org.jerkar.api.file;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("javadoc")
public class JkPathMatcherTest {

    @Test
    @Ignore
    public void testDoMatchOk() {
        this.testDoMatchOk("foo/bar.txt");
        this.testDoMatchOk("./foo/bar.txt");

        assertTrue(JkPathMatcher.include("**/*.java").matches(Paths.get("foo/subfoo/bar.java")));
        assertTrue(JkPathMatcher.include("com/**", "com").matches(Paths.get("com")));
        assertTrue(!JkPathMatcher.include("com/**/*").matches(Paths.get("com")));
    }

    @Test
    @Ignore
    public void testDoMatchNotOk() {
        final String pathString = "foo/bar.txt";
        Path path = Paths.get(pathString);
        assertTrue(!JkPathMatcher.include("**/*.tx").matches(path));
        assertTrue(!JkPathMatcher.include("foo/br.txt").matches(path));
        assertTrue(!JkPathMatcher.include("k*/bar.txt").matches(path));
    }

    private void testDoMatchOk(String pathString) {

        Path path = Paths.get(pathString).normalize();
        System.out.println("---------------------- " + pathString + ":" + path);

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.txt");
        assertTrue(matcher.matches(path));
        assertTrue(pathString, JkPathMatcher.include("**/*.txt").matches(path));

        matcher = FileSystems.getDefault().getPathMatcher("glob:foo/bar.txt");
        assertTrue(pathString , matcher.matches(path));
        assertTrue(pathString, JkPathMatcher.include("foo/bar.txt").matches(path));

        assertTrue(JkPathMatcher.include("foo/b?r.txt").matches(path));
        assertTrue(JkPathMatcher.include("f*/bar.txt").matches(path));
        assertTrue(JkPathMatcher.include("*/bar.txt").matches(path));
        assertTrue(JkPathMatcher.include("**").matches(path));
    }

}
