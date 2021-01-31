package dev.jeka.core.tool;

import org.junit.Test;

import static org.junit.Assert.*;

public class EnvironmentTest {

    @Test
    public void initialize() {
        Environment.initialize(new String[] {"-LV=true", "-JKC=HttpClientTaskBuild", "clean", "java#pack", "java#publish"
                , "-java#publish.localOnly", "-LB"});
        assertEquals("HttpClientTaskBuild", Environment.standardOptions.jkClassName());
    }
}