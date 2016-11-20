package org.jerkar.api.file;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkPathFilterTest {

    @Test
    public void testCaseSensitive() {
        final JkPathFilter exclude = JkPathFilter.exclude("meta/*.txt").caseSensitive(false);
        Assert.assertFalse(exclude.accept("meta/toto.txt"));
        Assert.assertFalse(exclude.accept("meta/toto.TXT"));

        final JkPathFilter include = JkPathFilter.include("meta/*.txt").caseSensitive(false);
        Assert.assertTrue(include.accept("meta/toto.txt"));
        Assert.assertTrue(include.accept("meta/toto.TXT"));
    }

    @Test
    public void testExclude() {
        final JkPathFilter exclude = JkPathFilter.exclude("meta/*.txt");
        Assert.assertFalse(exclude.accept("meta/toto.txt"));
        Assert.assertTrue(exclude.accept("meta/toto.TXT"));
    }

    @Test
    public void tesInclude() {
        final JkPathFilter include = JkPathFilter.include("meta/*.txt");
        Assert.assertFalse(include.accept("foo/toto.txt"));
        Assert.assertTrue(include.accept("meta/toto.txt"));
    }

}
