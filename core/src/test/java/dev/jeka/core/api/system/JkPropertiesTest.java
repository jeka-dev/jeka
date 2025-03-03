package dev.jeka.core.api.system;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JkPropertiesTest {

    @Test
    public void get() {
        Map<String, String> map = new HashMap<>();
        map.put("foo", "fooValue");
        map.put("bar", "bar ${foo}");
        assertEquals("bar fooValue", JkProperties.ofMap("", map).get("bar"));
        map.put("bar", "bar value");
        assertEquals("bar value", JkProperties.ofMap("", map).get("bar"));
    }
}