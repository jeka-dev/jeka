package dev.jeka.core.api.depmanagement;

import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;


class JkVersionTest {

    @Test
    void semanticVersionComparator() {
        Comparator<String> comp = JkVersion.VERSION_COMPARATOR;
        assertTrue(comp.compare("1", "2") < 0);
        assertTrue(comp.compare("1.0.1.RELEASE", "1.0.1.RC1") > 0);
        assertTrue(comp.compare("2toto4", "2toto2") > 0);
        assertTrue(comp.compare("0.8.9.RELEASE", "0.8.14.RELEASE") < 0);
        assertTrue(comp.compare("0.8.9.M9", "0.8.9.M11") < 0);
        assertTrue(comp.compare("1.0.0", "1.0.0.RC1") > 0);
        assertTrue(comp.compare("1.0.0", "1.0.0-RC1") > 0);
        assertTrue(comp.compare("1.0", "1.0.1") < 0);
        assertTrue(comp.compare("1.0.0", "1.0.0.1") < 0);
        assertEquals(0, comp.compare("1.0.0", "1.0.0.0"));
    }

    @Test
    void testIsDigitOnly() {
        assertTrue(JkVersion.of("12.3.21").isDigitsOnly());
        assertFalse(JkVersion.of("36.0.6.RC10").isDigitsOnly());
    }
}