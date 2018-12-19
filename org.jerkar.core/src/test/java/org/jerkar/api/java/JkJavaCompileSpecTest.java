package org.jerkar.api.java;

import org.junit.Test;

import static org.junit.Assert.*;

public class JkJavaCompileSpecTest {

    @Test
    public void setSourceVersion() {
        JkJavaCompileSpec spec = JkJavaCompileSpec.of();
        spec.setSourceVersion(JkJavaVersion.V8);
        spec.setSourceVersion(JkJavaVersion.V8);   // set it twice but only 1 entry should remain
        assertEquals(2, spec.getOptions().size());
    }
}