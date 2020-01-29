package dev.jeka.core.api.java;

import org.junit.Test;

import static org.junit.Assert.*;

public class JkJavaCompilerTest {

    @Test
    public void currentJdkSourceVersion() {
        assertEquals("12", JkJavaCompiler.currentJdkSourceVersion("12.0.1"));
        assertEquals("8", JkJavaCompiler.currentJdkSourceVersion("1.8.0_211"));
        assertEquals("9", JkJavaCompiler.currentJdkSourceVersion("9.0.1"));
    }
}