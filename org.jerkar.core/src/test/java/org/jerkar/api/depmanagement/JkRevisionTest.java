package org.jerkar.api.depmanagement;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkRevisionTest {

    @Test
    public void test() {
        assertTrue(JkVersion.name("1.0.1").isGreaterThan(JkVersion.name("1.0.0")));
    }

}
