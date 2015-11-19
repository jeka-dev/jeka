package org.jerkar.api.depmanagement;

import static org.junit.Assert.assertTrue;

import org.jerkar.api.depmanagement.JkVersion;
import org.junit.Test;

public class JkRevisionTest {

    @Test
    public void test() {
        assertTrue(JkVersion.ofName("1.0.1").isGreaterThan(JkVersion.ofName("1.0.0")));
    }

}
