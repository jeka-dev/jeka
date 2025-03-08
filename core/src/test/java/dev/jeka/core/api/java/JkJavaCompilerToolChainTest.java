package dev.jeka.core.api.java;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JkJavaCompilerToolChainTest {

    @Test
    public void currentJdkSourceVersion() {
        assertEquals("12", JkJavaCompilerToolChain.runningJdkVersion("12.0.1"));
        assertEquals("8", JkJavaCompilerToolChain.runningJdkVersion("1.8.0_211"));
        assertEquals("9", JkJavaCompilerToolChain.runningJdkVersion("9.0.1"));
    }
}