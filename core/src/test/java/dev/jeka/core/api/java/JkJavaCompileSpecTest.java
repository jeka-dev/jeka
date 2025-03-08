package dev.jeka.core.api.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JkJavaCompileSpecTest {

    @Test
    void setSourceVersion() {
        JkJavaCompileSpec spec = JkJavaCompileSpec.of();
        spec.setSourceVersion("8");
        spec.setSourceVersion("8");   // set it twice but only 1 entry should remain
        assertEquals(2, spec.getOptions().size());
    }
}