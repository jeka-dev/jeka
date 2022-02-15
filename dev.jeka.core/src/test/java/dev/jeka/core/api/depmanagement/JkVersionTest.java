package dev.jeka.core.api.depmanagement;

import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.assertTrue;

public class JkVersionTest {

    @Test
    public void semanticVersionComparator() {
        Comparator<String> comp = JkVersion.VERSION_COMPARATOR;
        assertTrue(comp.compare("1", "2") < 0);
        assertTrue(comp.compare("1.0.1.RELEASE", "1.0.1.RC1") > 0);
        assertTrue(comp.compare("2toto4", "2toto2") > 0);
        assertTrue(comp.compare("0.8.9.RELEASE", "0.8.14.RELEASE") < 0);
        assertTrue(comp.compare("0.8.9.M9", "0.8.9.M11") < 0);
        assertTrue(comp.compare("1.0.0", "1.0.0.RC1") > 0);
        assertTrue(comp.compare("1.0", "1.0.1") < 0);
        assertTrue(comp.compare("1.0.0", "1.0.0.1") < 0);
        assertTrue(comp.compare("1.0.0", "1.0.0.0") == 0);
    }
}