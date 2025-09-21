package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsIterable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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

        assertEquals("mac", JkCoordinate.of("mygroup:myname:mac::").getArtifactSpecification().getClassifier());
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