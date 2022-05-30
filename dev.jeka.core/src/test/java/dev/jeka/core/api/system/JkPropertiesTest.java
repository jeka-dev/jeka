package dev.jeka.core.api.system;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JkPropertiesTest {

    @Test
    public void get() {
        System.setProperty("foo", "fooValue");
        System.setProperty("bar", "bar ${foo}");
        assertEquals("bar fooValue", JkProperties.get("bar"));
        System.setProperty("bar", "bar value");
        assertEquals("bar value", JkProperties.get("bar"));
    }
}