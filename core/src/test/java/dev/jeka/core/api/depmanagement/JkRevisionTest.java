package dev.jeka.core.api.depmanagement;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("javadoc")
public class JkRevisionTest {

    @Test
    public void testComparable() {
        assertTrue(JkVersion.of("1.0.1").compareTo(JkVersion.of("1.0.0")) > 0);
    }

}
