package org.jerkar.api.depmanagement;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkRevisionTest {

    @Test
    public void test() {
        assertTrue(JkVersion.of("1.0.1").isGreaterThan(JkVersion.of("1.0.0")));
    }

}
