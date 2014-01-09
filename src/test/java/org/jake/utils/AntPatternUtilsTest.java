package org.jake.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AntPatternUtilsTest {

	@Test
	public void testDoMatchOk() {
		String path = "foo/bar.txt";
		assertTrue(AntPatternUtils.doMatch("**/*.txt", path));
		assertTrue(AntPatternUtils.doMatch("foo/bar.txt", path));
		assertTrue(AntPatternUtils.doMatch("foo/b?r.txt", path));
		assertTrue(AntPatternUtils.doMatch("f*/bar.txt", path));
		assertTrue(AntPatternUtils.doMatch("*/bar.txt", path));
		assertTrue(AntPatternUtils.doMatch("**", path));
	}
	
	@Test
	public void testDoMatchNotOk() {
		String path = "foo/bar.txt";
		assertTrue(!AntPatternUtils.doMatch("**/*.tx", path));
		assertTrue(!AntPatternUtils.doMatch("foo/br.txt", path));	
		assertTrue(!AntPatternUtils.doMatch("k*/bar.txt", path));
	}

}
