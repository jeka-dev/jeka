package dev.jeka.core.api.system;

import org.junit.Test;

import static org.junit.Assert.*;

public class JkPropertyTest {

    @Test
    public void get() {
        System.setProperty("foo", "fooValue");
        System.setProperty("bar", "bar ${foo}");
        assertEquals("bar fooValue", JkProperty.get("bar"));
        System.setProperty("bar", "bar value");
        assertEquals("bar value", JkProperty.get("bar"));
    }
}