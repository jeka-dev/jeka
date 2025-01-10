package dev.jeka.core.api.utils;

import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;

public class JkUtilsStringTest {

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
}