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

        assertTrue(JkPathMatcher.ofAccept("**/*.java").matches(Paths.get("foo/subfoo/bar.java")));
        assertTrue(JkPathMatcher.ofAccept("**.java").matches(Paths.get("Bar.java")));
        assertTrue(JkPathMatcher.ofAccept("**.java").matches(Paths.get("foo/Bar.java")));
        assertTrue(JkPathMatcher.ofAccept("*.java").matches(Paths.get("Bar.java")));
        assertTrue(JkPathMatcher.ofAccept("com/**", "com").matches(Paths.get("com")));
        assertTrue(!JkPathMatcher.ofAccept("com/**/*").matches(Paths.get("com")));

        final String pathString = "foo/bar.txt";
        Path path = Paths.get(pathString);
        assertTrue(!JkPathMatcher.ofAccept("**/*.tx").matches(path));
        assertTrue(!JkPathMatcher.ofAccept("foo/br.txt").matches(path));
        assertTrue(!JkPathMatcher.ofAccept("k*/bar.txt").matches(path));
        assertTrue(!JkPathMatcher.ofAccept("*.java").matches(Paths.get("foo/Bar.java")));
    }

    @Test
    public void testRefuse() {

        assertTrue(JkPathMatcher.ofReject("org/**", "com/**").matches(Paths.get("foo/subfoo/bar.java")));
        assertFalse(JkPathMatcher.ofReject("org/**", "com/**").matches(Paths.get("com/subfoo/bar.java")));
        assertFalse(JkPathMatcher.ofReject("org/**", "com/**").matches(Paths.get("org/subfoo/bar.java")));

        assertFalse(JkPathMatcher.ofReject("**/_*").matches(Paths.get("foo/subfoo/_Bar.java")));
        assertFalse(JkPathMatcher.ofReject("**/_*", "_*").matches(Paths.get("_Bar.java")));
    }

    @Test
    public void testAndRefuse() {
        assertTrue(JkPathMatcher.ofAccept("**/*.java").andReject("**/_*")
                .matches(Paths.get("foo/subfoo/Bar.java")));

        Path path = Paths.get("C:/samples/sample-dependee/AClassicBuild.java");
        assertTrue(JkPathMatcher.ofAccept("**/*.java").andReject("**/_*").matches(path));


    }

    private void testDoMatchOk(String pathString) {

        Path path = Paths.get(pathString).normalize();
        System.out.println("---------------------- " + pathString + ":" + path);

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.txt");
        assertTrue(matcher.matches(path));
        assertTrue(pathString, JkPathMatcher.ofAccept("**/*.txt").matches(path));

        matcher = FileSystems.getDefault().getPathMatcher("glob:foo/bar.txt");
        assertTrue(pathString , matcher.matches(path));
        assertTrue(pathString, JkPathMatcher.ofAccept("foo/bar.txt").matches(path));

        assertTrue(JkPathMatcher.ofAccept("foo/b?r.txt").matches(path));
        assertTrue(JkPathMatcher.ofAccept("f*/bar.txt").matches(path));
        assertTrue(JkPathMatcher.ofAccept("*/bar.txt").matches(path));
        assertTrue(JkPathMatcher.ofAccept("**").matches(path));
    }

}
