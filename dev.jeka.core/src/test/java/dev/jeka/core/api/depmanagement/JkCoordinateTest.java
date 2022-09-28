package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;


public class JkCoordinateTest {

    @Test
    public void testTestEquals() {
        String description = "mygroup:myname:myVersion";
        assertEquals(JkCoordinate.of(description), JkCoordinate.of(description));
    }

    @Test
    public void testOfDescription() {
        assertEquals(JkVersion.UNSPECIFIED, JkCoordinate.of("group:name:classifier::").getVersion());
        assertEquals(JkVersion.UNSPECIFIED, JkCoordinate.of("group:name::extension:").getVersion());
    }
}