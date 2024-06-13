package dev.jeka.core.api.depmanagement;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


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

    @Test
    public void testToString() {
        String desc = "org.lwjgl:lwjgl-bom::pom:3.3.1";
        JkCoordinate coordinate = JkCoordinate.of(desc);
        assertEquals(desc, coordinate.toString());

        desc = "org.lwjgl:lwjgl-bom:sources::3.3.1";
        coordinate = JkCoordinate.of(desc);
        assertEquals(desc, coordinate.toString());
    }
}