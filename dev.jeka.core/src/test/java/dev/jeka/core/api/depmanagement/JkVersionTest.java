package dev.jeka.core.api.depmanagement;

import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.*;

public class JkVersionTest {

    @Test
    public void semanticVersionComparator() {
        Comparator<String> comp = JkVersion.SEMANTIC_COMARATOR;
        assertTrue(comp.compare("1", "2") < 0);
        assertTrue(comp.compare("aall", "2") > 0);
        assertTrue(comp.compare("1.0.1.RELEASE", "1.0.1.RC-1") > 0);
        assertTrue(comp.compare("2.toto", "2.6") > 0);
    }
}