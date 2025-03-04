package dev.jeka.core.api.depmanagement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class JkCoordinateTest {

    @Test
    void testTestEquals() {
        String description = "mygroup:myname:myVersion";
        assertEquals(JkCoordinate.of(description), JkCoordinate.of(description));
    }

    @Test
    void testOfDescription() {
        assertEquals(JkVersion.UNSPECIFIED, JkCoordinate.of("group:name:classifier::").getVersion());
        assertEquals(JkVersion.UNSPECIFIED, JkCoordinate.of("group:name::extension:").getVersion());
    }

    @Test
    void testToString() {
        String desc = "org.lwjgl:lwjgl-bom::pom:3.3.1";
        JkCoordinate coordinate = JkCoordinate.of(desc);
        assertEquals(desc, coordinate.toString());

        desc = "org.lwjgl:lwjgl-bom:sources::3.3.1";
        coordinate = JkCoordinate.of(desc);
        assertEquals(desc, coordinate.toString());
    }
}