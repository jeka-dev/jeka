package dev.jeka.core.api.utils;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JkUtilsStringTest {

    @Test
    public void extractVariableToken() {
        assertEquals(new LinkedList(), JkUtilsString.extractVariableToken("kjlkjlkjljl"));
        assertEquals(JkUtilsIterable.listOf("foo", "bar"),
                JkUtilsString.extractVariableToken("yuiy ${foo} ds${bar}"));
        assertEquals(JkUtilsIterable.listOf("foo"),
                JkUtilsString.extractVariableToken("yuiy $${foo} d"));

    }

    @Test
    public void substringAfterLast() {
        assertEquals("eee", JkUtilsString.substringAfterLast("toto=eee", "toto="));
        assertEquals("", JkUtilsString.substringAfterLast("toto=", "toto="));
        assertEquals("", JkUtilsString.substringAfterLast("toto=", ""));
        assertEquals("", JkUtilsString.substringAfterLast("toto=", "a"));
    }

    @Test
    public void withMarginLeft() {
        String paragraph = "Aaaaaaaa aaaa aaa1\nBnnnnnnb  bbb2\nCCCccccccccc";
        System.out.println("------");
        System.out.print(JkUtilsString.withLeftMargin(paragraph, "  "));
        System.out.println("------");
    }

    @Test
    public void shortenPackageName() {
        assertEquals("c.e.MyClass.myMethod", JkUtilsString.shortenPackageName("com.example.MyClass.myMethod"));
        assertEquals("c.e.MyClass", JkUtilsString.shortenPackageName("com.example.MyClass"));
    }

    @Test
    public void removePackagePrefix() {
        assertEquals("MyClass.myMethod", JkUtilsString.removePackagePrefix("com.example.MyClass.myMethod"));
        assertEquals("MyClass", JkUtilsString.removePackagePrefix("com.example.MyClass"));
    }

    @Test
    public void testSplitWhiteSpaves() {
        List<String> tokens = JkUtilsString.splitWhiteSpaces(null);
        assertTrue(tokens.isEmpty());
    }

    @Test
    void matchesPattern_shouldHandleVariousPatterns() {
        // Test exact matches
        assertTrue(JkUtilsString.matchesPattern("hello", "hello"));
        assertTrue(JkUtilsString.matchesPattern("", ""));

        // Test single wildcard matches
        assertTrue(JkUtilsString.matchesPattern("hello", "h*"));
        assertTrue(JkUtilsString.matchesPattern("hello", "*o"));
        assertTrue(JkUtilsString.matchesPattern("hello", "h*o"));
        assertTrue(JkUtilsString.matchesPattern("hello", "*"));

        // Test multiple wildcards
        assertTrue(JkUtilsString.matchesPattern("hello world", "h*w*"));
        assertTrue(JkUtilsString.matchesPattern("hello world", "*e*o*"));
        assertTrue(JkUtilsString.matchesPattern("abc def ghi", "a*e*i"));

        // Test wildcard matching empty sequences
        assertTrue(JkUtilsString.matchesPattern("hello", "h*e*l*l*o"));
        assertTrue(JkUtilsString.matchesPattern("test", "t*e*s*t*"));

        // Test non-matches
        assertFalse(JkUtilsString.matchesPattern("hello", "world"));
        assertFalse(JkUtilsString.matchesPattern("hello", "h*x"));
        assertFalse(JkUtilsString.matchesPattern("hello", "x*o"));
        assertFalse(JkUtilsString.matchesPattern("hello", "hello world"));

        // Test null handling
        assertTrue(JkUtilsString.matchesPattern(null, null));
        assertFalse(JkUtilsString.matchesPattern("hello", null));
        assertFalse(JkUtilsString.matchesPattern(null, "hello"));

        // Test edge cases
        assertTrue(JkUtilsString.matchesPattern("", "*"));
        assertTrue(JkUtilsString.matchesPattern("test", "****"));
        assertFalse(JkUtilsString.matchesPattern("", "a"));
        assertFalse(JkUtilsString.matchesPattern("test", ""));

        // Test complex patterns
        assertTrue(JkUtilsString.matchesPattern("com.example.MyClass", "com.*Class"));
        assertTrue(JkUtilsString.matchesPattern("file.txt", "*.txt"));
        assertTrue(JkUtilsString.matchesPattern("HelloWorld", "*World"));
        assertFalse(JkUtilsString.matchesPattern("com.example.MyClass", "com.*Service"));
    }

}