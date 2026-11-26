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
    void get_selfReference_failsWithClearMessage() {
        Map<String, String> map = new HashMap<>();
        map.put("jk.test.foo", "${jk.test.foo}");
        map.put("jk.test.a", "x ${jk.test.b}");
        map.put("jk.test.b", "${jk.test.a}");
        map.put("jk.test.c", "${jk.test.d} ${jk.test.d}");
        map.put("jk.test.d", "d");
        JkProperties props = JkProperties.ofMap("", map);
        Exception e = assertThrows(IllegalArgumentException.class, () -> props.get("jk.test.foo"));
        assertEquals("Property 'jk.test.foo' references itself: jk.test.foo -> jk.test.foo.", e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> props.get("jk.test.a"));
        assertEquals("Property 'jk.test.a' references itself: jk.test.a -> jk.test.b -> jk.test.a.", e.getMessage());
        assertEquals("d d", props.get("jk.test.c"));  // referenced twice, but no cycle
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