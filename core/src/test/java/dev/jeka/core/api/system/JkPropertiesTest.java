package dev.jeka.core.api.system;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JkPropertiesTest {

    @Test
    void get() {
        Map<String, String> map = new HashMap<>();
        map.put("foo", "fooValue");
        map.put("bar", "bar ${foo}");
        assertEquals("bar fooValue", JkProperties.ofMap("", map).get("bar"));
        map.put("bar", "bar value");
        assertEquals("bar value", JkProperties.ofMap("", map).get("bar"));
    }

    @Test
    void get_undefinedReference_fails() {
        Map<String, String> map = new HashMap<>();
        map.put("bar", "${undefined.zzz}");
        Exception e = assertThrows(IllegalArgumentException.class, () -> JkProperties.ofMap("", map).get("bar"));
        assertEquals("Property 'bar' references '${undefined.zzz}' which is not defined.", e.getMessage());
    }

    @Test
    @Disabled("Only works on some macos system")
    void test_env() {
        String envValue = System.getenv("COMMAND_MODE");
        String propsDotValue = JkProperties.ofSysPropsThenEnv().get("command.mode");
        assertEquals(envValue, propsDotValue);
        String propsHyphenValue = JkProperties.ofSysPropsThenEnv().get("command-mode");
        assertEquals(envValue, propsHyphenValue);
    }
}