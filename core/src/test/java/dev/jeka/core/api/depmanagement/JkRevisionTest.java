package dev.jeka.core.api.depmanagement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JkRevisionTest {

    @Test
    void testComparable() {
        assertTrue(JkVersion.of("1.0.1").compareTo(JkVersion.of("1.0.0")) > 0);
    }

}
