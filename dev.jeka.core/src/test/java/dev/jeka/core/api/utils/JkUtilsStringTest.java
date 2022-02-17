package dev.jeka.core.api.utils;

import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.*;

public class JkUtilsStringTest {

    @Test
    public void extractVariableToken() {
        assertEquals(new LinkedList(), JkUtilsString.extractVariableToken("kjlkjlkjljl"));
        assertEquals(JkUtilsIterable.listOf("foo", "bar"),
                JkUtilsString.extractVariableToken("yuiy ${foo} ds${bar}"));
        assertEquals(JkUtilsIterable.listOf("foo"),
                JkUtilsString.extractVariableToken("yuiy $${foo} d"));

    }
}