package dev.jeka.core.tool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EnvironmentTest {

    @Test
    public void initialize() {
        Environment.initialize(new String[] {"-lv=true", "-kb=HttpClientTaskBuild", "clean", "project#pack", "project#publish"
                , "project#publish.localOnly", "-lb"});
        assertEquals("HttpClientTaskBuild", Environment.behavior.kbeanName.orElse(null));
    }

    @Test
    public void behaviorSettings_areParsed() {
        EnvBehaviorSettings behaviorSettings = Environment.createBehaviorSettings("-co", "-cw", "-kb=foo");
        assertTrue(behaviorSettings.cleanOutput);
        assertTrue(behaviorSettings.cleanWork);
        assertEquals("foo", behaviorSettings.kbeanName.get());
    }


}