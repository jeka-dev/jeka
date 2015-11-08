package org.jerkar.api.file;

import static org.junit.Assert.assertTrue;

import org.jerkar.api.file.AntPattern;
import org.junit.Test;

public class AntPatternTest {

    @Test
    public void testDoMatchOk() {
	this.testDoMatchOk("foo/bar.txt");
	this.testDoMatchOk(".\\foo\\bar.txt");
	this.testDoMatchOk("./foo/bar.txt");
	this.testDoMatchOk("/foo/bar.txt");

	assertTrue(AntPattern.of("**/*.java").doMatch("foo/subfoo/bar.java"));
	assertTrue(AntPattern.of("com/**").doMatch("com"));
	assertTrue(!AntPattern.of("com/**/*").doMatch("com"));
    }

    @Test
    public void testDoMatchNotOk() {
	String path = "foo/bar.txt";
	assertTrue(!AntPattern.of("**/*.tx").doMatch(path));
	assertTrue(!AntPattern.of("foo/br.txt").doMatch(path));
	assertTrue(!AntPattern.of("k*/bar.txt").doMatch(path));
    }

    private void testDoMatchOk(String path) {
	assertTrue(AntPattern.of("**/*.txt").doMatch(path));
	assertTrue(AntPattern.of("foo/bar.txt").doMatch(path));
	assertTrue(AntPattern.of("foo/b?r.txt").doMatch(path));
	assertTrue(AntPattern.of("f*/bar.txt").doMatch(path));
	assertTrue(AntPattern.of("*/bar.txt").doMatch(path));
	assertTrue(AntPattern.of("**").doMatch(path));
    }

}
