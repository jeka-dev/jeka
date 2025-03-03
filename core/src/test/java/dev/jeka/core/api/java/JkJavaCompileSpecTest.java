package dev.jeka.core.api.java;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JkJavaCompileSpecTest {

    @Test
    public void setSourceVersion() {
        JkJavaCompileSpec spec = JkJavaCompileSpec.of();
        spec.setSourceVersion("8");
        spec.setSourceVersion("8");   // set it twice but only 1 entry should remain
        assertEquals(2, spec.getOptions().size());
    }
}