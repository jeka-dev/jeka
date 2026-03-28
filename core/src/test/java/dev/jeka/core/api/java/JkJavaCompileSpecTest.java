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

    @Test
    void setProcessorPath() {
        JkJavaCompileSpec spec = JkJavaCompileSpec.of();
        java.nio.file.Path path1 = java.nio.file.Paths.get("path1");
        java.nio.file.Path path2 = java.nio.file.Paths.get("path2");
        spec.setProcessorPath(java.util.Arrays.asList(path1, path2));
        assertEquals(2, spec.getProcessorPath().getEntries().size());
        assertEquals(path1, spec.getProcessorPath().getEntries().get(0));
        assertEquals(path2, spec.getProcessorPath().getEntries().get(1));
    }
}