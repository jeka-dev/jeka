package dev.jeka.core.tool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EnvironmentTest {

    @Test
    public void initialize() {
        Environment.initialize(new String[] {"-lv=true", "-kb=HttpClientTaskBuild", "clean", "project#pack", "project#publish"
                , "project#publish.localOnly", "-lb"});
        assertEquals("HttpClientTaskBuild", Environment.standardOptions.kBeanName());
    }
}