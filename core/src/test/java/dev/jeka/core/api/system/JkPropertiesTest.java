package dev.jeka.core.api.system;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class JkPropertiesTest {

    @Test
    void get() {
        Map<String, String> map = new HashMap<>();
        map.put("foo", "fooValue");
        map.put("bar", "bar ${foo}");
        Assertions.assertEquals("bar fooValue", JkProperties.ofMap("", map).get("bar"));
        map.put("bar", "bar value");
        Assertions.assertEquals("bar value", JkProperties.ofMap("", map).get("bar"));
    }
}