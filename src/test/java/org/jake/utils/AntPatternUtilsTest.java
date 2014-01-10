package org.jake.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AntPatternUtilsTest {

	@Test
	public void testDoMatchOk() {
		this.testDoMatchOk("foo/bar.txt");
		this.testDoMatchOk(".\\foo\\bar.txt");
		this.testDoMatchOk("./foo/bar.txt");
		this.testDoMatchOk("/foo/bar.txt");
		
		assertTrue(AntPatternUtils.doMatch("**/*.java", "foo/subfoo/bar.java"));
		assertTrue(AntPatternUtils.doMatch("com/**", "com"));
		assertTrue(!AntPatternUtils.doMatch("com/**/*", "com"));
	}

	
	
	@Test
	public void testDoMatchNotOk() {
		String path = "foo/bar.txt";
		assertTrue(!AntPatternUtils.doMatch("**/*.tx", path));
		assertTrue(!AntPatternUtils.doMatch("foo/br.txt", path));	
		assertTrue(!AntPatternUtils.doMatch("k*/bar.txt", path));
	}
	
	
	private void testDoMatchOk(String path) {
		assertTrue(AntPatternUtils.doMatch("**/*.txt", path));
		assertTrue(AntPatternUtils.doMatch("foo/bar.txt", path));
		assertTrue(AntPatternUtils.doMatch("foo/b?r.txt", path));
		assertTrue(AntPatternUtils.doMatch("f*/bar.txt", path));
		assertTrue(AntPatternUtils.doMatch("*/bar.txt", path));
		assertTrue(AntPatternUtils.doMatch("**", path));
		
		
	}

}
